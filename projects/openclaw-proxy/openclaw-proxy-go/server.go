package main

import (
	"bytes"
	stdcontext "context"
	"crypto/subtle"
	"encoding/json"
	"errors"
	"io"
	"log"
	"net/http"
	"time"

	"github.com/gin-gonic/gin"
)

func writeError(context *gin.Context, status int, code, message string) {
	context.JSON(status, gin.H{
		"error": gin.H{
			"code":    code,
			"message": message,
		},
	})
}

func validChatPayload(body []byte) bool {
	var payload struct {
		Messages []struct {
			Content *string `json:"content"`
		} `json:"messages"`
	}
	if err := json.Unmarshal(body, &payload); err != nil || len(payload.Messages) == 0 {
		return false
	}
	for _, message := range payload.Messages {
		if message.Content == nil {
			return false
		}
	}
	return true
}

func newServer(
	cfg config,
	client *http.Client,
	logger *log.Logger,
	limiter *fixedWindowLimiter,
) http.Handler {
	router := gin.New()
	router.Use(gin.Recovery())
	router.GET("/ping", func(context *gin.Context) {
		context.JSON(http.StatusOK, gin.H{"status": "ok"})
	})
	router.POST("/v1/chat/completions", func(context *gin.Context) {
		started := time.Now()
		defer func() {
			logger.Printf(
				"status=%d duration_ms=%d",
				context.Writer.Status(),
				time.Since(started).Milliseconds(),
			)
		}()

		supplied := context.GetHeader("X-MJGA-Token")
		if subtle.ConstantTimeCompare(
			[]byte(supplied),
			[]byte(cfg.ClientToken),
		) != 1 {
			writeError(
				context,
				http.StatusUnauthorized,
				"unauthorized",
				"client token is invalid",
			)
			return
		}
		if !limiter.allow() {
			writeError(
				context,
				http.StatusTooManyRequests,
				"rate_limited",
				"request rate limit exceeded",
			)
			return
		}

		body, err := io.ReadAll(io.LimitReader(
			context.Request.Body,
			cfg.MaxRequestBytes+1,
		))
		if err != nil {
			writeError(
				context,
				http.StatusBadRequest,
				"invalid_request",
				"request JSON is invalid",
			)
			return
		}
		if int64(len(body)) > cfg.MaxRequestBytes {
			writeError(
				context,
				http.StatusRequestEntityTooLarge,
				"request_too_large",
				"request body is too large",
			)
			return
		}
		if !validChatPayload(body) {
			writeError(
				context,
				http.StatusBadRequest,
				"invalid_request",
				"request JSON is invalid",
			)
			return
		}

		requestContext, cancel := stdcontext.WithTimeout(
			context.Request.Context(),
			time.Duration(cfg.UpstreamTimeoutSeconds)*time.Second,
		)
		defer cancel()
		request, err := http.NewRequestWithContext(
			requestContext,
			http.MethodPost,
			cfg.APIURL,
			bytes.NewReader(body),
		)
		if err != nil {
			writeError(
				context,
				http.StatusBadGateway,
				"upstream_unavailable",
				"upstream service is unavailable",
			)
			return
		}
		request.Header.Set("Content-Type", "application/json; charset=utf-8")
		request.Header.Set("Authorization", "Bearer "+cfg.APIKey)
		response, err := client.Do(request)
		if err != nil {
			if errors.Is(err, stdcontext.DeadlineExceeded) {
				writeError(
					context,
					http.StatusGatewayTimeout,
					"upstream_timeout",
					"upstream request timed out",
				)
				return
			}
			writeError(
				context,
				http.StatusBadGateway,
				"upstream_unavailable",
				"upstream service is unavailable",
			)
			return
		}
		defer response.Body.Close()
		responseBody, err := io.ReadAll(io.LimitReader(
			response.Body,
			cfg.MaxResponseBytes+1,
		))
		if err != nil {
			writeError(
				context,
				http.StatusBadGateway,
				"upstream_unavailable",
				"upstream service is unavailable",
			)
			return
		}
		if int64(len(responseBody)) > cfg.MaxResponseBytes {
			writeError(
				context,
				http.StatusBadGateway,
				"upstream_response_too_large",
				"upstream response is too large",
			)
			return
		}
		contentType := response.Header.Get("Content-Type")
		if contentType == "" {
			contentType = "application/json; charset=utf-8"
		}
		context.Data(response.StatusCode, contentType, responseBody)
	})
	return router
}

package main

import (
	"bytes"
	"io"
	"log"
	"net/http"
	"os"
	"strings"
)

var (
	openAIAPIKey = os.Getenv("OPENAI_API_KEY")
	port        = os.Getenv("PORT")
)

// openclaw-proxy - OpenAI 反向代理
// 解决 J2ME 客户端 TLS 1.0 兼容性问题
// J2ME -> HTTP -> openclaw-proxy -> HTTPS -> OpenAI

func main() {
	if openAIAPIKey == "" {
		log.Fatal("OPENAI_API_KEY environment variable is required")
	}
	if port == "" {
		port = "8080"
	}

	r := gin.Default()

	// 健康检查
	r.GET("/ping", func(c *gin.Context) {
		c.String(http.StatusOK, "pong")
	})

	// 聊天接口 - J2ME 客户端请求聊天
	r.POST("/v1/chat/completions", proxyHandler)

	log.Printf("openclaw-proxy starting on :%s", port)
	log.Fatal(r.Run(":" + port))
}

func proxyHandler(c *gin.Context) {
	// 读取 J2ME 客户端请求
	body, err := io.ReadAll(c.Request.Body)
	if err != nil {
		log.Printf("Failed to read request: %v", err)
		c.JSON(http.StatusBadRequest, gin.H{"error": "failed to read request"})
		return
	}

	// 创建转发请求到 OpenAI
	req, err := http.NewRequest("POST", "https://api.openai.com/v1/chat/completions", bytes.NewReader(body))
	if err != nil {
		log.Printf("Failed to create proxy request: %v", err)
		c.JSON(http.StatusBadGateway, gin.H{"error": "failed to create request"})
		return
	}

	// 设置头
	req.Header.Set("Content-Type", "application/json")
	req.Header.Set("Authorization", "Bearer "+openAIAPIKey)

	// 发送请求
	client := &http.Client{}
	resp, err := client.Do(req)
	if err != nil {
		log.Printf("Failed to call OpenAI API: %v", err)
		c.JSON(http.StatusBadGateway, gin.H{"error": "failed to call OpenAI"})
		return
	}
	defer resp.Body.Close()

	// 读取 OpenAI 响应
	respBody, err := io.ReadAll(resp.Body)
	if err != nil {
		log.Printf("Failed to read OpenAI response: %v", err)
		c.JSON(http.StatusBadGateway, gin.H{"error": "failed to read response"})
		return
	}

	// 设置响应头，原样返回给 J2ME
	c.Header("Content-Type", "application/json")
	c.Status(resp.StatusCode)
	c.Writer.Write(respBody)
}

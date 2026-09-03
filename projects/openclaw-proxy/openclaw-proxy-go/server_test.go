package main

import (
	"bytes"
	"context"
	"encoding/json"
	"errors"
	"io"
	"log"
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"
	"time"
)

func serverConfig() config {
	return config{
		APIKey:                 "upstream-secret",
		ClientToken:            "device-secret",
		APIURL:                 "https://upstream.test/v1/chat/completions",
		Port:                   8080,
		UpstreamTimeoutSeconds: 1,
		MaxRequestBytes:        256,
		MaxResponseBytes:       256,
		RateLimitPerMinute:     10,
	}
}

func testServer(cfg config, client *http.Client) http.Handler {
	return newServer(
		cfg,
		client,
		log.New(io.Discard, "", 0),
		newFixedWindowLimiter(cfg.RateLimitPerMinute, time.Now),
	)
}

func performRequest(handler http.Handler, method, path, token string, body []byte) *httptest.ResponseRecorder {
	request := httptest.NewRequest(method, path, bytes.NewReader(body))
	if token != "" {
		request.Header.Set("X-MJGA-Token", token)
	}
	response := httptest.NewRecorder()
	handler.ServeHTTP(response, request)
	return response
}

func assertJSONEquals(t *testing.T, expected string, actual []byte) {
	t.Helper()
	var expectedValue any
	var actualValue any
	if err := json.Unmarshal([]byte(expected), &expectedValue); err != nil {
		t.Fatalf("invalid expected JSON: %v", err)
	}
	if err := json.Unmarshal(actual, &actualValue); err != nil {
		t.Fatalf("invalid actual JSON %q: %v", actual, err)
	}
	if !valuesEqual(expectedValue, actualValue) {
		t.Fatalf("JSON = %s, want %s", actual, expected)
	}
}

func valuesEqual(left, right any) bool {
	leftJSON, _ := json.Marshal(left)
	rightJSON, _ := json.Marshal(right)
	return bytes.Equal(leftJSON, rightJSON)
}

func TestPing(t *testing.T) {
	handler := testServer(serverConfig(), &http.Client{})
	response := performRequest(handler, http.MethodGet, "/ping", "", nil)
	if response.Code != http.StatusOK {
		t.Fatalf("status = %d, want %d", response.Code, http.StatusOK)
	}
	assertJSONEquals(t, `{"status":"ok"}`, response.Body.Bytes())
}

func TestAuthentication(t *testing.T) {
	handler := testServer(serverConfig(), &http.Client{})
	body := []byte(`{"messages":[{"role":"user","content":"hello"}]}`)
	want := `{"error":{"code":"unauthorized","message":"client token is invalid"}}`
	for _, test := range []struct {
		name  string
		token string
	}{
		{name: "missing token"},
		{name: "wrong token", token: "wrong"},
	} {
		t.Run(test.name, func(t *testing.T) {
			response := performRequest(
				handler,
				http.MethodPost,
				"/v1/chat/completions",
				test.token,
				body,
			)
			if response.Code != http.StatusUnauthorized {
				t.Fatalf("status = %d, want %d", response.Code, http.StatusUnauthorized)
			}
			assertJSONEquals(t, want, response.Body.Bytes())
		})
	}
}

func TestRequestValidation(t *testing.T) {
	handler := testServer(serverConfig(), &http.Client{})
	for _, body := range []string{
		`{`,
		`{"model":"test"}`,
		`{"messages":[]}`,
		`{"messages":[{"role":"user","content":1}]}`,
	} {
		response := performRequest(
			handler,
			http.MethodPost,
			"/v1/chat/completions",
			"device-secret",
			[]byte(body),
		)
		if response.Code != http.StatusBadRequest {
			t.Fatalf("body %q status = %d, want %d", body, response.Code, http.StatusBadRequest)
		}
		assertJSONEquals(
			t,
			`{"error":{"code":"invalid_request","message":"request JSON is invalid"}}`,
			response.Body.Bytes(),
		)
	}
}

func TestRequestBodyLimit(t *testing.T) {
	cfg := serverConfig()
	cfg.MaxRequestBytes = 8
	handler := testServer(cfg, &http.Client{})
	response := performRequest(
		handler,
		http.MethodPost,
		"/v1/chat/completions",
		"device-secret",
		[]byte("123456789"),
	)
	if response.Code != http.StatusRequestEntityTooLarge {
		t.Fatalf(
			"status = %d, want %d",
			response.Code,
			http.StatusRequestEntityTooLarge,
		)
	}
	assertJSONEquals(
		t,
		`{"error":{"code":"request_too_large","message":"request body is too large"}}`,
		response.Body.Bytes(),
	)
}

func TestRateLimit(t *testing.T) {
	upstream := httptest.NewServer(http.HandlerFunc(func(writer http.ResponseWriter, request *http.Request) {
		writer.Header().Set("Content-Type", "application/json")
		writer.WriteHeader(http.StatusOK)
		_, _ = writer.Write([]byte(`{"choices":[]}`))
	}))
	defer upstream.Close()

	cfg := serverConfig()
	cfg.APIURL = upstream.URL + "/v1/chat/completions"
	cfg.RateLimitPerMinute = 1
	handler := testServer(cfg, upstream.Client())
	body := []byte(`{"messages":[{"role":"user","content":"hello"}]}`)
	first := performRequest(
		handler,
		http.MethodPost,
		"/v1/chat/completions",
		"device-secret",
		body,
	)
	if first.Code != http.StatusOK {
		t.Fatalf("first status = %d, want %d", first.Code, http.StatusOK)
	}
	limited := performRequest(
		handler,
		http.MethodPost,
		"/v1/chat/completions",
		"device-secret",
		body,
	)
	if limited.Code != http.StatusTooManyRequests {
		t.Fatalf(
			"limited status = %d, want %d",
			limited.Code,
			http.StatusTooManyRequests,
		)
	}
	assertJSONEquals(
		t,
		`{"error":{"code":"rate_limited","message":"request rate limit exceeded"}}`,
		limited.Body.Bytes(),
	)
}

type roundTripFunc func(*http.Request) (*http.Response, error)

func (function roundTripFunc) RoundTrip(request *http.Request) (*http.Response, error) {
	return function(request)
}

func TestForwardsRequestAndReturnsUpstreamResponse(t *testing.T) {
	observed := make(chan *http.Request, 1)
	observedBody := make(chan []byte, 1)
	upstream := httptest.NewServer(http.HandlerFunc(func(writer http.ResponseWriter, request *http.Request) {
		body, _ := io.ReadAll(request.Body)
		observed <- request.Clone(request.Context())
		observedBody <- body
		writer.Header().Set("Content-Type", "application/json")
		writer.WriteHeader(http.StatusCreated)
		_, _ = writer.Write([]byte(`{"choices":[]}`))
	}))
	defer upstream.Close()

	cfg := serverConfig()
	cfg.APIURL = upstream.URL + "/v1/chat/completions"
	handler := testServer(cfg, upstream.Client())
	body := []byte(`{"messages":[{"role":"user","content":"hello"}]}`)
	response := performRequest(
		handler,
		http.MethodPost,
		"/v1/chat/completions",
		"device-secret",
		body,
	)
	if response.Code != http.StatusCreated {
		t.Fatalf("status = %d, want %d", response.Code, http.StatusCreated)
	}
	if response.Body.String() != `{"choices":[]}` {
		t.Fatalf("body = %q", response.Body.String())
	}
	request := <-observed
	if request.Method != http.MethodPost || request.URL.Path != "/v1/chat/completions" {
		t.Fatalf("upstream request = %s %s", request.Method, request.URL.Path)
	}
	if request.Host != strings.TrimPrefix(upstream.URL, "http://") {
		t.Fatalf("upstream host = %q, want URL host from %q", request.Host, upstream.URL)
	}
	if request.Header.Get("Authorization") != "Bearer upstream-secret" {
		t.Fatalf("Authorization = %q", request.Header.Get("Authorization"))
	}
	if request.Header.Get("Content-Type") != "application/json; charset=utf-8" {
		t.Fatalf("Content-Type = %q", request.Header.Get("Content-Type"))
	}
	if !bytes.Equal(<-observedBody, body) {
		t.Fatal("upstream body did not match client request")
	}
}

func TestPreservesUpstreamNonSuccessResponse(t *testing.T) {
	upstream := httptest.NewServer(http.HandlerFunc(func(writer http.ResponseWriter, request *http.Request) {
		writer.Header().Set("Content-Type", "application/json")
		writer.WriteHeader(http.StatusTeapot)
		_, _ = writer.Write([]byte(`{"error":{"code":"teapot"}}`))
	}))
	defer upstream.Close()

	cfg := serverConfig()
	cfg.APIURL = upstream.URL
	response := performRequest(
		testServer(cfg, upstream.Client()),
		http.MethodPost,
		"/v1/chat/completions",
		"device-secret",
		[]byte(`{"messages":[{"content":"hello"}]}`),
	)
	if response.Code != http.StatusTeapot {
		t.Fatalf("status = %d, want %d", response.Code, http.StatusTeapot)
	}
	if response.Body.String() != `{"error":{"code":"teapot"}}` {
		t.Fatalf("body = %q", response.Body.String())
	}
}

func TestMapsUpstreamTimeout(t *testing.T) {
	client := &http.Client{Transport: roundTripFunc(func(request *http.Request) (*http.Response, error) {
		return nil, context.DeadlineExceeded
	})}
	response := performRequest(
		testServer(serverConfig(), client),
		http.MethodPost,
		"/v1/chat/completions",
		"device-secret",
		[]byte(`{"messages":[{"content":"hello"}]}`),
	)
	if response.Code != http.StatusGatewayTimeout {
		t.Fatalf("status = %d, want %d", response.Code, http.StatusGatewayTimeout)
	}
	assertJSONEquals(
		t,
		`{"error":{"code":"upstream_timeout","message":"upstream request timed out"}}`,
		response.Body.Bytes(),
	)
}

func TestMapsUpstreamConnectionFailure(t *testing.T) {
	client := &http.Client{Transport: roundTripFunc(func(request *http.Request) (*http.Response, error) {
		return nil, errors.New("offline upstream")
	})}
	response := performRequest(
		testServer(serverConfig(), client),
		http.MethodPost,
		"/v1/chat/completions",
		"device-secret",
		[]byte(`{"messages":[{"content":"hello"}]}`),
	)
	if response.Code != http.StatusBadGateway {
		t.Fatalf("status = %d, want %d", response.Code, http.StatusBadGateway)
	}
	assertJSONEquals(
		t,
		`{"error":{"code":"upstream_unavailable","message":"upstream service is unavailable"}}`,
		response.Body.Bytes(),
	)
}

func TestRejectsUpstreamResponseOverLimit(t *testing.T) {
	cfg := serverConfig()
	cfg.MaxResponseBytes = 8
	client := &http.Client{Transport: roundTripFunc(func(request *http.Request) (*http.Response, error) {
		return &http.Response{
			StatusCode: http.StatusOK,
			Header:     make(http.Header),
			Body:       io.NopCloser(strings.NewReader("123456789")),
			Request:    request,
		}, nil
	})}
	response := performRequest(
		testServer(cfg, client),
		http.MethodPost,
		"/v1/chat/completions",
		"device-secret",
		[]byte(`{"messages":[{"content":"hello"}]}`),
	)
	if response.Code != http.StatusBadGateway {
		t.Fatalf("status = %d, want %d", response.Code, http.StatusBadGateway)
	}
	assertJSONEquals(
		t,
		`{"error":{"code":"upstream_response_too_large","message":"upstream response is too large"}}`,
		response.Body.Bytes(),
	)
}

func TestLogsMetadataWithoutSecretsOrContent(t *testing.T) {
	var output bytes.Buffer
	logger := log.New(&output, "", 0)
	client := &http.Client{Transport: roundTripFunc(func(request *http.Request) (*http.Response, error) {
		return &http.Response{
			StatusCode: http.StatusOK,
			Header:     http.Header{"Content-Type": []string{"application/json"}},
			Body:       io.NopCloser(strings.NewReader(`{"choices":[]}`)),
			Request:    request,
		}, nil
	})}
	cfg := serverConfig()
	handler := newServer(
		cfg,
		client,
		logger,
		newFixedWindowLimiter(cfg.RateLimitPerMinute, time.Now),
	)
	response := performRequest(
		handler,
		http.MethodPost,
		"/v1/chat/completions",
		"device-secret",
		[]byte(`{"messages":[{"content":"PRIVATE-PROMPT-42"}]}`),
	)
	if response.Code != http.StatusOK {
		t.Fatalf("status = %d, want %d", response.Code, http.StatusOK)
	}
	logText := output.String()
	if !strings.Contains(logText, "status=200") || !strings.Contains(logText, "duration_ms=") {
		t.Fatalf("safe request metadata missing from log: %q", logText)
	}
	for _, secret := range []string{"upstream-secret", "device-secret", "PRIVATE-PROMPT-42"} {
		if strings.Contains(logText, secret) {
			t.Fatalf("log leaked %q: %q", secret, logText)
		}
	}
}

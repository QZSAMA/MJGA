package main

import (
	"fmt"
	"strconv"
	"strings"
)

const defaultAPIURL = "https://ark.cn-beijing.volces.com/api/coding/v3/chat/completions"

type lookupFunc func(string) (string, bool)

type config struct {
	APIKey                 string
	ClientToken            string
	APIURL                 string
	Port                   int
	UpstreamTimeoutSeconds int
	MaxRequestBytes        int64
	MaxResponseBytes       int64
	RateLimitPerMinute     int
}

func positiveInt(lookup lookupFunc, name string, fallback int) (int, error) {
	raw, ok := lookup(name)
	if !ok || strings.TrimSpace(raw) == "" {
		return fallback, nil
	}
	value, err := strconv.Atoi(raw)
	if err != nil || value <= 0 {
		return 0, fmt.Errorf("%s must be a positive integer", name)
	}
	return value, nil
}

func loadConfig(lookup lookupFunc) (config, error) {
	apiKey, _ := lookup("API_KEY")
	clientToken, _ := lookup("CLIENT_TOKEN")
	if strings.TrimSpace(apiKey) == "" {
		return config{}, fmt.Errorf("API_KEY is required")
	}
	if strings.TrimSpace(clientToken) == "" {
		return config{}, fmt.Errorf("CLIENT_TOKEN is required")
	}

	apiURL, ok := lookup("API_URL")
	if !ok || strings.TrimSpace(apiURL) == "" {
		apiURL = defaultAPIURL
	}
	port, err := positiveInt(lookup, "PORT", 8080)
	if err != nil {
		return config{}, err
	}
	timeout, err := positiveInt(lookup, "UPSTREAM_TIMEOUT_SECONDS", 60)
	if err != nil {
		return config{}, err
	}
	maxRequest, err := positiveInt(lookup, "MAX_REQUEST_BYTES", 8192)
	if err != nil {
		return config{}, err
	}
	maxResponse, err := positiveInt(lookup, "MAX_RESPONSE_BYTES", 32768)
	if err != nil {
		return config{}, err
	}
	rate, err := positiveInt(lookup, "RATE_LIMIT_PER_MINUTE", 10)
	if err != nil {
		return config{}, err
	}

	return config{
		APIKey:                 strings.TrimSpace(apiKey),
		ClientToken:            strings.TrimSpace(clientToken),
		APIURL:                 strings.TrimSpace(apiURL),
		Port:                   port,
		UpstreamTimeoutSeconds: timeout,
		MaxRequestBytes:        int64(maxRequest),
		MaxResponseBytes:       int64(maxResponse),
		RateLimitPerMinute:     rate,
	}, nil
}

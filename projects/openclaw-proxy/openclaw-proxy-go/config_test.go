package main

import (
	"strings"
	"testing"
	"time"
)

func mapLookup(values map[string]string) lookupFunc {
	return func(name string) (string, bool) {
		value, ok := values[name]
		return value, ok
	}
}

func validConfigValues() map[string]string {
	return map[string]string{
		"API_KEY":      "upstream-secret",
		"CLIENT_TOKEN": "device-secret",
	}
}

func TestLoadConfigRequiresSecrets(t *testing.T) {
	tests := []struct {
		name    string
		values  map[string]string
		message string
	}{
		{
			name:    "missing API key",
			values:  map[string]string{"CLIENT_TOKEN": "device-secret"},
			message: "API_KEY is required",
		},
		{
			name:    "missing client token",
			values:  map[string]string{"API_KEY": "upstream-secret"},
			message: "CLIENT_TOKEN is required",
		},
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			_, err := loadConfig(mapLookup(test.values))
			if err == nil || !strings.Contains(err.Error(), test.message) {
				t.Fatalf("loadConfig() error = %v, want %q", err, test.message)
			}
		})
	}
}

func TestLoadConfigRejectsInvalidPositiveIntegers(t *testing.T) {
	settings := []string{
		"PORT",
		"UPSTREAM_TIMEOUT_SECONDS",
		"MAX_REQUEST_BYTES",
		"MAX_RESPONSE_BYTES",
		"RATE_LIMIT_PER_MINUTE",
	}
	for _, name := range settings {
		for _, value := range []string{"0", "not-a-number"} {
			t.Run(name+"="+value, func(t *testing.T) {
				values := validConfigValues()
				values[name] = value
				_, err := loadConfig(mapLookup(values))
				if err == nil || !strings.Contains(err.Error(), name) {
					t.Fatalf("loadConfig() error = %v, want error naming %s", err, name)
				}
			})
		}
	}
}

func TestLoadConfigDefaults(t *testing.T) {
	values := validConfigValues()
	values["API_KEY"] = "  upstream-secret  "
	values["CLIENT_TOKEN"] = "  device-secret  "
	cfg, err := loadConfig(mapLookup(values))
	if err != nil {
		t.Fatalf("loadConfig() error = %v", err)
	}
	if cfg.APIKey != "upstream-secret" || cfg.ClientToken != "device-secret" {
		t.Fatalf("secrets were not trimmed: %#v", cfg)
	}
	if cfg.APIURL != defaultAPIURL {
		t.Fatalf("APIURL = %q, want %q", cfg.APIURL, defaultAPIURL)
	}
	if cfg.Port != 8080 || cfg.UpstreamTimeoutSeconds != 60 {
		t.Fatalf("unexpected network defaults: %#v", cfg)
	}
	if cfg.MaxRequestBytes != 8192 || cfg.MaxResponseBytes != 32768 {
		t.Fatalf("unexpected byte limits: %#v", cfg)
	}
	if cfg.RateLimitPerMinute != 10 {
		t.Fatalf("RateLimitPerMinute = %d, want 10", cfg.RateLimitPerMinute)
	}
}

type fakeClock struct {
	current time.Time
}

func (clock *fakeClock) Now() time.Time {
	return clock.current
}

func TestFixedWindowLimiter(t *testing.T) {
	clock := &fakeClock{current: time.Unix(1_000, 0)}
	limiter := newFixedWindowLimiter(2, clock.Now)
	if !limiter.allow() || !limiter.allow() {
		t.Fatal("first two requests should be allowed")
	}
	if limiter.allow() {
		t.Fatal("third request in the same window should be rejected")
	}
	clock.current = clock.current.Add(time.Minute)
	if !limiter.allow() {
		t.Fatal("request after the fixed window resets should be allowed")
	}
}

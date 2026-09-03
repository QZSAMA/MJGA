package main

import (
	"fmt"
	"log"
	"net/http"
	"os"
	"time"
)

func main() {
	cfg, err := loadConfig(os.LookupEnv)
	if err != nil {
		log.Fatal(err)
	}

	logger := log.New(os.Stdout, "", log.LstdFlags)
	client := &http.Client{}
	handler := newServer(
		cfg,
		client,
		logger,
		newFixedWindowLimiter(cfg.RateLimitPerMinute, time.Now),
	)
	server := &http.Server{
		Addr:              fmt.Sprintf(":%d", cfg.Port),
		Handler:           handler,
		ReadHeaderTimeout: 5 * time.Second,
	}
	if err := server.ListenAndServe(); err != nil && err != http.ErrServerClosed {
		logger.Fatal(err)
	}
}

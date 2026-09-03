package main

import (
	"sync"
	"time"
)

type fixedWindowLimiter struct {
	mu          sync.Mutex
	limit       int
	count       int
	windowStart time.Time
	now         func() time.Time
}

func newFixedWindowLimiter(limit int, now func() time.Time) *fixedWindowLimiter {
	return &fixedWindowLimiter{
		limit:       limit,
		now:         now,
		windowStart: now(),
	}
}

func (limiter *fixedWindowLimiter) allow() bool {
	limiter.mu.Lock()
	defer limiter.mu.Unlock()

	current := limiter.now()
	if current.Sub(limiter.windowStart) >= time.Minute {
		limiter.windowStart = current
		limiter.count = 0
	}
	if limiter.count >= limiter.limit {
		return false
	}
	limiter.count++
	return true
}

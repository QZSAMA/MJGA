"""Small process-local rate limiter for the MJGA proxy."""

import threading
import time
from collections.abc import Callable


class FixedWindowLimiter:
    """Allow at most ``limit`` requests during each 60-second window."""

    def __init__(
        self,
        limit: int,
        clock: Callable[[], float] = time.monotonic,
    ) -> None:
        self._limit = limit
        self._clock = clock
        self._window_started = clock()
        self._count = 0
        self._lock = threading.Lock()

    def allow(self) -> bool:
        """Consume one allowance, returning false when the window is full."""
        with self._lock:
            now = self._clock()
            if now - self._window_started >= 60:
                self._window_started = now
                self._count = 0
            if self._count >= self._limit:
                return False
            self._count += 1
            return True

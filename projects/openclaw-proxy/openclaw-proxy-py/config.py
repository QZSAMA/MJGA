"""Validated runtime configuration for the MJGA proxy."""

from dataclasses import dataclass
import os
from typing import Mapping


DEFAULT_API_URL = "https://ark.cn-beijing.volces.com/api/coding/v3/chat/completions"


def _positive_int(values: Mapping[str, str], name: str, default: int) -> int:
    """Read a strictly positive integer setting."""
    raw = values.get(name, str(default))
    try:
        value = int(raw)
    except ValueError as exc:
        raise ValueError(f"{name} must be a positive integer") from exc
    if value <= 0:
        raise ValueError(f"{name} must be a positive integer")
    return value


@dataclass(frozen=True)
class Settings:
    """Immutable, validated proxy settings."""

    api_key: str
    client_token: str
    api_url: str
    port: int
    upstream_timeout_seconds: int
    max_request_bytes: int
    max_response_bytes: int
    rate_limit_per_minute: int

    @classmethod
    def from_mapping(cls, values: Mapping[str, str]) -> "Settings":
        """Build settings from an environment-like mapping."""
        api_key = values.get("API_KEY", "").strip()
        client_token = values.get("CLIENT_TOKEN", "").strip()
        if not api_key:
            raise ValueError("API_KEY is required")
        if not client_token:
            raise ValueError("CLIENT_TOKEN is required")
        return cls(
            api_key=api_key,
            client_token=client_token,
            api_url=values.get("API_URL", DEFAULT_API_URL).strip(),
            port=_positive_int(values, "PORT", 8080),
            upstream_timeout_seconds=_positive_int(
                values, "UPSTREAM_TIMEOUT_SECONDS", 60
            ),
            max_request_bytes=_positive_int(values, "MAX_REQUEST_BYTES", 8192),
            max_response_bytes=_positive_int(values, "MAX_RESPONSE_BYTES", 32768),
            rate_limit_per_minute=_positive_int(
                values, "RATE_LIMIT_PER_MINUTE", 10
            ),
        )

    @classmethod
    def from_env(cls) -> "Settings":
        """Build settings from process environment variables."""
        return cls.from_mapping(os.environ)

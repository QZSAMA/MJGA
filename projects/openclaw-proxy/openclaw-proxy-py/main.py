#!/usr/bin/env python3
"""Secure HTTP entry point for the MJGA text proxy."""

import json
import logging
import secrets
import time

import httpx
from fastapi import FastAPI, Request
from starlette.responses import JSONResponse, Response

from config import Settings
from limiter import FixedWindowLimiter


LOGGER = logging.getLogger("mjga.proxy")


class RequestTooLarge(Exception):
    """Raised when a client request exceeds its configured byte limit."""


class InvalidRequest(Exception):
    """Raised when a request is not a supported chat-completion payload."""


class UpstreamResponseTooLarge(Exception):
    """Raised when the upstream response exceeds its configured byte limit."""


def error_response(status: int, code: str, message: str) -> JSONResponse:
    """Return the shared proxy error envelope."""
    return JSONResponse(
        status_code=status,
        content={"error": {"code": code, "message": message}},
    )


async def read_limited_body(request: Request, limit: int) -> bytes:
    """Read at most ``limit`` bytes without buffering an unbounded request."""
    body = bytearray()
    async for chunk in request.stream():
        body.extend(chunk)
        if len(body) > limit:
            raise RequestTooLarge()
    return bytes(body)


def validate_payload(body: bytes) -> None:
    """Validate the minimal OpenAI-compatible request shape used by MJGA."""
    try:
        data = json.loads(body.decode("utf-8"))
    except (UnicodeDecodeError, json.JSONDecodeError) as exc:
        raise InvalidRequest() from exc

    messages = data.get("messages") if isinstance(data, dict) else None
    if not isinstance(messages, list) or not messages:
        raise InvalidRequest()
    for message in messages:
        if not isinstance(message, dict) or not isinstance(
            message.get("content"), str
        ):
            raise InvalidRequest()


async def read_limited_response(response: httpx.Response, limit: int) -> bytes:
    """Buffer at most one byte beyond the configured upstream response limit."""
    body = bytearray()
    async for chunk in response.aiter_bytes():
        remaining = limit + 1 - len(body)
        body.extend(chunk[:remaining])
        if len(body) > limit:
            raise UpstreamResponseTooLarge()
    return bytes(body)


def create_app(settings: Settings | None = None, transport=None) -> FastAPI:
    """Create a proxy app with injectable settings and upstream transport."""
    resolved = settings or Settings.from_env()
    app = FastAPI()
    app.state.settings = resolved
    app.state.transport = transport
    app.state.limiter = FixedWindowLimiter(resolved.rate_limit_per_minute)

    @app.middleware("http")
    async def log_request(request: Request, call_next):
        started = time.monotonic()
        response = await call_next(request)
        elapsed_ms = int((time.monotonic() - started) * 1000)
        LOGGER.info(
            "%s %s status=%d elapsed_ms=%d",
            request.method,
            request.url.path,
            response.status_code,
            elapsed_ms,
        )
        return response

    @app.get("/ping")
    async def ping():
        return {"status": "ok"}

    @app.post("/v1/chat/completions")
    async def proxy(request: Request):
        supplied = request.headers.get("X-MJGA-Token", "")
        if not secrets.compare_digest(supplied, resolved.client_token):
            return error_response(401, "unauthorized", "client token is invalid")
        if not app.state.limiter.allow():
            return error_response(429, "rate_limited", "request rate limit exceeded")

        try:
            body = await read_limited_body(request, resolved.max_request_bytes)
        except RequestTooLarge:
            return error_response(413, "request_too_large", "request body is too large")
        try:
            validate_payload(body)
        except InvalidRequest:
            return error_response(400, "invalid_request", "request JSON is invalid")

        headers = {
            "Content-Type": "application/json; charset=utf-8",
            "Authorization": f"Bearer {resolved.api_key}",
        }
        try:
            async with httpx.AsyncClient(
                transport=transport,
                timeout=resolved.upstream_timeout_seconds,
            ) as client:
                async with client.stream(
                    "POST",
                    resolved.api_url,
                    content=body,
                    headers=headers,
                ) as upstream_response:
                    upstream_body = await read_limited_response(
                        upstream_response,
                        resolved.max_response_bytes,
                    )
                    upstream_status = upstream_response.status_code
                    content_type = upstream_response.headers.get(
                        "content-type", "application/json; charset=utf-8"
                    )
        except UpstreamResponseTooLarge:
            return error_response(
                502,
                "upstream_response_too_large",
                "upstream response is too large",
            )
        except httpx.TimeoutException:
            return error_response(504, "upstream_timeout", "upstream request timed out")
        except httpx.TransportError:
            return error_response(
                502,
                "upstream_unavailable",
                "upstream service is unavailable",
            )
        return Response(
            content=upstream_body,
            status_code=upstream_status,
            headers={"Content-Type": content_type},
        )

    return app


if __name__ == "__main__":
    import uvicorn

    runtime_settings = Settings.from_env()
    uvicorn.run(
        create_app(runtime_settings),
        host="0.0.0.0",
        port=runtime_settings.port,
    )

import logging

import httpx
from fastapi.testclient import TestClient

from config import Settings
from main import create_app


def settings(**overrides):
    values = dict(
        api_key="upstream-secret",
        client_token="device-secret",
        api_url="https://upstream.test/v1/chat/completions",
        port=8080,
        upstream_timeout_seconds=1,
        max_request_bytes=256,
        max_response_bytes=256,
        rate_limit_per_minute=2,
    )
    values.update(overrides)
    return Settings(**values)


def upstream(handler):
    return httpx.MockTransport(handler)


def test_ping_does_not_require_token():
    app = create_app(settings(), upstream(lambda request: httpx.Response(200)))
    response = TestClient(app).get("/ping")
    assert response.status_code == 200
    assert response.json() == {"status": "ok"}


def test_chat_rejects_missing_or_wrong_token():
    app = create_app(settings(), upstream(lambda request: httpx.Response(200)))
    client = TestClient(app)
    payload = {"model": "test", "messages": [{"role": "user", "content": "hello"}]}
    missing = client.post("/v1/chat/completions", json=payload)
    wrong = client.post(
        "/v1/chat/completions",
        json=payload,
        headers={"X-MJGA-Token": "wrong"},
    )
    assert missing.status_code == 401
    assert wrong.status_code == 401
    assert missing.json()["error"]["code"] == "unauthorized"


def auth_headers():
    return {"X-MJGA-Token": "device-secret"}


def test_rejects_invalid_json_and_missing_messages():
    app = create_app(settings(), upstream(lambda request: httpx.Response(200)))
    client = TestClient(app)
    invalid = client.post(
        "/v1/chat/completions", content=b"{", headers=auth_headers()
    )
    missing = client.post(
        "/v1/chat/completions", json={"model": "test"}, headers=auth_headers()
    )
    assert invalid.status_code == 400
    assert missing.status_code == 400
    assert invalid.json()["error"]["code"] == "invalid_request"


def test_rejects_request_over_limit():
    app = create_app(
        settings(max_request_bytes=8),
        upstream(lambda request: httpx.Response(200)),
    )
    response = TestClient(app).post(
        "/v1/chat/completions",
        content=b'{"messages":[{"role":"user","content":"too large"}]}',
        headers=auth_headers(),
    )
    assert response.status_code == 413
    assert response.json()["error"]["code"] == "request_too_large"


def test_enforces_fixed_window_limit():
    app = create_app(
        settings(rate_limit_per_minute=1),
        upstream(lambda request: httpx.Response(200, json={})),
    )
    client = TestClient(app)
    payload = {"model": "test", "messages": [{"role": "user", "content": "hello"}]}
    assert (
        client.post(
            "/v1/chat/completions", json=payload, headers=auth_headers()
        ).status_code
        == 200
    )
    limited = client.post(
        "/v1/chat/completions", json=payload, headers=auth_headers()
    )
    assert limited.status_code == 429
    assert limited.json()["error"]["code"] == "rate_limited"


def test_forwards_to_configured_upstream_with_bearer_token():
    observed = {}

    def handler(request):
        observed["url"] = str(request.url)
        observed["authorization"] = request.headers.get("Authorization")
        observed["content_type"] = request.headers.get("Content-Type")
        observed["body"] = request.content
        return httpx.Response(
            200,
            content=b'{"choices":[]}',
            headers={"Content-Type": "application/json"},
        )

    app = create_app(settings(), upstream(handler))
    payload = {
        "model": "test",
        "messages": [{"role": "user", "content": "hello"}],
    }
    response = TestClient(app).post(
        "/v1/chat/completions", json=payload, headers=auth_headers()
    )

    assert response.status_code == 200
    assert response.content == b'{"choices":[]}'
    assert observed["url"] == "https://upstream.test/v1/chat/completions"
    assert observed["authorization"] == "Bearer upstream-secret"
    assert observed["content_type"] == "application/json; charset=utf-8"
    assert observed["body"] == response.request.content


def test_preserves_upstream_non_success_status_and_body():
    app = create_app(
        settings(),
        upstream(
            lambda request: httpx.Response(
                429,
                content=b'{"error":{"code":"upstream_limited"}}',
                headers={"Content-Type": "application/json"},
            )
        ),
    )
    payload = {"messages": [{"role": "user", "content": "hello"}]}
    response = TestClient(app).post(
        "/v1/chat/completions", json=payload, headers=auth_headers()
    )
    assert response.status_code == 429
    assert response.content == b'{"error":{"code":"upstream_limited"}}'


def test_maps_upstream_timeout():
    def handler(request):
        raise httpx.ReadTimeout("slow upstream", request=request)

    app = create_app(settings(), upstream(handler))
    payload = {"messages": [{"role": "user", "content": "hello"}]}
    response = TestClient(app).post(
        "/v1/chat/completions", json=payload, headers=auth_headers()
    )
    assert response.status_code == 504
    assert response.json()["error"]["code"] == "upstream_timeout"


def test_maps_upstream_connection_failure():
    def handler(request):
        raise httpx.ConnectError("offline upstream", request=request)

    app = create_app(settings(), upstream(handler))
    payload = {"messages": [{"role": "user", "content": "hello"}]}
    response = TestClient(app).post(
        "/v1/chat/completions", json=payload, headers=auth_headers()
    )
    assert response.status_code == 502
    assert response.json()["error"]["code"] == "upstream_unavailable"


def test_rejects_upstream_response_over_limit():
    app = create_app(
        settings(max_response_bytes=8),
        upstream(lambda request: httpx.Response(200, content=b"123456789")),
    )
    payload = {"messages": [{"role": "user", "content": "hello"}]}
    response = TestClient(app).post(
        "/v1/chat/completions", json=payload, headers=auth_headers()
    )
    assert response.status_code == 502
    assert response.json()["error"]["code"] == "upstream_response_too_large"


def test_logs_request_metadata_without_secrets_or_content(caplog):
    caplog.set_level(logging.INFO, logger="mjga.proxy")
    app = create_app(
        settings(),
        upstream(lambda request: httpx.Response(200, json={"choices": []})),
    )
    payload = {
        "messages": [{"role": "user", "content": "PRIVATE-PROMPT-42"}]
    }
    response = TestClient(app).post(
        "/v1/chat/completions", json=payload, headers=auth_headers()
    )
    assert response.status_code == 200
    assert caplog.records
    log_text = caplog.text
    assert "POST" in log_text
    assert "/v1/chat/completions" in log_text
    assert "upstream-secret" not in log_text
    assert "device-secret" not in log_text
    assert "PRIVATE-PROMPT-42" not in log_text

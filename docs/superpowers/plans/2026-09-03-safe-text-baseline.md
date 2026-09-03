# MJGA Safe Text Baseline Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Deliver a secure, bounded, reproducible J2ME-to-LLM text path with matching Go/Python proxy behavior and automated regression tests.

**Architecture:** Preserve the existing OpenAI-compatible `/v1/chat/completions` contract. Both proxies load validated configuration, authenticate `X-MJGA-Token`, apply a process-local fixed-window limit, validate bounded JSON, call a fixed HTTPS upstream with bounded time and response size, and emit a shared error envelope. The J2ME client receives build-injected endpoint/token/model properties, sends and decodes UTF-8, uses a bounded JSON scanner, and caps in-memory history.

**Tech Stack:** Java ME MIDP 2.0/CLDC 1.1, Java/Ant, Go 1.21/Gin, Python 3.10+/FastAPI/HTTPX/pytest, Docker, GitHub Actions.

## File map

### Create

- `projects/openclaw-proxy/openclaw-proxy-py/config.py` — validated Python proxy configuration.
- `projects/openclaw-proxy/openclaw-proxy-py/limiter.py` — process-local fixed-window limiter.
- `projects/openclaw-proxy/openclaw-proxy-py/tests/test_config.py` — Python configuration tests.
- `projects/openclaw-proxy/openclaw-proxy-py/tests/test_proxy.py` — Python HTTP contract tests.
- `projects/openclaw-proxy/openclaw-proxy-go/config.go` — validated Go proxy configuration.
- `projects/openclaw-proxy/openclaw-proxy-go/limiter.go` — Go fixed-window limiter.
- `projects/openclaw-proxy/openclaw-proxy-go/server.go` — testable Go HTTP handler.
- `projects/openclaw-proxy/openclaw-proxy-go/config_test.go` — Go configuration tests.
- `projects/openclaw-proxy/openclaw-proxy-go/server_test.go` — Go HTTP contract tests.
- `apps/mjga-j2me/src/com/mjga/network/HttpResponse.java` — bounded HTTP result value.
- `apps/mjga-j2me/src/com/mjga/network/ResponseReader.java` — bounded UTF-8 response reader.
- `apps/mjga-j2me/src/com/mjga/ui/ChatHistory.java` — capped in-memory Q&A history.
- `apps/mjga-j2me/test/com/mjga/util/JsonParserTest.java` — JSON request/response tests.
- `apps/mjga-j2me/test/com/mjga/network/ResponseReaderTest.java` — UTF-8 and size-limit tests.
- `apps/mjga-j2me/test/com/mjga/ui/ChatHistoryTest.java` — history cap tests.
- `apps/mjga-j2me/config.properties.example` — non-secret build configuration template.
- `apps/mjga-j2me/run-emulator.ps1` — Windows emulator launcher.
- `.github/workflows/baseline.yml` — Java, Python, Go, Docker, and secret-pattern checks.

### Modify

- `projects/openclaw-proxy/openclaw-proxy-py/main.py` — app factory and secure proxy pipeline.
- `projects/openclaw-proxy/openclaw-proxy-py/pyproject.toml` — test dependencies.
- `projects/openclaw-proxy/openclaw-proxy-py/uv.lock` — locked test dependencies.
- `projects/openclaw-proxy/openclaw-proxy-go/main.go` — configuration and server bootstrap only.
- `projects/openclaw-proxy/openclaw-proxy-go/go.mod` and `go.sum` — reproducible Go dependencies.
- `projects/openclaw-proxy/openclaw-proxy-go/Dockerfile` — reproducible build.
- `apps/mjga-j2me/src/com/mjga/util/JsonParser.java` — non-regex JSON scanner.
- `apps/mjga-j2me/src/com/mjga/network/HttpClient.java` — token, UTF-8, status, bounded response.
- `apps/mjga-j2me/src/com/mjga/ui/ChatScreen.java` — bounded history and UI-thread updates.
- `apps/mjga-j2me/src/com/mjga/midlet/MJGAMidlet.java` — application-property configuration.
- `apps/mjga-j2me/build.xml` — tests, optional resources, injected properties, valid JAD.
- `apps/mjga-j2me/run-emulator.sh` — existing-jar classpath.
- `.vscode/launch.json` — remove embedded credentials.
- `.gitignore` — local J2ME config and Python test caches.
- `README.md`, client/proxy READMEs, and `docs/05-roadmap.md` — verified state and secure deployment.

### Delete

- `apps/mjga-j2me/build.gradle` and `apps/mjga-j2me/pom.xml` — unused competing build definitions.
- `projects/openclaw-proxy/openclaw-proxy` — tracked 21 MB generated binary.

## Task 1: Add validated Python configuration

**Files:**
- Create: `projects/openclaw-proxy/openclaw-proxy-py/config.py`
- Create: `projects/openclaw-proxy/openclaw-proxy-py/tests/test_config.py`
- Modify: `projects/openclaw-proxy/openclaw-proxy-py/pyproject.toml`

- [ ] **Step 1: Add pytest as a development dependency**

Add this section to `pyproject.toml`:

```toml
[dependency-groups]
dev = [
    "pytest>=8.3.0",
]
```

Run:

```powershell
cd projects/openclaw-proxy/openclaw-proxy-py
uv lock
uv sync --group dev
```

Expected: `uv.lock` updates and pytest is installed.

- [ ] **Step 2: Write failing configuration tests**

Create `tests/test_config.py`:

```python
import pytest

from config import Settings


BASE_ENV = {
    "API_KEY": "upstream-secret",
    "CLIENT_TOKEN": "device-secret",
}


def test_requires_api_key():
    with pytest.raises(ValueError, match="API_KEY is required"):
        Settings.from_mapping({"CLIENT_TOKEN": "device-secret"})


def test_requires_client_token():
    with pytest.raises(ValueError, match="CLIENT_TOKEN is required"):
        Settings.from_mapping({"API_KEY": "upstream-secret"})


@pytest.mark.parametrize(
    "name,value",
    [
        ("PORT", "0"),
        ("UPSTREAM_TIMEOUT_SECONDS", "0"),
        ("MAX_REQUEST_BYTES", "-1"),
        ("MAX_RESPONSE_BYTES", "abc"),
        ("RATE_LIMIT_PER_MINUTE", "0"),
    ],
)
def test_rejects_invalid_positive_integer(name, value):
    env = dict(BASE_ENV)
    env[name] = value
    with pytest.raises(ValueError, match=name):
        Settings.from_mapping(env)


def test_loads_defaults():
    settings = Settings.from_mapping(BASE_ENV)
    assert settings.port == 8080
    assert settings.upstream_timeout_seconds == 60
    assert settings.max_request_bytes == 8192
    assert settings.max_response_bytes == 32768
    assert settings.rate_limit_per_minute == 10
```

- [ ] **Step 3: Verify RED**

Run:

```powershell
uv run pytest tests/test_config.py -q
```

Expected: collection fails because `config.Settings` does not exist.

- [ ] **Step 4: Implement the minimal settings object**

Create `config.py`:

```python
from dataclasses import dataclass
import os
from typing import Mapping


DEFAULT_API_URL = "https://ark.cn-beijing.volces.com/api/coding/v3/chat/completions"


def _positive_int(values: Mapping[str, str], name: str, default: int) -> int:
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
            upstream_timeout_seconds=_positive_int(values, "UPSTREAM_TIMEOUT_SECONDS", 60),
            max_request_bytes=_positive_int(values, "MAX_REQUEST_BYTES", 8192),
            max_response_bytes=_positive_int(values, "MAX_RESPONSE_BYTES", 32768),
            rate_limit_per_minute=_positive_int(values, "RATE_LIMIT_PER_MINUTE", 10),
        )

    @classmethod
    def from_env(cls) -> "Settings":
        return cls.from_mapping(os.environ)
```

- [ ] **Step 5: Verify GREEN and commit**

Run:

```powershell
uv run pytest tests/test_config.py -q
```

Expected: all configuration tests pass.

Commit:

```powershell
git add projects/openclaw-proxy/openclaw-proxy-py
git commit -m "test(proxy-py): validate secure configuration"
```

## Task 2: Implement the Python proxy contract

**Files:**
- Create: `projects/openclaw-proxy/openclaw-proxy-py/limiter.py`
- Create: `projects/openclaw-proxy/openclaw-proxy-py/tests/test_proxy.py`
- Modify: `projects/openclaw-proxy/openclaw-proxy-py/main.py`

- [ ] **Step 1: Write failing health and authentication tests**

Create the common fixture and first tests in `tests/test_proxy.py`:

```python
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
```

- [ ] **Step 2: Verify RED**

Run:

```powershell
uv run pytest tests/test_proxy.py -q
```

Expected: import fails because `create_app` with injected settings/transport is absent.

- [ ] **Step 3: Add the minimal app factory and constant-time token check**

Replace global environment loading in `main.py` with these public boundaries:

```python
def create_app(settings=None, transport=None):
    resolved = settings or Settings.from_env()
    app = FastAPI()
    app.state.settings = resolved
    app.state.transport = transport
    app.state.limiter = FixedWindowLimiter(resolved.rate_limit_per_minute)

    @app.get("/ping")
    async def ping():
        return {"status": "ok"}

    @app.post("/v1/chat/completions")
    async def proxy(request: Request):
        supplied = request.headers.get("X-MJGA-Token", "")
        if not secrets.compare_digest(supplied, resolved.client_token):
            return error_response(401, "unauthorized", "client token is invalid")
        return error_response(501, "not_implemented", "proxy pipeline is not implemented")

    return app
```

Add an `error_response(status, code, message)` helper that returns `JSONResponse` with the design envelope. Do not create a module-level app; the `__main__` block must call `uvicorn.run(create_app(), ...)` so missing credentials fail startup.

- [ ] **Step 4: Verify the first GREEN state**

Run:

```powershell
uv run pytest tests/test_proxy.py -q
```

Expected: ping and authentication tests pass.

- [ ] **Step 5: Add failing request-validation and limit tests**

Append tests for:

```python
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
    app = create_app(settings(rate_limit_per_minute=1), upstream(lambda request: httpx.Response(200, json={})))
    client = TestClient(app)
    payload = {"model": "test", "messages": [{"role": "user", "content": "hello"}]}
    assert client.post("/v1/chat/completions", json=payload, headers=auth_headers()).status_code == 200
    limited = client.post("/v1/chat/completions", json=payload, headers=auth_headers())
    assert limited.status_code == 429
    assert limited.json()["error"]["code"] == "rate_limited"
```

Verify RED: current authenticated requests return `501`, not the expected statuses.

- [ ] **Step 6: Implement bounded reading, JSON validation, and limiter**

Create `limiter.py` with a lock-protected `FixedWindowLimiter(limit, clock=time.monotonic)` whose `allow()` resets after 60 seconds and otherwise increments a global counter.

In `main.py`, add:

```python
async def read_limited_body(request: Request, limit: int) -> bytes:
    body = bytearray()
    async for chunk in request.stream():
        body.extend(chunk)
        if len(body) > limit:
            raise RequestTooLarge()
    return bytes(body)


def validate_payload(body: bytes) -> None:
    try:
        data = json.loads(body.decode("utf-8"))
    except (UnicodeDecodeError, json.JSONDecodeError) as exc:
        raise InvalidRequest() from exc
    messages = data.get("messages") if isinstance(data, dict) else None
    if not isinstance(messages, list) or not messages:
        raise InvalidRequest()
    for message in messages:
        if not isinstance(message, dict) or not isinstance(message.get("content"), str):
            raise InvalidRequest()
```

Apply operations in this order: authenticate, rate-limit, bounded read, validate. Return `413`, `400`, or `429` using the shared error envelope.

- [ ] **Step 7: Add failing upstream behavior tests**

Add tests proving:

- the upstream receives the configured URL and `Authorization: Bearer upstream-secret`;
- successful content/status is returned;
- upstream non-2xx status is preserved;
- `httpx.ReadTimeout` maps to `504 upstream_timeout`;
- `httpx.ConnectError` maps to `502 upstream_unavailable`;
- a body larger than `max_response_bytes` maps to `502 upstream_response_too_large`;
- `caplog` does not contain upstream key, client token, or the unique prompt `PRIVATE-PROMPT-42`.

Use `httpx.MockTransport`; do not access a real API.

Run:

```powershell
uv run pytest tests/test_proxy.py -q
```

Expected: upstream cases fail because the pipeline is not implemented.

- [ ] **Step 8: Implement the upstream pipeline**

Use one request-scoped `httpx.AsyncClient` with injected transport and configured timeout. Read the upstream response with a `max_response_bytes + 1` bound, return raw upstream content/status for bounded responses, and map HTTPX timeout/transport errors to the agreed error envelope. Log only method, path, status, and elapsed milliseconds.

The forwarding request must set only:

```python
headers = {
    "Content-Type": "application/json; charset=utf-8",
    "Authorization": f"Bearer {resolved.api_key}",
}
```

- [ ] **Step 9: Verify all Python tests and commit**

Run:

```powershell
uv run pytest -q
python -m compileall -q config.py limiter.py main.py tests
```

Expected: all tests pass and compileall is silent.

Commit:

```powershell
git add projects/openclaw-proxy/openclaw-proxy-py
git commit -m "feat(proxy-py): secure and bound proxy requests"
```

## Task 3: Add validated Go configuration and limiter

**Files:**
- Create: `projects/openclaw-proxy/openclaw-proxy-go/config.go`
- Create: `projects/openclaw-proxy/openclaw-proxy-go/config_test.go`
- Create: `projects/openclaw-proxy/openclaw-proxy-go/limiter.go`

- [ ] **Step 1: Write failing Go configuration tests**

Create table-driven tests that call `loadConfig(mapLookup(map[string]string{...}))`. They must cover missing `API_KEY`, missing `CLIENT_TOKEN`, each non-positive/non-numeric integer, and defaults `8080/60/8192/32768/10`.

Use this production API in the tests:

```go
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

func loadConfig(lookup lookupFunc) (config, error)
```

- [ ] **Step 2: Verify RED**

Run:

```powershell
go test ./... -run TestLoadConfig -v
```

Expected: compile failure because `loadConfig` does not exist. If Go is unavailable locally, record that limitation and rely on the CI job introduced in Task 8; do not claim local success.

- [ ] **Step 3: Implement minimal configuration parsing**

Create `config.go` with required trimmed secrets, the existing Ark default URL, and positive numeric limits:

```go
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
    if err != nil { return config{}, err }
    timeout, err := positiveInt(lookup, "UPSTREAM_TIMEOUT_SECONDS", 60)
    if err != nil { return config{}, err }
    maxRequest, err := positiveInt(lookup, "MAX_REQUEST_BYTES", 8192)
    if err != nil { return config{}, err }
    maxResponse, err := positiveInt(lookup, "MAX_RESPONSE_BYTES", 32768)
    if err != nil { return config{}, err }
    rate, err := positiveInt(lookup, "RATE_LIMIT_PER_MINUTE", 10)
    if err != nil { return config{}, err }
    return config{
        APIKey: strings.TrimSpace(apiKey), ClientToken: strings.TrimSpace(clientToken),
        APIURL: strings.TrimSpace(apiURL), Port: port,
        UpstreamTimeoutSeconds: timeout, MaxRequestBytes: int64(maxRequest),
        MaxResponseBytes: int64(maxResponse), RateLimitPerMinute: rate,
    }, nil
}
```

`main.go` will pass `os.LookupEnv`.

- [ ] **Step 4: Write failing limiter tests, then implement**

Test a limiter created with `newFixedWindowLimiter(2, fakeClock.Now)` allows two calls, rejects the third, and allows a call after advancing 60 seconds. Implement it with `sync.Mutex`, window start, count, limit, and injected `func() time.Time`.

The production implementation in `limiter.go` is:

```go
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
    return &fixedWindowLimiter{limit: limit, now: now, windowStart: now()}
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
```

- [ ] **Step 5: Verify and commit**

Run:

```powershell
go test ./... -run 'TestLoadConfig|TestFixedWindowLimiter' -v
```

Expected: all configuration and limiter tests pass.

Commit:

```powershell
git add projects/openclaw-proxy/openclaw-proxy-go
git commit -m "test(proxy-go): validate configuration and rate limits"
```

## Task 4: Implement the Go proxy contract and reproducible image

**Files:**
- Create: `projects/openclaw-proxy/openclaw-proxy-go/server.go`
- Create: `projects/openclaw-proxy/openclaw-proxy-go/server_test.go`
- Modify: `projects/openclaw-proxy/openclaw-proxy-go/main.go`
- Modify: `projects/openclaw-proxy/openclaw-proxy-go/go.mod`
- Create: `projects/openclaw-proxy/openclaw-proxy-go/go.sum`
- Modify: `projects/openclaw-proxy/openclaw-proxy-go/Dockerfile`

- [ ] **Step 1: Write failing health/authentication tests**

Define the testable server boundary:

```go
func newServer(cfg config, client *http.Client, logger *log.Logger, limiter *fixedWindowLimiter) http.Handler
```

Use `httptest.NewServer` as the upstream and `httptest.NewRecorder` for the proxy. Test `/ping`, missing/wrong token, and the exact `unauthorized` envelope.

- [ ] **Step 2: Verify RED, then add the minimal router**

Run `go test ./... -run 'TestPing|TestAuthentication' -v`; expect compile failure. Implement `gin.New()`, `gin.Recovery()`, `/ping`, constant-time token comparison via `crypto/subtle`, and the shared JSON error helper. Re-run until these tests pass.

- [ ] **Step 3: Add failing bounds, validation, and upstream tests**

Add table-driven tests for `400`, `413`, `429`, upstream success, upstream non-2xx, timeout, connection error, response too large, and log redaction. Use a custom `roundTripFunc` for transport failures:

```go
type roundTripFunc func(*http.Request) (*http.Response, error)

func (f roundTripFunc) RoundTrip(r *http.Request) (*http.Response, error) {
    return f(r)
}
```

Verify RED before implementing each group.

- [ ] **Step 4: Implement the bounded Go handler**

The handler must:

1. Authenticate with `subtle.ConstantTimeCompare`.
2. Check the global limiter.
3. Read `MaxRequestBytes + 1` through `io.LimitReader` and reject overflow.
4. Decode a small validation struct and require at least one message with string content.
5. Create the upstream request with context timeout.
6. Set only Content-Type and upstream Authorization.
7. Read at most `MaxResponseBytes + 1` and reject overflow.
8. Return bounded upstream content and status.
9. Log only status and duration.

Keep `main.go` limited to `loadConfig(os.LookupEnv)`, HTTP client construction, server construction, and `http.Server.ListenAndServe()`.

- [ ] **Step 5: Generate dependency checksums and fix Docker**

Run:

```powershell
go mod tidy
```

Keep Dockerfile dependency caching:

```dockerfile
COPY go.mod go.sum ./
RUN go mod download
COPY . .
RUN CGO_ENABLED=0 GOOS=linux go build -trimpath -ldflags="-s -w" -o /openclaw-proxy .
```

- [ ] **Step 6: Verify and commit**

Run:

```powershell
go test ./...
go vet ./...
docker build -t mjga-openclaw-proxy:test .
```

Expected: tests/vet pass and image builds. Record unavailable local tools explicitly; CI must run all three commands before completion.

Commit:

```powershell
git add projects/openclaw-proxy/openclaw-proxy-go
git commit -m "feat(proxy-go): secure and bound proxy requests"
```

## Task 5: Replace fragile J2ME JSON parsing

**Files:**
- Create: `apps/mjga-j2me/test/com/mjga/util/JsonParserTest.java`
- Modify: `apps/mjga-j2me/src/com/mjga/util/JsonParser.java`

- [ ] **Step 1: Write the failing executable test**

Create a Java test class with `main`, `assertEquals`, and `assertNull`. Cover:

```java
assertEquals(
    "{\"model\":\"m\",\"messages\":[{\"role\":\"user\",\"content\":\"中文\\\"quote\\\"\\\\path\\nline\"}]}",
    JsonParser.buildChatRequest("m", "中文\"quote\"\\path\nline")
);
assertEquals(
    "中文\"quote\"\\path\nline\tA",
    JsonParser.extractChatResponse(
        "{\"choices\":[{\"message\":{\"content\":\"中文\\\"quote\\\"\\\\path\\nline\\t\\u0041\"}}]}"
    )
);
assertNull(JsonParser.extractChatResponse("{\"choices\":[{\"message\":{\"content\":\"unterminated}"));
```

- [ ] **Step 2: Verify RED**

Compile and run against the current implementation:

```powershell
cd apps/mjga-j2me
javac -encoding UTF-8 -cp "lib/cldcapi11.jar;lib/midpapi20.jar" -d build-test src/com/mjga/util/JsonParser.java test/com/mjga/util/JsonParserTest.java
java -cp "build-test;lib/cldcapi11.jar;lib/midpapi20.jar" com.mjga.util.JsonParserTest
```

Expected: escaped-quote response test fails because current parsing stops at the escaped quote.

- [ ] **Step 3: Implement a bounded JSON string scanner**

Replace regex-based unescaping with character-by-character helpers:

```java
private static int findContentValue(String json) {
    int key = json.indexOf("\"content\"");
    if (key < 0) return -1;
    int index = key + 9;
    while (index < json.length() && Character.isWhitespace(json.charAt(index))) index++;
    if (index >= json.length() || json.charAt(index) != ':') return -1;
    index++;
    while (index < json.length() && Character.isWhitespace(json.charAt(index))) index++;
    if (index >= json.length() || json.charAt(index) != '\"') return -1;
    return index + 1;
}

private static String scanJsonString(String json, int start) {
    StringBuffer out = new StringBuffer();
    for (int index = start; index < json.length(); index++) {
        char value = json.charAt(index);
        if (value == '\"') return out.toString();
        if (value != '\\') {
            out.append(value);
            continue;
        }
        if (++index >= json.length()) return null;
        char escaped = json.charAt(index);
        switch (escaped) {
            case '\"': out.append('\"'); break;
            case '\\': out.append('\\'); break;
            case '/': out.append('/'); break;
            case 'b': out.append('\b'); break;
            case 'f': out.append('\f'); break;
            case 'n': out.append('\n'); break;
            case 'r': out.append('\r'); break;
            case 't': out.append('\t'); break;
            case 'u':
                if (index + 4 >= json.length()) return null;
                int code = 0;
                for (int offset = 1; offset <= 4; offset++) {
                    int hex = hexValue(json.charAt(index + offset));
                    if (hex < 0) return null;
                    code = (code << 4) | hex;
                }
                out.append((char) code);
                index += 4;
                break;
            default: return null;
        }
    }
    return null;
}

private static int hexValue(char value) {
    if (value >= '0' && value <= '9') return value - '0';
    if (value >= 'a' && value <= 'f') return value - 'a' + 10;
    if (value >= 'A' && value <= 'F') return value - 'A' + 10;
    return -1;
}
```

`scanJsonString` must return `null` for unknown, incomplete, or unterminated escapes. Rewrite request escaping as one `StringBuffer` scan so it does not depend on regex APIs absent from strict CLDC implementations.

- [ ] **Step 4: Verify GREEN and commit**

Run the same compile/run commands. Expected output: `JsonParserTest: PASS`.

Commit:

```powershell
git add apps/mjga-j2me/src/com/mjga/util/JsonParser.java apps/mjga-j2me/test/com/mjga/util/JsonParserTest.java
git commit -m "fix(j2me): parse escaped JSON content safely"
```

## Task 6: Bound J2ME HTTP I/O and add device authentication

**Files:**
- Create: `apps/mjga-j2me/src/com/mjga/network/HttpResponse.java`
- Create: `apps/mjga-j2me/src/com/mjga/network/ResponseReader.java`
- Create: `apps/mjga-j2me/test/com/mjga/network/ResponseReaderTest.java`
- Modify: `apps/mjga-j2me/src/com/mjga/network/HttpClient.java`
- Modify: `apps/mjga-j2me/src/com/mjga/ui/ChatScreen.java`

- [ ] **Step 1: Write failing UTF-8 and overflow tests**

The test must exercise this API:

```java
assertEquals("中文回复", ResponseReader.readUtf8(
    new ByteArrayInputStream("中文回复".getBytes("UTF-8")), 64));
assertThrowsIOException(new RunnableWithIOException() {
    public void run() throws IOException {
        ResponseReader.readUtf8(new ByteArrayInputStream(new byte[9]), 8);
    }
});
```

Verify RED because `ResponseReader` is absent.

- [ ] **Step 2: Implement the minimal bounded reader**

Use `ByteArrayOutputStream`, a 256-byte buffer, a running total, and `new String(bytes, "UTF-8")`. Throw `IOException("response too large")` as soon as total bytes exceed the configured maximum.

- [ ] **Step 3: Verify reader GREEN**

Compile and run `ResponseReaderTest`; expected output is `ResponseReaderTest: PASS`.

- [ ] **Step 4: Refactor the HTTP result and client**

Introduce immutable `HttpResponse` fields `statusCode`, `body`, and `errorCode`, with factories for HTTP and network results. Change the client constructor to:

```java
public HttpClient(String clientToken, int maxResponseBytes)
```

Change the request method to:

```java
public HttpResponse sendPost(String url, String body)
```

It must encode once with `body.getBytes("UTF-8")`, set `Content-Type: application/json; charset=utf-8`, `Content-Length`, `Connection: close`, and `X-MJGA-Token`, obtain the response code before choosing input/error behavior, and decode through `ResponseReader`. Keep at most three attempts, but retry only `IOException`; never retry a received HTTP status.

- [ ] **Step 5: Update ChatScreen error mapping and UI-thread access**

Map `401`, `413`, `429`, `502`, `504`, oversized response, and network failure to short Chinese messages. Capture input on the command/UI thread before starting the worker. Perform all `StringItem`, `TextField`, form rebuild, `processing`, and display changes inside `Display.callSerially()` callbacks.

- [ ] **Step 6: Compile the complete client and commit**

Run:

```powershell
javac -encoding UTF-8 -source 8 -target 8 -cp "lib/cldcapi11.jar;lib/midpapi20.jar" -d build-manual (Get-ChildItem -Recurse -Filter '*.java' src | ForEach-Object FullName)
```

Expected: compilation succeeds with no errors.

Commit:

```powershell
git add apps/mjga-j2me/src apps/mjga-j2me/test
git commit -m "fix(j2me): bound UTF-8 HTTP responses"
```

## Task 7: Cap chat history and inject MIDlet configuration

**Files:**
- Create: `apps/mjga-j2me/src/com/mjga/ui/ChatHistory.java`
- Create: `apps/mjga-j2me/test/com/mjga/ui/ChatHistoryTest.java`
- Create: `apps/mjga-j2me/config.properties.example`
- Modify: `apps/mjga-j2me/src/com/mjga/ui/ChatScreen.java`
- Modify: `apps/mjga-j2me/src/com/mjga/midlet/MJGAMidlet.java`
- Modify: `.gitignore`

- [ ] **Step 1: Write a failing history-cap test**

Test `new ChatHistory(10)`, add 11 numbered pairs, then assert size is 10, the first retained question is `q2`, and the last is `q11`. Verify RED because the class is absent.

- [ ] **Step 2: Implement and integrate ChatHistory**

Move `QAPair` into the same package as a package-private value class. `ChatHistory.add(question, answer)` removes element zero when at capacity, and exposes `size()`/`get(index)`. Replace direct `Vector` ownership in `ChatScreen` with `ChatHistory(10)`.

- [ ] **Step 3: Verify history GREEN**

Compile/run `ChatHistoryTest`; expected output is `ChatHistoryTest: PASS`.

- [ ] **Step 4: Add application-property configuration**

Create `config.properties.example` containing only dummy values:

```properties
mjga.api.url=http://192.168.1.10:8080/v1/chat/completions
mjga.client.token=replace-with-a-long-random-token
mjga.model=ark-code-latest
```

Add `apps/mjga-j2me/config.properties` to `.gitignore`. In `MJGAMidlet`, read `MJGA-Api-Url`, `MJGA-Client-Token`, and `MJGA-Model` with `getAppProperty()`. URL and token are required; model defaults to `ark-code-latest`. If invalid, show a persistent configuration alert and never construct `ChatScreen`.

- [ ] **Step 5: Verify complete compilation and commit**

Run the client compilation plus all three executable tests. Expected: all pass.

Commit:

```powershell
git add .gitignore apps/mjga-j2me/src apps/mjga-j2me/test apps/mjga-j2me/config.properties.example
git commit -m "feat(j2me): inject config and cap chat history"
```

## Task 8: Make builds reproducible and add CI

**Files:**
- Modify: `apps/mjga-j2me/build.xml`
- Modify: `apps/mjga-j2me/run-emulator.sh`
- Create: `apps/mjga-j2me/run-emulator.ps1`
- Delete: `apps/mjga-j2me/build.gradle`
- Delete: `apps/mjga-j2me/pom.xml`
- Delete: `projects/openclaw-proxy/openclaw-proxy`
- Modify: `.vscode/launch.json`
- Create: `.github/workflows/baseline.yml`

- [ ] **Step 1: Add an Ant test target and observe current packaging failure**

Add `test.src.dir`, `test.build.dir`, three test main classes, and a `test` target that compiles/runs them with `failonerror="true"`. Copy `config.properties.example` to untracked `config.properties`, then run `ant clean test dist`.

Expected before packaging fixes: tests pass but `resources` or JAD validation exposes the known packaging defects.

- [ ] **Step 2: Fix Ant packaging**

Load `config.properties`, fail with clear messages when `mjga.api.url` or `mjga.client.token` is absent, use source/target `1.7`, mark the `res` fileset `erroronmissingdir="false"`, write the custom MJGA properties and `MIDlet-Permissions: javax.microedition.io.Connector.http` to manifest/JAD, and calculate `MIDlet-Jar-Size` with Ant `<length>` after JAR creation.

Make `dist` depend on `clean,test,jad` with no duplicate packaging path.

- [ ] **Step 3: Fix emulator entry points**

Remove nonexistent `microemu-device-large.jar` from `run-emulator.sh`. Use the same semicolon-separated classpath in `run-emulator.ps1` and validate `dist/MJGA.jad` before launch.

- [ ] **Step 4: Remove generated/competing artifacts and exposed launch credentials**

Delete Gradle/POM and the tracked proxy binary. Replace `.vscode/launch.json` inline `env` block with:

```json
"envFile": "${workspaceFolder}/projects/openclaw-proxy/openclaw-proxy-py/.env"
```

Ensure `.env`, caches, build and test output remain ignored.

- [ ] **Step 5: Add CI**

Create `.github/workflows/baseline.yml` with three jobs:

- Python: setup Python 3.12 and uv, `uv sync --group dev`, `uv run pytest -q`.
- Go: setup Go from `go.mod`, `go test ./...`, `go vet ./...`, Docker build.
- J2ME: setup JDK 17, install Ant, copy example config, `ant clean test dist`, then assert JAD contains nonzero `MIDlet-Jar-Size`, HTTP permission, API URL, token, and model.

Add a fourth no-secret check using `git grep -nE` to reject UUID-like literals on lines containing `API_KEY` or `CLIENT_TOKEN`; exclude documentation examples that contain only non-UUID placeholders.

- [ ] **Step 6: Verify and commit**

Run all locally available jobs plus `git diff --check`. Expected: Ant/Python pass locally; Go/Docker pass locally or are evidenced by CI.

Commit:

```powershell
git add -A
git commit -m "build: make safe baseline reproducible"
```

## Task 9: Align documentation with verified behavior

**Files:**
- Modify: `README.md`
- Modify: `apps/mjga-j2me/README.md`
- Modify: `projects/openclaw-proxy/README.md`
- Modify: `projects/openclaw-proxy/openclaw-proxy-go/README.md`
- Modify: `projects/openclaw-proxy/openclaw-proxy-py/README.md`
- Modify: `docs/03-networking.md`
- Modify: `docs/05-roadmap.md`

- [ ] **Step 1: Add documentation acceptance checks**

Before editing, run searches proving current inconsistencies:

```powershell
rg -n "LwUIT|OPENAI_API_KEY|/chat`|压缩响应|microemu-device-large|API Key.*硬编码" README.md apps projects docs
```

Save the mismatches in the task log; this is the RED evidence for documentation behavior.

- [ ] **Step 2: Update setup and security documentation**

Document exact `API_KEY`, `CLIENT_TOKEN`, `API_URL`, limits and timeouts; J2ME `config.properties`; `X-MJGA-Token`; mock-first testing; Ant-only packaging; Windows/Linux emulator commands; and the requirement to use LAN/VPN rather than expose plaintext HTTP publicly.

Add a prominent notice that the previously committed upstream key must be revoked and rotated, because deleting it from the current tree does not remove Git history.

- [ ] **Step 3: Make roadmap status evidence-based**

Separate status into:

- source implemented;
- automated checks passed;
- MicroEmulator verified;
- W995 verified.

Mark only states supported by this implementation run. Keep W995 networking and installation unchecked.

- [ ] **Step 4: Verify stale claims are gone and commit**

Run:

```powershell
rg -n "LwUIT|OPENAI_API_KEY|microemu-device-large|API Key.*硬编码" README.md apps projects docs
git diff --check
```

Expected: no stale implementation claim or embedded credential remains; any historical-risk mention contains no secret value.

Commit:

```powershell
git add README.md apps/mjga-j2me/README.md projects/openclaw-proxy docs/03-networking.md docs/05-roadmap.md
git commit -m "docs: document secure text baseline"
```

## Task 10: Final integrated verification

**Files:** all files changed by Tasks 1–9.

- [ ] **Step 1: Run the full local suite**

```powershell
cd projects/openclaw-proxy/openclaw-proxy-py
uv run pytest -q
python -m compileall -q config.py limiter.py main.py tests

cd ../openclaw-proxy-go
go test ./...
go vet ./...
docker build -t mjga-openclaw-proxy:test .

cd ../../../apps/mjga-j2me
Copy-Item config.properties.example config.properties
ant clean test dist
```

Expected: all available checks pass. If Go, Docker, or Ant is unavailable, CI must supply the missing evidence before completion.

- [ ] **Step 2: Run security and repository checks**

```powershell
git grep -nE '(API_KEY|CLIENT_TOKEN).*[0-9a-fA-F]{8}-[0-9a-fA-F-]{27,}'
git ls-files | Select-String '(^|/)(\.env|config\.properties)$|projects/openclaw-proxy/openclaw-proxy$'
git diff origin/dev...HEAD --check
git status --short
```

Expected: no credential-like literal, local secret file, generated proxy binary, whitespace error, or unplanned working-tree change.

- [ ] **Step 3: Review acceptance criteria**

Cross-check every item in `docs/superpowers/specs/2026-09-03-safe-text-baseline-design.md` section 9. Record MicroEmulator, Docker, and W995 items exactly as passed, failed, unavailable, or pending; never infer success.

- [ ] **Step 4: Final commit if verification required tracked adjustments**

```powershell
git add -A
git commit -m "chore: finalize safe text baseline"
```

Skip this commit when the working tree is already clean.

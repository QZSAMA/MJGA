# openclaw-proxy

MJGA 的 HTTP→HTTPS 文本代理提供 Go 和 Python 两个实现，并共享同一请求、安全和错误契约。

```text
J2ME -- HTTP + X-MJGA-Token --> proxy -- HTTPS + Bearer API_KEY --> LLM
```

J2ME 一侧是明文 HTTP。代理只能放在可信 LAN、VPN 或受控隧道中，不要直接暴露到公网。

## 实现选择

| 实现 | 目录 | 环境 |
|---|---|---|
| Go | `openclaw-proxy-go/` | Go 1.21；可构建静态 Docker 镜像 |
| Python | `openclaw-proxy-py/` | Python 3.10+、uv、FastAPI/HTTPX |

两者都支持：

- `GET /ping`（无需令牌）；
- `POST /v1/chat/completions`；
- 恒定时间校验 `X-MJGA-Token`；
- 固定窗口进程内限流；
- 有界请求、响应和上游超时；
- OpenAI 兼容 JSON 最小结构校验；
- 上游状态/内容原样返回以及统一代理错误信封；
- 只记录状态和耗时，不记录密钥、令牌或提示词。

## 配置

| 环境变量 | 必填 | 默认值 |
|---|---:|---|
| `API_KEY` | 是 | 无 |
| `CLIENT_TOKEN` | 是 | 无 |
| `API_URL` | 否 | `https://ark.cn-beijing.volces.com/api/coding/v3/chat/completions` |
| `PORT` | 否 | `8080` |
| `UPSTREAM_TIMEOUT_SECONDS` | 否 | `60` |
| `MAX_REQUEST_BYTES` | 否 | `8192` |
| `MAX_RESPONSE_BYTES` | 否 | `32768` |
| `RATE_LIMIT_PER_MINUTE` | 否 | `10` |

所有数值必须为正整数；`API_KEY` 或 `CLIENT_TOKEN` 缺失时服务拒绝启动。

## 启动

Python：

```bash
cd projects/openclaw-proxy/openclaw-proxy-py
uv sync --group dev
export API_KEY=replace-with-your-upstream-key
export CLIENT_TOKEN=replace-with-a-long-random-token
uv run python main.py
```

Go：

```bash
cd projects/openclaw-proxy/openclaw-proxy-go
go build -o openclaw-proxy .
export API_KEY=replace-with-your-upstream-key
export CLIENT_TOKEN=replace-with-a-long-random-token
./openclaw-proxy
```

Go Docker：

```bash
docker build -t mjga-openclaw-proxy .
docker run --rm -p 8080:8080 \
  -e API_KEY=replace-with-your-upstream-key \
  -e CLIENT_TOKEN=replace-with-a-long-random-token \
  mjga-openclaw-proxy
```

## 调用示例

```bash
curl http://localhost:8080/ping

curl -X POST http://localhost:8080/v1/chat/completions \
  -H 'Content-Type: application/json; charset=utf-8' \
  -H 'X-MJGA-Token: replace-with-a-long-random-token' \
  -d '{"model":"ark-code-latest","messages":[{"role":"user","content":"你好"}]}'
```

代理自身错误格式：

```json
{"error":{"code":"unauthorized","message":"client token is invalid"}}
```

可能的代理错误码包括 `unauthorized`、`invalid_request`、`request_too_large`、`rate_limited`、`upstream_timeout`、`upstream_unavailable` 和 `upstream_response_too_large`。收到上游 HTTP 响应时，状态和有界响应体保持不变。

## 测试

```bash
# Python（全部使用 MockTransport，不调用真实 LLM）
cd openclaw-proxy-py
uv sync --group dev
uv run pytest -q

# Go（httptest/自定义 RoundTripper，不调用真实 LLM）
cd ../openclaw-proxy-go
go test ./...
go vet ./...
docker build -t mjga-openclaw-proxy:test .
```

## 密钥历史风险

仓库旧历史中曾出现真实上游密钥。当前源码删除该值并不等于从 Git 历史中删除；密钥所有者必须在上游平台撤销并轮换它。请勿把 `.env`、设备真实令牌或上游密钥提交到仓库。

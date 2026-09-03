# J2ME 网络协议与安全边界

## 为什么需要代理

W995 的老旧 TLS 能力无法可靠连接现代 HTTPS LLM API。MJGA 因此把 TLS 终止放在 Go/Python 代理：

```text
W995 -- HTTP --> MJGA proxy -- HTTPS --> OpenAI-compatible upstream
```

手机到代理的 HTTP 是明文链路，包含设备令牌和对话文本。它只适用于可信家庭 LAN、VPN 或受控隧道；不能因为上游使用 HTTPS，就把公网 HTTP 暴露视为安全。

## 端点

### `GET /ping`

无需鉴权，用于存活检查：

```json
{"status":"ok"}
```

### `POST /v1/chat/completions`

请求头：

```text
Content-Type: application/json; charset=utf-8
X-MJGA-Token: <与代理 CLIENT_TOKEN 相同的共享令牌>
```

请求体为最小 OpenAI 兼容结构：

```json
{
  "model": "ark-code-latest",
  "messages": [
    {"role": "user", "content": "你好"}
  ]
}
```

代理固定使用服务端配置的 `API_URL`，并只向上游设置 JSON Content-Type 与 `Authorization: Bearer <API_KEY>`。客户端不能通过请求覆盖上游目标或授权头。

收到上游 HTTP 响应时，代理在响应大小范围内原样返回状态和内容。代理自身错误使用统一信封：

```json
{
  "error": {
    "code": "rate_limited",
    "message": "request rate limit exceeded"
  }
}
```

| HTTP | code | 含义 |
|---:|---|---|
| 400 | `invalid_request` | UTF-8/JSON 或 messages 结构无效 |
| 401 | `unauthorized` | 设备令牌缺失或错误 |
| 413 | `request_too_large` | 请求超过配置上限 |
| 429 | `rate_limited` | 当前 60 秒窗口已满 |
| 502 | `upstream_unavailable` | 无法连接或读取上游 |
| 502 | `upstream_response_too_large` | 上游响应超过配置上限 |
| 504 | `upstream_timeout` | 上游超时 |

## J2ME I/O 约束

客户端遵循以下顺序：

1. 用 UTF-8 把 JSON 请求体编码一次；
2. 以字节数设置 Content-Length；
3. 发送 `Connection: close` 与 `X-MJGA-Token`；
4. 先读取 HTTP 状态，再读取响应；
5. 以 256 字节缓冲累计，超过 32768 字节立即失败；
6. 用 UTF-8 解码，逐字符扫描 JSON 字符串转义；
7. 收到 HTTP 状态后不重试；只对尚未收到状态的 I/O 失败最多尝试三次；
8. 始终关闭输入流、输出流和连接。

JAD/Manifest 声明：

```text
MIDlet-Permissions: javax.microedition.io.Connector.http
```

## 配置对应关系

代理环境：

- `API_KEY`：上游密钥，只存在于代理；
- `CLIENT_TOKEN`：J2ME 共享令牌；
- `API_URL`：固定 HTTPS 上游；
- `UPSTREAM_TIMEOUT_SECONDS`、`MAX_REQUEST_BYTES`、`MAX_RESPONSE_BYTES`、`RATE_LIMIT_PER_MINUTE`：资源边界。

客户端 `config.properties`：

- `mjga.api.url`：代理的 `/v1/chat/completions` HTTP URL；
- `mjga.client.token`：必须等于 `CLIENT_TOKEN`；
- `mjga.model`：请求中的模型/端点 ID。

不要把 `API_KEY` 写入 JAD/JAR。`config.properties` 被忽略，但打包产物会包含客户端令牌，因此 JAD/JAR 也不应公开分发。

## 测试策略

代理合同测试全部使用 Python `MockTransport`、Go `httptest`/自定义 `RoundTripper`，不会调用真实 LLM。J2ME 在 Java SE 上运行 JSON、UTF-8/大小边界和历史上限测试。MicroEmulator 与 W995 真机仍需单独做联网验收。

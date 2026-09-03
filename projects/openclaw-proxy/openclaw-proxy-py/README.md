# openclaw-proxy-py

Python 3.10+、FastAPI 和 HTTPX 实现的 MJGA 有界文本代理。完整协议和安全说明见 [上级文档](../README.md)。

## 安装、测试和运行

```bash
uv sync --frozen --group dev
uv run pytest -q
uv run python -m compileall -q config.py limiter.py main.py tests

export API_KEY=replace-with-your-upstream-key
export CLIENT_TOKEN=replace-with-a-long-random-token
uv run python main.py
```

Windows PowerShell：

```powershell
$env:API_KEY = "replace-with-your-upstream-key"
$env:CLIENT_TOKEN = "replace-with-a-long-random-token"
uv run python main.py
```

任一必填密钥缺失都会阻止启动。可选环境变量为 `API_URL`、`PORT`、`UPSTREAM_TIMEOUT_SECONDS`、`MAX_REQUEST_BYTES`、`MAX_RESPONSE_BYTES` 和 `RATE_LIMIT_PER_MINUTE`，默认值见上级文档。

测试通过 `create_app(settings, transport)` 注入 `httpx.MockTransport`，覆盖鉴权、JSON 校验、限流、请求/响应边界、上游异常和日志脱敏，不访问真实 LLM。

本地 `.env` 与 `.venv` 已被 Git 忽略；不要提交真实上游密钥或设备令牌。

# openclaw-proxy-go

Go 1.21 + Gin 实现的 MJGA 有界文本代理。完整协议和安全说明见 [上级文档](../README.md)。

## 运行

```bash
go test ./...
go vet ./...
go build -o openclaw-proxy .

export API_KEY=replace-with-your-upstream-key
export CLIENT_TOKEN=replace-with-a-long-random-token
./openclaw-proxy
```

任一必填密钥缺失都会阻止启动。可选环境变量为 `API_URL`、`PORT`、`UPSTREAM_TIMEOUT_SECONDS`、`MAX_REQUEST_BYTES`、`MAX_RESPONSE_BYTES` 和 `RATE_LIMIT_PER_MINUTE`，默认值见上级文档。

## Docker

```bash
docker build -t mjga-openclaw-proxy .
docker run --rm -p 8080:8080 \
  -e API_KEY=replace-with-your-upstream-key \
  -e CLIENT_TOKEN=replace-with-a-long-random-token \
  mjga-openclaw-proxy
```

Dockerfile 使用 `go.mod/go.sum` 缓存依赖，并构建去除路径/符号信息的静态 Linux 二进制。

## 代码边界

- `config.go`：必填密钥与正整数配置验证；
- `limiter.go`：线程安全的 60 秒固定窗口；
- `server.go`：可注入 HTTP 客户端、日志器和限流器的路由；
- `main.go`：配置加载和 `http.Server` 启动；
- `*_test.go`：`httptest` 与自定义 `RoundTripper` 合同测试，不访问真实 LLM。

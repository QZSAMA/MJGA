# MJGA — Make Java-phone Great Again

MJGA 让 Sony Ericsson W995 等 MIDP 2.0 / CLDC 1.1 功能机成为轻量 AI 文本终端。手机通过受控网络中的 HTTP 代理，把 OpenAI 兼容请求转发到现代 HTTPS LLM API；计算留在服务端，设备只负责输入、传输和显示。

## 当前范围

安全文本基线已经在源码层实现：

- J2ME 文本输入、UTF-8 请求/响应、JSON 转义、错误提示、折叠显示和最近 10 组历史；
- Go 1.21/Gin 与 Python 3.10+/FastAPI 两个等价代理；
- `X-MJGA-Token` 客户端鉴权、固定窗口限流、请求/响应大小限制、上游超时和脱敏日志；
- Ant 单一构建入口、JAR/JAD 配置注入及 GitHub Actions 回归检查。

本基线不包含音频、图片、流式响应或持久会话。MicroEmulator 联网与 W995 真机安装/联网仍待验证。

## 架构

```text
J2ME / W995
  │  HTTP + X-MJGA-Token（仅 LAN / VPN / 受控隧道）
  ▼
openclaw-proxy（Go 或 Python）
  │  HTTPS + Authorization: Bearer <API_KEY>
  ▼
OpenAI 兼容 LLM API
```

J2ME 到代理之间是明文 HTTP，不应把代理端口直接暴露到公网。请在家庭内网、VPN 或受控隧道中部署。

## 项目结构

```text
MJGA/
├── apps/mjga-j2me/                 # MIDP 2.0 / CLDC 1.1 客户端
├── projects/openclaw-proxy/
│   ├── openclaw-proxy-go/          # Go 1.21 + Gin
│   └── openclaw-proxy-py/          # Python 3.10+ + FastAPI/HTTPX
├── docs/                            # 设计、网络和路线图文档
└── .github/workflows/baseline.yml  # Python、Go、Docker、J2ME、安全检查
```

## 快速开始

### 1. 启动一个代理

代理必须同时配置上游密钥和设备共享令牌；任一缺失都会拒绝启动。下面以 Python 版为例：

```bash
cd projects/openclaw-proxy/openclaw-proxy-py
uv sync --group dev
export API_KEY=replace-with-your-upstream-key
export CLIENT_TOKEN=replace-with-a-long-random-token
uv run python main.py
```

Windows PowerShell 使用 `$env:API_KEY=...` 与 `$env:CLIENT_TOKEN=...`。Go/Docker 使用方法见 [代理文档](./projects/openclaw-proxy/README.md)。

### 2. 构建 J2ME 客户端

要求 JDK 17、Apache Ant，以及仓库内的 CLDC/MIDP API JAR：

```bash
cd apps/mjga-j2me
cp config.properties.example config.properties
# 编辑代理 URL、令牌和模型；令牌必须与 CLIENT_TOKEN 一致
ant clean test dist
```

产物为 `dist/MJGA.jar` 和 `dist/MJGA.jad`。`config.properties` 被 Git 忽略，构建会把三项配置写入 MIDlet Manifest/JAD。

启动 MicroEmulator：

```bash
./run-emulator.sh
```

Windows PowerShell：

```powershell
.\run-emulator.ps1
```

模拟器脚本已具备可运行入口，但本轮尚未完成 GUI 联网验收；真机状态也仍为待验证。

## 代理配置

| 环境变量 | 必填 | 默认值 |
|---|---:|---|
| `API_KEY` | 是 | 无 |
| `CLIENT_TOKEN` | 是 | 无 |
| `API_URL` | 否 | Ark OpenAI 兼容 chat-completions URL |
| `PORT` | 否 | `8080` |
| `UPSTREAM_TIMEOUT_SECONDS` | 否 | `60` |
| `MAX_REQUEST_BYTES` | 否 | `8192` |
| `MAX_RESPONSE_BYTES` | 否 | `32768` |
| `RATE_LIMIT_PER_MINUTE` | 否 | `10` |

健康检查为 `GET /ping`；文本接口为 `POST /v1/chat/completions`，请求必须携带 `X-MJGA-Token`。

## 验证状态

本次安全文本基线已在本地验证：

- Python：20 项 pytest 通过，模块字节码编译通过；
- Go：`go test ./...` 与 `go vet ./...` 通过；
- J2ME：JSON、UTF-8 响应、历史上限三组测试通过，`ant clean test dist` 通过；
- 仓库：凭据模式、跟踪的本地配置/生成二进制和 whitespace 检查通过。

Docker 镜像构建由 CI 执行，本机未安装 Docker。MicroEmulator GUI 与 W995 真机尚无通过证据。

## 安全提示

仓库历史中曾提交过真实上游 API 密钥。即使当前源码已移除，该值仍存在于旧 Git 历史中；密钥所有者必须在上游平台撤销并轮换它。项目不会自动重写 Git 历史。

## 文档

- [J2ME 客户端](./apps/mjga-j2me/README.md)
- [代理部署](./projects/openclaw-proxy/README.md)
- [网络协议与安全边界](./docs/03-networking.md)
- [证据化路线图](./docs/05-roadmap.md)
- [安全文本基线设计](./docs/superpowers/specs/2026-09-03-safe-text-baseline-design.md)

## License

MIT License

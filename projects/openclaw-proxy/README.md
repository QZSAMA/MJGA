# openclaw-proxy - AI 反向代理服务

> 为 J2ME 客户端提供 HTTP 访问 LLM API 的代理服务
> 提供 Go 和 Python 两个版本，可以按需选择

## 📋 项目简介

openclaw-proxy 是 MJGA 项目的反向代理服务，解决 J2ME 客户端 TLS 1.0 兼容性问题。

**架构图：**
```
J2ME 客户端 (W995)
        ↓
  HTTP (plaintext)
        ↓
 openclaw-proxy ← 本项目
        ↓
  HTTPS (TLS 1.2+)
        ↓
字节跳动 / OpenAI API
```

## 🎯 核心功能

- ✅ 接收 J2ME HTTP 请求，转发到 LLM API (HTTPS)
- ✅ 设置 Authorization Bearer 头
- ✅ 原样返回响应给 J2ME
- ✅ 处理 JSON 格式请求与响应
- ✅ 终端显示调试日志

## 📦 可用版本

| 版本 | 目录 | 推荐场景 |
|------|------|---------|
| **Go** | `openclaw-proxy-go/` | 生产环境部署，单二进制文件 |
| **Python** | `openclaw-proxy-py/` | 开发调试，使用 uv 包管理 |

---

## 🚀 快速开始

### 方式一：Python 版本开发（推荐）

```bash
cd projects/openclaw-proxy/openclaw-proxy-py

# 使用 uv 安装依赖
uv sync

# 设置 API KEY (你的字节跳动 API KEY)
export API_KEY=your-api-key-here

# 启动服务
uv run python main.py
```

服务会在 `http://localhost:8080` 启动。

### 方式二：Go 版本部署（推荐生产）

```bash
cd projects/openclaw-proxy/openclaw-proxy-go

# 编译
go build -o openclaw-proxy

# 设置 API KEY 并启动
export API_KEY=your-api-key-here
./openclaw-proxy
```

### Docker 部署 (Go 版本)

```bash
cd openclaw-proxy-go
docker build -t openclaw-proxy .
docker run -d -p 8080:8080 \
  -e API_KEY=your-api-key \
  openclaw-proxy
```

## 🔧 环境变量

| 变量 | 说明 | 默认值 |
|------|------|--------|
| `API_KEY` | 字节跳动 API 密钥 | **必须设置** |
| `API_URL` | LLM API 端点 | `https://ark.cn-beijing.volces.com/api/coding/v3/chat/completions` |
| `PORT` | 监听端口 | `8080` |

## 📡 API 端点

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/v1/chat/completions` | 聊天请求转发 |

**请求格式：**
```json
{
  "model": "ark-code-latest",
  "messages": [
    {"role": "user", "content": "问题内容"}
  ]
}
```

**响应格式：**
```json
{
  "choices": [
    {
      "message": {
        "content": "AI 回答内容"
      }
    }
  ]
}
```

## 📁 目录结构

```
projects/openclaw-proxy/
├── README.md              # 本文档
├── openclaw-proxy-go/     # Go 版本
│   ├── main.go           # 主程序
│   ├── go.mod            # Go 模块定义
│   ├── go.sum            # 依赖锁定
│   ├── Dockerfile        # Docker 部署
│   └── README.md         # Go 版本详细说明
└── openclaw-proxy-py/     # Python 版本
    ├── main.py           # 主程序
    ├── pyproject.toml    # uv 项目配置
    ├── .gitignore        # Git 忽略
    └── README.md         # Python 版本详细说明
```

## 🔍 为什么需要反向代理？

W995 手机的 J2ME `HttpsConnection` 只支持 **TLS 1.0**：
- ❌ TLS 1.0 已被现代服务弃用
- ❌ 字节跳动 API 需要 TLS 1.2+
- ✅ 使用反向代理，J2ME 发 HTTP，代理转 HTTPS

**J2ME 客户端配置：**
- API URL: `http://你的代理地址:8080/v1/chat/completions`

## 📄 License

MIT License

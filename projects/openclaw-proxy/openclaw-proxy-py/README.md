# openclaw-proxy-py - Python 版本

> 基于 FastAPI 的 LLM 反向代理，使用 uv 进行包管理

## 🚀 快速开始

### 环境要求

- Python 3.10+
- [uv](https://github.com/astral-sh/uv) - 超快的包管理器

### 安装依赖

```bash
# 进入目录
cd projects/openclaw-proxy/openclaw-proxy-py

# 使用 uv 安装依赖（自动创建虚拟环境）
uv sync
```

### 启动服务

```bash
# 设置 API KEY
export API_KEY=your-api-key-here

# 可选：自定义 API URL 和端口
# export API_URL=https://ark.cn-beijing.volces.com/api/coding/v3/chat/completions
# export PORT=8080

# 启动服务
uv run python main.py
```

服务启动后会在 `http://0.0.0.0:8080` 监听。

## 🔧 环境变量

| 变量 | 说明 | 默认值 |
|------|------|--------|
| `API_KEY` | 字节跳动 API 密钥 | **必须设置** |
| `API_URL` | LLM API 端点 | `https://ark.cn-beijing.volces.com/api/coding/v3/chat/completions` |
| `PORT` | 监听端口 | `8080` |

## 📡 测试接口

```bash
# 发送测试请求
curl -X POST http://localhost:8080/v1/chat/completions \
  -H "Content-Type: application/json" \
  -d '{
    "model": "ark-code-latest",
    "messages": [{"role": "user", "content": "你好"}]
  }'
```

## 📦 技术栈

- **FastAPI** - 高性能 Web 框架
- **httpx** - 异步 HTTP 客户端
- **uv** - 包管理工具

## 📁 项目结构

```
openclaw-proxy-py/
├── main.py            # 主程序
├── pyproject.toml     # uv 项目配置
└── README.md          # 本文档
```

## 💡 开发调试

uv 会自动创建 `.venv` 虚拟环境，你可以：

```bash
# 进入虚拟环境
source .venv/bin/activate  # Linux/macOS

# 直接运行 Python
uv run python
```

## 📄 License

MIT License

# openclaw-proxy-go - Go 版本

> 高性能 LLM 反向代理，编译后生成单二进制文件，方便部署

## 🚀 快速开始

### 环境要求

- Go 1.20+

### 编译运行

```bash
# 进入目录
cd projects/openclaw-proxy/openclaw-proxy-go

# 编译
go build -o openclaw-proxy

# 设置 API KEY
export API_KEY=your-api-key-here

# 启动服务
./openclaw-proxy
```

服务启动后会在 `http://0.0.0.0:8080` 监听。

### Docker 部署

```bash
# 构建镜像
docker build -t openclaw-proxy .

# 运行容器
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

- **Gin** - Go Web 框架
- **net/http** - 标准库 HTTP 客户端
- **encoding/json** - 标准库 JSON 处理

## 📁 项目结构

```
openclaw-proxy-go/
├── main.go            # 主程序
├── go.mod             # Go 模块定义
├── go.sum             # 依赖锁定
├── Dockerfile         # Docker 部署
└── README.md          # 本文档
```

## 💡 特性

- ✅ 高性能，Go 原生并发
- ✅ 单二进制文件，部署方便
- ✅ 可编译到多种平台
- ✅ Docker 部署友好

## 📄 License

MIT License

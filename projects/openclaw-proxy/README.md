# openclaw-proxy - LLM 反向代理

> 为 J2ME 客户端提供 HTTP 访问 LLM API 的代理服务<br>
> 当前默认配置：字节跳动火山方舟

## 🎯 作用

解决 J2ME 客户端 TLS 版本兼容性问题：
- J2ME `HttpConnection` 只支持 **TLS 1.0**
- 现代 OpenAI API 要求 **TLS 1.2+**
- 本代理接收 J2ME 的明文 HTTP 请求，通过 HTTPS 转发给 OpenAI
- 将响应返回给 J2ME 客户端

## 🏗️ 架构

```
J2ME (W995) --HTTP--> openclaw-proxy --HTTPS--> OpenAI API
                   ^                      |
                   |                      v
                   \---------Response-------/
```

## ✨ 功能

- ✅ 接收 J2ME 聊天请求
- ✅ 转发给 OpenAI API
- ✅ 原样返回响应给 J2ME
- ⏳ 压缩响应减小体积适配低速网络
- ⏳ 语音合成格式转换 (text → mp3)
- ⏳ 图片尺寸压缩 (AI image → 240x320 for J2ME)

## 🚀 快速开始

### 本地运行

```bash
# 进入项目目录
cd projects/openclaw-proxy

# 设置环境变量
export API_KEY=80f1b58b-9fc6-40f5-bc0b-968181fdae12
# 默认已经配置好了：
# API_URL=https://ark.cn-beijing.volces.com/api/coding/v3
# MODEL=ark-code-latest
# PORT=8080

# 编译
go build -o openclaw-proxy

# 运行
./openclaw-proxy
```

> 默认已经配置了你的字节跳动 ark，只需要设置 `API_KEY` 即可运行

### Docker 部署

```bash
docker build -t openclaw-proxy .
docker run -d -p 8080:8080 \
  -e API_KEY=80f1b58b-9fc6-40f5-bc0b-968181fdae12 \
  openclaw-proxy
```

### 环境变量说明

| 变量 | 说明 | 默认 |
|------|------|------|
| `API_KEY` | API 密钥 (Authorization Bearer) | **必须设置** |
| `API_URL` | LLM API 端点 | `https://ark.cn-beijing.volces.com/api/v3/chat/completions` |
| `MODEL` | 模型/端点 ID | `80f1b58b-9fc6-40f5-bc0b-968181fdae12` |
| `PORT` | 监听端口 | `8080` |

> 已经配置好了你的信息：
> - **API_KEY** 需要你设置环境变量 → `export API_KEY=80f1b58b-9fc6-40f5-bc0b-968181fdae12`
> - **API_URL** = `https://ark.cn-beijing.volces.com/api/coding/v3/chat/completions`
> - **MODEL** = `ark-code-latest` (你的 endpoint ID)

### API 格式说明

字节跳动火山方舟 coding endpoint API 格式兼容 OpenAI:
- **请求**:
```json
{
  "messages": [
    {"role": "user", "content": "question"}
  ]
}
```
- **响应** (OpenAI 格式):
```json
{
  "choices": [
    {
      "message": {
        "content": "AI reply content"
      }
    }
  ]
}
```

### 测试

```bash
# 健康检查
curl http://localhost:8080/ping
# => pong

# 测试聊天
curl -X POST http://localhost:8080/v1/chat/completions \
  -H "Content-Type: application/json" \
  -d '{
    "input": "Hello!"
  }'
```

## 📝 API 接口

| 接口 | 方法 | 说明 |
|------|------|------|
| `/ping` | GET | 健康检查 |
| `/v1/chat/completions` | POST | 转发聊天请求到 OpenAI |

请求和响应格式与 OpenAI API 完全一致，J2ME 客户端可以直接使用。

## 📝 项目结构

```
projects/openclaw-proxy/
├── src/                # 源代码
├── README.md           # 本文档
└── ...
```

## 📄 许可证

MIT License

# openclaw-proxy - OpenAI 反向代理

> 为 J2ME 客户端提供 HTTP 访问 OpenAI API 的代理服务

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
export OPENAI_API_KEY=your-openai-api-key-here
export PORT=8080

# 编译
go build -o openclaw-proxy

# 运行
./openclaw-proxy
```

### Docker 部署

```bash
docker build -t openclaw-proxy .
docker run -d -p 8080:8080 \
  -e OPENAI_API_KEY=your-key \
  openclaw-proxy
```

### 测试

```bash
# 健康检查
curl http://your-vps-ip:8080/ping
# => pong

# 测试聊天
curl -X POST http://your-vps-ip:8080/v1/chat/completions \
  -H "Content-Type: application/json" \
  -d '{
    "model": "gpt-3.5-turbo",
    "messages": [{"role": "user", "content": "Hello!"}]
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

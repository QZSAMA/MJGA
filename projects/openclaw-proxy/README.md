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

## ✨ 功能规划

- [ ] 接收 J2ME 聊天请求
- [ ] 转发给 OpenAI API
- [ ] 压缩响应减小体积适配低速网络
- [ ] 语音合成格式转换 (text → mp3)
- [ ] 图片尺寸压缩 (AI image → 240x320 for J2ME)

## 🚀 快速开始

> 开发中...

## 📝 项目结构

```
projects/openclaw-proxy/
├── src/                # 源代码
├── README.md           # 本文档
└── ...
```

## 📄 许可证

MIT License

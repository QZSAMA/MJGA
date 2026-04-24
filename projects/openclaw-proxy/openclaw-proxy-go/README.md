# openclaw-proxy - Go 版本

> LLM reverse proxy for J2ME client<br>
> Single binary deployment after compilation, good for production

## 🚀 Quick Start

### Requirements
- Go 1.21+

### Install & Run

```bash
# Enter directory
cd projects/openclaw-proxy/openclaw-proxy-go

# Set environment variables
export API_KEY=80f1b58b-9fc6-40f5-bc0b-968181fdae12
# Defaults already set:
# API_URL=https://ark.cn-beijing.volces.com/api/coding/v3/chat/completions
# MODEL=ark-code-latest
# PORT=8080
# FORCE_ENGLISH=y  # Enable force English reply

# Build
go build -o openclaw-proxy

# Run
./openclaw-proxy
```

## 📋 Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `API_KEY` | API key (Authorization Bearer) | **Required** |
| `API_URL` | LLM API endpoint | `https://ark.cn-beijing.volces.com/api/coding/v3/chat/completions` |
| `MODEL` | Model/endpoint ID | `ark-code-latest` |
| `PORT` | Listen port | `8080` |
| `FORCE_ENGLISH` | Force reply in English (y/true/1 to enable) | Not set = disabled |

## ✨ Features

- ✅ Receives J2ME chat request
- ✅ Forwards to LLM API
- ✅ Returns response to J2ME
- ✅ Prints AI reply in terminal for debugging
- ✅ Supports `FORCE_ENGLISH` to fix emulator Chinese display issue

## 🐳 Docker Deployment

```bash
docker build -t openclaw-proxy .
docker run -d -p 8080:8080 \
  -e API_KEY=80f1b58b-9fc6-40f5-bc0b-968181fdae12 \
  openclaw-proxy
```

## 📝 API

Same OpenAI compatible format, J2ME client works directly.

## 📄 License

MIT License

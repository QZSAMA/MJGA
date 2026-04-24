# openclaw-proxy - Python 版本

> LLM reverse proxy for J2ME client<br>
> Python with uv dependency management

## 🚀 Quick Start

### Requirements
- Python 3.10+
- [uv](https://github.com/astral-sh/uv) for dependency management

### Install & Run

```bash
# Enter directory
cd projects/openclaw-proxy/openclaw-proxy-py

# Install dependencies with uv
uv sync

# Set environment variables
export API_KEY=80f1b58b-9fc6-40f5-bc0b-968181fdae12
# Defaults already set:
# API_URL=https://ark.cn-beijing.volces.com/api/coding/v3/chat/completions
# MODEL=ark-code-latest
# PORT=8080
# FORCE_ENGLISH=y  # Enable force English reply

# Run
uv run python main.py
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

## 📝 API

Same as Go version, OpenAI compatible format. J2ME client works directly.

## 📄 License

MIT License

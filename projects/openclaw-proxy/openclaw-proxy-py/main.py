#!/usr/bin/env python3
import os
import json
import httpx
from fastapi import FastAPI, Request
from starlette.responses import Response

app = FastAPI()

API_KEY = os.getenv("API_KEY") or "80f1b58b-9fc6-40f5-bc0b-968181fdae12"
API_URL = os.getenv("API_URL") or "https://ark.cn-beijing.volces.com/api/coding/v3/chat/completions"
MODEL = os.getenv("MODEL") or "ark-code-latest"
PORT = int(os.getenv("PORT") or "8080")
FORCE_ENGLISH = os.getenv("FORCE_ENGLISH", "").lower() in ["true", "y", "1", "yes"]


@app.get("/ping")
async def ping():
    return "pong"


@app.post("/v1/chat/completions")
async def proxy(request: Request):
    # 完整读取请求 body 并重新处理
    body_bytes = await request.body()
    body_str = body_bytes.decode("utf-8")
    print(f"\nReceived request: {body_str}")

    # 如果设置了 FORCE_ENGLISH，修改用户消息要求英文回复
    if FORCE_ENGLISH:
        try:
            data = json.loads(body_bytes)
            messages = data.get("messages", [])
            if messages and len(messages) > 0:
                last_msg = messages[-1]
                content = last_msg.get("content", "")
                last_msg["content"] = content + "\n\nPlease answer in English only."
                messages[-1] = last_msg
                data["messages"] = messages
                body_str = json.dumps(data)
        except Exception as e:
            print(f"Failed to modify request: {e}")

    # 创建转发请求
    headers = {
        "Content-Type": "application/json; charset=utf-8",
        "Authorization": f"Bearer {API_KEY}",
        "Content-Length": str(len(body_str.encode("utf-8"))),
    }

    # 发送请求
    async with httpx.AsyncClient() as client:
        response = await client.request(
            "POST",
            API_URL,
            content=body_str.encode("utf-8"),
            headers=headers,
            timeout=httpx.Timeout(60.0),
        )

    print(f"LLM API responded with status: {response.status_code}")

    # 提取并打印回复内容（方便调试）
    try:
        resp_json = response.json()
        content = extract_answer(resp_json)
        if content:
            print(f"\n🤖 AI reply: {content}\n")
    except Exception as e:
        print(f"Failed to extract answer: {e}")
    # 返回给客户端 - 直接返回完整字节，和 Go 版本一致
    # 删除 Content-Length，让 Starlette 自动计算
    resp_headers = dict(response.headers)
    resp_headers.pop("Content-Length", None)
    resp_headers.pop("content-length", None)
    
    response = Response(
        content=response.content,
        status_code=response.status_code,
        headers=resp_headers,
        media_type=response.headers.get("content-type", "application/json; charset=utf-8"),
    )
    
    return response
        

def extract_answer(resp_json):
    """从 OpenAI 格式响应中提取回复内容"""
    choices = resp_json.get("choices", [])
    if choices and len(choices) > 0:
        message = choices[0].get("message", {})
        content = message.get("content", "")
        return content
    return None


if __name__ == "__main__":
    import uvicorn

    if not API_KEY:
        raise ValueError("API_KEY environment variable is required")

    print(f"openclaw-proxy starting on :{PORT}")
    print(f"  API_URL: {API_URL}")
    print(f"  MODEL: {MODEL}")
    print(f"  FORCE_ENGLISH: {FORCE_ENGLISH}")
    print()

    uvicorn.run(app, host="0.0.0.0", port=PORT)

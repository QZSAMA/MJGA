package main

import (
	"bytes"
	"encoding/json"
	"io"
	"log"
	"net/http"
	"os"
	"strings"

	"github.com/gin-gonic/gin"
)

var (
	apiKey       = os.Getenv("API_KEY")
	apiUrl       = os.Getenv("API_URL")
	model        = os.Getenv("MODEL")
	port         = os.Getenv("PORT")
	forceEnglish = os.Getenv("FORCE_ENGLISH")
)

// openclaw-proxy - LLM 反向代理
// 解决 J2ME 客户端 TLS 1.0 兼容性问题
// J2ME -> HTTP -> openclaw-proxy -> HTTPS -> LLM API
//
// 默认配置（字节跳动 ark）：
//   API_URL: https://ark.cn-beijing.volces.com/api/coding/v3/chat/completions
//   MODEL: ark-code-latest
//   API_KEY: 需要设置为你的 API key

func main() {
	if apiKey == "" {
		apiKey = "80f1b58b-9fc6-40f5-bc0b-968181fdae12"
		// log.Fatal("API_KEY environment variable is required")
	}
	// 使用环境变量覆盖默认 URL
	if apiUrl == "" {
		apiUrl = "https://ark.cn-beijing.volces.com/api/coding/v3/chat/completions"
	}
	// 使用环境变量覆盖默认模型
	// 你的 endpoint id: ark-code-latest
	if model == "" {
		model = "ark-code-latest"
	}
	if port == "" {
		port = "8080"
	}

	r := gin.Default()

	// 健康检查
	r.GET("/ping", func(c *gin.Context) {
		c.String(http.StatusOK, "pong")
	})

	// 聊天接口 - J2ME 客户端请求聊天
	r.POST("/v1/chat/completions", proxyHandler)

	log.Printf("openclaw-proxy starting on :%s", port)
	log.Printf("  API_URL: %s", apiUrl)
	log.Printf("  MODEL: %s", model)
	log.Printf("  FORCE_ENGLISH: %s", forceEnglish)
	log.Println()

	log.Fatal(r.Run(":" + port))
}

func proxyHandler(c *gin.Context) {
	// 读取 J2ME 客户端请求
	body, err := io.ReadAll(c.Request.Body)
	if err != nil {
		log.Printf("Failed to read request: %v", err)
		c.JSON(http.StatusBadRequest, gin.H{"error": "failed to read request"})
		return
	}

	log.Printf("\nReceived request: %s\n", string(body))

	// 如果设置了 FORCE_ENGLISH，修改用户消息要求英文回复
	if forceEnglish == "true" || forceEnglish == "y" || forceEnglish == "1" || forceEnglish == "yes" {
		// 读取原始 body
		var bodyMap map[string]interface{}
		json.Unmarshal(body, &bodyMap)
		// 修改最后一条用户消息，在末尾追加要求英文回复
		if messages, ok := bodyMap["messages"].([]interface{}); ok && len(messages) > 0 {
			lastMsg := messages[len(messages)-1].(map[string]interface{})
			if content, ok := lastMsg["content"].(string); ok {
				lastMsg["content"] = content + "\n\nPlease answer in English only."
				messages[len(messages)-1] = lastMsg
				bodyMap["messages"] = messages
				// 重新序列化
				var newBody bytes.Buffer
				json.NewEncoder(&newBody).Encode(bodyMap)
				// 替换 body
				body = newBody.Bytes()
			}
		}
	}

	// 创建转发请求到 LLM API
	req, err := http.NewRequest("POST", apiUrl, bytes.NewReader(body))
	if err != nil {
		log.Printf("Failed to create proxy request: %v", err)
		c.JSON(http.StatusBadGateway, gin.H{"error": "failed to create request"})
		return
	}

	// 设置头
	req.Header.Set("Content-Type", "application/json; charset=utf-8")
	req.Header.Set("Authorization", "Bearer "+apiKey)

	// 发送请求
	client := &http.Client{}
	resp, err := client.Do(req)
	if err != nil {
		log.Printf("Failed to call LLM API: %v", err)
		c.JSON(http.StatusBadGateway, gin.H{"error": "failed to call LLM"})
		return
	}
	log.Printf("LLM API responded with status: %d", resp.StatusCode)
	defer resp.Body.Close()

	// 读取 LLM 响应
	respBody, err := io.ReadAll(resp.Body)
	if err != nil {
		log.Printf("Failed to read LLM response: %v", err)
		c.JSON(http.StatusBadGateway, gin.H{"error": "failed to read response"})
		return
	}

	respStr := string(respBody)
	// 在终端打印 AI 回复，方便调试
	log.Printf("AI response received:\n%s\n", respStr)

	// 提取并打印回复内容（方便调试）
	// 格式: {"choices":[{"message":{"content":"..."}}
	contentStart := strings.Index(respStr, "\"content\":\"")
	if contentStart != -1 {
		contentStart += len("\"content\":\"")
		contentEnd := strings.Index(respStr[contentStart:], "\"")
		if contentEnd != -1 {
			content := respStr[contentStart : contentStart+contentEnd]
			log.Printf("\n🤖 AI reply: %s\n\n", content)
		}
	}

	// 设置响应头，原样返回给 J2ME
	c.Header("Content-Type", "application/json; charset=utf-8")
	c.Status(resp.StatusCode)
	c.Writer.Write(respBody)
}

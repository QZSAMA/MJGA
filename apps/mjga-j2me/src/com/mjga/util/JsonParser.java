package com.mjga.util;

/**
 * JSON 解析工具 - 手工解析 OpenAI 聊天响应
 * 
 * 因为 J2ME 内存有限，我们只解析需要的关键字段
 * OpenAI 响应格式:
 * {
 *   "choices": [
 *     {
 *       "message": {
 *         "content": "回复文本"
 *       }
 *     }
 *   ]
 * }
 * 
 * 我们只需要提取第一个 choice 的 message.content
 */
public class JsonParser {
    
    /**
     * 从 OpenAI 响应中提取回复文本
     * 
     * @param jsonResponse 完整 JSON 响应字符串
     * @return 提取出的回复文本，失败返回 null
     */
    public static String extractChatResponse(String jsonResponse) {
        if (jsonResponse == null || jsonResponse.length() == 0) {
            return null;
        }
        
        // 找第一个 "content":" 
        int contentStart = jsonResponse.indexOf("\"content\":\"");
        if (contentStart == -1) {
            // 试试单引号或空格不同的格式
            contentStart = jsonResponse.indexOf("content\": \"");
            if (contentStart == -1) {
                return null;
            }
            contentStart += "content\": \"".length();
        } else {
            contentStart += "\"content\":\"".length();
        }
        
        // 从这里开始找下一个引号
        int contentEnd = jsonResponse.indexOf("\"", contentStart);
        if (contentEnd == -1) {
            // 试试找逗号或闭括号
            contentEnd = jsonResponse.indexOf(",", contentStart);
            if (contentEnd == -1) {
                contentEnd = jsonResponse.indexOf("}", contentStart);
                if (contentEnd == -1) {
                    return null;
                }
            }
        }
        
        // 提取内容
        String content = jsonResponse.substring(contentStart, contentEnd);
        
        // 简单处理转义
        content = content.replaceAll("\\\\\"", "\"");
        content = content.replaceAll("\\\\n", "\n");
        
        return content;
    }
    
    /**
     * 构造聊天请求 JSON
     * 
     * @param model 模型名称
     * @param userMessage 用户消息
     * @return JSON 字符串
     */
    public static String buildChatRequest(String model, String userMessage) {
        StringBuffer sb = new StringBuffer();
        sb.append("{");
        sb.append("\"model\":\"").append(model).append("\",");
        sb.append("\"messages\":[");
        sb.append("{\"role\":\"user\",\"content\":\"").append(escapeJson(userMessage)).append("\"}");
        sb.append("]");
        sb.append("}");
        return sb.toString();
    }
    
    /**
     * 简单转义 JSON 字符串
     */
    private static String escapeJson(String s) {
        if (s == null) return "";
        s = s.replace("\\", "\\\\");
        s = s.replace("\"", "\\\"");
        s = s.replace("\n", "\\n");
        s = s.replace("\r", "");
        return s;
    }
}

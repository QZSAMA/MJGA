package com.mjga.util;

/**
 * Minimal JSON helpers for the OpenAI-compatible text contract.
 *
 * The implementation deliberately avoids regular expressions and third-party
 * parsers so it remains usable on strict CLDC/MIDP devices.
 */
public final class JsonParser {
    /**
     * Keeps answers short and plain-text only for the tiny feature-phone
     * screen, which renders Markdown syntax literally.
     */
    static final String SYSTEM_PROMPT =
        "你是运行在小屏功能手机上的助手，屏幕只能显示纯文本，每行约15个汉字。"
        + "规则：1.只输出纯文本，禁止使用Markdown符号，如井号、星号、反引号、表格；"
        + "2.直接给出答案，不要寒暄，不要复述问题；"
        + "3.默认回答不超过两句话、50个汉字；需要分点时最多列3点、每点不超过15字；"
        + "只有用户明确要求详细或展开时才可以加长；"
        + "4.用户没要求代码就不要给代码；"
        + "5.需要分点时用1. 2. 3.这样的纯文本编号。";

    /** Maximum number of previous Q&A turns included in one request. */
    static final int MAX_HISTORY_TURNS = 5;
    /** Per-message character cap for history to bound the request body. */
    static final int MAX_HISTORY_MESSAGE_CHARS = 150;

    private JsonParser() {
    }

    /**
     * Extract the first string value associated with a content key.
     *
     * @param jsonResponse complete JSON response
     * @return decoded content, or null when the field/string is invalid
     */
    public static String extractChatResponse(String jsonResponse) {
        if (jsonResponse == null || jsonResponse.length() == 0) {
            return null;
        }
        int valueStart = findContentValue(jsonResponse);
        if (valueStart < 0) {
            return null;
        }
        return scanJsonString(jsonResponse, valueStart);
    }

    /**
     * Build the small chat-completions request used by the device client.
     */
    public static String buildChatRequest(String model, String userMessage) {
        return buildChatRequest(model, null, null, userMessage);
    }

    /**
     * Build a chat-completions request that carries recent conversation
     * context. Only the most recent turns are included and each history
     * message is truncated so the body stays within the proxy limit.
     *
     * @param priorQuestions previous user questions, oldest first
     * @param priorAnswers   previous assistant answers, oldest first
     * @param userMessage    the new user question
     */
    public static String buildChatRequest(
            String model,
            String[] priorQuestions,
            String[] priorAnswers,
            String userMessage) {
        StringBuffer request = new StringBuffer();
        request.append("{\"model\":\"");
        request.append(escapeJson(model));
        request.append("\",\"messages\":[{\"role\":\"system\",\"content\":\"");
        request.append(escapeJson(SYSTEM_PROMPT));
        request.append("\"}");

        if (priorQuestions != null && priorAnswers != null) {
            int turnCount = Math.min(priorQuestions.length, priorAnswers.length);
            int start = turnCount > MAX_HISTORY_TURNS
                ? turnCount - MAX_HISTORY_TURNS
                : 0;
            for (int index = start; index < turnCount; index++) {
                appendMessage(request, "user", priorQuestions[index], true);
                appendMessage(request, "assistant", priorAnswers[index], true);
            }
        }

        appendMessage(request, "user", userMessage, false);
        request.append("]}");
        return request.toString();
    }

    private static void appendMessage(
            StringBuffer request, String role, String content, boolean clip) {
        request.append(",{\"role\":\"");
        request.append(role);
        request.append("\",\"content\":\"");
        request.append(escapeJson(clip ? clipHistory(content) : safe(content)));
        request.append("\"}");
    }

    private static String safe(String content) {
        return content == null ? "" : content;
    }

    private static String clipHistory(String content) {
        if (content == null) {
            return "";
        }
        if (content.length() <= MAX_HISTORY_MESSAGE_CHARS) {
            return content;
        }
        return content.substring(0, MAX_HISTORY_MESSAGE_CHARS) + "…";
    }

    private static int findContentValue(String json) {
        int key = json.indexOf("\"content\"");
        if (key < 0) {
            return -1;
        }
        int index = key + 9;
        while (index < json.length()
                && Character.isWhitespace(json.charAt(index))) {
            index++;
        }
        if (index >= json.length() || json.charAt(index) != ':') {
            return -1;
        }
        index++;
        while (index < json.length()
                && Character.isWhitespace(json.charAt(index))) {
            index++;
        }
        if (index >= json.length() || json.charAt(index) != '"') {
            return -1;
        }
        return index + 1;
    }

    private static String scanJsonString(String json, int start) {
        StringBuffer output = new StringBuffer();
        for (int index = start; index < json.length(); index++) {
            char value = json.charAt(index);
            if (value == '"') {
                return output.toString();
            }
            if (value != '\\') {
                if (value < 0x20) {
                    return null;
                }
                output.append(value);
                continue;
            }
            index++;
            if (index >= json.length()) {
                return null;
            }
            char escaped = json.charAt(index);
            switch (escaped) {
                case '"':
                    output.append('"');
                    break;
                case '\\':
                    output.append('\\');
                    break;
                case '/':
                    output.append('/');
                    break;
                case 'b':
                    output.append('\b');
                    break;
                case 'f':
                    output.append('\f');
                    break;
                case 'n':
                    output.append('\n');
                    break;
                case 'r':
                    output.append('\r');
                    break;
                case 't':
                    output.append('\t');
                    break;
                case 'u':
                    if (index + 4 >= json.length()) {
                        return null;
                    }
                    int code = 0;
                    for (int offset = 1; offset <= 4; offset++) {
                        int hex = hexValue(json.charAt(index + offset));
                        if (hex < 0) {
                            return null;
                        }
                        code = (code << 4) | hex;
                    }
                    output.append((char) code);
                    index += 4;
                    break;
                default:
                    return null;
            }
        }
        return null;
    }

    private static int hexValue(char value) {
        if (value >= '0' && value <= '9') {
            return value - '0';
        }
        if (value >= 'a' && value <= 'f') {
            return value - 'a' + 10;
        }
        if (value >= 'A' && value <= 'F') {
            return value - 'A' + 10;
        }
        return -1;
    }

    private static String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        StringBuffer escaped = new StringBuffer();
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            switch (character) {
                case '"':
                    escaped.append("\\\"");
                    break;
                case '\\':
                    escaped.append("\\\\");
                    break;
                case '\b':
                    escaped.append("\\b");
                    break;
                case '\f':
                    escaped.append("\\f");
                    break;
                case '\n':
                    escaped.append("\\n");
                    break;
                case '\r':
                    escaped.append("\\r");
                    break;
                case '\t':
                    escaped.append("\\t");
                    break;
                default:
                    if (character < 0x20) {
                        appendUnicodeEscape(escaped, character);
                    } else {
                        escaped.append(character);
                    }
                    break;
            }
        }
        return escaped.toString();
    }

    private static void appendUnicodeEscape(StringBuffer output, char value) {
        final String digits = "0123456789abcdef";
        output.append("\\u");
        output.append(digits.charAt((value >> 12) & 0x0f));
        output.append(digits.charAt((value >> 8) & 0x0f));
        output.append(digits.charAt((value >> 4) & 0x0f));
        output.append(digits.charAt(value & 0x0f));
    }
}

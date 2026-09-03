package com.mjga.util;

/**
 * Minimal JSON helpers for the OpenAI-compatible text contract.
 *
 * The implementation deliberately avoids regular expressions and third-party
 * parsers so it remains usable on strict CLDC/MIDP devices.
 */
public final class JsonParser {
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
        StringBuffer request = new StringBuffer();
        request.append("{\"model\":\"");
        request.append(escapeJson(model));
        request.append("\",\"messages\":[{\"role\":\"user\",\"content\":\"");
        request.append(escapeJson(userMessage));
        request.append("\"}]}");
        return request.toString();
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

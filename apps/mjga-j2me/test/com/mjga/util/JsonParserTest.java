package com.mjga.util;

public final class JsonParserTest {
    private JsonParserTest() {
    }

    public static void main(String[] args) {
        assertEquals(
            "{\"model\":\"m\",\"messages\":[{\"role\":\"user\",\"content\":\"中文\\\"quote\\\"\\\\path\\nline\"}]}",
            JsonParser.buildChatRequest("m", "中文\"quote\"\\path\nline")
        );
        assertEquals(
            "中文\"quote\"\\path\nline\tA",
            JsonParser.extractChatResponse(
                "{\"choices\":[{\"message\":{\"content\":\"中文\\\"quote\\\"\\\\path\\nline\\t\\u0041\"}}]}"
            )
        );
        assertNull(
            JsonParser.extractChatResponse(
                "{\"choices\":[{\"message\":{\"content\":\"unterminated}"
            )
        );
        assertNull(JsonParser.extractChatResponse("{\"content\":\"bad\\xescape\"}"));
        assertNull(JsonParser.extractChatResponse("{\"content\":\"bad\\u12XZ\"}"));
        System.out.println("JsonParserTest: PASS");
    }

    private static void assertEquals(String expected, String actual) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError(
                "expected <" + expected + "> but was <" + actual + ">"
            );
        }
    }

    private static void assertNull(String actual) {
        if (actual != null) {
            throw new AssertionError("expected <null> but was <" + actual + "> ");
        }
    }
}

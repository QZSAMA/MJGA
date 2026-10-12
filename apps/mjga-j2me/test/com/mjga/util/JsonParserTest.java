package com.mjga.util;

public final class JsonParserTest {
    private JsonParserTest() {
    }

    public static void main(String[] args) {
        assertEquals(
            "{\"model\":\"m\",\"messages\":["
                + "{\"role\":\"system\",\"content\":\"" + JsonParser.SYSTEM_PROMPT + "\"},"
                + "{\"role\":\"user\",\"content\":\"中文\\\"quote\\\"\\\\path\\nline\"}]}",
            JsonParser.buildChatRequest("m", "中文\"quote\"\\path\nline")
        );
        assertEquals(
            "{\"model\":\"m\",\"messages\":["
                + "{\"role\":\"system\",\"content\":\"" + JsonParser.SYSTEM_PROMPT + "\"},"
                + "{\"role\":\"user\",\"content\":\"q1\"},"
                + "{\"role\":\"assistant\",\"content\":\"a1\"},"
                + "{\"role\":\"user\",\"content\":\"q2\"}]}",
            JsonParser.buildChatRequest(
                "m",
                new String[] {"q1"},
                new String[] {"a1"},
                "q2"
            )
        );
        StringBuffer longAnswer = new StringBuffer();
        for (int index = 0; index < 200; index++) {
            longAnswer.append('字');
        }
        String[] manyQuestions = new String[JsonParser.MAX_HISTORY_TURNS + 1];
        String[] manyAnswers = new String[JsonParser.MAX_HISTORY_TURNS + 1];
        for (int index = 0; index < manyQuestions.length; index++) {
            manyQuestions[index] = "old" + index;
            manyAnswers[index] = index == 0 ? longAnswer.toString() : "a" + index;
        }
        String windowed = JsonParser.buildChatRequest(
            "m", manyQuestions, manyAnswers, "now");
        assertFalse(windowed.indexOf("old0") >= 0, "oldest turn must be dropped");
        assertTrue(windowed.indexOf("old1") >= 0, "second-oldest turn is kept");
        assertTrue(windowed.indexOf("\"role\":\"user\",\"content\":\"now\"}]}") >= 0,
            "current question is last");

        String[] longQuestions = new String[] {"q-long"};
        String[] longAnswers = new String[] {longAnswer.toString()};
        String clipped = JsonParser.buildChatRequest(
            "m", longQuestions, longAnswers, "now");
        assertTrue(clipped.indexOf(longAnswer.toString()) < 0,
            "long history answer must be clipped");
        assertTrue(clipped.length() < 1000, "request stays bounded");

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

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void assertFalse(boolean condition, String message) {
        assertTrue(!condition, message);
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

package com.mjga.ui;

public final class ChatHistoryTest {
    private ChatHistoryTest() {
    }

    public static void main(String[] args) {
        ChatHistory history = new ChatHistory(10);
        for (int number = 1; number <= 11; number++) {
            history.add("q" + number, "a" + number);
        }
        assertEquals(10, history.size());
        assertEquals("q2", history.get(0).question);
        assertEquals("q11", history.get(9).question);
        System.out.println("ChatHistoryTest: PASS");
    }

    private static void assertEquals(int expected, int actual) {
        if (expected != actual) {
            throw new AssertionError(
                "expected <" + expected + "> but was <" + actual + ">"
            );
        }
    }

    private static void assertEquals(String expected, String actual) {
        if (!expected.equals(actual)) {
            throw new AssertionError(
                "expected <" + expected + "> but was <" + actual + ">"
            );
        }
    }
}

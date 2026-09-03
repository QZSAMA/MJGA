package com.mjga.network;

import java.io.ByteArrayInputStream;
import java.io.IOException;

public final class ResponseReaderTest {
    private ResponseReaderTest() {
    }

    public static void main(String[] args) throws Exception {
        assertEquals(
            "中文回复",
            ResponseReader.readUtf8(
                new ByteArrayInputStream("中文回复".getBytes("UTF-8")),
                64
            )
        );
        assertThrowsIOException(new RunnableWithIOException() {
            public void run() throws IOException {
                ResponseReader.readUtf8(
                    new ByteArrayInputStream(new byte[9]),
                    8
                );
            }
        });
        System.out.println("ResponseReaderTest: PASS");
    }

    private static void assertEquals(String expected, String actual) {
        if (!expected.equals(actual)) {
            throw new AssertionError(
                "expected <" + expected + "> but was <" + actual + ">"
            );
        }
    }

    private static void assertThrowsIOException(
            RunnableWithIOException operation) throws Exception {
        try {
            operation.run();
        } catch (IOException expected) {
            return;
        }
        throw new AssertionError("expected IOException");
    }

    private interface RunnableWithIOException {
        void run() throws IOException;
    }
}

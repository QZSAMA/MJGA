package com.mjga.network;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/** Reads a bounded HTTP response using the protocol's UTF-8 encoding. */
public final class ResponseReader {
    private static final int BUFFER_SIZE = 256;

    private ResponseReader() {
    }

    public static String readUtf8(InputStream input, int maxBytes)
            throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[BUFFER_SIZE];
        int total = 0;
        int count;
        while ((count = input.read(buffer)) != -1) {
            if (count > maxBytes - total) {
                throw new IOException("response too large");
            }
            output.write(buffer, 0, count);
            total += count;
        }
        return new String(output.toByteArray(), "UTF-8");
    }
}

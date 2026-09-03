package com.mjga.network;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import javax.microedition.io.Connector;
import javax.microedition.io.HttpConnection;

/** Bounded UTF-8 HTTP client for the MJGA proxy. */
public final class HttpClient {
    private static final int MAX_ATTEMPTS = 3;

    private final String clientToken;
    private final int maxResponseBytes;

    public HttpClient(String clientToken, int maxResponseBytes) {
        this.clientToken = clientToken == null ? "" : clientToken;
        this.maxResponseBytes = maxResponseBytes;
    }

    public HttpResponse sendPost(String url, String body) {
        final byte[] requestBytes;
        try {
            requestBytes = body.getBytes("UTF-8");
        } catch (UnsupportedEncodingException error) {
            return HttpResponse.network(HttpResponse.ERROR_NETWORK);
        }

        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            HttpConnection connection = null;
            OutputStream output = null;
            InputStream input = null;
            int statusCode = -1;
            try {
                connection = (HttpConnection) Connector.open(url);
                connection.setRequestMethod(HttpConnection.POST);
                connection.setRequestProperty(
                    "Content-Type",
                    "application/json; charset=utf-8"
                );
                connection.setRequestProperty(
                    "Content-Length",
                    Integer.toString(requestBytes.length)
                );
                connection.setRequestProperty("Connection", "close");
                connection.setRequestProperty("X-MJGA-Token", this.clientToken);

                output = connection.openOutputStream();
                output.write(requestBytes);
                output.flush();

                statusCode = connection.getResponseCode();
                try {
                    input = connection.openInputStream();
                    String responseBody = ResponseReader.readUtf8(
                        input,
                        this.maxResponseBytes
                    );
                    return HttpResponse.http(statusCode, responseBody);
                } catch (IOException readError) {
                    if ("response too large".equals(readError.getMessage())) {
                        return HttpResponse.network(
                            HttpResponse.ERROR_RESPONSE_TOO_LARGE
                        );
                    }
                    if (statusCode >= 400) {
                        return HttpResponse.http(statusCode, "");
                    }
                    return HttpResponse.network(HttpResponse.ERROR_NETWORK);
                }
            } catch (IOException error) {
                if (statusCode >= 0 || attempt == MAX_ATTEMPTS - 1) {
                    return HttpResponse.network(HttpResponse.ERROR_NETWORK);
                }
                pauseBeforeRetry(attempt);
            } finally {
                close(input);
                close(output);
                close(connection);
            }
        }
        return HttpResponse.network(HttpResponse.ERROR_NETWORK);
    }

    private static void pauseBeforeRetry(int attempt) {
        try {
            Thread.sleep(1000L * (attempt + 1));
        } catch (InterruptedException ignored) {
            // MIDP has no portable interruption recovery requirement here.
        }
    }

    private static void close(InputStream input) {
        if (input != null) {
            try {
                input.close();
            } catch (IOException ignored) {
                // Best-effort cleanup.
            }
        }
    }

    private static void close(OutputStream output) {
        if (output != null) {
            try {
                output.close();
            } catch (IOException ignored) {
                // Best-effort cleanup.
            }
        }
    }

    private static void close(HttpConnection connection) {
        if (connection != null) {
            try {
                connection.close();
            } catch (IOException ignored) {
                // Best-effort cleanup.
            }
        }
    }
}

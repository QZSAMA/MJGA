package com.mjga.network;

/** Immutable result returned by the MIDP HTTP client. */
public final class HttpResponse {
    public static final String ERROR_NETWORK = "network_failure";
    public static final String ERROR_RESPONSE_TOO_LARGE = "response_too_large";

    private final int statusCode;
    private final String body;
    private final String errorCode;

    private HttpResponse(int statusCode, String body, String errorCode) {
        this.statusCode = statusCode;
        this.body = body;
        this.errorCode = errorCode;
    }

    public static HttpResponse http(int statusCode, String body) {
        return new HttpResponse(statusCode, body, null);
    }

    public static HttpResponse network(String errorCode) {
        return new HttpResponse(-1, null, errorCode);
    }

    public int getStatusCode() {
        return this.statusCode;
    }

    public String getBody() {
        return this.body;
    }

    public String getErrorCode() {
        return this.errorCode;
    }

    public boolean isHttpResponse() {
        return this.statusCode >= 0;
    }
}

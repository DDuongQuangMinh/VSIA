package com.k1ngtle.vsia.signality.internet.web;

import java.util.LinkedHashMap;
import java.util.Map;

public record W128HttpResponse(
        int status,
        String reason,
        String contentType,
        String body,
        Map<String, String> headers
) {
    public W128HttpResponse {
        reason = reason == null ? "" : reason;
        contentType = contentType == null ? "application/octet-stream" : contentType;
        body = body == null ? "" : body;
        headers = headers == null ? Map.of() : Map.copyOf(headers);
    }

    public static W128HttpResponse text(int status, String reason, String body) {
        return new W128HttpResponse(
                status,
                reason,
                "text/plain; charset=utf-8",
                body,
                new LinkedHashMap<>()
        );
    }
}

package com.k1ngtle.vsia.phone.browser;

public record BrowserResponse(
        boolean success,
        int statusCode,
        String reason,
        String contentType,
        String body,
        String styleSheet,
        String url,
        String transport,
        String routeSummary,
        boolean openWifiSettingsSuggested
) {
    public BrowserResponse {
        reason = safe(reason);
        contentType = safe(contentType);
        body = safe(body);
        styleSheet = safe(styleSheet);
        url = safe(url);
        transport = safe(transport);
        routeSummary = safe(routeSummary);
    }

    public static BrowserResponse networkError(
            String url,
            String reason,
            String body,
            boolean openWifiSettingsSuggested
    ) {
        return new BrowserResponse(
                false,
                0,
                reason,
                "text/plain; charset=utf-8",
                body,
                "",
                url,
                "",
                "",
                openWifiSettingsSuggested
        );
    }

    public static BrowserResponse http(
            String url,
            int statusCode,
            String reason,
            String contentType,
            String body,
            String styleSheet,
            String transport,
            String routeSummary
    ) {
        return new BrowserResponse(
                statusCode >= 200 && statusCode < 400,
                statusCode,
                reason,
                contentType,
                body,
                styleSheet,
                url,
                transport,
                routeSummary,
                false
        );
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}

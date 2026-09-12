package com.k1ngtle.vsia.phone.browser;

public record BrowserRequest(
        String url,
        String method
) {
    public BrowserRequest(String url) {
        this(url, "GET");
    }
}

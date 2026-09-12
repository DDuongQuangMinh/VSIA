package com.k1ngtle.vsia.phone.browser;

import com.k1ngtle.vsia.phone.network.PhoneNetworkController;

public final class PhoneBrowser {
    private final BrowserHistory history = new BrowserHistory();

    public BrowserResponse open(String url) {
        String normalized = normalize(url);
        BrowserRequest request = new BrowserRequest(normalized);

        BrowserResponse response = PhoneNetworkController.get().request(request);

        if (response.success()) {
            history.visit(normalized);
        }

        return response;
    }

    public BrowserHistory history() {
        return history;
    }

    private String normalize(String url) {
        if (url == null || url.isBlank()) {
            return "intranet.vsia";
        }

        String value = url.trim();

        if (value.startsWith("https://")) {
            value = value.substring("https://".length());
        } else if (value.startsWith("http://")) {
            value = value.substring("http://".length());
        }

        int slash = value.indexOf('/');
        if (slash >= 0) {
            value = value.substring(0, slash);
        }

        return value.toLowerCase();
    }
}

package com.k1ngtle.vsia.phone.browser;

import java.util.Locale;

public record BrowserRequest(
        String url,
        String method
) {
    public BrowserRequest {
        url = normalize(url);
        method = method == null || method.isBlank()
                ? "GET"
                : method.trim().toUpperCase(Locale.ROOT);
    }

    public BrowserRequest(String url) {
        this(url, "GET");
    }

    public String host() {
        int slash = url.indexOf('/');
        String authority = slash >= 0 ? url.substring(0, slash) : url;

        int colon = authority.indexOf(':');
        if (colon >= 0) {
            authority = authority.substring(0, colon);
        }

        return authority.trim().toLowerCase(Locale.ROOT);
    }

    public String path() {
        int slash = url.indexOf('/');

        if (slash < 0) {
            return "/";
        }

        String path = url.substring(slash);
        return path.isBlank() ? "/" : path;
    }

    public String displayUrl() {
        return url;
    }

    public static String resolve(String baseUrl, String href) {
        BrowserRequest base = new BrowserRequest(baseUrl);
        String target = href == null ? "" : href.trim();

        if (target.isBlank()) {
            return base.url();
        }

        String lower = target.toLowerCase(Locale.ROOT);

        if (lower.startsWith("javascript:")
                || lower.startsWith("mailto:")
                || lower.startsWith("data:")
                || target.startsWith("#")) {
            return base.url();
        }

        if (lower.startsWith("http://") || lower.startsWith("https://")) {
            return new BrowserRequest(target).url();
        }

        if (target.startsWith("//")) {
            return new BrowserRequest(target.substring(2)).url();
        }

        if (target.startsWith("/")) {
            return new BrowserRequest(base.host() + target).url();
        }

        String basePath = base.path();
        int query = basePath.indexOf('?');

        if (query >= 0) {
            basePath = basePath.substring(0, query);
        }

        int lastSlash = basePath.lastIndexOf('/');
        String directory = lastSlash >= 0
                ? basePath.substring(0, lastSlash + 1)
                : "/";

        return new BrowserRequest(base.host() + directory + target).url();
    }

    private static String normalize(String raw) {
        String value = raw == null ? "" : raw.trim();

        if (value.isBlank()) {
            return "";
        }

        String lower = value.toLowerCase(Locale.ROOT);

        if (lower.startsWith("https://")) {
            value = value.substring("https://".length());
        } else if (lower.startsWith("http://")) {
            value = value.substring("http://".length());
        } else if (value.startsWith("//")) {
            value = value.substring(2);
        }

        int fragment = value.indexOf('#');
        if (fragment >= 0) {
            value = value.substring(0, fragment);
        }

        while (value.startsWith("/")) {
            value = value.substring(1);
        }

        int slash = value.indexOf('/');
        String authority = slash >= 0 ? value.substring(0, slash) : value;
        String path = slash >= 0 ? value.substring(slash) : "/";

        authority = authority.trim().toLowerCase(Locale.ROOT);

        if (path.isBlank()) {
            path = "/";
        }

        return authority + path;
    }
}

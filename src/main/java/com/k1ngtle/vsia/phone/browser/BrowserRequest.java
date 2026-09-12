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

    public String authority() {
        int slash = url.indexOf('/');

        return slash >= 0
                ? url.substring(0, slash)
                : url;
    }

    public String host() {
        String authority = authority();

        int at = authority.indexOf('@');

        String candidate =
                at >= 0
                        ? authority.substring(0, at)
                        : authority;

        int colon = candidate.indexOf(':');

        if (colon >= 0) {
            candidate = candidate.substring(0, colon);
        }

        return candidate.trim().toLowerCase(Locale.ROOT);
    }

    public String path() {
        int slash = url.indexOf('/');

        if (slash < 0) {
            return "/";
        }

        String path = url.substring(slash);

        return path.isBlank()
                ? "/"
                : path;
    }

    public String directRackIp() {
        String authority = authority();
        int at = authority.indexOf('@');

        if (at >= 0) {
            String candidate =
                    authority.substring(at + 1).trim();

            return isIpv4(candidate)
                    ? candidate
                    : "";
        }

        String host = host();

        return isIpv4(host)
                ? host
                : "";
    }

    public boolean usesDirectRack() {
        return !directRackIp().isBlank();
    }

    public boolean isPureRackAddress() {
        return isIpv4(host()) && !authority().contains("@");
    }

    public DirectRackTarget directRackTarget() {
        if (!isPureRackAddress()) {
            return null;
        }

        String remainder =
                path().startsWith("/")
                        ? path().substring(1)
                        : path();

        if (remainder.isBlank()) {
            return null;
        }

        int slash = remainder.indexOf('/');

        String targetHost =
                slash >= 0
                        ? remainder.substring(0, slash)
                        : remainder;

        String targetPath =
                slash >= 0
                        ? remainder.substring(slash)
                        : "/";

        targetHost =
                targetHost.trim().toLowerCase(Locale.ROOT);

        if (targetHost.isBlank()) {
            return null;
        }

        return new DirectRackTarget(
                directRackIp(),
                targetHost,
                targetPath.isBlank() ? "/" : targetPath
        );
    }

    public String displayUrl() {
        String path = path();

        if ("/".equals(path)) {
            return authority();
        }

        return authority() + path;
    }

    public static String resolve(
            String baseUrl,
            String href
    ) {
        BrowserRequest base =
                new BrowserRequest(baseUrl);

        String target =
                href == null
                        ? ""
                        : href.trim();

        if (target.isBlank()) {
            return base.displayUrl();
        }

        String lower =
                target.toLowerCase(Locale.ROOT);

        if (lower.startsWith("javascript:")
                || lower.startsWith("mailto:")
                || lower.startsWith("data:")
                || target.startsWith("#")) {
            return base.displayUrl();
        }

        if (lower.startsWith("https://")) {
            target = target.substring("https://".length());
        } else if (lower.startsWith("http://")) {
            target = target.substring("http://".length());
        } else if (target.startsWith("//")) {
            target = target.substring(2);
        }

        if (target.indexOf('/') > 0 && target.contains(".")) {
            int firstSlash = target.indexOf('/');
            String firstPart = target.substring(0, firstSlash);

            if (firstPart.contains(".") || firstPart.contains("@")) {
                return new BrowserRequest(target).displayUrl();
            }
        } else if (!target.startsWith("/") && (target.contains("@") || target.matches(".*\\..*"))) {
            return new BrowserRequest(target).displayUrl();
        }

        String authority = base.authority();

        if (target.startsWith("/")) {
            return new BrowserRequest(authority + target).displayUrl();
        }

        String basePath = base.path();
        int query = basePath.indexOf('?');

        if (query >= 0) {
            basePath = basePath.substring(0, query);
        }

        int lastSlash = basePath.lastIndexOf('/');
        String directory =
                lastSlash >= 0
                        ? basePath.substring(0, lastSlash + 1)
                        : "/";

        return new BrowserRequest(authority + directory + target).displayUrl();
    }

    private static String normalize(String raw) {
        String value =
                raw == null
                        ? ""
                        : raw.trim();

        if (value.isBlank()) {
            return "";
        }

        String lower =
                value.toLowerCase(Locale.ROOT);

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
        String authority =
                slash >= 0
                        ? value.substring(0, slash)
                        : value;

        String path =
                slash >= 0
                        ? value.substring(slash)
                        : "/";

        authority = authority.trim().toLowerCase(Locale.ROOT);

        if (path.isBlank()) {
            path = "/";
        }

        if ("/".equals(path)) {
            return authority;
        }

        return authority + path;
    }

    private static boolean isIpv4(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }

        String[] parts = value.split("\\.");

        if (parts.length != 4) {
            return false;
        }

        for (String part : parts) {
            try {
                int n = Integer.parseInt(part);

                if (n < 0 || n > 255) {
                    return false;
                }
            } catch (NumberFormatException ignored) {
                return false;
            }
        }

        return true;
    }

    public record DirectRackTarget(
            String rackIp,
            String host,
            String path
    ) {
    }
}

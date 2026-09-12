package com.k1ngtle.vsia.phone.browser;

import com.k1ngtle.vsia.phone.network.PhoneNetworkRoute;

import java.util.List;

public record BrowserResponse(
        boolean success,
        int statusCode,
        String title,
        List<String> lines,
        PhoneNetworkRoute route,
        boolean openWifiSettingsSuggested
) {
    public static BrowserResponse ok(String title, List<String> lines, PhoneNetworkRoute route) {
        return new BrowserResponse(true, 200, title, lines, route, false);
    }

    public static BrowserResponse networkError(String title, List<String> lines, boolean openWifiSettingsSuggested) {
        return new BrowserResponse(false, 0, title, lines, null, openWifiSettingsSuggested);
    }

    public static BrowserResponse notFound(PhoneNetworkRoute route) {
        return new BrowserResponse(
                false,
                404,
                "Safari cannot open the page",
                List.of("The requested server returned 404."),
                route,
                false
        );
    }
}

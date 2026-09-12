package com.k1ngtle.vsia.phone.browser;

import com.k1ngtle.vsia.phone.network.PhoneNetworkRoute;

import java.util.List;

public final class WebsiteRenderer {
    private WebsiteRenderer() {
    }

    public static BrowserResponse handle(BrowserRequest request, PhoneNetworkRoute route) {
        return switch (request.url()) {
            case "intranet.vsia" -> BrowserResponse.ok(
                    "VSIA Network",
                    List.of(
                            "Welcome to the internal",
                            "network.",
                            "",
                            "[Server Status: ONLINE]"
                    ),
                    route
            );

            case "status.vsia" -> BrowserResponse.ok(
                    "VSIA Status",
                    List.of(
                            "[Core Network: ONLINE]",
                            "[DNS: ONLINE]",
                            "[HTTP: ONLINE]"
                    ),
                    route
            );

            default -> BrowserResponse.notFound(route);
        };
    }
}

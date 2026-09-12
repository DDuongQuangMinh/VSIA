package com.k1ngtle.vsia.phone.browser;

import com.k1ngtle.vsia.signality.internet.provider.InternetDnsAnswer;
import com.k1ngtle.vsia.signality.internet.provider.InternetRegistrySavedData;
import com.k1ngtle.vsia.signality.internet.web.W128HttpResponse;
import com.k1ngtle.vsia.signality.internet.web.W128WebHostService;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PhoneBrowserServerService {
    private static final int MAX_STYLE_CHARACTERS = 262_144;

    private static final Pattern LINK_TAG =
            Pattern.compile("(?is)<link\\b[^>]*>");

    private static final Pattern ATTRIBUTE =
            Pattern.compile(
                    "([A-Za-z_:][-A-Za-z0-9_:.]*)\\s*=\\s*(?:\"([^\"]*)\"|'([^']*)'|([^\\s>]+))"
            );

    private PhoneBrowserServerService() {
    }

    public static ServerPage fetch(
            ServerPlayer player,
            String rawUrl,
            String transport
    ) {
        BrowserRequest request = new BrowserRequest(rawUrl);

        if (request.host().isBlank()) {
            return ServerPage.error(
                    request.url(),
                    400,
                    "Bad Request",
                    "Invalid website hostname.",
                    transport
            );
        }

        ServerLevel level = player.serverLevel();
        long now = System.currentTimeMillis();

        Optional<InternetDnsAnswer> answer =
                InternetRegistrySavedData.get(level)
                        .resolveFirst(
                                request.host(),
                                "A",
                                now
                        );

        if (answer.isEmpty()) {
            return ServerPage.error(
                    request.url(),
                    404,
                    "Server Not Found",
                    "ISP1 DNS has no A record for "
                            + request.host()
                            + ".",
                    transport
            );
        }

        String rackIp = answer.get().value();

        W128HttpResponse http =
                W128WebHostService.serve(
                        level,
                        rackIp,
                        request.host(),
                        request.path(),
                        now
                );

        if (http == null) {
            return ServerPage.error(
                    request.url(),
                    404,
                    "Not Found",
                    "No W1.28+ published website is available for this host.",
                    transport
            );
        }

        String styleSheet = "";

        if (http.status() >= 200
                && http.status() < 400
                && http.contentType()
                .toLowerCase(Locale.ROOT)
                .contains("html")) {
            styleSheet = loadStyleSheets(
                    level,
                    rackIp,
                    request,
                    http.body(),
                    now
            );
        }

        return new ServerPage(
                request.url(),
                http.status(),
                http.reason(),
                http.contentType(),
                http.body(),
                styleSheet,
                normalizeTransport(transport),
                routeSummary(transport)
        );
    }

    private static String loadStyleSheets(
            ServerLevel level,
            String rackIp,
            BrowserRequest request,
            String html,
            long now
    ) {
        List<String> hrefs = styleSheetHrefs(html);

        if (hrefs.isEmpty()) {
            return "";
        }

        StringBuilder combined = new StringBuilder();
        int loaded = 0;

        for (String href : hrefs) {
            if (loaded >= 4) {
                break;
            }

            String resolved = BrowserRequest.resolve(
                    request.url(),
                    href
            );

            BrowserRequest cssRequest = new BrowserRequest(resolved);

            if (!request.host().equals(cssRequest.host())) {
                continue;
            }

            W128HttpResponse css =
                    W128WebHostService.serve(
                            level,
                            rackIp,
                            cssRequest.host(),
                            cssRequest.path(),
                            now
                    );

            if (css == null
                    || css.status() < 200
                    || css.status() >= 400
                    || !css.contentType()
                    .toLowerCase(Locale.ROOT)
                    .contains("css")) {
                continue;
            }

            if (combined.length() > 0) {
                combined.append('\n');
            }

            int remaining =
                    MAX_STYLE_CHARACTERS
                            - combined.length();

            if (remaining <= 0) {
                break;
            }

            String body = css.body();

            if (body.length() > remaining) {
                combined.append(body, 0, remaining);
            } else {
                combined.append(body);
            }

            loaded++;
        }

        return combined.toString();
    }

    private static List<String> styleSheetHrefs(String html) {
        List<String> result = new ArrayList<>();

        Matcher matcher = LINK_TAG.matcher(
                html == null ? "" : html
        );

        while (matcher.find()) {
            String tag = matcher.group();
            String rel = attribute(tag, "rel");

            if (!rel.toLowerCase(Locale.ROOT).contains("stylesheet")) {
                continue;
            }

            String href = attribute(tag, "href");

            if (!href.isBlank()) {
                result.add(href);
            }
        }

        return result;
    }

    private static String attribute(String source, String name) {
        Matcher matcher = ATTRIBUTE.matcher(
                source == null ? "" : source
        );

        while (matcher.find()) {
            if (!matcher.group(1).equalsIgnoreCase(name)) {
                continue;
            }

            if (matcher.group(2) != null) return matcher.group(2);
            if (matcher.group(3) != null) return matcher.group(3);
            if (matcher.group(4) != null) return matcher.group(4);
        }

        return "";
    }

    private static String normalizeTransport(String transport) {
        if ("CELLULAR".equalsIgnoreCase(transport)) {
            return "CELLULAR";
        }

        return "WIFI";
    }

    private static String routeSummary(String transport) {
        if ("CELLULAR".equalsIgnoreCase(transport)) {
            return "Browser → Cellular → UE → gNB → 5G Core → UPF → DNS → HTTP";
        }

        return "Browser → Wi-Fi → 802.11 → AP → Router → DNS → HTTP";
    }

    public record ServerPage(
            String url,
            int statusCode,
            String reason,
            String contentType,
            String body,
            String styleSheet,
            String transport,
            String routeSummary
    ) {
        private static ServerPage error(
                String url,
                int statusCode,
                String reason,
                String body,
                String transport
        ) {
            return new ServerPage(
                    url,
                    statusCode,
                    reason,
                    "text/plain; charset=utf-8",
                    body,
                    "",
                    normalizeTransport(transport),
                    PhoneBrowserServerService.routeSummary(transport)
            );
        }
    }
}

package com.k1ngtle.vsia.signality.internet.web;

import com.k1ngtle.vsia.signality.internet.OSINetworkPacket;
import com.k1ngtle.vsia.signality.internet.provider.InternetDnsAnswer;
import com.k1ngtle.vsia.signality.internet.provider.InternetRegistrySavedData;
import com.k1ngtle.vsia.signality.internet.provider.InternetRegistryValidators;
import net.minecraft.server.level.ServerLevel;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class W128WebHostService {
    private W128WebHostService() {
    }

    public static W128HttpResponse serve(
            ServerLevel level,
            String rackIp,
            OSINetworkPacket request
    ) {
        if (level == null || request == null) {
            return null;
        }

        String host = extractHost(request);
        if (legacyHost(host)) {
            return null;
        }

        String path = request.payload.getString("path");
        return serve(level, rackIp, host, path, System.currentTimeMillis());
    }

    public static W128HttpResponse serve(
            ServerLevel level,
            String rackIp,
            String requestedHost,
            String requestedPath,
            long nowMillis
    ) {
        String host = W128WebRegistrySavedData.normalizeHost(requestedHost);
        if (legacyHost(host)) {
            return null;
        }

        if (!InternetRegistryValidators.validHostname(host)) {
            return error(400, "Bad Request", "Invalid HTTP Host header.");
        }

        W128WebRegistrySavedData web = W128WebRegistrySavedData.get(level);
        W128WebProject project = web.project(host).orElse(null);
        if (project == null) {
            return error(404, "Not Found", "No W1.28 virtual host is published for " + host + ".");
        }

        W128WebBuildResult authority = web.validateAuthority(level, project, nowMillis);
        if (!authority.success()) {
            return error(410, "Gone", authority.message());
        }

        if (!project.published()) {
            return error(404, "Not Found", "The website is not published.");
        }

        if (project.boundServerIp().isBlank()) {
            return error(503, "Service Unavailable", "The website has no bound Server Rack IPv4 address.");
        }

        String actualRackIp = rackIp == null ? "" : rackIp.trim();
        if (!project.boundServerIp().equals(actualRackIp)) {
            return error(
                    421,
                    "Misdirected Request",
                    "Host " + host + " is bound to " + project.boundServerIp() + ", not " + actualRackIp + "."
            );
        }

        Optional<InternetDnsAnswer> dns = InternetRegistrySavedData.get(level)
                .resolveFirst(host, "A", nowMillis);

        if (dns.isEmpty() || !actualRackIp.equals(dns.get().value())) {
            return error(421, "Misdirected Request", "ISP1 DNS no longer directs this host to this Server Rack.");
        }

        String path;
        try {
            String requested = requestedPath == null || requestedPath.isBlank() ? "/" : requestedPath;
            if (requested.endsWith("/") && !"/".equals(requested)) {
                requested = requested + "index.html";
            }
            path = W128WebRegistrySavedData.normalizePath(requested);
        } catch (IllegalArgumentException exception) {
            return error(400, "Bad Request", exception.getMessage());
        }

        // W1.30 workspace metadata is private and is never exposed by HTTP.
        if (W130Workspace.internalPath(path)) {
            return error(404, "Not Found", "File not found: " + path);
        }

        W128WebFile file = project.file(path);

        if (file == null
                && project.mode() == W128WebMode.REACT
                && !lastPathSegment(path).contains(".")) {
            file = project.file("/index.html");
        }

        if (file == null) {
            return error(404, "Not Found", "File not found: " + path);
        }

        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Server", "VSIA-W1.28");
        headers.put("ETag", etag(file.content()));
        headers.put("X-VSIA-Web-Host", project.host());
        headers.put("X-VSIA-Build-Revision", Long.toString(project.buildRevision()));
        headers.put("Content-Security-Policy", "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data:");
        headers.put(
                "Cache-Control",
                file.contentType().startsWith("text/html")
                        ? "no-cache"
                        : "public, max-age=60"
        );

        return new W128HttpResponse(
                200,
                "OK",
                file.contentType(),
                file.content(),
                headers
        );
    }

    public static String extractHost(OSINetworkPacket request) {
        if (request == null) {
            return "";
        }

        String host = request.payload.getString("host");
        if (host != null && !host.isBlank()) {
            return W128WebRegistrySavedData.normalizeHost(host);
        }

        byte[] wire = request.payload.getByteArray("request_wire");
        if (wire.length > 0) {
            String head = new String(wire, StandardCharsets.US_ASCII);
            for (String line : head.split("\\r?\\n")) {
                int colon = line.indexOf(':');
                if (colon > 0 && "host".equalsIgnoreCase(line.substring(0, colon).trim())) {
                    return W128WebRegistrySavedData.normalizeHost(line.substring(colon + 1).trim());
                }
            }
        }

        return W128WebRegistrySavedData.normalizeHost(request.targetIp);
    }

    public static byte[] responseWire(W128HttpResponse response) {
        String body = response.body();
        byte[] bodyBytes = body.getBytes(StandardCharsets.UTF_8);

        StringBuilder head = new StringBuilder()
                .append("HTTP/1.1 ")
                .append(response.status())
                .append(' ')
                .append(response.reason())
                .append("\r\n")
                .append("Content-Type: ")
                .append(response.contentType())
                .append("\r\n")
                .append("Content-Length: ")
                .append(bodyBytes.length)
                .append("\r\n");

        response.headers().forEach((name, value) ->
                head.append(name).append(": ").append(value).append("\r\n")
        );

        head.append("Connection: close\r\n\r\n");
        byte[] headBytes = head.toString().getBytes(StandardCharsets.US_ASCII);
        byte[] wire = new byte[headBytes.length + bodyBytes.length];
        System.arraycopy(headBytes, 0, wire, 0, headBytes.length);
        System.arraycopy(bodyBytes, 0, wire, headBytes.length, bodyBytes.length);
        return wire;
    }

    private static W128HttpResponse error(int status, String reason, String body) {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Server", "VSIA-W1.28");
        headers.put("Cache-Control", "no-store");
        return new W128HttpResponse(
                status,
                reason,
                "text/plain; charset=utf-8",
                body,
                headers
        );
    }

    private static boolean legacyHost(String host) {
        String normalized = W128WebRegistrySavedData.normalizeHost(host);
        if (normalized.isBlank() || InternetRegistryValidators.validIpv4(normalized)) {
            return true;
        }
        return normalized.equals("vsia-net.com") || normalized.endsWith(".vsia-net.com");
    }

    private static String lastPathSegment(String path) {
        int slash = path.lastIndexOf('/');
        return slash < 0 ? path : path.substring(slash + 1);
    }

    private static String etag(String content) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(content.getBytes(StandardCharsets.UTF_8));
            return "\"" + HexFormat.of().formatHex(digest, 0, 12) + "\"";
        } catch (Exception ignored) {
            return "\"" + Integer.toHexString(content.hashCode()).toLowerCase(Locale.ROOT) + "\"";
        }
    }
}

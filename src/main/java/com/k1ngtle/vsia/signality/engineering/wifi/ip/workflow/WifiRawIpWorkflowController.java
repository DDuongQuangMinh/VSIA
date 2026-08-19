package com.k1ngtle.vsia.signality.engineering.wifi.ip.workflow;

import java.util.Locale;

public final class WifiRawIpWorkflowController {
    public static final long PHASE_TIMEOUT_MICROS =
            8_000_000L;

    private WifiRawIpWorkflowState state =
            WifiRawIpWorkflowState.IDLE;

    private String hostname = "";
    private String path = "/";
    private String assignedIp = "";
    private String dnsServerIp = "";
    private String dnsServerMac = "";
    private String targetIp = "";
    private String targetMac = "";
    private long deadlineMicros;
    private String detail = "IDLE";

    public WifiRawIpWorkflowSnapshot snapshot() {
        return new WifiRawIpWorkflowSnapshot(
                state,
                hostname,
                path,
                assignedIp,
                dnsServerIp,
                dnsServerMac,
                targetIp,
                targetMac,
                deadlineMicros,
                detail
        );
    }

    public boolean start(
            String requestedHostname,
            String requestedPath,
            long nowMicros,
            WifiRawIpWorkflowActions actions
    ) {
        if (actions == null) {
            throw new IllegalArgumentException("actions");
        }

        String normalizedHost =
                normalizeHost(requestedHostname);

        if (normalizedHost.isBlank()) {
            fail(
                    "RAW workflow rejected: hostname is empty",
                    actions
            );
            return false;
        }

        reset();

        hostname = normalizedHost;
        path = normalizePath(requestedPath);
        state = WifiRawIpWorkflowState.DHCP;
        arm(nowMicros);

        if (!actions.startDhcp()) {
            fail(
                    "RAW workflow failed: DHCP could not start",
                    actions
            );
            return false;
        }

        announce(
                "RAW HTTP DHCP: acquiring IPv4 for "
                        + hostname,
                actions
        );

        return true;
    }

    public void onDhcpAck(
            String leasedIp,
            String resolverIp,
            long nowMicros,
            WifiRawIpWorkflowActions actions
    ) {
        if (state != WifiRawIpWorkflowState.DHCP) {
            return;
        }

        if (!usableIpv4(leasedIp)) {
            fail(
                    "RAW workflow failed: DHCP ACK has invalid lease",
                    actions
            );
            return;
        }

        if (!usableIpv4(resolverIp)) {
            fail(
                    "RAW workflow failed: DHCP ACK has invalid DNS server",
                    actions
            );
            return;
        }

        assignedIp = leasedIp;
        dnsServerIp = resolverIp;
        state = WifiRawIpWorkflowState.DNS_ARP;
        arm(nowMicros);

        if (!actions.arp(dnsServerIp)) {
            fail(
                    "RAW workflow failed: DNS-server ARP could not start",
                    actions
            );
            return;
        }

        announce(
                "RAW HTTP DNS ARP: "
                        + dnsServerIp,
                actions
        );
    }

    public void onDhcpNak(
            WifiRawIpWorkflowActions actions
    ) {
        if (state != WifiRawIpWorkflowState.DHCP) {
            return;
        }

        fail(
                "RAW workflow failed: DHCP NAK",
                actions
        );
    }

    public void onArpResolved(
            String ip,
            String mac,
            long nowMicros,
            WifiRawIpWorkflowActions actions
    ) {
        if (!usableIpv4(ip)
                || mac == null
                || mac.isBlank()) {
            return;
        }

        if (state == WifiRawIpWorkflowState.DNS_ARP
                && ip.equals(dnsServerIp)) {
            dnsServerMac = mac;
            state = WifiRawIpWorkflowState.DNS_QUERY;
            arm(nowMicros);

            if (!actions.dnsA(
                    hostname,
                    dnsServerIp,
                    dnsServerMac
            )) {
                fail(
                        "RAW workflow failed: DNS query could not start",
                        actions
                );
                return;
            }

            announce(
                    "RAW HTTP DNS query: "
                            + hostname
                            + " via "
                            + dnsServerIp,
                    actions
            );
            return;
        }

        if (state == WifiRawIpWorkflowState.TARGET_ARP
                && ip.equals(targetIp)) {
            targetMac = mac;
            startTcp(
                    nowMicros,
                    actions
            );
        }
    }

    public void onDnsResponse(
            String responseName,
            String answerIp,
            int responseCode,
            long nowMicros,
            WifiRawIpWorkflowActions actions
    ) {
        if (state != WifiRawIpWorkflowState.DNS_QUERY) {
            return;
        }

        String normalized =
                normalizeHost(responseName);

        if (!hostname.equals(normalized)) {
            return;
        }

        if (responseCode == 3) {
            fail(
                    "RAW workflow failed: DNS NXDOMAIN "
                            + hostname,
                    actions
            );
            return;
        }

        if (responseCode != 0
                || !usableIpv4(answerIp)) {
            fail(
                    "RAW workflow failed: DNS returned no usable A record",
                    actions
            );
            return;
        }

        targetIp = answerIp;

        if (targetIp.equals(dnsServerIp)
                && dnsServerMac != null
                && !dnsServerMac.isBlank()) {
            targetMac = dnsServerMac;
            startTcp(
                    nowMicros,
                    actions
            );
            return;
        }

        state = WifiRawIpWorkflowState.TARGET_ARP;
        arm(nowMicros);

        if (!actions.arp(targetIp)) {
            fail(
                    "RAW workflow failed: target ARP could not start",
                    actions
            );
            return;
        }

        announce(
                "RAW HTTP target ARP: "
                        + targetIp,
                actions
        );
    }

    public void onHttpResponse(
            int statusCode,
            long nowMicros,
            WifiRawIpWorkflowActions actions
    ) {
        if (state != WifiRawIpWorkflowState.TCP_HTTP) {
            return;
        }

        state = WifiRawIpWorkflowState.COMPLETE;
        deadlineMicros = 0L;

        detail =
                "RAW HTTP complete: "
                        + hostname
                        + " -> "
                        + targetIp
                        + " | HTTP "
                        + statusCode;

        actions.status(detail);
    }

    public void tick(
            long nowMicros,
            WifiRawIpWorkflowActions actions
    ) {
        if (!state.active()
                || deadlineMicros <= 0L
                || nowMicros <= deadlineMicros) {
            return;
        }

        fail(
                "RAW workflow timeout in "
                        + state.name(),
                actions
        );
    }

    public void clear(
            WifiRawIpWorkflowActions actions
    ) {
        reset();

        if (actions != null) {
            actions.status(
                    "RAW HTTP workflow cleared"
            );
        }
    }

    private void startTcp(
            long nowMicros,
            WifiRawIpWorkflowActions actions
    ) {
        state = WifiRawIpWorkflowState.TCP_HTTP;
        arm(nowMicros);

        if (!actions.tcpHttp(
                hostname,
                targetIp,
                targetMac,
                path
        )) {
            fail(
                    "RAW workflow failed: TCP HTTP could not start",
                    actions
            );
            return;
        }

        announce(
                "RAW HTTP TCP: "
                        + hostname
                        + " ["
                        + targetIp
                        + "]",
                actions
        );
    }

    private void arm(long nowMicros) {
        deadlineMicros =
                Math.max(0L, nowMicros)
                        + PHASE_TIMEOUT_MICROS;
    }

    private void announce(
            String value,
            WifiRawIpWorkflowActions actions
    ) {
        detail = value;
        actions.status(value);
    }

    private void fail(
            String value,
            WifiRawIpWorkflowActions actions
    ) {
        state = WifiRawIpWorkflowState.FAILED;
        deadlineMicros = 0L;
        detail = value;
        actions.status(value);
    }

    private void reset() {
        state = WifiRawIpWorkflowState.IDLE;
        hostname = "";
        path = "/";
        assignedIp = "";
        dnsServerIp = "";
        dnsServerMac = "";
        targetIp = "";
        targetMac = "";
        deadlineMicros = 0L;
        detail = "IDLE";
    }

    private static String normalizeHost(String value) {
        if (value == null) {
            return "";
        }

        String result =
                value.trim()
                        .toLowerCase(Locale.ROOT);

        while (result.endsWith(".")) {
            result =
                    result.substring(
                            0,
                            result.length() - 1
                    );
        }

        if (result.length() > 253) {
            return "";
        }

        return result;
    }

    private static String normalizePath(String value) {
        if (value == null
                || value.isBlank()) {
            return "/";
        }

        return value.startsWith("/")
                ? value
                : "/"
                + value;
    }

    private static boolean usableIpv4(String value) {
        if (value == null
                || value.isBlank()) {
            return false;
        }

        String[] parts =
                value.split("\\.");

        if (parts.length != 4) {
            return false;
        }

        try {
            for (String part : parts) {
                int octet =
                        Integer.parseInt(part);

                if (octet < 0
                        || octet > 255) {
                    return false;
                }
            }
        } catch (NumberFormatException ignored) {
            return false;
        }

        return !value.equals("0.0.0.0")
                && !value.equals("255.255.255.255");
    }
}

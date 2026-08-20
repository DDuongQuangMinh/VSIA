package com.k1ngtle.vsia.signality.engineering.firewall;

import java.util.Locale;

public record FirewallFlowKey(
        IpFamily family,
        String protocol,
        String sourceIp,
        int sourcePort,
        String destinationIp,
        int destinationPort
) {
    public FirewallFlowKey {
        if (family == null) throw new IllegalArgumentException("family");
        protocol = normalizeProtocol(protocol);
        sourceIp = requireAddress(sourceIp);
        destinationIp = requireAddress(destinationIp);
        validatePort(sourcePort);
        validatePort(destinationPort);
    }

    public FirewallFlowKey reverse() {
        return new FirewallFlowKey(
                family,
                protocol,
                destinationIp,
                destinationPort,
                sourceIp,
                sourcePort
        );
    }

    public boolean transportProtocol() {
        return protocol.equals("TCP") || protocol.equals("UDP");
    }

    private static String normalizeProtocol(String protocol) {
        if (protocol == null || protocol.isBlank()) return "IP";
        return protocol.trim().toUpperCase(Locale.ROOT);
    }

    private static String requireAddress(String address) {
        if (address == null || address.isBlank()) {
            throw new IllegalArgumentException("address");
        }
        return address.trim();
    }

    private static void validatePort(int port) {
        if (port < 0 || port > 65535) {
            throw new IllegalArgumentException("port");
        }
    }
}

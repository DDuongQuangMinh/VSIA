package com.k1ngtle.vsia.signality.engineering.firewall;

public record Nat44Mapping(
        String protocol,
        String insideLocalIp,
        int insideLocalPort,
        String insideGlobalIp,
        int insideGlobalPort,
        String outsideIp,
        int outsidePort,
        long createdMillis,
        long expiresAtMillis
) {
    public boolean expired(long nowMillis) {
        return nowMillis >= expiresAtMillis;
    }

    public boolean outboundMatches(FirewallPacketView packet) {
        return packet.family() == IpFamily.IPV4
                && protocol.equalsIgnoreCase(packet.protocol())
                && insideLocalIp.equals(packet.sourceIp())
                && insideLocalPort == packet.sourcePort()
                && outsideIp.equals(packet.destinationIp())
                && outsidePort == packet.destinationPort();
    }

    public boolean inboundMatches(FirewallPacketView packet) {
        return packet.family() == IpFamily.IPV4
                && protocol.equalsIgnoreCase(packet.protocol())
                && outsideIp.equals(packet.sourceIp())
                && outsidePort == packet.sourcePort()
                && insideGlobalIp.equals(packet.destinationIp())
                && insideGlobalPort == packet.destinationPort();
    }
}

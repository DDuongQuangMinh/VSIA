package com.k1ngtle.vsia.signality.engineering.wifi.ip.raw;

public record RawIpv4ReassemblyKey(
        String sourceIp,
        String destinationIp,
        int protocol,
        int identification
) {
}

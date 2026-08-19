package com.k1ngtle.vsia.signality.engineering.wifi.ip.router;

import com.k1ngtle.vsia.signality.engineering.wifi.ip.routing.Ipv4Prefix;

public record RouterInterface(
        String name,
        String ipv4Address,
        int prefixLength,
        String macAddress,
        boolean enabled
) {
    public RouterInterface {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Router interface name is empty");
        }
        if (!Ipv4Prefix.isUsableUnicast(ipv4Address)) {
            throw new IllegalArgumentException("Invalid router interface IPv4");
        }
        if (prefixLength < 0 || prefixLength > 32) {
            throw new IllegalArgumentException("Invalid router prefix length");
        }
        macAddress = macAddress == null ? "" : macAddress;
    }

    public String network() {
        return Ipv4Prefix.network(ipv4Address, prefixLength);
    }

    public boolean contains(String ip) {
        return Ipv4Prefix.matches(ip, ipv4Address, prefixLength);
    }
}

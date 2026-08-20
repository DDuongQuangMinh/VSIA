package com.k1ngtle.vsia.signality.engineering.wifi.ip.ipv6;

public record Ipv6DnsRecord(
        String name,
        Ipv6Address address,
        int ttlSeconds
) {
    public Ipv6DnsRecord {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("name");
        if (address == null) throw new IllegalArgumentException("address");
        if (ttlSeconds < 0) throw new IllegalArgumentException("ttlSeconds");
    }

    public String type() {
        return "AAAA";
    }
}

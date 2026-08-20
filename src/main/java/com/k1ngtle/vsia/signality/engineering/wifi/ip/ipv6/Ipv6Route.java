package com.k1ngtle.vsia.signality.engineering.wifi.ip.ipv6;

public record Ipv6Route(
        Ipv6Prefix prefix,
        Ipv6Address nextHop,
        String egressInterface,
        int metric
) {
    public Ipv6Route {
        if (prefix == null) throw new IllegalArgumentException("prefix");
        if (egressInterface == null || egressInterface.isBlank()) throw new IllegalArgumentException("egressInterface");
        if (metric < 0) throw new IllegalArgumentException("metric");
    }

    public boolean onLink() {
        return nextHop == null;
    }
}

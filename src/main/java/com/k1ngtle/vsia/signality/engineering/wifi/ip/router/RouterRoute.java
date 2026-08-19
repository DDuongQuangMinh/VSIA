package com.k1ngtle.vsia.signality.engineering.wifi.ip.router;

import com.k1ngtle.vsia.signality.engineering.wifi.ip.routing.Ipv4Prefix;

public record RouterRoute(
        String network,
        int prefixLength,
        String nextHop,
        String egressInterface,
        int metric,
        String source
) {
    public RouterRoute {
        network = Ipv4Prefix.network(network, prefixLength);
        nextHop = nextHop == null ? "" : nextHop;
        egressInterface = egressInterface == null ? "" : egressInterface;
        source = source == null ? "" : source;
        if (metric < 0) throw new IllegalArgumentException("metric");
    }

    public boolean matches(String destination) {
        return Ipv4Prefix.matches(destination, network, prefixLength);
    }

    public boolean connected() {
        return nextHop.isBlank() || "0.0.0.0".equals(nextHop);
    }
}

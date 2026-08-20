package com.k1ngtle.vsia.signality.engineering.wifi.ip.ipv6;

public final class DualStackRouteSelector {
    private DualStackRouteSelector() {
    }

    public static Family select(
            boolean ipv4Available,
            boolean ipv6Available,
            boolean hasA,
            boolean hasAaaa
    ) {
        if (ipv6Available && hasAaaa) {
            return Family.IPV6;
        }

        if (ipv4Available && hasA) {
            return Family.IPV4;
        }

        if (ipv6Available) {
            return Family.IPV6;
        }

        if (ipv4Available) {
            return Family.IPV4;
        }

        return Family.NONE;
    }

    public enum Family {
        IPV4,
        IPV6,
        NONE
    }
}

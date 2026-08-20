package com.k1ngtle.vsia.signality.engineering.firewall.w118;

public record W118DhcpLease(
        String clientMac,
        String ipv4,
        long startMillis,
        long endMillis,
        long t1Millis,
        long t2Millis
) {
    public boolean expired(long nowMillis) {
        return nowMillis >= endMillis;
    }

    public long remainingMillis(long nowMillis) {
        return Math.max(
                0L,
                endMillis - nowMillis
        );
    }
}

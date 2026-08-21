package com.k1ngtle.vsia.signality.engineering.wifi.bridge.w119;

public record W119StationEntry(
        String macAddress,
        int associationId,
        long lastSeenMillis
) {
    public boolean expired(
            long nowMillis,
            long lifetimeMillis
    ) {
        return nowMillis - lastSeenMillis >= lifetimeMillis;
    }
}

package com.k1ngtle.vsia.signality.engineering.wifi.ip;

public record WifiIpNeighbor(
        String ipAddress,
        String macAddress,
        long learnedAtMicros
) {
}

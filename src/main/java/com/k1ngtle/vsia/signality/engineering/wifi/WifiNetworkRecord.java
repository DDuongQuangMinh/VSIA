package com.k1ngtle.vsia.signality.engineering.wifi;

public record WifiNetworkRecord(
        String ssid,
        String bssid,
        String security,
        String networkProfile,
        double frequencyHz,
        long lastSeenNanos
) {
}

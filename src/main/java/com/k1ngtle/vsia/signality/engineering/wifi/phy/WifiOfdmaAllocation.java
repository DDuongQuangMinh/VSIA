package com.k1ngtle.vsia.signality.engineering.wifi.phy;

public record WifiOfdmaAllocation(
        String stationId,
        WifiResourceUnit resourceUnit,
        int toneStart,
        int toneCount,
        double weight
) {
}

package com.k1ngtle.vsia.signality.engineering.wifi.phy;

public record WifiPhyRateResult(
        double grossPhyRateBps,
        double effectivePhyRateBps,
        double symbolRateHz,
        int dataTones,
        int bitsPerSubcarrier,
        double codingRate,
        int spatialStreams,
        double activeBandwidthFraction
) {
}

package com.k1ngtle.vsia.signality.engineering.wifi.instrument;

public record WifiEngineeringSample(
        long sequence,
        double snrDb,
        double sinrDb,
        double ber,
        double fer,
        double mediumEnergyDbm,
        double phyRateBps,
        int decoderIterations,
        boolean delivered
) {
}

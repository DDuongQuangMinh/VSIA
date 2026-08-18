package com.k1ngtle.vsia.signality.engineering.wifi.phy;

public record WifiPpduEstimate(
        long psduBits,
        int ofdmSymbols,
        double preambleTimeUs,
        double dataTimeUs,
        double totalTimeUs,
        double nominalRateBps
) {
}

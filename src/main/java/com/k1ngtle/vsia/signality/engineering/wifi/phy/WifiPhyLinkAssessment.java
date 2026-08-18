package com.k1ngtle.vsia.signality.engineering.wifi.phy;

public record WifiPhyLinkAssessment(
        WifiPhyConfiguration configuration,
        WifiOfdmNumerology numerology,
        WifiMimoAssessment mimo,
        WifiPhyRateResult rate,
        double inputSinrDb,
        double dopplerHz,
        double normalizedFrequencyOffset,
        double ofdmDesiredPowerFraction,
        double ofdmIciPowerFraction,
        double effectiveSinrDb
) {
}

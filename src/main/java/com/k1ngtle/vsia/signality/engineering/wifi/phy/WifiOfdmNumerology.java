package com.k1ngtle.vsia.signality.engineering.wifi.phy;

public record WifiOfdmNumerology(
        WifiPhyGeneration generation,
        WifiChannelWidth channelWidth,
        int fftSize,
        double subcarrierSpacingHz,
        double usefulSymbolTimeUs,
        WifiGuardInterval guardInterval,
        int occupiedTones,
        int dataTones,
        int pilotTones
) {
    public double symbolTimeUs() {
        return usefulSymbolTimeUs
                + guardInterval.microseconds();
    }

    public double symbolRateHz() {
        return 1_000_000.0
                / symbolTimeUs();
    }
}

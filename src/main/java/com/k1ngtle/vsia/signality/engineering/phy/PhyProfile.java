package com.k1ngtle.vsia.signality.engineering.phy;

public record PhyProfile(
        double centerFrequencyHz,
        double bandwidthHz,
        double txPowerDbm,
        double txGainDbi,
        double rxGainDbi,
        double receiverNoiseFigureDb,
        Modulation modulation,
        CodingProfile coding,
        int spatialStreams,
        double guardEfficiency,
        double macEfficiency
) {
    public PhyProfile {
        if (centerFrequencyHz <= 0.0) {
            throw new IllegalArgumentException("centerFrequencyHz");
        }

        if (bandwidthHz <= 0.0) {
            throw new IllegalArgumentException("bandwidthHz");
        }

        if (modulation == null || coding == null) {
            throw new IllegalArgumentException("modulation/coding");
        }

        if (spatialStreams < 1) {
            throw new IllegalArgumentException("spatialStreams");
        }

        if (guardEfficiency <= 0.0 || guardEfficiency > 1.0) {
            throw new IllegalArgumentException("guardEfficiency");
        }

        if (macEfficiency <= 0.0 || macEfficiency > 1.0) {
            throw new IllegalArgumentException("macEfficiency");
        }
    }
}

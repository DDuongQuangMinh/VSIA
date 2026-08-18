package com.k1ngtle.vsia.signality.engineering.conformance;

public record RfLabScenario(
        double frequencyHz,
        double distanceMeters,
        double bandwidthHz,
        double txPowerDbm,
        double txGainDbi,
        double rxGainDbi,
        double receiverNoiseFigureDb,
        double additionalLossDb
) {
    public RfLabScenario {
        if (frequencyHz <= 0.0) {
            throw new IllegalArgumentException(
                    "frequencyHz"
            );
        }

        if (distanceMeters <= 0.0) {
            throw new IllegalArgumentException(
                    "distanceMeters"
            );
        }

        if (bandwidthHz <= 0.0) {
            throw new IllegalArgumentException(
                    "bandwidthHz"
            );
        }
    }
}

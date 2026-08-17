package com.k1ngtle.vsia.signality.internet.network;

import com.k1ngtle.vsia.signality.engineering.phy.CodingProfile;
import com.k1ngtle.vsia.signality.engineering.phy.Modulation;
import com.k1ngtle.vsia.signality.engineering.phy.PhyProfile;

public record PhyDefinition(
        Modulation modulation,
        String codingId,
        double codingRate,
        double codingGainDb,
        int spatialStreams,
        double guardEfficiency,
        double macEfficiency,
        double receiverNoiseFigureDb,
        int fftSize,
        int dataSubcarriers,
        double subcarrierSpacingHz,
        int cyclicPrefixSamples
) {
    public PhyDefinition {
        if (modulation == null) {
            throw new IllegalArgumentException("modulation");
        }

        if (codingId == null || codingId.isBlank()) {
            throw new IllegalArgumentException("codingId");
        }

        if (codingRate <= 0.0 || codingRate > 1.0) {
            throw new IllegalArgumentException("codingRate must be in (0, 1]");
        }

        if (spatialStreams < 1) {
            throw new IllegalArgumentException("spatialStreams must be >= 1");
        }

        if (guardEfficiency <= 0.0 || guardEfficiency > 1.0) {
            throw new IllegalArgumentException("guardEfficiency must be in (0, 1]");
        }

        if (macEfficiency <= 0.0 || macEfficiency > 1.0) {
            throw new IllegalArgumentException("macEfficiency must be in (0, 1]");
        }

        if (receiverNoiseFigureDb < 0.0) {
            throw new IllegalArgumentException("receiverNoiseFigureDb must be >= 0");
        }

        if (fftSize < 0) {
            throw new IllegalArgumentException("fftSize must be >= 0");
        }

        if (fftSize > 0 && Integer.bitCount(fftSize) != 1) {
            throw new IllegalArgumentException("fftSize must be 0 or a power of two");
        }

        if (dataSubcarriers < 0 || dataSubcarriers > fftSize && fftSize > 0) {
            throw new IllegalArgumentException("Invalid dataSubcarriers");
        }

        if (subcarrierSpacingHz < 0.0) {
            throw new IllegalArgumentException("subcarrierSpacingHz must be >= 0");
        }

        if (cyclicPrefixSamples < 0
                || (fftSize > 0 && cyclicPrefixSamples > fftSize)) {
            throw new IllegalArgumentException("Invalid cyclicPrefixSamples");
        }
    }

    public CodingProfile codingProfile() {
        return new CodingProfile(
                codingId,
                codingRate,
                codingGainDb
        );
    }

    public PhyProfile toRuntimeProfile(
            double centerFrequencyHz,
            double bandwidthHz,
            double transmitPowerWatts,
            double antennaGainLinear
    ) {
        return new PhyProfile(
                centerFrequencyHz,
                bandwidthHz,
                wattsToDbm(transmitPowerWatts),
                linearGainToDbi(antennaGainLinear),
                linearGainToDbi(antennaGainLinear),
                receiverNoiseFigureDb,
                modulation,
                codingProfile(),
                spatialStreams,
                guardEfficiency,
                macEfficiency
        );
    }

    public boolean usesOfdm() {
        return fftSize > 0
                && dataSubcarriers > 0
                && subcarrierSpacingHz > 0.0;
    }

    private static double wattsToDbm(double watts) {
        if (watts <= 0.0) {
            return Double.NEGATIVE_INFINITY;
        }

        return 10.0 * Math.log10(watts * 1000.0);
    }

    private static double linearGainToDbi(double linearGain) {
        if (linearGain <= 0.0) {
            return 0.0;
        }

        return 10.0 * Math.log10(linearGain);
    }
}

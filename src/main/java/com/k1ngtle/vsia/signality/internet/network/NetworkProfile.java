package com.k1ngtle.vsia.signality.internet.network;

import net.minecraft.resources.ResourceLocation;

import java.util.Arrays;

public record NetworkProfile(
        ResourceLocation id,
        NetworkKind kind,
        String displayName,
        String compatibilityGroup,
        double[] frequenciesHz,
        double defaultFrequencyHz,
        double bandwidthHz,
        double transmitPowerWatts,
        double antennaGain,
        double sensitivityWatts,
        double maximumRangeBlocks,
        String protocol,
        String security
) {
    public NetworkProfile {
        if (id == null) {
            throw new IllegalArgumentException("id");
        }
        if (kind == null) {
            throw new IllegalArgumentException("kind");
        }
        if (displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException("displayName");
        }
        if (compatibilityGroup == null || compatibilityGroup.isBlank()) {
            throw new IllegalArgumentException("compatibilityGroup");
        }
        if (frequenciesHz == null || frequenciesHz.length == 0) {
            throw new IllegalArgumentException("frequenciesHz");
        }
        frequenciesHz = Arrays.copyOf(frequenciesHz, frequenciesHz.length);
        for (double frequency : frequenciesHz) {
            if (frequency <= 0.0) {
                throw new IllegalArgumentException("All frequencies must be positive");
            }
        }
        if (defaultFrequencyHz <= 0.0) {
            throw new IllegalArgumentException("defaultFrequencyHz");
        }
        if (bandwidthHz <= 0.0) {
            throw new IllegalArgumentException("bandwidthHz");
        }
        if (transmitPowerWatts <= 0.0) {
            throw new IllegalArgumentException("transmitPowerWatts");
        }
        if (antennaGain <= 0.0) {
            throw new IllegalArgumentException("antennaGain");
        }
        if (sensitivityWatts <= 0.0) {
            throw new IllegalArgumentException("sensitivityWatts");
        }
        if (maximumRangeBlocks <= 0.0) {
            throw new IllegalArgumentException("maximumRangeBlocks");
        }
        protocol = protocol == null ? "" : protocol;
        security = security == null ? "" : security;
    }

    @Override
    public double[] frequenciesHz() {
        return Arrays.copyOf(frequenciesHz, frequenciesHz.length);
    }

    public boolean supportsFrequency(double frequencyHz) {
        double halfBandwidth = bandwidthHz / 2.0;
        for (double configured : frequenciesHz) {
            if (Math.abs(configured - frequencyHz) <= halfBandwidth) {
                return true;
            }
        }
        return false;
    }
}

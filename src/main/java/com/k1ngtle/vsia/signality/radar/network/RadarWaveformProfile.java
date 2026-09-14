package com.k1ngtle.vsia.signality.radar.network;

public record RadarWaveformProfile(
        double wavelengthMeters,
        double pulseRepetitionFrequencyHz,
        double bandwidthHz,
        int coherentPulses,
        boolean staggeredPrf
) {
    private static final double C =
            299_792_458.0;

    public RadarWaveformProfile {
        if (wavelengthMeters <= 0.0) {
            throw new IllegalArgumentException(
                    "wavelengthMeters must be > 0"
            );
        }

        if (pulseRepetitionFrequencyHz <= 0.0) {
            throw new IllegalArgumentException(
                    "pulseRepetitionFrequencyHz must be > 0"
            );
        }

        if (bandwidthHz <= 0.0) {
            throw new IllegalArgumentException(
                    "bandwidthHz must be > 0"
            );
        }

        coherentPulses =
                Math.max(
                        1,
                        coherentPulses
                );
    }

    public double rangeResolutionMeters() {
        return C
                / (
                2.0
                        * bandwidthHz
        );
    }

    public double unambiguousRangeMeters() {
        return C
                / (
                2.0
                        * pulseRepetitionFrequencyHz
        );
    }

    public double coherentProcessingIntervalSeconds() {
        return coherentPulses
                / pulseRepetitionFrequencyHz;
    }

    public double radialVelocityResolutionMps() {
        double cpi =
                coherentProcessingIntervalSeconds();

        return wavelengthMeters
                / (
                2.0
                        * cpi
        );
    }

    public double unambiguousRadialVelocityMps() {
        return wavelengthMeters
                * pulseRepetitionFrequencyHz
                / 4.0;
    }

    public double resolveRange(
            double trueRangeMeters
    ) {
        if (staggeredPrf) {
            return trueRangeMeters;
        }

        double unambiguous =
                unambiguousRangeMeters();

        if (unambiguous <= 0.0) {
            return trueRangeMeters;
        }

        return positiveModulo(
                trueRangeMeters,
                unambiguous
        );
    }

    public double resolveRadialVelocity(
            double trueClosureMps
    ) {
        if (staggeredPrf) {
            return trueClosureMps;
        }

        double vmax =
                unambiguousRadialVelocityMps();

        if (vmax <= 0.0) {
            return trueClosureMps;
        }

        double period =
                vmax
                        * 2.0;

        return positiveModulo(
                trueClosureMps + vmax,
                period
        )
                - vmax;
    }

    public static RadarWaveformProfile realisticDefault(
            double wavelengthMeters
    ) {
        return new RadarWaveformProfile(
                wavelengthMeters,
                2_000.0,
                10_000_000.0,
                32,
                true
        );
    }

    private static double positiveModulo(
            double value,
            double modulus
    ) {
        double result =
                value
                        % modulus;

        if (result < 0.0) {
            result += modulus;
        }

        return result;
    }
}

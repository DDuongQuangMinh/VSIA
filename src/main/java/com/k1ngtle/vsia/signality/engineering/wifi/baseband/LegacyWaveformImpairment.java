package com.k1ngtle.vsia.signality.engineering.wifi.baseband;

public record LegacyWaveformImpairment(
        double sampleRateHz,
        double cfoHz,
        double snrDb,
        long noiseSeed,
        int leadingZeroSamples,
        double phaseNoiseStdRadPerSample
) {
    public LegacyWaveformImpairment {
        if (sampleRateHz <= 0.0) {
            throw new IllegalArgumentException(
                    "sampleRateHz"
            );
        }

        if (leadingZeroSamples < 0) {
            throw new IllegalArgumentException(
                    "leadingZeroSamples"
            );
        }

        phaseNoiseStdRadPerSample =
                Math.max(
                        0.0,
                        phaseNoiseStdRadPerSample
                );
    }

    public LegacyWaveformImpairment(
            double sampleRateHz,
            double cfoHz,
            double snrDb,
            long noiseSeed,
            int leadingZeroSamples
    ) {
        this(
                sampleRateHz,
                cfoHz,
                snrDb,
                noiseSeed,
                leadingZeroSamples,
                0.0
        );
    }

    public static LegacyWaveformImpairment clean() {
        return new LegacyWaveformImpairment(
                20_000_000.0,
                0.0,
                Double.POSITIVE_INFINITY,
                1L,
                0,
                0.0
        );
    }
}

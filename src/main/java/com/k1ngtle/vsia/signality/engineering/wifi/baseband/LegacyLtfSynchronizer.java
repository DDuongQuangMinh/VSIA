package com.k1ngtle.vsia.signality.engineering.wifi.baseband;

import com.k1ngtle.vsia.signality.engineering.math.Complex;

public final class LegacyLtfSynchronizer {
    private LegacyLtfSynchronizer() {
    }

    public static int refinePacketStart(
            Complex[] samples,
            int coarsePacketStart
    ) {
        Complex[] ltf =
                LegacyTrainingFields.ltfTimeDomain();

        Complex[] reference =
                new Complex[
                        LegacyTrainingFields.LTF_SYMBOL_SAMPLES
                        ];

        System.arraycopy(
                ltf,
                LegacyTrainingFields.LTF_CP_SAMPLES,
                reference,
                0,
                reference.length
        );

        int predictedLtf =
                coarsePacketStart
                        + LegacyTrainingFields.STF_SAMPLES;

        int searchLow =
                Math.max(
                        0,
                        predictedLtf - 24
                );

        int searchHigh =
                Math.min(
                        samples.length
                                - LegacyTrainingFields.LTF_SAMPLES,
                        predictedLtf + 24
                );

        int bestLtf =
                predictedLtf;

        double bestMetric =
                Double.NEGATIVE_INFINITY;

        for (int candidate = searchLow;
             candidate <= searchHigh;
             candidate++) {
            double metric =
                    correlationMetric(
                            samples,
                            candidate
                                    + LegacyTrainingFields.LTF_CP_SAMPLES,
                            reference
                    )
                            + correlationMetric(
                            samples,
                            candidate
                                    + LegacyTrainingFields.LTF_CP_SAMPLES
                                    + LegacyTrainingFields.LTF_SYMBOL_SAMPLES,
                            reference
                    );

            if (metric > bestMetric) {
                bestMetric =
                        metric;

                bestLtf =
                        candidate;
            }
        }

        return bestLtf
                - LegacyTrainingFields.STF_SAMPLES;
    }

    private static double correlationMetric(
            Complex[] samples,
            int start,
            Complex[] reference
    ) {
        Complex correlation =
                Complex.ZERO;

        double energySamples =
                0.0;

        double energyReference =
                0.0;

        for (int i = 0;
             i < reference.length;
             i++) {
            Complex observed =
                    samples[start + i];

            Complex expected =
                    reference[i];

            correlation =
                    correlation.add(
                            WifiComplexMath.conjugate(
                                    expected
                            ).multiply(
                                    observed
                            )
                    );

            energySamples +=
                    observed.magnitudeSquared();

            energyReference +=
                    expected.magnitudeSquared();
        }

        return WifiComplexMath.magnitude(
                correlation
        )
                / Math.sqrt(
                Math.max(
                        1.0E-30,
                        energySamples
                                * energyReference
                )
        );
    }
}

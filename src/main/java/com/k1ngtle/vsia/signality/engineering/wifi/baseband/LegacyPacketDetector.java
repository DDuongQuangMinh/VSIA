package com.k1ngtle.vsia.signality.engineering.wifi.baseband;

import com.k1ngtle.vsia.signality.engineering.math.Complex;

public final class LegacyPacketDetector {
    public static final int LAG =
            16;

    public static final int WINDOW =
            64;

    private LegacyPacketDetector() {
    }

    public static LegacyPacketDetection detect(
            Complex[] samples
    ) {
        if (samples.length
                < LAG + WINDOW) {
            throw new IllegalArgumentException(
                    "Not enough samples for STF detection"
            );
        }

        int maxStart =
                samples.length
                        - LAG
                        - WINDOW;

        double[] metrics =
                new double[
                        maxStart + 1
                        ];

        double bestMetric =
                Double.NEGATIVE_INFINITY;

        int bestIndex =
                0;

        for (int start = 0;
             start <= maxStart;
             start++) {
            Complex correlation =
                    Complex.ZERO;

            double energyA =
                    0.0;

            double energyB =
                    0.0;

            for (int n = 0;
                 n < WINDOW;
                 n++) {
                Complex a =
                        samples[start + n];

                Complex b =
                        samples[
                                start
                                        + n
                                        + LAG
                                ];

                correlation =
                        correlation.add(
                                WifiComplexMath.conjugate(
                                        a
                                ).multiply(
                                        b
                                )
                        );

                energyA +=
                        a.magnitudeSquared();

                energyB +=
                        b.magnitudeSquared();
            }

            double denominator =
                    Math.sqrt(
                            Math.max(
                                    1.0E-30,
                                    energyA
                                            * energyB
                            )
                    );

            double metric =
                    WifiComplexMath.magnitude(
                            correlation
                    )
                            / denominator;

            if (energyA < 1.0E-18
                    || energyB < 1.0E-18) {
                metric =
                        0.0;
            }

            metrics[start] =
                    metric;

            if (metric > bestMetric) {
                bestMetric =
                        metric;

                bestIndex =
                        start;
            }
        }

        double threshold =
                Math.max(
                        0.85,
                        bestMetric * 0.97
                );

        int searchStart =
                Math.max(
                        0,
                        bestIndex - 128
                );

        int refined =
                bestIndex;

        for (int start = searchStart;
             start <= bestIndex;
             start++) {
            if (metrics[start] >= threshold) {
                refined =
                        start;
                break;
            }
        }

        return new LegacyPacketDetection(
                refined,
                metrics[refined]
        );
    }
}

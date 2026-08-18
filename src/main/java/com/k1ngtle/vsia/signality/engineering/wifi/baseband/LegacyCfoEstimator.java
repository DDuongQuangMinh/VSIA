package com.k1ngtle.vsia.signality.engineering.wifi.baseband;

import com.k1ngtle.vsia.signality.engineering.math.Complex;

public final class LegacyCfoEstimator {
    private LegacyCfoEstimator() {
    }

    public static double estimateHz(
            Complex[] samples,
            int start,
            double sampleRateHz
    ) {
        Complex sum =
                Complex.ZERO;

        int count =
                Math.min(
                        96,
                        samples.length
                                - start
                                - LegacyPacketDetector.LAG
                );

        if (count <= 0) {
            throw new IllegalArgumentException(
                    "Not enough samples for CFO estimation"
            );
        }

        for (int i = 0;
             i < count;
             i++) {
            Complex a =
                    samples[
                            start + i
                            ];

            Complex b =
                    samples[
                            start
                                    + i
                                    + LegacyPacketDetector.LAG
                            ];

            sum =
                    sum.add(
                            WifiComplexMath.conjugate(
                                    a
                            ).multiply(
                                    b
                            )
                    );
        }

        double phase =
                WifiComplexMath.phase(
                        sum
                );

        return phase
                * sampleRateHz
                / (
                2.0
                        * Math.PI
                        * LegacyPacketDetector.LAG
        );
    }

    public static Complex[] correct(
            Complex[] samples,
            double cfoHz,
            double sampleRateHz,
            int referenceIndex
    ) {
        Complex[] output =
                new Complex[
                        samples.length
                        ];

        for (int i = 0;
             i < samples.length;
             i++) {
            double phase =
                    -2.0
                            * Math.PI
                            * cfoHz
                            * (
                            i - referenceIndex
                    )
                            / sampleRateHz;

            output[i] =
                    samples[i]
                            .multiply(
                                    new Complex(
                                            Math.cos(
                                                    phase
                                            ),
                                            Math.sin(
                                                    phase
                                            )
                                    )
                            );
        }

        return output;
    }
}

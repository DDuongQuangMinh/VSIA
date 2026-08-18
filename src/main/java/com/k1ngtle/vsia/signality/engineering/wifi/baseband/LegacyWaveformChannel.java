package com.k1ngtle.vsia.signality.engineering.wifi.baseband;

import com.k1ngtle.vsia.signality.engineering.math.Complex;

import java.util.Random;

public final class LegacyWaveformChannel {
    private LegacyWaveformChannel() {
    }

    public static Complex[] apply(
            Complex[] input,
            LegacyWaveformImpairment impairment
    ) {
        int offset =
                impairment.leadingZeroSamples();

        Complex[] output =
                new Complex[
                        offset + input.length
                        ];

        for (int i = 0;
             i < offset;
             i++) {
            output[i] =
                    Complex.ZERO;
        }

        double averagePower =
                averagePower(
                        input
                );

        double noisePower =
                Double.isFinite(
                        impairment.snrDb()
                )
                        ? averagePower
                        / Math.pow(
                        10.0,
                        impairment.snrDb()
                                / 10.0
                )
                        : 0.0;

        double sigma =
                Math.sqrt(
                        Math.max(
                                0.0,
                                noisePower
                        )
                                / 2.0
                );

        Random random =
                new Random(
                        impairment.noiseSeed()
                );

        double phaseNoise =
                0.0;

        for (int i = 0;
             i < input.length;
             i++) {
            if (impairment.phaseNoiseStdRadPerSample()
                    > 0.0) {
                phaseNoise +=
                        random.nextGaussian()
                                * impairment
                                .phaseNoiseStdRadPerSample();
            }

            double phase =
                    2.0
                            * Math.PI
                            * impairment.cfoHz()
                            * i
                            / impairment.sampleRateHz()
                            + phaseNoise;

            Complex rotation =
                    new Complex(
                            Math.cos(
                                    phase
                            ),
                            Math.sin(
                                    phase
                            )
                    );

            Complex shifted =
                    input[i]
                            .multiply(
                                    rotation
                            );

            double noiseRe =
                    sigma == 0.0
                            ? 0.0
                            : random.nextGaussian()
                            * sigma;

            double noiseIm =
                    sigma == 0.0
                            ? 0.0
                            : random.nextGaussian()
                            * sigma;

            output[offset + i] =
                    shifted.add(
                            new Complex(
                                    noiseRe,
                                    noiseIm
                            )
                    );
        }

        return output;
    }

    private static double averagePower(
            Complex[] values
    ) {
        if (values.length == 0) {
            return 0.0;
        }

        double sum =
                0.0;

        for (Complex value : values) {
            sum +=
                    value.magnitudeSquared();
        }

        return sum
                / values.length;
    }
}

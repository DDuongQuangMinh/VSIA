package com.k1ngtle.vsia.signality.engineering.wifi.baseband;

import com.k1ngtle.vsia.signality.engineering.math.Complex;
import com.k1ngtle.vsia.signality.engineering.math.Fft;

public final class LegacyChannelEstimator {
    private LegacyChannelEstimator() {
    }

    public static LegacyChannelEstimate estimateFromLtf(
            Complex[] samples,
            int ltfStart
    ) {
        int symbol1Start =
                ltfStart
                        + LegacyTrainingFields.LTF_CP_SAMPLES;

        int symbol2Start =
                symbol1Start
                        + LegacyTrainingFields.LTF_SYMBOL_SAMPLES;

        if (symbol2Start
                + LegacyTrainingFields.LTF_SYMBOL_SAMPLES
                > samples.length) {
            throw new IllegalArgumentException(
                    "L-LTF exceeds sample buffer"
            );
        }

        Complex[] first =
                new Complex[
                        LegacyTrainingFields.LTF_SYMBOL_SAMPLES
                        ];

        Complex[] second =
                new Complex[
                        LegacyTrainingFields.LTF_SYMBOL_SAMPLES
                        ];

        System.arraycopy(
                samples,
                symbol1Start,
                first,
                0,
                first.length
        );

        System.arraycopy(
                samples,
                symbol2Start,
                second,
                0,
                second.length
        );

        Complex[] fftA =
                Fft.fft(
                        first
                );

        Complex[] fftB =
                Fft.fft(
                        second
                );

        Complex[] reference =
                LegacyTrainingFields.ltfFrequency();

        Complex[] response =
                new Complex[
                        reference.length
                        ];

        double noiseAccumulator =
                0.0;

        int noiseCount =
                0;

        for (int i = 0;
             i < reference.length;
             i++) {
            if (reference[i].magnitudeSquared()
                    < 1.0E-18) {
                response[i] =
                        Complex.ZERO;

                continue;
            }

            Complex average =
                    fftA[i]
                            .add(
                                    fftB[i]
                            )
                            .scale(
                                    0.5
                            );

            Complex difference =
                    fftA[i]
                            .subtract(
                                    fftB[i]
                            );

            noiseAccumulator +=
                    0.25
                            * difference
                            .magnitudeSquared();

            noiseCount++;

            response[i] =
                    WifiComplexMath.divide(
                            average,
                            reference[i]
                    );
        }

        return new LegacyChannelEstimate(
                response,
                noiseCount == 0
                        ? 1.0E-12
                        : noiseAccumulator
                        / noiseCount
        );
    }
}

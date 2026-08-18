package com.k1ngtle.vsia.signality.engineering.wifi.baseband;

import com.k1ngtle.vsia.signality.engineering.math.Complex;
import com.k1ngtle.vsia.signality.engineering.math.Fft;

import java.util.Arrays;

public final class LegacyTrainingFields {
    public static final int FFT_SIZE =
            64;

    public static final int SHORT_PERIOD_SAMPLES =
            16;

    public static final int SHORT_REPETITIONS =
            10;

    public static final int STF_SAMPLES =
            SHORT_PERIOD_SAMPLES
                    * SHORT_REPETITIONS;

    public static final int LTF_CP_SAMPLES =
            32;

    public static final int LTF_SYMBOL_SAMPLES =
            64;

    public static final int LTF_SAMPLES =
            LTF_CP_SAMPLES
                    + 2
                    * LTF_SYMBOL_SAMPLES;

    private static final Complex[] STF_FREQUENCY =
            buildStfFrequency();

    private static final Complex[] LTF_FREQUENCY =
            buildLtfFrequency();

    private static final Complex[] STF_TIME =
            buildStfTime();

    private static final Complex[] LTF_TIME =
            buildLtfTime();

    private LegacyTrainingFields() {
    }

    public static Complex[] stfFrequency() {
        return STF_FREQUENCY.clone();
    }

    public static Complex[] ltfFrequency() {
        return LTF_FREQUENCY.clone();
    }

    public static Complex[] stfTimeDomain() {
        return STF_TIME.clone();
    }

    public static Complex[] ltfTimeDomain() {
        return LTF_TIME.clone();
    }

    private static Complex[] buildStfFrequency() {
        Complex[] bins =
                new Complex[
                        FFT_SIZE
                        ];

        Arrays.fill(
                bins,
                Complex.ZERO
        );

        int[] carriers =
                new int[] {
                        -24,
                        -20,
                        -16,
                        -12,
                        -8,
                        -4,
                        4,
                        8,
                        12,
                        16,
                        20,
                        24
                };

        int[] re =
                new int[] {
                        1,
                        -1,
                        1,
                        -1,
                        -1,
                        1,
                        -1,
                        -1,
                        1,
                        1,
                        1,
                        1
                };

        int[] im =
                new int[] {
                        1,
                        1,
                        -1,
                        -1,
                        1,
                        1,
                        1,
                        -1,
                        1,
                        -1,
                        -1,
                        1
                };

        double scale =
                Math.sqrt(
                        13.0 / 6.0
                );

        for (int i = 0;
             i < carriers.length;
             i++) {
            bins[bin(
                    carriers[i]
            )] =
                    new Complex(
                            re[i] * scale,
                            im[i] * scale
                    );
        }

        return bins;
    }

    private static Complex[] buildLtfFrequency() {
        Complex[] bins =
                new Complex[
                        FFT_SIZE
                        ];

        Arrays.fill(
                bins,
                Complex.ZERO
        );

        int[] sequence =
                new int[] {
                        1, 1, -1, -1, 1, 1, -1, 1,
                        -1, 1, 1, 1, 1, 1, 1, -1,
                        -1, 1, 1, -1, 1, -1, 1, 1,
                        1, 1,
                        1,
                        -1, -1, 1, 1, -1, 1, -1,
                        1, -1, -1, -1, -1, -1, 1, 1,
                        -1, -1, 1, -1, 1, -1, 1, 1,
                        1, 1
                };

        int cursor =
                0;

        for (int k = -26;
             k <= 26;
             k++) {
            if (k == 0) {
                continue;
            }

            bins[bin(
                    k
            )] =
                    new Complex(
                            sequence[cursor++],
                            0.0
                    );
        }

        return bins;
    }

    private static Complex[] buildStfTime() {
        Complex[] oneLong =
                Fft.ifft(
                        STF_FREQUENCY
                );

        Complex[] result =
                new Complex[
                        STF_SAMPLES
                        ];

        for (int repetition = 0;
             repetition < SHORT_REPETITIONS;
             repetition++) {
            System.arraycopy(
                    oneLong,
                    0,
                    result,
                    repetition
                            * SHORT_PERIOD_SAMPLES,
                    SHORT_PERIOD_SAMPLES
            );
        }

        return result;
    }

    private static Complex[] buildLtfTime() {
        Complex[] symbol =
                Fft.ifft(
                        LTF_FREQUENCY
                );

        Complex[] result =
                new Complex[
                        LTF_SAMPLES
                        ];

        System.arraycopy(
                symbol,
                symbol.length - LTF_CP_SAMPLES,
                result,
                0,
                LTF_CP_SAMPLES
        );

        System.arraycopy(
                symbol,
                0,
                result,
                LTF_CP_SAMPLES,
                symbol.length
        );

        System.arraycopy(
                symbol,
                0,
                result,
                LTF_CP_SAMPLES
                        + symbol.length,
                symbol.length
        );

        return result;
    }

    private static int bin(
            int signedSubcarrier
    ) {
        return (
                signedSubcarrier
                        + FFT_SIZE
        )
                % FFT_SIZE;
    }
}

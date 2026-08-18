package com.k1ngtle.vsia.signality.engineering.wifi.baseband;

import java.util.Arrays;

public final class WifiSoftViterbiDecoder {
    private static final int STATES =
            64;

    private WifiSoftViterbiDecoder() {
    }

    public static int[] decode(
            double[] puncturedLlrs,
            int inputBitCount,
            BccCodeRate rate
    ) {
        double[] received =
                depuncture(
                        puncturedLlrs,
                        inputBitCount * 2,
                        rate
                );

        double[] previousMetric =
                new double[
                        STATES
                        ];

        double[] currentMetric =
                new double[
                        STATES
                        ];

        Arrays.fill(
                previousMetric,
                Double.POSITIVE_INFINITY
        );

        previousMetric[0] =
                0.0;

        byte[][] previousState =
                new byte[
                        inputBitCount
                        ][
                        STATES
                        ];

        byte[][] decidedBit =
                new byte[
                        inputBitCount
                        ][
                        STATES
                        ];

        for (int time = 0;
             time < inputBitCount;
             time++) {
            Arrays.fill(
                    currentMetric,
                    Double.POSITIVE_INFINITY
            );

            double llrA =
                    received[time * 2];

            double llrB =
                    received[
                            time * 2 + 1
                            ];

            for (int state = 0;
                 state < STATES;
                 state++) {
                double base =
                        previousMetric[state];

                if (!Double.isFinite(
                        base
                )) {
                    continue;
                }

                for (int input = 0;
                     input <= 1;
                     input++) {
                    int register =
                            (
                                    (state << 1)
                                            | input
                            )
                                    & 0x7F;

                    int next =
                            register & 0x3F;

                    int a =
                            WifiBccEncoder.parity(
                                    register
                                            & WifiBccEncoder.GENERATOR_A_OCTAL
                            );

                    int b =
                            WifiBccEncoder.parity(
                                    register
                                            & WifiBccEncoder.GENERATOR_B_OCTAL
                            );

                    double metric =
                            base
                                    + bitCost(
                                    llrA,
                                    a
                            )
                                    + bitCost(
                                    llrB,
                                    b
                            );

                    if (metric
                            < currentMetric[next]) {
                        currentMetric[next] =
                                metric;

                        previousState[time][next] =
                                (byte) state;

                        decidedBit[time][next] =
                                (byte) input;
                    }
                }
            }

            double[] swap =
                    previousMetric;

            previousMetric =
                    currentMetric;

            currentMetric =
                    swap;
        }

        int state =
                bestState(
                        previousMetric
                );

        int[] decoded =
                new int[
                        inputBitCount
                        ];

        for (int time = inputBitCount - 1;
             time >= 0;
             time--) {
            decoded[time] =
                    decidedBit[time][state]
                            & 1;

            state =
                    previousState[time][state]
                            & 0x3F;
        }

        return decoded;
    }

    private static double bitCost(
            double llr,
            int expected
    ) {
        if (Double.isNaN(
                llr
        )) {
            return 0.0;
        }

        double signed =
                expected == 1
                        ? llr
                        : -llr;

        if (signed > 40.0) {
            return Math.exp(
                    -signed
            );
        }

        if (signed < -40.0) {
            return -signed;
        }

        return Math.log1p(
                Math.exp(
                        -signed
                )
        );
    }

    private static double[] depuncture(
            double[] input,
            int motherLength,
            BccCodeRate rate
    ) {
        double[] output =
                new double[
                        motherLength
                        ];

        Arrays.fill(
                output,
                Double.NaN
        );

        int[] pattern =
                rate.puncturePattern();

        int cursor =
                0;

        for (int i = 0;
             i < motherLength;
             i++) {
            if (pattern[i % pattern.length] == 0) {
                continue;
            }

            if (cursor >= input.length) {
                throw new IllegalArgumentException(
                        "Soft punctured stream too short"
                );
            }

            output[i] =
                    input[cursor++];
        }

        if (cursor != input.length) {
            throw new IllegalArgumentException(
                    "Soft punctured stream too long"
            );
        }

        return output;
    }

    private static int bestState(
            double[] metric
    ) {
        int best =
                0;

        for (int i = 1;
             i < metric.length;
             i++) {
            if (metric[i]
                    < metric[best]) {
                best =
                        i;
            }
        }

        return best;
    }
}

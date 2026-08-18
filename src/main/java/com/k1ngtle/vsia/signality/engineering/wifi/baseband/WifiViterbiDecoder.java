package com.k1ngtle.vsia.signality.engineering.wifi.baseband;

import java.util.Arrays;

public final class WifiViterbiDecoder {
    private static final int STATES =
            64;

    private static final int INF =
            1_000_000_000;

    private WifiViterbiDecoder() {
    }

    public static int[] decode(
            int[] puncturedBits,
            int inputBitCount,
            BccCodeRate rate
    ) {
        if (inputBitCount < 0) {
            throw new IllegalArgumentException(
                    "inputBitCount"
            );
        }

        int[] received =
                WifiBccPuncturer
                        .depunctureToErasures(
                                puncturedBits,
                                inputBitCount * 2,
                                rate
                        );

        int[] previousMetric =
                new int[
                        STATES
                        ];

        int[] currentMetric =
                new int[
                        STATES
                        ];

        Arrays.fill(
                previousMetric,
                INF
        );

        previousMetric[0] =
                0;

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
                    INF
            );

            int rxA =
                    received[time * 2];

            int rxB =
                    received[
                            time * 2 + 1
                            ];

            for (int state = 0;
                 state < STATES;
                 state++) {
                int baseMetric =
                        previousMetric[state];

                if (baseMetric >= INF) {
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

                    int nextState =
                            register
                                    & 0x3F;

                    int expectedA =
                            WifiBccEncoder.parity(
                                    register
                                            & WifiBccEncoder
                                            .GENERATOR_A_OCTAL
                            );

                    int expectedB =
                            WifiBccEncoder.parity(
                                    register
                                            & WifiBccEncoder
                                            .GENERATOR_B_OCTAL
                            );

                    int branch =
                            mismatch(
                                    rxA,
                                    expectedA
                            )
                                    + mismatch(
                                    rxB,
                                    expectedB
                            );

                    int metric =
                            baseMetric
                                    + branch;

                    if (metric
                            < currentMetric[nextState]) {
                        currentMetric[nextState] =
                                metric;

                        previousState[time][nextState] =
                                (byte) state;

                        decidedBit[time][nextState] =
                                (byte) input;
                    }
                }
            }

            int[] swap =
                    previousMetric;

            previousMetric =
                    currentMetric;

            currentMetric =
                    swap;
        }

        int finalState =
                bestState(
                        previousMetric
                );

        int[] decoded =
                new int[
                        inputBitCount
                        ];

        int state =
                finalState;

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

    private static int mismatch(
            int received,
            int expected
    ) {
        if (received < 0) {
            return 0;
        }

        return received == expected
                ? 0
                : 1;
    }

    private static int bestState(
            int[] metrics
    ) {
        int best =
                0;

        for (int i = 1;
             i < metrics.length;
             i++) {
            if (metrics[i]
                    < metrics[best]) {
                best =
                        i;
            }
        }

        return best;
    }
}

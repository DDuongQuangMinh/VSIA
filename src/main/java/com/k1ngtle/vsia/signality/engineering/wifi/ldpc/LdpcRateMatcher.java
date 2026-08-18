package com.k1ngtle.vsia.signality.engineering.wifi.ldpc;

import java.util.Arrays;

public final class LdpcRateMatcher {
    private LdpcRateMatcher() {
    }

    public static int[] transmitBits(
            int[] motherCodeword,
            LdpcRateMatchPlan plan
    ) {
        if (motherCodeword.length
                != plan.motherCodewordBits()) {
            throw new IllegalArgumentException(
                    "mother codeword length mismatch"
            );
        }

        int firstTransmitted =
                plan.shortenedBits();

        int lastExclusive =
                motherCodeword.length
                        - plan.puncturedBits();

        if (firstTransmitted >= lastExclusive) {
            throw new IllegalArgumentException(
                    "rate matching removes entire codeword"
            );
        }

        return Arrays.copyOfRange(
                motherCodeword,
                firstTransmitted,
                lastExclusive
        );
    }

    public static double[] restoreLlrs(
            double[] transmittedLlrs,
            LdpcRateMatchPlan plan
    ) {
        if (transmittedLlrs.length
                != plan.transmittedBits()) {
            throw new IllegalArgumentException(
                    "transmitted LLR length mismatch"
            );
        }

        double[] mother =
                new double[
                        plan.motherCodewordBits()
                        ];

        int cursor =
                0;

        for (int i = 0;
             i < mother.length;
             i++) {
            if (i < plan.shortenedBits()) {
                mother[i] =
                        -50.0;

                continue;
            }

            if (i >= mother.length
                    - plan.puncturedBits()) {
                mother[i] =
                        0.0;

                continue;
            }

            mother[i] =
                    transmittedLlrs[cursor++];
        }

        return mother;
    }
}

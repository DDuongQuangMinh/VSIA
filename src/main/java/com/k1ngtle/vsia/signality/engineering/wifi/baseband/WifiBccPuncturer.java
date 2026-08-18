package com.k1ngtle.vsia.signality.engineering.wifi.baseband;

import java.util.Arrays;

public final class WifiBccPuncturer {
    private WifiBccPuncturer() {
    }

    public static int[] puncture(
            int[] motherRateHalfBits,
            BccCodeRate rate
    ) {
        if (motherRateHalfBits == null) {
            return new int[0];
        }

        int[] pattern =
                rate.puncturePattern();

        int kept =
                0;

        for (int i = 0;
             i < motherRateHalfBits.length;
             i++) {
            if (pattern[i % pattern.length] != 0) {
                kept++;
            }
        }

        int[] result =
                new int[
                        kept
                        ];

        int cursor =
                0;

        for (int i = 0;
             i < motherRateHalfBits.length;
             i++) {
            if (pattern[i % pattern.length] != 0) {
                result[cursor++] =
                        motherRateHalfBits[i]
                                & 1;
            }
        }

        return result;
    }

    public static int[] depunctureToErasures(
            int[] puncturedBits,
            int motherLength,
            BccCodeRate rate
    ) {
        int[] result =
                new int[
                        motherLength
                        ];

        Arrays.fill(
                result,
                -1
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

            if (cursor >= puncturedBits.length) {
                throw new IllegalArgumentException(
                        "Punctured bitstream is shorter than expected"
                );
            }

            result[i] =
                    puncturedBits[cursor++]
                            & 1;
        }

        if (cursor != puncturedBits.length) {
            throw new IllegalArgumentException(
                    "Punctured bitstream is longer than expected"
            );
        }

        return result;
    }
}

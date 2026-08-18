package com.k1ngtle.vsia.signality.engineering.wifi.baseband;

public final class WifiScrambler {
    private WifiScrambler() {
    }

    public static int[] apply(
            int[] input,
            int seed
    ) {
        if (input == null) {
            return new int[0];
        }

        int state =
                seed & 0x7F;

        if (state == 0) {
            throw new IllegalArgumentException(
                    "802.11 scrambler seed must be non-zero"
            );
        }

        int[] output =
                new int[
                        input.length
                        ];

        for (int i = 0;
             i < input.length;
             i++) {
            int feedback =
                    (
                            (state >>> 6)
                                    ^ (state >>> 3)
                    )
                            & 1;

            output[i] =
                    (input[i] & 1)
                            ^ feedback;

            state =
                    (
                            (state << 1)
                                    & 0x7E
                    )
                            | feedback;
        }

        return output;
    }
}

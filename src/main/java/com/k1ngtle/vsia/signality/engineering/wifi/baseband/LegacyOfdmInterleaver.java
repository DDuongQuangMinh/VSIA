package com.k1ngtle.vsia.signality.engineering.wifi.baseband;

public final class LegacyOfdmInterleaver {
    private LegacyOfdmInterleaver() {
    }

    public static int[] interleave(
            int[] input,
            int bitsPerSubcarrier
    ) {
        int nCbps =
                input.length;

        validate(
                nCbps,
                bitsPerSubcarrier
        );

        int[] output =
                new int[
                        nCbps
                        ];

        int s =
                Math.max(
                        bitsPerSubcarrier / 2,
                        1
                );

        for (int k = 0;
             k < nCbps;
             k++) {
            int i =
                    (
                            nCbps / 16
                    )
                            * (k % 16)
                            + k / 16;

            int j =
                    s
                            * (i / s)
                            + (
                            i
                                    + nCbps
                                    - (
                                    16 * i
                            )
                                    / nCbps
                    )
                            % s;

            output[j] =
                    input[k]
                            & 1;
        }

        return output;
    }

    public static int[] deinterleave(
            int[] input,
            int bitsPerSubcarrier
    ) {
        int nCbps =
                input.length;

        validate(
                nCbps,
                bitsPerSubcarrier
        );

        int[] output =
                new int[
                        nCbps
                        ];

        int s =
                Math.max(
                        bitsPerSubcarrier / 2,
                        1
                );

        for (int k = 0;
             k < nCbps;
             k++) {
            int i =
                    (
                            nCbps / 16
                    )
                            * (k % 16)
                            + k / 16;

            int j =
                    s
                            * (i / s)
                            + (
                            i
                                    + nCbps
                                    - (
                                    16 * i
                            )
                                    / nCbps
                    )
                            % s;

            output[k] =
                    input[j]
                            & 1;
        }

        return output;
    }

    private static void validate(
            int nCbps,
            int bitsPerSubcarrier
    ) {
        if (nCbps <= 0
                || nCbps % 16 != 0) {
            throw new IllegalArgumentException(
                    "N_CBPS must be positive and divisible by 16"
            );
        }

        if (bitsPerSubcarrier <= 0) {
            throw new IllegalArgumentException(
                    "bitsPerSubcarrier"
            );
        }
    }
}

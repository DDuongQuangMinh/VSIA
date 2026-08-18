package com.k1ngtle.vsia.signality.engineering.wifi.baseband;

public final class WifiBccEncoder {
    public static final int CONSTRAINT_LENGTH =
            7;

    public static final int GENERATOR_A_OCTAL =
            0133;

    public static final int GENERATOR_B_OCTAL =
            0171;

    private WifiBccEncoder() {
    }

    public static int[] encodeMotherRateHalf(
            int[] inputBits
    ) {
        if (inputBits == null) {
            return new int[0];
        }

        int[] output =
                new int[
                        inputBits.length * 2
                        ];

        int register =
                0;

        int cursor =
                0;

        for (int input : inputBits) {
            register =
                    (
                            (register << 1)
                                    | (input & 1)
                    )
                            & 0x7F;

            output[cursor++] =
                    parity(
                            register
                                    & GENERATOR_A_OCTAL
                    );

            output[cursor++] =
                    parity(
                            register
                                    & GENERATOR_B_OCTAL
                    );
        }

        return output;
    }

    public static int[] encode(
            int[] inputBits,
            BccCodeRate rate
    ) {
        int[] mother =
                encodeMotherRateHalf(
                        inputBits
                );

        return WifiBccPuncturer.puncture(
                mother,
                rate
        );
    }

    static int parity(
            int value
    ) {
        return Integer.bitCount(
                value
        )
                & 1;
    }
}

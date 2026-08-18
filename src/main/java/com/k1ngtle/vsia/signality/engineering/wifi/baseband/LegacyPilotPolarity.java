package com.k1ngtle.vsia.signality.engineering.wifi.baseband;

public final class LegacyPilotPolarity {
    private static final int PERIOD =
            127;

    private static final int[] POLARITY =
            build();

    private LegacyPilotPolarity() {
    }

    public static int forSymbol(
            int symbolIndex
    ) {
        int index =
                Math.floorMod(
                        symbolIndex,
                        PERIOD
                );

        return POLARITY[index];
    }

    public static int[] first(
            int count
    ) {
        int[] result =
                new int[
                        Math.max(
                                0,
                                count
                        )
                        ];

        for (int i = 0;
             i < result.length;
             i++) {
            result[i] =
                    forSymbol(
                            i
                    );
        }

        return result;
    }

    private static int[] build() {
        int[] result =
                new int[
                        PERIOD
                        ];

        int state =
                0x7F;

        for (int i = 0;
             i < PERIOD;
             i++) {
            int output =
                    state & 1;

            result[i] =
                    output == 0
                            ? 1
                            : -1;

            int feedback =
                    (
                            (state >>> 6)
                                    ^ (state >>> 3)
                    )
                            & 1;

            state =
                    (
                            (state << 1)
                                    & 0x7E
                    )
                            | feedback;
        }

        return result;
    }
}

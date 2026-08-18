package com.k1ngtle.vsia.signality.engineering.wifi.baseband;

import com.k1ngtle.vsia.signality.engineering.math.Complex;
import com.k1ngtle.vsia.signality.engineering.phy.Modulation;

public final class WifiConstellationMapper {
    private WifiConstellationMapper() {
    }

    public static Complex[] map(
            int[] bits,
            Modulation modulation
    ) {
        int bitsPerSymbol =
                modulation.bitsPerSymbol();

        if (bits.length % bitsPerSymbol != 0) {
            throw new IllegalArgumentException(
                    "Bit count must be divisible by modulation bits/symbol"
            );
        }

        Complex[] output =
                new Complex[
                        bits.length
                                / bitsPerSymbol
                        ];

        for (int symbol = 0;
             symbol < output.length;
             symbol++) {
            output[symbol] =
                    mapOne(
                            bits,
                            symbol * bitsPerSymbol,
                            modulation
                    );
        }

        return output;
    }

    public static int[] demapHard(
            Complex[] symbols,
            Modulation modulation
    ) {
        int bitsPerSymbol =
                modulation.bitsPerSymbol();

        int[] output =
                new int[
                        symbols.length
                                * bitsPerSymbol
                        ];

        for (int symbol = 0;
             symbol < symbols.length;
             symbol++) {
            int[] bits =
                    demapOne(
                            symbols[symbol],
                            modulation
                    );

            System.arraycopy(
                    bits,
                    0,
                    output,
                    symbol * bitsPerSymbol,
                    bits.length
            );
        }

        return output;
    }

    private static Complex mapOne(
            int[] bits,
            int offset,
            Modulation modulation
    ) {
        if (modulation == Modulation.BPSK) {
            return new Complex(
                    bits[offset] == 0
                            ? -1.0
                            : 1.0,
                    0.0
            );
        }

        if (!modulation.isSquareQam()) {
            throw new IllegalArgumentException(
                    "Unsupported non-square modulation: "
                            + modulation
            );
        }

        int bitsPerSymbol =
                modulation.bitsPerSymbol();

        int axisBits =
                bitsPerSymbol / 2;

        int side =
                1 << axisBits;

        int iGray =
                bitsToInteger(
                        bits,
                        offset,
                        axisBits
                );

        int qGray =
                bitsToInteger(
                        bits,
                        offset + axisBits,
                        axisBits
                );

        int iBinary =
                grayToBinary(
                        iGray
                );

        int qBinary =
                grayToBinary(
                        qGray
                );

        double i =
                2.0
                        * iBinary
                        - (
                        side - 1
                );

        double q =
                2.0
                        * qBinary
                        - (
                        side - 1
                );

        double normalization =
                Math.sqrt(
                        2.0
                                * (
                                modulation.order() - 1
                        )
                                / 3.0
                );

        return new Complex(
                i / normalization,
                q / normalization
        );
    }

    private static int[] demapOne(
            Complex symbol,
            Modulation modulation
    ) {
        if (modulation == Modulation.BPSK) {
            return new int[] {
                    symbol.re() >= 0.0
                            ? 1
                            : 0
            };
        }

        int bitsPerSymbol =
                modulation.bitsPerSymbol();

        int axisBits =
                bitsPerSymbol / 2;

        int side =
                1 << axisBits;

        double normalization =
                Math.sqrt(
                        2.0
                                * (
                                modulation.order() - 1
                        )
                                / 3.0
                );

        int iBinary =
                nearestLevelIndex(
                        symbol.re()
                                * normalization,
                        side
                );

        int qBinary =
                nearestLevelIndex(
                        symbol.im()
                                * normalization,
                        side
                );

        int iGray =
                binaryToGray(
                        iBinary
                );

        int qGray =
                binaryToGray(
                        qBinary
                );

        int[] bits =
                new int[
                        bitsPerSymbol
                        ];

        integerToBits(
                iGray,
                bits,
                0,
                axisBits
        );

        integerToBits(
                qGray,
                bits,
                axisBits,
                axisBits
        );

        return bits;
    }

    private static int nearestLevelIndex(
            double level,
            int side
    ) {
        double raw =
                (
                        level
                                + (
                                side - 1
                        )
                )
                        / 2.0;

        return Math.max(
                0,
                Math.min(
                        side - 1,
                        (int) Math.round(
                                raw
                        )
                )
        );
    }

    private static int bitsToInteger(
            int[] bits,
            int offset,
            int count
    ) {
        int value =
                0;

        for (int i = 0;
             i < count;
             i++) {
            value =
                    (
                            value << 1
                    )
                            | (
                            bits[offset + i]
                                    & 1
                    );
        }

        return value;
    }

    private static void integerToBits(
            int value,
            int[] target,
            int offset,
            int count
    ) {
        for (int i = 0;
             i < count;
             i++) {
            int shift =
                    count - 1 - i;

            target[offset + i] =
                    (
                            value >>> shift
                    )
                            & 1;
        }
    }

    private static int binaryToGray(
            int value
    ) {
        return value
                ^ (
                value >>> 1
        );
    }

    private static int grayToBinary(
            int gray
    ) {
        int value =
                gray;

        for (int shift = 1;
             shift < 32;
             shift <<= 1) {
            value ^=
                    value >>> shift;
        }

        return value;
    }
}

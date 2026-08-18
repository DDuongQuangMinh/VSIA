package com.k1ngtle.vsia.signality.engineering.wifi.baseband;

public final class WifiBitOrder {
    private WifiBitOrder() {
    }

    public static int[] bytesToLsbFirstBits(
            byte[] bytes
    ) {
        if (bytes == null) {
            return new int[0];
        }

        int[] bits =
                new int[
                        bytes.length * 8
                        ];

        int cursor =
                0;

        for (byte value : bytes) {
            int unsigned =
                    value & 0xFF;

            for (int bit = 0;
                 bit < 8;
                 bit++) {
                bits[cursor++] =
                        (unsigned >>> bit)
                                & 1;
            }
        }

        return bits;
    }

    public static byte[] lsbFirstBitsToBytes(
            int[] bits
    ) {
        if (bits == null
                || bits.length == 0) {
            return new byte[0];
        }

        byte[] bytes =
                new byte[
                        (bits.length + 7) / 8
                        ];

        for (int i = 0;
             i < bits.length;
             i++) {
            if ((bits[i] & 1) != 0) {
                bytes[i / 8] |=
                        (byte) (
                                1 << (i % 8)
                        );
            }
        }

        return bytes;
    }

    public static int[] concatenate(
            int[]... arrays
    ) {
        int total =
                0;

        for (int[] array : arrays) {
            if (array != null) {
                total +=
                        array.length;
            }
        }

        int[] result =
                new int[
                        total
                        ];

        int offset =
                0;

        for (int[] array : arrays) {
            if (array == null) {
                continue;
            }

            System.arraycopy(
                    array,
                    0,
                    result,
                    offset,
                    array.length
            );

            offset +=
                    array.length;
        }

        return result;
    }
}

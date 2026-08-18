package com.k1ngtle.vsia.signality.engineering.wifi.ip;

public final class InternetChecksum {
    private InternetChecksum() {
    }

    public static int compute(
            byte[] data
    ) {
        return compute(
                data,
                0,
                data == null ? 0 : data.length
        );
    }

    public static int compute(
            byte[] data,
            int offset,
            int length
    ) {
        if (data == null) {
            throw new IllegalArgumentException(
                    "data"
            );
        }

        if (offset < 0
                || length < 0
                || offset + length > data.length) {
            throw new IllegalArgumentException(
                    "Invalid checksum range"
            );
        }

        long sum =
                0L;

        int end =
                offset + length;

        int i =
                offset;

        while (i + 1 < end) {
            sum +=
                    (
                            (
                                    data[i] & 0xFF
                            )
                                    << 8
                    )
                            | (
                            data[i + 1] & 0xFF
                    );

            sum =
                    fold(
                            sum
                    );

            i +=
                    2;
        }

        if (i < end) {
            sum +=
                    (
                            data[i] & 0xFF
                    )
                            << 8;

            sum =
                    fold(
                            sum
                    );
        }

        sum =
                fold(
                        sum
                );

        return (
                int
        ) (
                ~sum
                        & 0xFFFFL
        );
    }

    private static long fold(
            long value
    ) {
        while (
                (
                        value
                                >>> 16
                )
                        != 0L
        ) {
            value =
                    (
                            value
                                    & 0xFFFFL
                    )
                            + (
                            value
                                    >>> 16
                    );
        }

        return value;
    }
}

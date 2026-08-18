package com.k1ngtle.vsia.signality.engineering.wifi.tcp;

public final class TcpSequence {
    private static final long MASK =
            0xFFFF_FFFFL;

    private TcpSequence() {
    }

    public static long normalize(
            long value
    ) {
        return value
                & MASK;
    }

    public static long add(
            long sequence,
            long delta
    ) {
        return normalize(
                sequence
                        + delta
        );
    }

    public static boolean before(
            long a,
            long b
    ) {
        int diff =
                (
                        int
                ) (
                        normalize(
                                a
                        )
                                - normalize(
                                b
                        )
                );

        return diff < 0;
    }

    public static boolean after(
            long a,
            long b
    ) {
        return before(
                b,
                a
        );
    }

    public static boolean beforeOrEqual(
            long a,
            long b
    ) {
        return normalize(
                a
        )
                == normalize(
                b
        )
                || before(
                a,
                b
        );
    }

    public static long distance(
            long from,
            long to
    ) {
        return normalize(
                to
                        - from
        );
    }
}

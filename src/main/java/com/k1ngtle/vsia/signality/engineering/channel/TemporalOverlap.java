package com.k1ngtle.vsia.signality.engineering.channel;

public final class TemporalOverlap {
    private TemporalOverlap() {
    }

    public static long overlapTicks(
            long startA,
            long endA,
            long startB,
            long endB
    ) {
        if (endA < startA
                || endB < startB) {
            return 0L;
        }

        long low =
                Math.max(
                        startA,
                        startB
                );

        long high =
                Math.min(
                        endA,
                        endB
                );

        if (high < low) {
            return 0L;
        }

        return high
                - low
                + 1L;
    }

    public static double fractionOfDesired(
            long desiredStart,
            long desiredEnd,
            long interfererStart,
            long interfererEnd
    ) {
        long desiredTicks =
                desiredEnd
                        - desiredStart
                        + 1L;

        if (desiredTicks <= 0L) {
            return 0.0;
        }

        return Math.max(
                0.0,
                Math.min(
                        1.0,
                        overlapTicks(
                                desiredStart,
                                desiredEnd,
                                interfererStart,
                                interfererEnd
                        )
                                / (double) desiredTicks
                )
        );
    }
}

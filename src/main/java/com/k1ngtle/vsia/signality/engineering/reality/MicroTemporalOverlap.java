package com.k1ngtle.vsia.signality.engineering.reality;

public final class MicroTemporalOverlap {
    private MicroTemporalOverlap() {
    }

    public static long overlapMicros(
            long startA,
            long endA,
            long startB,
            long endB
    ) {
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
            RfMicroTiming desired,
            RfMicroTiming interferer
    ) {
        long overlap =
                overlapMicros(
                        desired.startMicros(),
                        desired.endMicros(),
                        interferer.startMicros(),
                        interferer.endMicros()
                );

        return Math.max(
                0.0,
                Math.min(
                        1.0,
                        overlap
                                / (double) desired.durationMicros()
                )
        );
    }
}

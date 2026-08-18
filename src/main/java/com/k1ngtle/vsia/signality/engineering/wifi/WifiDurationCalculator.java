package com.k1ngtle.vsia.signality.engineering.wifi;

public final class WifiDurationCalculator {
    private WifiDurationCalculator() {
    }

    public static int dataDurationUs(
            WifiMacTimingProfile timing
    ) {
        return clampDuration(
                timing.sifsUs()
                        + timing.ackTimeoutUs()
        );
    }

    public static int rtsDurationUs(
            WifiMacTimingProfile timing
    ) {
        return clampDuration(
                timing.sifsUs()
                        + timing.ctsTimeoutUs()
                        + timing.sifsUs()
                        + timing.ackTimeoutUs()
        );
    }

    public static int ctsDurationUs(
            int receivedRtsDurationUs,
            WifiMacTimingProfile timing
    ) {
        return clampDuration(
                Math.max(
                        0,
                        receivedRtsDurationUs
                                - timing.sifsUs()
                                - timing.ctsTimeoutUs()
                )
        );
    }

    private static int clampDuration(
            int value
    ) {
        return Math.max(
                0,
                Math.min(
                        0x7FFF,
                        value
                )
        );
    }
}

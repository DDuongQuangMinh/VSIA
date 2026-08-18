package com.k1ngtle.vsia.signality.engineering.wifi.phy;

public record WifiPuncturingPattern(
        int twentyMhzSegments,
        long inactiveMask
) {
    public WifiPuncturingPattern {
        if (twentyMhzSegments < 1
                || twentyMhzSegments > 16) {
            throw new IllegalArgumentException(
                    "twentyMhzSegments must be 1..16"
            );
        }
    }

    public int activeSegments() {
        int active =
                0;

        for (int i = 0;
             i < twentyMhzSegments;
             i++) {
            if ((
                    inactiveMask
                            & (
                            1L << i
                    )
            ) == 0L) {
                active++;
            }
        }

        return active;
    }

    public double activeFraction() {
        return activeSegments()
                / (double) twentyMhzSegments;
    }

    public static WifiPuncturingPattern none(
            WifiChannelWidth width
    ) {
        return new WifiPuncturingPattern(
                Math.max(
                        1,
                        width.mhz() / 20
                ),
                0L
        );
    }
}

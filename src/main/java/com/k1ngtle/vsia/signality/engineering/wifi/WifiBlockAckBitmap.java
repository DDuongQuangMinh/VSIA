package com.k1ngtle.vsia.signality.engineering.wifi;

public final class WifiBlockAckBitmap {
    private final int startingSequence;
    private long bitmap;

    public WifiBlockAckBitmap(
            int startingSequence
    ) {
        this.startingSequence =
                startingSequence
                        & 0x0FFF;
    }

    public int startingSequence() {
        return startingSequence;
    }

    public long bitmap() {
        return bitmap;
    }

    public void acknowledge(
            int sequence
    ) {
        int delta =
                sequenceDistance(
                        startingSequence,
                        sequence
                );

        if (delta >= 0
                && delta < 64) {
            bitmap |=
                    1L << delta;
        }
    }

    public boolean acknowledged(
            int sequence
    ) {
        int delta =
                sequenceDistance(
                        startingSequence,
                        sequence
                );

        if (delta < 0
                || delta >= 64) {
            return false;
        }

        return (
                bitmap
                        & (
                        1L << delta
                )
        )
                != 0L;
    }

    private static int sequenceDistance(
            int start,
            int value
    ) {
        return (
                value
                        - start
                        + 4096
        )
                & 0x0FFF;
    }
}

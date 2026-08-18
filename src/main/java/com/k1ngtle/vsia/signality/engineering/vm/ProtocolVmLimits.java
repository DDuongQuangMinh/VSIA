package com.k1ngtle.vsia.signality.engineering.vm;

public record ProtocolVmLimits(
        int maxInstructionsPerRun,
        int maxBufferBytes,
        int maxFrameBytes,
        int maxTimers,
        int maxTimerDelayTicks
) {
    public static final ProtocolVmLimits DEFAULT =
            new ProtocolVmLimits(
                    4096,
                    262_144,
                    65_536,
                    32,
                    72_000
            );

    public ProtocolVmLimits {
        if (maxInstructionsPerRun < 1
                || maxInstructionsPerRun > 1_000_000) {
            throw new IllegalArgumentException(
                    "maxInstructionsPerRun"
            );
        }

        if (maxBufferBytes < 1
                || maxBufferBytes > 16_777_216) {
            throw new IllegalArgumentException(
                    "maxBufferBytes"
            );
        }

        if (maxFrameBytes < 1
                || maxFrameBytes > maxBufferBytes) {
            throw new IllegalArgumentException(
                    "maxFrameBytes"
            );
        }

        if (maxTimers < 0 || maxTimers > 1024) {
            throw new IllegalArgumentException(
                    "maxTimers"
            );
        }

        if (maxTimerDelayTicks < 1
                || maxTimerDelayTicks > 1_000_000) {
            throw new IllegalArgumentException(
                    "maxTimerDelayTicks"
            );
        }
    }
}

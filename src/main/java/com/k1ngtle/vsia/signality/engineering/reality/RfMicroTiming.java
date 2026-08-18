package com.k1ngtle.vsia.signality.engineering.reality;

import java.util.UUID;

public record RfMicroTiming(
        UUID transmissionId,
        String dimensionId,
        double centerFrequencyHz,
        double bandwidthHz,
        long startMicros,
        long endMicros
) {
    public RfMicroTiming {
        if (transmissionId == null) {
            throw new IllegalArgumentException(
                    "transmissionId"
            );
        }

        dimensionId =
                dimensionId == null
                        ? ""
                        : dimensionId;

        if (centerFrequencyHz <= 0.0
                || bandwidthHz <= 0.0) {
            throw new IllegalArgumentException(
                    "frequency/bandwidth"
            );
        }

        if (endMicros < startMicros) {
            throw new IllegalArgumentException(
                    "endMicros < startMicros"
            );
        }
    }

    public long durationMicros() {
        return Math.max(
                1L,
                endMicros
                        - startMicros
                        + 1L
        );
    }
}

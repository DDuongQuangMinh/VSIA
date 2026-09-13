package com.k1ngtle.vsia.signality.internet.satellite;

import java.util.UUID;

public record SatellitePacket(
        UUID sourceTerminalId,
        UUID destinationTerminalId,
        long sequenceNumber,
        int ttl,
        long timestampNanos,
        String payload
) {
    public SatellitePacket {
        if (sourceTerminalId == null) {
            throw new IllegalArgumentException(
                    "sourceTerminalId"
            );
        }

        if (destinationTerminalId == null) {
            throw new IllegalArgumentException(
                    "destinationTerminalId"
            );
        }

        ttl =
                Math.max(
                        0,
                        ttl
                );

        payload =
                payload == null
                        ? ""
                        : payload;
    }
}

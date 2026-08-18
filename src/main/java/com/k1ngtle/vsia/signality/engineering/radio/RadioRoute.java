package com.k1ngtle.vsia.signality.engineering.radio;

import java.util.UUID;

public record RadioRoute(
        UUID destination,
        UUID nextHop,
        int hopCount,
        int destinationSequenceNumber,
        long expiresAtNanos
) {
    public boolean expired(long nowNanos) {
        return nowNanos >= expiresAtNanos;
    }
}

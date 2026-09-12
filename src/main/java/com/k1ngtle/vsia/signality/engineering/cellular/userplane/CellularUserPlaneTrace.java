package com.k1ngtle.vsia.signality.engineering.cellular.userplane;

import java.util.List;

// CELLULAR_USER_PLANE_V1
public record CellularUserPlaneTrace(
        boolean success,
        int sequence,
        String sourceAddress,
        String targetAddress,
        int payloadBytes,
        long roundTripMicros,
        int ttl,
        String route,
        String failureReason,
        List<CellularUserPlaneHop> hops
) {
    public CellularUserPlaneTrace {
        sourceAddress = sourceAddress == null ? "" : sourceAddress;
        targetAddress = targetAddress == null ? "" : targetAddress;
        route = route == null ? "" : route;
        failureReason = failureReason == null ? "" : failureReason;
        hops = hops == null ? List.of() : List.copyOf(hops);
    }

    public double roundTripMillis() {
        return roundTripMicros / 1000.0;
    }
}

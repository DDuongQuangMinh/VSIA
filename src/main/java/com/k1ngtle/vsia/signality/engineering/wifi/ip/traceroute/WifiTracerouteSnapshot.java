package com.k1ngtle.vsia.signality.engineering.wifi.ip.traceroute;

import java.util.List;

public record WifiTracerouteSnapshot(
        long traceId,
        String destinationIp,
        int maxHops,
        int currentTtl,
        int currentAttempt,
        boolean running,
        boolean destinationReached,
        String finalStatus,
        List<WifiTracerouteHop> hops
) {
    public WifiTracerouteSnapshot {
        hops = List.copyOf(hops);
    }
}

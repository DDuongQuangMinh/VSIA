package com.k1ngtle.vsia.signality.engineering.wifi.ip.traceroute;

public record WifiTracerouteHop(
        int ttl,
        String responderIp,
        int attempts,
        double rttMs,
        String result
) {
}

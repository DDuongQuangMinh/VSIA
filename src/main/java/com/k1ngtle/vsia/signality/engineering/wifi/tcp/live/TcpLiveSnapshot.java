package com.k1ngtle.vsia.signality.engineering.wifi.tcp.live;

public record TcpLiveSnapshot(
        String sessionId,
        String state,
        String peerIp,
        String peerMac,
        int localPort,
        int remotePort,
        long congestionWindowBytes,
        long slowStartThresholdBytes,
        long bytesInFlight,
        double srttMs,
        double rtoMs,
        int retransmissions,
        String status
) {
    public static TcpLiveSnapshot idle() {
        return new TcpLiveSnapshot(
                "",
                "CLOSED",
                "",
                "",
                0,
                0,
                0L,
                0L,
                0L,
                Double.NaN,
                1000.0,
                0,
                "IDLE"
        );
    }
}

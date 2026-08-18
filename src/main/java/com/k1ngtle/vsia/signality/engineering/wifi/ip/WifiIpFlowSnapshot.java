package com.k1ngtle.vsia.signality.engineering.wifi.ip;

public record WifiIpFlowSnapshot(
        String localIp,
        String peerIp,
        String peerMac,
        int txPackets,
        int rxPackets,
        long txBytes,
        long rxBytes,
        int lostPackets,
        double lastRttMs,
        double averageRttMs,
        double jitterMs,
        double goodputKbps,
        String lastProtocol,
        String lastStatus
) {
}

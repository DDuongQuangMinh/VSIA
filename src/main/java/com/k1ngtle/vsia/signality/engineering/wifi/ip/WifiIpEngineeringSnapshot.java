package com.k1ngtle.vsia.signality.engineering.wifi.ip;

public record WifiIpEngineeringSnapshot(
        String localIp,
        String localMac,
        String peerIp,
        String peerMac,
        int neighborCount,
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
        String status
) {
}

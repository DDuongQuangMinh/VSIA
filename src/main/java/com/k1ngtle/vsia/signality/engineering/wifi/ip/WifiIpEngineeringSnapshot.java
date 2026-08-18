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
        String tcpState,
        int tcpLocalPort,
        int tcpRemotePort,
        long tcpCongestionWindowBytes,
        long tcpSlowStartThresholdBytes,
        long tcpBytesInFlight,
        double tcpSrttMs,
        double tcpRtoMs,
        int tcpRetransmissions,
        String tcpStatus,
        String status
) {
}

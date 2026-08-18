package com.k1ngtle.vsia.signality.engineering.wifi.tcp.raw.live;

public record TcpRawLiveCarrierStats(
        long encodedPackets,
        long decodedPackets,
        long encodedBytes,
        long decodedBytes,
        long decodeDrops
) {
}

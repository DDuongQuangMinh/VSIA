package com.k1ngtle.vsia.signality.engineering.wifi.tcp.stream;

public record TcpSackBlock(
        long leftEdge,
        long rightEdge
) {
    public TcpSackBlock {
        leftEdge &= 0xFFFF_FFFFL;
        rightEdge &= 0xFFFF_FFFFL;
    }
}

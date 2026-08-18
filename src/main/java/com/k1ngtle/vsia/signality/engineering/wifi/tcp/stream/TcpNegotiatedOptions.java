package com.k1ngtle.vsia.signality.engineering.wifi.tcp.stream;

public record TcpNegotiatedOptions(
        int localMss,
        int peerMss,
        boolean sackPermitted,
        int windowScale,
        boolean timestamps
) {
    public int effectiveMss() {
        return Math.max(
                536,
                Math.min(
                        localMss,
                        peerMss
                )
        );
    }

    public long scaledWindow(
            int advertisedWindow
    ) {
        int shift =
                Math.max(
                        0,
                        Math.min(
                                14,
                                windowScale
                        )
                );

        return (
                advertisedWindow
                        & 0xFFFFL
        ) << shift;
    }
}

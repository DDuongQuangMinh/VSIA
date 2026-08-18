package com.k1ngtle.vsia.signality.engineering.wifi.tcp.stream;

import java.util.List;

public record TcpReceiveResult(
        long cumulativeAck,
        int newlyAcceptedBytes,
        int duplicateBytes,
        int bufferedOutOfOrderBytes,
        int advertisedWindowBytes,
        boolean outOfOrder,
        boolean duplicateOnly,
        List<TcpSackBlock> sackBlocks
) {
    public TcpReceiveResult {
        sackBlocks = List.copyOf(sackBlocks);
    }
}

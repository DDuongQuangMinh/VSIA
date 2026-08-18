package com.k1ngtle.vsia.signality.engineering.wifi.tcp;

public record TcpConnectionSnapshot(
        TcpState state,
        long sendUnacknowledged,
        long sendNext,
        long receiveNext,
        int receiverWindow,
        long congestionWindowBytes,
        long slowStartThresholdBytes,
        long bytesInFlight,
        double srttMs,
        double rttvarMs,
        double rtoMs,
        int duplicateAcks,
        boolean fastRecovery,
        int retransmissions,
        String lastEvent
) {
}

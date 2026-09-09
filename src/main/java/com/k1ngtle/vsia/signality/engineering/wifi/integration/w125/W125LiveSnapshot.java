package com.k1ngtle.vsia.signality.engineering.wifi.integration.w125;

public record W125LiveSnapshot(
        W125LiveStage stage,
        boolean finished,
        boolean passed,
        String protocolId,
        String detail,
        long elapsedTicks,
        String stationState,
        String securityState,
        String selectedSecurity,
        long ackRx
) {
}

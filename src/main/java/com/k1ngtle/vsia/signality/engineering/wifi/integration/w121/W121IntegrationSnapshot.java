package com.k1ngtle.vsia.signality.engineering.wifi.integration.w121;

public record W121IntegrationSnapshot(
        W121IntegrationStage stage,
        W121IntegrationFailure failure,
        boolean finished,
        boolean passed,
        String detail,
        long elapsedTicks,
        String stationState,
        String securityState,
        String stationIp,
        String selectedBssid,
        String workflowState,
        String workflowDetail,
        String apBridgeStatus,
        long stationTraceEvents,
        long apTraceEvents,
        long retries,
        long ackRx,
        long dsTx,
        long dsRx
) {
}

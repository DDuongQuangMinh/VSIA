package com.k1ngtle.vsia.signality.engineering.wifi.integration.w124;

import java.util.List;

public record W124QosSnapshot(
        W124QosStage stage,
        W124QosFailure failure,
        boolean finished,
        boolean passed,
        String detail,
        long elapsedTicks,
        String stationState,
        String securityState,
        String selectedBssid,
        int pendingData,
        List<W124QosClassResult> classResults,
        int mixedAccepted,
        long mixedSuccesses,
        long mixedRetries,
        long mixedDrops,
        long mixedDeferrals,
        int mixedQueuePeak,
        int cwVoice,
        int cwVideo,
        int cwBestEffort,
        int cwBackground
) {
}

package com.k1ngtle.vsia.signality.engineering.wifi.integration.mb1;

import com.k1ngtle.vsia.signality.engineering.wifi.smartconnect.WifiBand;

public record Mb1LiveSnapshot(
        Mb1LiveStage stage,
        boolean finished,
        boolean passed,
        String detail,
        long elapsedTicks,
        String stationState,
        String securityState,
        String selectedBssid,
        double activeFrequencyHz,
        WifiBand selectedBand,
        double twoFourSnrDb,
        double fiveSnrDb,
        String decisionBssid,
        WifiBand decisionBand,
        double decisionScore,
        long ackRx
) {
}

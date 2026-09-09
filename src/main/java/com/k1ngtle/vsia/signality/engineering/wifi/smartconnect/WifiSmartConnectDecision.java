package com.k1ngtle.vsia.signality.engineering.wifi.smartconnect;

import com.k1ngtle.vsia.signality.engineering.wifi.WifiNetworkRecord;

public record WifiSmartConnectDecision(
        boolean available,
        WifiNetworkRecord network,
        WifiBand band,
        double snrDb,
        double score,
        String reason
) {
    public static WifiSmartConnectDecision unavailable(
            String reason
    ) {
        return new WifiSmartConnectDecision(
                false,
                null,
                WifiBand.UNKNOWN,
                Double.NEGATIVE_INFINITY,
                Double.NEGATIVE_INFINITY,
                reason == null
                        ? ""
                        : reason
        );
    }
}

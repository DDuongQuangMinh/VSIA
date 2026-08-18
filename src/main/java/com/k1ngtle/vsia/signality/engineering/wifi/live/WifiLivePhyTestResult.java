package com.k1ngtle.vsia.signality.engineering.wifi.live;

public record WifiLivePhyTestResult(
        String id,
        boolean passed,
        String detail
) {
}

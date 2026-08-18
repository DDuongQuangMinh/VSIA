package com.k1ngtle.vsia.signality.engineering.wifi.phy;

public record WifiPhyTestResult(
        String id,
        boolean passed,
        String detail
) {
}

package com.k1ngtle.vsia.signality.engineering.wifi.baseband;

public record WifiBasebandTestResult(
        String id,
        boolean passed,
        String detail
) {
}

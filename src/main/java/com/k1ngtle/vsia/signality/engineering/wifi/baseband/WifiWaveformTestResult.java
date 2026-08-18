package com.k1ngtle.vsia.signality.engineering.wifi.baseband;

public record WifiWaveformTestResult(
        String id,
        boolean passed,
        String detail
) {
}

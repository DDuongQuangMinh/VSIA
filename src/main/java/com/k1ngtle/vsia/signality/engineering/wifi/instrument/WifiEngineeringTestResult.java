package com.k1ngtle.vsia.signality.engineering.wifi.instrument;

public record WifiEngineeringTestResult(
        String id,
        boolean passed,
        String detail
) {
}

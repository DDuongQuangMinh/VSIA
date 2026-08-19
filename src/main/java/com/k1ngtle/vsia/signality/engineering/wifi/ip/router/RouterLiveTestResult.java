package com.k1ngtle.vsia.signality.engineering.wifi.ip.router;

public record RouterLiveTestResult(
        String id,
        boolean passed,
        String detail
) {
}

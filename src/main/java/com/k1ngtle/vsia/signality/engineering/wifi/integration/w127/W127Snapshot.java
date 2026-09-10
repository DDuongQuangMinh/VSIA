package com.k1ngtle.vsia.signality.engineering.wifi.integration.w127;

public record W127Snapshot(
        W127Stage stage,
        W127Failure failure,
        boolean finished,
        boolean passed,
        String detail,
        long elapsedTicks,
        String resolution,
        String transfer,
        String trace,
        W127FaultSnapshot faults,
        boolean internetSuitePassed,
        boolean wifiRegressionPassed,
        String wifiRegressionDetail
) {
}

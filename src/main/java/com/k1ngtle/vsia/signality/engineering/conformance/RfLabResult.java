package com.k1ngtle.vsia.signality.engineering.conformance;

public record RfLabResult(
        double pathLossDb,
        double receivedPowerDbm,
        double noiseFloorDbm,
        double snrDb,
        double shannonCapacityBps
) {
}

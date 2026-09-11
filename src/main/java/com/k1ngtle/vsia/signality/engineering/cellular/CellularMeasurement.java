package com.k1ngtle.vsia.signality.engineering.cellular;

public record CellularMeasurement(
        double pathLossDb,
        double receivedPowerDbm,
        double rsrpDbm,
        double rssiDbm,
        double rsrqDb,
        double sinrDb,
        double noiseFloorDbm,
        int cqi,
        double shannonCapacityBps,
        int timingAdvanceUnits
) {
}

package com.k1ngtle.vsia.signality.engineering.phy;

public record PhyResult(
        double pathLossDb,
        double receivedPowerDbm,
        double noiseFloorDbm,
        double snrDb,
        double shannonCapacityBps,
        double rawPhyRateBps,
        double effectiveRateBps,
        double bitErrorRate,
        double frameErrorRate,
        boolean decodable
) {
}

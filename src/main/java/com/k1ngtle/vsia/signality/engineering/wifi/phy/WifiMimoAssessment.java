package com.k1ngtle.vsia.signality.engineering.wifi.phy;

public record WifiMimoAssessment(
        int channelRank,
        int usableSpatialStreams,
        double spatialEfficiency,
        double arrayGainDb
) {
}

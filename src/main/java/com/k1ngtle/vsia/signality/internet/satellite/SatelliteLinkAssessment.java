package com.k1ngtle.vsia.signality.internet.satellite;

public record SatelliteLinkAssessment(
        boolean visible,
        String satelliteName,
        double sourceElevationDeg,
        double targetElevationDeg,
        double sourceSlantRangeMeters,
        double targetSlantRangeMeters,
        double uplinkReceivedPowerDbm,
        double downlinkReceivedPowerDbm,
        double uplinkSnrDb,
        double downlinkSnrDb,
        double propagationDelayMs,
        double uplinkDopplerHz,
        double downlinkDopplerHz,
        double packetSuccessProbability
) {
    public static SatelliteLinkAssessment unavailable() {
        return new SatelliteLinkAssessment(
                false,
                "",
                Double.NEGATIVE_INFINITY,
                Double.NEGATIVE_INFINITY,
                0.0,
                0.0,
                Double.NEGATIVE_INFINITY,
                Double.NEGATIVE_INFINITY,
                Double.NEGATIVE_INFINITY,
                Double.NEGATIVE_INFINITY,
                0.0,
                0.0,
                0.0,
                0.0
        );
    }

    public double bottleneckSnrDb() {
        return Math.min(
                uplinkSnrDb,
                downlinkSnrDb
        );
    }
}

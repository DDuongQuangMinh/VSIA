package com.k1ngtle.vsia.signality.engineering.channel;

public record RfChannelAssessment(
        double rawDesiredPowerWatts,
        double effectiveDesiredPowerWatts,
        double interferencePowerWatts,
        double noisePowerWatts,
        double rawSnrDb,
        double sinrDb,
        double materialLossDb,
        double shadowingDb,
        double fadingDb,
        double transmitDirectionalGainDbi,
        double receiveDirectionalGainDbi,
        double polarizationMismatchLossDb,
        double radialRelativeVelocityMetersPerSecond,
        double dopplerHz,
        int overlappingInterferers
) {
    public RfChannelAssessment(
            double rawDesiredPowerWatts,
            double effectiveDesiredPowerWatts,
            double interferencePowerWatts,
            double noisePowerWatts,
            double rawSnrDb,
            double sinrDb,
            double materialLossDb,
            double shadowingDb,
            double fadingDb,
            double dopplerHz,
            int overlappingInterferers
    ) {
        this(
                rawDesiredPowerWatts,
                effectiveDesiredPowerWatts,
                interferencePowerWatts,
                noisePowerWatts,
                rawSnrDb,
                sinrDb,
                materialLossDb,
                shadowingDb,
                fadingDb,
                0.0,
                0.0,
                0.0,
                0.0,
                dopplerHz,
                overlappingInterferers
        );
    }
    public double interferencePowerDbm() {
        return wattsToDbmSafe(
                interferencePowerWatts
        );
    }

    public double noisePowerDbm() {
        return wattsToDbmSafe(
                noisePowerWatts
        );
    }

    public double effectiveDesiredPowerDbm() {
        return wattsToDbmSafe(
                effectiveDesiredPowerWatts
        );
    }

    private static double wattsToDbmSafe(
            double watts
    ) {
        if (watts <= 0.0) {
            return Double.NEGATIVE_INFINITY;
        }

        return 10.0
                * Math.log10(
                watts * 1000.0
        );
    }
}

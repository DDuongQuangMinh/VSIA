package com.k1ngtle.vsia.signality.engineering.phy;

import com.k1ngtle.vsia.signality.engineering.math.RfMath;

public final class LinkBudgetEngine {
    private LinkBudgetEngine() {
    }

    public static PhyResult evaluateFromReceivedPower(
            PhyProfile profile,
            double receivedPowerDbm,
            long frameBits
    ) {
        double noiseDbm =
                RfMath.noiseFloorDbm(
                        profile.bandwidthHz(),
                        RfMath.STANDARD_TEMPERATURE_K,
                        profile.receiverNoiseFigureDb()
                );

        double snrDb =
                RfMath.snrDb(
                        receivedPowerDbm,
                        noiseDbm
                );

        double capacity =
                RfMath.shannonCapacityBps(
                        profile.bandwidthHz(),
                        snrDb
                );

        double requestedRate =
                profile.bandwidthHz()
                        * profile.modulation().bitsPerSymbol()
                        * profile.coding().rate()
                        * profile.spatialStreams()
                        * profile.guardEfficiency();

        double rawPhyRate =
                Math.min(
                        requestedRate,
                        capacity
                );

        double effectiveRate =
                rawPhyRate
                        * profile.macEfficiency();

        double ber =
                ErrorRateModel.bitErrorRate(
                        profile.modulation(),
                        snrDb,
                        profile.coding().codingGainDb()
                );

        double fer =
                ErrorRateModel.frameErrorRate(
                        ber,
                        frameBits
                );

        boolean decodable =
                receivedPowerDbm > noiseDbm
                        && fer < 0.5;

        return new PhyResult(
                Double.NaN,
                receivedPowerDbm,
                noiseDbm,
                snrDb,
                capacity,
                rawPhyRate,
                effectiveRate,
                ber,
                fer,
                decodable
        );
    }
}

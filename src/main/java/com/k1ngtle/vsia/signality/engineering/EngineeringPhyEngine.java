package com.k1ngtle.vsia.signality.engineering;

import com.k1ngtle.vsia.signality.engineering.math.RfMath;
import com.k1ngtle.vsia.signality.engineering.phy.LinkBudgetEngine;
import com.k1ngtle.vsia.signality.engineering.phy.PhyProfile;
import com.k1ngtle.vsia.signality.engineering.phy.PhyResult;

import java.util.concurrent.ThreadLocalRandom;

public final class EngineeringPhyEngine {
    private EngineeringPhyEngine() {
    }

    public static PhyResult evaluateReceivedFrame(
            PhyProfile profile,
            double receivedPowerWatts,
            long frameBits
    ) {
        return LinkBudgetEngine.evaluateFromReceivedPower(
                profile,
                RfMath.wattsToDbm(
                        receivedPowerWatts
                ),
                frameBits
        );
    }

    public static boolean shouldDeliverFrame(
            PhyResult result
    ) {
        if (!result.decodable()) {
            return false;
        }

        double successProbability =
                1.0
                        - result.frameErrorRate();

        return ThreadLocalRandom
                .current()
                .nextDouble()
                < successProbability;
    }
}

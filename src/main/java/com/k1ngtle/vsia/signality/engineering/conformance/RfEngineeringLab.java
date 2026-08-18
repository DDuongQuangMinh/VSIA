package com.k1ngtle.vsia.signality.engineering.conformance;

import com.k1ngtle.vsia.signality.engineering.math.RfMath;

import java.util.ArrayList;
import java.util.List;

public final class RfEngineeringLab {

    private RfEngineeringLab() {
    }

    public static RfLabResult evaluate(
            RfLabScenario scenario
    ) {
        double pathLossDb =
                RfMath.freeSpacePathLossDb(
                        scenario.distanceMeters(),
                        scenario.frequencyHz()
                );

        double receivedPowerDbm =
                scenario.txPowerDbm()
                        + scenario.txGainDbi()
                        + scenario.rxGainDbi()
                        - pathLossDb
                        - scenario.additionalLossDb();

        double noiseFloorDbm =
                RfMath.noiseFloorDbm(
                        scenario.bandwidthHz(),
                        RfMath.STANDARD_TEMPERATURE_K,
                        scenario.receiverNoiseFigureDb()
                );

        double snrDb =
                receivedPowerDbm
                        - noiseFloorDbm;

        double shannonCapacityBps =
                RfMath.shannonCapacityBps(
                        scenario.bandwidthHz(),
                        snrDb
                );

        return new RfLabResult(
                pathLossDb,
                receivedPowerDbm,
                noiseFloorDbm,
                snrDb,
                shannonCapacityBps
        );
    }

    public static List<RfLabResult> distanceSweep(
            RfLabScenario base,
            double[] distancesMeters
    ) {
        List<RfLabResult> results =
                new ArrayList<>(
                        distancesMeters.length
                );

        for (double distance
                : distancesMeters) {
            results.add(
                    evaluate(
                            new RfLabScenario(
                                    base.frequencyHz(),
                                    distance,
                                    base.bandwidthHz(),
                                    base.txPowerDbm(),
                                    base.txGainDbi(),
                                    base.rxGainDbi(),
                                    base.receiverNoiseFigureDb(),
                                    base.additionalLossDb()
                            )
                    )
            );
        }

        return List.copyOf(
                results
        );
    }
}

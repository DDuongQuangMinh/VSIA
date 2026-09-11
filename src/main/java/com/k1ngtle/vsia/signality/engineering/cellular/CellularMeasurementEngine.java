package com.k1ngtle.vsia.signality.engineering.cellular;

public final class CellularMeasurementEngine {
    private static final double SPEED_OF_LIGHT_MPS = 299_792_458.0;
    private static final double LTE_TA_STEP_SECONDS = 16.0 / (15000.0 * 2048.0);

    private CellularMeasurementEngine() {
    }

    public static CellularMeasurement evaluate(
            double frequencyHz,
            double distanceMeters,
            double transmitPowerDbm,
            double transmitGainDbi,
            double receiveGainDbi,
            double bandwidthHz,
            int resourceBlocks,
            double interferenceDbm,
            double receiverNoiseFigureDb,
            double additionalLossDb
    ) {
        if (!(frequencyHz > 0.0)
                || !(distanceMeters > 0.0)
                || !(bandwidthHz > 0.0)) {
            throw new IllegalArgumentException(
                    "frequency, distance and bandwidth must be > 0"
            );
        }

        int rb = Math.max(1, resourceBlocks);
        double pathLossDb = urbanMacroPathLossDb(
                frequencyHz,
                distanceMeters
        ) + Math.max(0.0, additionalLossDb);

        double receivedPowerDbm =
                transmitPowerDbm
                        + transmitGainDbi
                        + receiveGainDbi
                        - pathLossDb;

        double referenceElements = Math.max(
                1.0,
                rb * 12.0
        );

        double rsrpDbm =
                receivedPowerDbm
                        - 10.0 * Math.log10(referenceElements);

        double noiseFloorDbm =
                -174.0
                        + 10.0 * Math.log10(bandwidthHz)
                        + Math.max(0.0, receiverNoiseFigureDb);

        double interferenceWatts = dbmToWatts(interferenceDbm);
        double noiseWatts = dbmToWatts(noiseFloorDbm);
        double signalWatts = dbmToWatts(receivedPowerDbm);

        double sinrLinear =
                signalWatts
                        / Math.max(
                        1.0e-18,
                        interferenceWatts + noiseWatts
                );

        double sinrDb =
                10.0 * Math.log10(
                        Math.max(
                                1.0e-18,
                                sinrLinear
                        )
                );

        double rssiWatts =
                signalWatts
                        + interferenceWatts
                        + noiseWatts;

        double rssiDbm =
                wattsToDbm(rssiWatts);

        double rsrqDb =
                10.0 * Math.log10(rb)
                        + rsrpDbm
                        - rssiDbm;

        int cqi = cqiFromSinrDb(sinrDb);

        double capacityBps =
                bandwidthHz
                        * (Math.log1p(sinrLinear)
                        / Math.log(2.0));

        int timingAdvance =
                timingAdvanceUnits(distanceMeters);

        return new CellularMeasurement(
                pathLossDb,
                receivedPowerDbm,
                rsrpDbm,
                rssiDbm,
                rsrqDb,
                sinrDb,
                noiseFloorDbm,
                cqi,
                capacityBps,
                timingAdvance
        );
    }

    public static double freeSpacePathLossDb(
            double frequencyHz,
            double distanceMeters
    ) {
        if (!(frequencyHz > 0.0)
                || !(distanceMeters > 0.0)) {
            throw new IllegalArgumentException(
                    "frequencyHz and distanceMeters must be > 0"
            );
        }

        return 20.0 * Math.log10(distanceMeters)
                + 20.0 * Math.log10(frequencyHz)
                + 20.0 * Math.log10(4.0 * Math.PI / SPEED_OF_LIGHT_MPS);
    }

    public static double urbanMacroPathLossDb(
            double frequencyHz,
            double distanceMeters
    ) {
        double fcGhz = frequencyHz / 1.0e9;
        double d = Math.max(10.0, distanceMeters);

        double umaLos =
                28.0
                        + 22.0 * Math.log10(d)
                        + 20.0 * Math.log10(fcGhz);

        double fspl = freeSpacePathLossDb(
                frequencyHz,
                d
        );

        return Math.max(fspl, umaLos);
    }

    public static int cqiFromSinrDb(double sinrDb) {
        double[] thresholds = {
                -6.7, -4.7, -2.3, 0.2, 2.4,
                4.3, 5.9, 8.1, 10.3, 11.7,
                14.1, 16.3, 18.7, 21.0
        };

        int cqi = 1;

        for (double threshold : thresholds) {
            if (sinrDb >= threshold) {
                cqi++;
            } else {
                break;
            }
        }

        return Math.max(1, Math.min(15, cqi));
    }

    public static int timingAdvanceUnits(double distanceMeters) {
        double oneWayMetersPerTa =
                SPEED_OF_LIGHT_MPS
                        * LTE_TA_STEP_SECONDS
                        * 0.5;

        return Math.max(
                0,
                (int) Math.round(
                        Math.max(0.0, distanceMeters)
                                / oneWayMetersPerTa
                )
        );
    }

    private static double dbmToWatts(double dbm) {
        if (!Double.isFinite(dbm)) {
            return dbm == Double.NEGATIVE_INFINITY
                    ? 0.0
                    : Double.POSITIVE_INFINITY;
        }

        return Math.pow(
                10.0,
                (dbm - 30.0) / 10.0
        );
    }

    private static double wattsToDbm(double watts) {
        if (!(watts > 0.0)) {
            return Double.NEGATIVE_INFINITY;
        }

        return 10.0 * Math.log10(watts) + 30.0;
    }
}

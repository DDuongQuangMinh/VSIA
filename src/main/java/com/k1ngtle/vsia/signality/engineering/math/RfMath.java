package com.k1ngtle.vsia.signality.engineering.math;

public final class RfMath {
    public static final double SPEED_OF_LIGHT_MPS = 299_792_458.0;
    public static final double BOLTZMANN = 1.380649e-23;
    public static final double STANDARD_TEMPERATURE_K = 290.0;

    private RfMath() {
    }

    public static double wattsToDbm(double watts) {
        if (watts <= 0.0) {
            return Double.NEGATIVE_INFINITY;
        }

        return 10.0 * Math.log10(watts * 1000.0);
    }

    public static double dbmToWatts(double dbm) {
        return Math.pow(10.0, (dbm - 30.0) / 10.0);
    }

    public static double freeSpacePathLossDb(double distanceMeters, double frequencyHz) {
        if (distanceMeters <= 0.0) {
            return 0.0;
        }

        if (frequencyHz <= 0.0) {
            throw new IllegalArgumentException("frequencyHz must be positive");
        }

        double wavelength = SPEED_OF_LIGHT_MPS / frequencyHz;
        return 20.0 * Math.log10(4.0 * Math.PI * distanceMeters / wavelength);
    }

    public static double thermalNoiseWatts(double bandwidthHz, double temperatureK) {
        if (bandwidthHz <= 0.0 || temperatureK <= 0.0) {
            throw new IllegalArgumentException("bandwidthHz and temperatureK must be positive");
        }

        return BOLTZMANN * temperatureK * bandwidthHz;
    }

    public static double noiseFloorDbm(
            double bandwidthHz,
            double temperatureK,
            double noiseFigureDb
    ) {
        return wattsToDbm(
                thermalNoiseWatts(
                        bandwidthHz,
                        temperatureK
                )
        ) + noiseFigureDb;
    }

    public static double snrDb(double signalDbm, double noiseDbm) {
        return signalDbm - noiseDbm;
    }

    public static double dbToLinear(double db) {
        return Math.pow(10.0, db / 10.0);
    }

    public static double shannonCapacityBps(double bandwidthHz, double snrDb) {
        return bandwidthHz
                * (Math.log1p(dbToLinear(snrDb)) / Math.log(2.0));
    }

    public static double receivedPowerDbm(
            double txPowerDbm,
            double txGainDbi,
            double rxGainDbi,
            double pathLossDb,
            double additionalLossDb
    ) {
        return txPowerDbm
                + txGainDbi
                + rxGainDbi
                - pathLossDb
                - additionalLossDb;
    }
}

package com.k1ngtle.vsia.signality.internet.satellite;

import net.minecraft.world.phys.Vec3;

public final class SatelliteLinkMath {
    private SatelliteLinkMath() {
    }

    public static SatelliteLinkAssessment assess(
            Vec3 sourceGround,
            Vec3 targetGround,
            SatelliteOrbitState satellite,
            SatelliteBandPreset band,
            double minimumElevationDeg
    ) {
        Vec3 satellitePosition =
                satellite.positionEcefMeters();

        double sourceElevation =
                SatelliteOrbitModel.elevationDeg(
                        sourceGround,
                        satellitePosition
                );

        double targetElevation =
                SatelliteOrbitModel.elevationDeg(
                        targetGround,
                        satellitePosition
                );

        if (sourceElevation
                < minimumElevationDeg
                || targetElevation
                < minimumElevationDeg) {
            return SatelliteLinkAssessment.unavailable();
        }

        double sourceRange =
                sourceGround
                        .distanceTo(
                                satellitePosition
                        );

        double targetRange =
                targetGround
                        .distanceTo(
                                satellitePosition
                        );

        double uplinkAtmosphericLoss =
                atmosphericLossDb(
                        band,
                        sourceElevation
                );

        double downlinkAtmosphericLoss =
                atmosphericLossDb(
                        band,
                        targetElevation
                );

        double uplinkRxDbm =
                wattsToDbm(
                        band.terminalPowerWatts()
                )
                        + band.terminalGainDbi()
                        + band.satelliteGainDbi()
                        - freeSpacePathLossDb(
                        band.uplinkHz(),
                        sourceRange
                )
                        - uplinkAtmosphericLoss
                        - 0.5;

        double downlinkRxDbm =
                wattsToDbm(
                        band.satellitePowerWatts()
                )
                        + band.satelliteGainDbi()
                        + band.terminalGainDbi()
                        - freeSpacePathLossDb(
                        band.downlinkHz(),
                        targetRange
                )
                        - downlinkAtmosphericLoss
                        - 0.5;

        double satelliteNoiseDbm =
                noiseFloorDbm(
                        band.bandwidthHz(),
                        band.satelliteNoiseFigureDb()
                );

        double terminalNoiseDbm =
                noiseFloorDbm(
                        band.bandwidthHz(),
                        band.terminalNoiseFigureDb()
                );

        double uplinkSnr =
                uplinkRxDbm
                        - satelliteNoiseDbm;

        double downlinkSnr =
                downlinkRxDbm
                        - terminalNoiseDbm;

        double delayMs =
                (
                        sourceRange
                                + targetRange
                )
                        / SatelliteOrbitModel
                        .LIGHT_SPEED_METERS_PER_SEC
                        * 1000.0;

        double sourceRadialVelocity =
                SatelliteOrbitModel
                        .radialVelocityMetersPerSecond(
                                sourceGround,
                                satellite
                        );

        double targetRadialVelocity =
                SatelliteOrbitModel
                        .radialVelocityMetersPerSecond(
                                targetGround,
                                satellite
                        );

        double uplinkDoppler =
                -sourceRadialVelocity
                        / SatelliteOrbitModel
                        .LIGHT_SPEED_METERS_PER_SEC
                        * band.uplinkHz();

        double downlinkDoppler =
                targetRadialVelocity
                        / SatelliteOrbitModel
                        .LIGHT_SPEED_METERS_PER_SEC
                        * band.downlinkHz();

        double bottleneckSnr =
                Math.min(
                        uplinkSnr,
                        downlinkSnr
                );

        double success =
                logisticPacketSuccess(
                        bottleneckSnr
                );

        return new SatelliteLinkAssessment(
                true,
                satellite.name(),
                sourceElevation,
                targetElevation,
                sourceRange,
                targetRange,
                uplinkRxDbm,
                downlinkRxDbm,
                uplinkSnr,
                downlinkSnr,
                delayMs,
                uplinkDoppler,
                downlinkDoppler,
                success
        );
    }

    public static double freeSpacePathLossDb(
            double frequencyHz,
            double distanceMeters
    ) {
        double wavelength =
                SatelliteOrbitModel
                        .LIGHT_SPEED_METERS_PER_SEC
                        / frequencyHz;

        return 20.0
                * Math.log10(
                4.0
                        * Math.PI
                        * Math.max(
                        1.0,
                        distanceMeters
                )
                        / wavelength
        );
    }

    public static double noiseFloorDbm(
            double bandwidthHz,
            double noiseFigureDb
    ) {
        return -174.0
                + 10.0
                * Math.log10(
                Math.max(
                        1.0,
                        bandwidthHz
                )
        )
                + noiseFigureDb;
    }

    private static double atmosphericLossDb(
            SatelliteBandPreset band,
            double elevationDeg
    ) {
        double sin =
                Math.sin(
                        Math.toRadians(
                                Math.max(
                                        5.0,
                                        elevationDeg
                                )
                        )
                );

        return band
                .zenithAtmosphericLossDb()
                / Math.max(
                0.15,
                sin
        );
    }

    private static double wattsToDbm(
            double watts
    ) {
        return 10.0
                * Math.log10(
                Math.max(
                        1.0E-15,
                        watts
                )
                        * 1000.0
        );
    }

    private static double logisticPacketSuccess(
            double snrDb
    ) {
        double value =
                1.0
                        / (
                        1.0
                                + Math.exp(
                                -(snrDb - 2.0)
                                        / 1.75
                        )
                );

        return Math.max(
                0.0,
                Math.min(
                        1.0,
                        value
                )
        );
    }
}

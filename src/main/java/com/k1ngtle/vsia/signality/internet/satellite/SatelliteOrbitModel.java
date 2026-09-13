package com.k1ngtle.vsia.signality.internet.satellite;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public final class SatelliteOrbitModel {
    public static final double EARTH_RADIUS_METERS =
            6_371_000.0;

    public static final double EARTH_MU =
            3.986004418E14;

    public static final double EARTH_ROTATION_RAD_PER_SEC =
            7.2921159E-5;

    public static final double LIGHT_SPEED_METERS_PER_SEC =
            299_792_458.0;

    public static final double LEO_ALTITUDE_METERS =
            550_000.0;

    public static final double INCLINATION_RAD =
            Math.toRadians(
                    53.0
            );

    public static final double SIMULATION_SPEED =
            60.0;

    private static final int PLANES =
            12;

    private static final int SATELLITES_PER_PLANE =
            12;

    private SatelliteOrbitModel() {
    }

    public static double simulationSeconds(
            ServerLevel level
    ) {
        return level.getGameTime()
                / 20.0
                * SIMULATION_SPEED;
    }

    public static List<SatelliteOrbitState> constellation(
            double simulationSeconds
    ) {
        List<SatelliteOrbitState> states =
                new ArrayList<>(
                        PLANES
                                * SATELLITES_PER_PLANE
                );

        double radius =
                EARTH_RADIUS_METERS
                        + LEO_ALTITUDE_METERS;

        double meanMotion =
                Math.sqrt(
                        EARTH_MU
                                / (
                                radius
                                        * radius
                                        * radius
                        )
                );

        for (int plane = 0;
             plane < PLANES;
             plane++) {
            double raan =
                    2.0
                            * Math.PI
                            * plane
                            / PLANES;

            for (int index = 0;
                 index < SATELLITES_PER_PLANE;
                 index++) {
                double phase =
                        2.0
                                * Math.PI
                                * index
                                / SATELLITES_PER_PLANE
                                + plane
                                * Math.PI
                                / SATELLITES_PER_PLANE;

                double anomaly =
                        meanMotion
                                * simulationSeconds
                                + phase;

                SatelliteOrbitState state =
                        state(
                                "LEO-"
                                        + (
                                        plane + 1
                                )
                                        + "-"
                                        + (
                                        index + 1
                                ),
                                radius,
                                raan,
                                anomaly,
                                simulationSeconds,
                                meanMotion
                        );

                states.add(
                        state
                );
            }
        }

        return List.copyOf(
                states
        );
    }

    private static SatelliteOrbitState state(
            String name,
            double radius,
            double raan,
            double anomaly,
            double simulationSeconds,
            double meanMotion
    ) {
        double xOrb =
                radius
                        * Math.cos(
                        anomaly
                );

        double yOrb =
                radius
                        * Math.sin(
                        anomaly
                );

        double vxOrb =
                -radius
                        * meanMotion
                        * Math.sin(
                        anomaly
                );

        double vyOrb =
                radius
                        * meanMotion
                        * Math.cos(
                        anomaly
                );

        Vec3 positionEci =
                rotateOrbitToEci(
                        xOrb,
                        yOrb,
                        raan
                );

        Vec3 velocityEci =
                rotateOrbitToEci(
                        vxOrb,
                        vyOrb,
                        raan
                );

        double earthAngle =
                EARTH_ROTATION_RAD_PER_SEC
                        * simulationSeconds;

        Vec3 positionEcef =
                rotateZ(
                        positionEci,
                        -earthAngle
                );

        Vec3 rotatedVelocity =
                rotateZ(
                        velocityEci,
                        -earthAngle
                );

        Vec3 earthRotationVelocity =
                new Vec3(
                        -EARTH_ROTATION_RAD_PER_SEC
                                * positionEcef.y,
                        EARTH_ROTATION_RAD_PER_SEC
                                * positionEcef.x,
                        0.0
                );

        Vec3 velocityEcef =
                rotatedVelocity
                        .subtract(
                                earthRotationVelocity
                        );

        return new SatelliteOrbitState(
                name,
                positionEcef,
                velocityEcef
        );
    }

    private static Vec3 rotateOrbitToEci(
            double xOrb,
            double yOrb,
            double raan
    ) {
        double xInclined =
                xOrb;

        double yInclined =
                yOrb
                        * Math.cos(
                        INCLINATION_RAD
                );

        double zInclined =
                yOrb
                        * Math.sin(
                        INCLINATION_RAD
                );

        double cosRaan =
                Math.cos(
                        raan
                );

        double sinRaan =
                Math.sin(
                        raan
                );

        return new Vec3(
                xInclined
                        * cosRaan
                        - yInclined
                        * sinRaan,
                xInclined
                        * sinRaan
                        + yInclined
                        * cosRaan,
                zInclined
        );
    }

    private static Vec3 rotateZ(
            Vec3 value,
            double angle
    ) {
        double cos =
                Math.cos(
                        angle
                );

        double sin =
                Math.sin(
                        angle
                );

        return new Vec3(
                value.x
                        * cos
                        - value.y
                        * sin,
                value.x
                        * sin
                        + value.y
                        * cos,
                value.z
        );
    }

    public static Vec3 groundPositionEcef(
            BlockPos pos
    ) {
        double altitude =
                Math.max(
                        -64.0,
                        pos.getY()
                );

        double latitude =
                pos.getZ()
                        / EARTH_RADIUS_METERS;

        latitude =
                Math.max(
                        -Math.toRadians(
                                85.0
                        ),
                        Math.min(
                                Math.toRadians(
                                        85.0
                                ),
                                latitude
                        )
                );

        double cosLat =
                Math.cos(
                        latitude
                );

        double longitude =
                pos.getX()
                        / Math.max(
                        1.0,
                        EARTH_RADIUS_METERS
                                * cosLat
                );

        double radius =
                EARTH_RADIUS_METERS
                        + altitude;

        return new Vec3(
                radius
                        * cosLat
                        * Math.cos(
                        longitude
                ),
                radius
                        * cosLat
                        * Math.sin(
                        longitude
                ),
                radius
                        * Math.sin(
                        latitude
                )
        );
    }

    public static double elevationDeg(
            Vec3 groundEcef,
            Vec3 satelliteEcef
    ) {
        Vec3 lineOfSight =
                satelliteEcef
                        .subtract(
                                groundEcef
                        );

        double range =
                lineOfSight.length();

        if (range <= 0.0) {
            return 90.0;
        }

        Vec3 up =
                groundEcef.normalize();

        double sinElevation =
                lineOfSight
                        .scale(
                                1.0 / range
                        )
                        .dot(
                                up
                        );

        sinElevation =
                Math.max(
                        -1.0,
                        Math.min(
                                1.0,
                                sinElevation
                        )
                );

        return Math.toDegrees(
                Math.asin(
                        sinElevation
                )
        );
    }

    public static double radialVelocityMetersPerSecond(
            Vec3 groundEcef,
            SatelliteOrbitState satellite
    ) {
        Vec3 line =
                satellite
                        .positionEcefMeters()
                        .subtract(
                                groundEcef
                        );

        if (line.lengthSqr()
                <= 1.0E-12) {
            return 0.0;
        }

        return satellite
                .velocityEcefMetersPerSecond()
                .dot(
                        line.normalize()
                );
    }
}

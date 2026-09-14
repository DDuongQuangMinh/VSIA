package com.k1ngtle.vsia.signality.radar.network;

import net.minecraft.world.phys.Vec3;

public final class RadarPpiProjection {
    private RadarPpiProjection() {
    }

    public static Point project(
            Vec3 radarOrigin,
            Vec3 northAxis,
            RadarNetworkTrack track,
            double displayRangeMeters
    ) {
        Vec3 delta =
                track.position()
                        .subtract(
                                radarOrigin
                        );

        Vec3 horizontal =
                new Vec3(
                        delta.x,
                        0.0,
                        delta.z
                );

        double range =
                horizontal.length();

        Vec3 north =
                new Vec3(
                        northAxis.x,
                        0.0,
                        northAxis.z
                );

        if (north.lengthSqr()
                < 1.0E-10) {
            north =
                    new Vec3(
                            0.0,
                            0.0,
                            -1.0
                    );
        } else {
            north =
                    north.normalize();
        }

        Vec3 direction =
                range < 1.0E-9
                        ? north
                        : horizontal.scale(
                        1.0 / range
                );

        double dot =
                north.x
                        * direction.x
                        + north.z
                        * direction.z;

        double cross =
                north.x
                        * direction.z
                        - north.z
                        * direction.x;

        double bearing =
                Math.atan2(
                        cross,
                        dot
                );

        double normalized =
                range
                        / Math.max(
                        1.0,
                        displayRangeMeters
                );

        return new Point(
                Math.sin(
                        bearing
                )
                        * normalized,
                -Math.cos(
                        bearing
                )
                        * normalized,
                range,
                bearing,
                normalized <= 1.0
        );
    }

    public record Point(
            double x,
            double y,
            double rangeMeters,
            double bearingRad,
            boolean insideRange
    ) {
    }
}

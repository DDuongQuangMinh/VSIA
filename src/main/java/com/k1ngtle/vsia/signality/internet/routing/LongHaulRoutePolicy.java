package com.k1ngtle.vsia.signality.internet.routing;

import net.minecraft.world.phys.Vec3;

import java.util.Locale;

public final class LongHaulRoutePolicy {
    public static final double BLOCKS_PER_KILOMETER =
            1000.0;

    public static final double SATELLITE_REQUIRED_DISTANCE_BLOCKS =
            5000.0;

    private LongHaulRoutePolicy() {
    }

    public static boolean requiresSatellite(
            Vec3 source,
            Vec3 destination
    ) {
        if (source == null
                || destination == null) {
            return false;
        }

        return source.distanceToSqr(
                destination
        ) >= SATELLITE_REQUIRED_DISTANCE_BLOCKS
                * SATELLITE_REQUIRED_DISTANCE_BLOCKS;
    }

    public static double distanceBlocks(
            Vec3 source,
            Vec3 destination
    ) {
        if (source == null
                || destination == null) {
            return 0.0;
        }

        return source.distanceTo(
                destination
        );
    }

    public static double distanceKilometers(
            Vec3 source,
            Vec3 destination
    ) {
        return distanceBlocks(
                source,
                destination
        ) / BLOCKS_PER_KILOMETER;
    }

    public static String describeDistance(
            double distanceBlocks
    ) {
        return String.format(
                Locale.ROOT,
                "%.3f km (%d blocks)",
                distanceBlocks
                        / BLOCKS_PER_KILOMETER,
                Math.round(
                        distanceBlocks
                )
        );
    }
}

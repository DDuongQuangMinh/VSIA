package com.k1ngtle.vsia.cockpit.program;

import java.util.UUID;

/** Shared fail-closed access gate; a session additionally binds the physical server objects. */
public final class DisplayLaptopSessionRules {
    public static final double MAX_DISTANCE_SQUARED = 64.0;
    private DisplayLaptopSessionRules() { }
    public static boolean accessible(UUID expected, UUID actual, boolean sameDimension,
            boolean chunkLoaded, boolean permitted, double distanceSquared) {
        return expected != null && expected.equals(actual) && sameDimension && chunkLoaded && permitted
                && Double.isFinite(distanceSquared) && distanceSquared >= 0
                && distanceSquared <= MAX_DISTANCE_SQUARED;
    }
}

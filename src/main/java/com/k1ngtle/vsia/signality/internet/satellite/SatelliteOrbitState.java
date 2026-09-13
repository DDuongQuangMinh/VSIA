package com.k1ngtle.vsia.signality.internet.satellite;

import net.minecraft.world.phys.Vec3;

public record SatelliteOrbitState(
        String name,
        Vec3 positionEcefMeters,
        Vec3 velocityEcefMetersPerSecond
) {
}

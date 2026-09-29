package com.k1ngtle.vsia.cockpit.display.telemetry;

import net.minecraft.world.phys.Vec3;

public record AircraftTelemetry(
        Vec3 position,
        Vec3 velocity,
        double headingDeg,
        double pitchDeg,
        double rollDeg,
        double altitudeMeters,
        double speedMps,
        double verticalSpeedMps
) {
}

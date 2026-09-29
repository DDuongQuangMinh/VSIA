package com.k1ngtle.vsia.cockpit.display.telemetry;

import net.minecraft.world.phys.Vec3;

public record AircraftTelemetry(
        boolean shipDetected,
        String shipLabel,
        Vec3 position,
        Vec3 velocity,
        double headingDeg,
        double pitchDeg,
        double rollDeg,
        double altitudeMeters,
        double speedMps,
        double verticalSpeedMps
) {
    public static AircraftTelemetry noShip(
            Vec3 position
    ) {
        return new AircraftTelemetry(
                false,
                "---No Ship Detected---",
                position,
                Vec3.ZERO,
                0.0,
                0.0,
                0.0,
                position.y,
                0.0,
                0.0
        );
    }
}

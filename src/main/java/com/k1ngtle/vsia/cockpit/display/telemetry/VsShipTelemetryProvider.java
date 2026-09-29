package com.k1ngtle.vsia.cockpit.display.telemetry;

import com.k1ngtle.vsia.cockpit.F35CockpitSeatBlockEntity;
import com.k1ngtle.vsia.cockpit.F35VsShipHelper;
import net.minecraft.world.phys.Vec3;

public final class VsShipTelemetryProvider
        implements AircraftTelemetryProvider {
    @Override
    public AircraftTelemetry capture(
            F35CockpitSeatBlockEntity cockpit,
            float partialTick
    ) {
        F35VsShipHelper.ShipSnapshot ship =
                F35VsShipHelper.shipSnapshot(
                        cockpit
                );

        if (!ship.detected()) {
            return AircraftTelemetry.noShip(
                    ship.worldCenter()
            );
        }

        Vec3 velocity =
                ship.velocity();

        return new AircraftTelemetry(
                true,
                ship.shipSlug(),
                ship.worldCenter(),
                velocity,
                ship.headingDeg(),
                ship.pitchDeg(),
                ship.rollDeg(),
                ship.worldCenter().y,
                velocity.length(),
                velocity.y
        );
    }
}

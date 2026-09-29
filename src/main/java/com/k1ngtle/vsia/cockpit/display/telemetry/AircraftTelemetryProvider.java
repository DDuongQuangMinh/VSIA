package com.k1ngtle.vsia.cockpit.display.telemetry;

import com.k1ngtle.vsia.cockpit.F35CockpitSeatBlockEntity;

public interface AircraftTelemetryProvider {
    AircraftTelemetry capture(
            F35CockpitSeatBlockEntity cockpit,
            float partialTick
    );
}

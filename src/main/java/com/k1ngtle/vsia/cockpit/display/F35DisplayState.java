package com.k1ngtle.vsia.cockpit.display;

import com.k1ngtle.vsia.cockpit.display.stores.F35StoresSnapshot;
import com.k1ngtle.vsia.cockpit.display.telemetry.AircraftTelemetry;
import java.util.List;
import org.jetbrains.annotations.Nullable;

public record F35DisplayState(
        AircraftTelemetry ownship,
        String radarNetwork,
        List<F35RadarTrackView> tracks,
        @Nullable F35RadarTrackView selectedTrack,
        double radarRangeMeters,
        F35StoresSnapshot stores,
        long localTimeMillis
) {
    public F35DisplayState {
        tracks =
                List.copyOf(
                        tracks
                );
    }
}

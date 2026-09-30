package com.k1ngtle.vsia.cockpit.display;

import com.k1ngtle.vsia.cockpit.detection.F35DetectionContact;
import com.k1ngtle.vsia.cockpit.detection.F35ShipSilhouette;
import com.k1ngtle.vsia.cockpit.display.stores.F35StoresSnapshot;
import com.k1ngtle.vsia.cockpit.display.telemetry.AircraftTelemetry;
import java.util.List;
import org.jetbrains.annotations.Nullable;

public record F35DisplayState(
        AircraftTelemetry ownship,
        String radarNetwork,
        List<F35RadarTrackView> tracks,
        List<F35DetectionContact> detections,
        F35ShipSilhouette shipSilhouette,
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

        detections =
                List.copyOf(
                        detections
                );

        shipSilhouette =
                shipSilhouette == null
                        ? F35ShipSilhouette.empty()
                        : shipSilhouette;
    }

    public int totalContactCount() {
        return tracks.size()
                + detections.size();
    }
}

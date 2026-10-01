package com.k1ngtle.vsia.cockpit.display;

import com.k1ngtle.vsia.cockpit.F35CockpitSeatBlockEntity;
import com.k1ngtle.vsia.cockpit.client.F35DisplayClientConfig;
import com.k1ngtle.vsia.cockpit.client.F35TargetLockClient;
import com.k1ngtle.vsia.cockpit.detection.F35DetectionContact;
import com.k1ngtle.vsia.cockpit.detection.F35ShipSilhouette;
import com.k1ngtle.vsia.cockpit.display.stores.F35StoresRegistry;
import com.k1ngtle.vsia.cockpit.display.stores.F35StoresSnapshot;
import com.k1ngtle.vsia.cockpit.display.telemetry.AircraftTelemetry;
import com.k1ngtle.vsia.cockpit.display.telemetry.F35TelemetryRegistry;
import java.util.List;

public final class F35DisplayStateFactory {
    private static final double[] RANGE_STEPS =
            new double[]{
                    500.0,
                    1000.0,
                    5000.0,
                    10_000.0,
                    20_000.0,
                    40_000.0,
                    80_000.0
            };

    private F35DisplayStateFactory() {
    }

    public static F35DisplayState capture(
            F35CockpitSeatBlockEntity cockpit,
            float partialTick
    ) {
        F35DisplayClientConfig.bind(cockpit);
        F35TargetLockClient.bind(cockpit.cockpitId());
        AircraftTelemetry telemetry =
                F35TelemetryRegistry
                        .provider()
                        .capture(
                                cockpit,
                                partialTick
                        );

        F35StoresSnapshot stores =
                F35StoresRegistry
                        .provider()
                        .capture(
                                cockpit
                        );

        F35ClientDetectionCache.Snapshot detection =
                F35ClientDetectionCache.snapshot(cockpit.cockpitId());

        List<F35DetectionContact> detections =
                detection.contacts();

        F35ShipSilhouette silhouette =
                detection.silhouette();

        if (!telemetry.shipDetected()) {
            return new F35DisplayState(
                    telemetry,
                    "NO-SHIP",
                    List.of(),
                    List.of(),
                    F35ShipSilhouette.empty(),
                    null,
                    0.0,
                    stores,
                    System.currentTimeMillis()
            );
        }

        F35ClientRadarCache.Snapshot radar =
                F35ClientRadarCache.snapshot();

        List<F35RadarTrackView> tracks =
                radar.tracks();

        F35TargetLockClient.validate(
                tracks,
                detections
        );

        F35RadarTrackView selected =
                F35TargetLockClient.lockedRadarTrack(
                        tracks
                );

        double farthestRadar =
                tracks.stream()
                        .mapToDouble(
                                track ->
                                        track.position()
                                                .distanceTo(
                                                        telemetry.position()
                                                )
                        )
                        .max()
                        .orElse(
                                0.0
                        );

        double farthestDetection =
                detections.stream()
                        .mapToDouble(
                                contact ->
                                        contact.position()
                                                .distanceTo(
                                                        telemetry.position()
                                                )
                        )
                        .max()
                        .orElse(
                                0.0
                        );

        double farthest =
                Math.max(
                        farthestRadar,
                        farthestDetection
                );

        double forcedRange =
                F35DisplayClientConfig.forcedRadarRangeMeters();

        double radarRange =
                forcedRange > 0.0
                        ? forcedRange
                        : selectRange(
                        farthest
                );

        return new F35DisplayState(
                telemetry,
                radar.networkId(),
                tracks,
                detections,
                silhouette,
                selected,
                radarRange,
                stores,
                System.currentTimeMillis()
        );
    }

    private static double selectRange(
            double farthestMeters
    ) {
        double requested =
                Math.max(
                        250.0,
                        farthestMeters
                                * 1.25
                );

        for (double step :
                RANGE_STEPS) {
            if (requested <= step) {
                return step;
            }
        }

        return RANGE_STEPS[
                RANGE_STEPS.length - 1
                ];
    }
}

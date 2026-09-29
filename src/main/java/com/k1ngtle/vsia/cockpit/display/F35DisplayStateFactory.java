package com.k1ngtle.vsia.cockpit.display;

import com.k1ngtle.vsia.cockpit.F35CockpitSeatBlockEntity;
import com.k1ngtle.vsia.cockpit.display.stores.F35StoresRegistry;
import com.k1ngtle.vsia.cockpit.display.stores.F35StoresSnapshot;
import com.k1ngtle.vsia.cockpit.display.telemetry.AircraftTelemetry;
import com.k1ngtle.vsia.cockpit.display.telemetry.F35TelemetryRegistry;
import java.util.Comparator;
import java.util.List;

public final class F35DisplayStateFactory {
    private static final double[] RANGE_STEPS =
            new double[]{
                    500.0,
                    1000.0,
                    5000.0,
                    10_000.0,
                    20_000.0,
                    40_000.0
            };

    private F35DisplayStateFactory() {
    }

    public static F35DisplayState capture(
            F35CockpitSeatBlockEntity cockpit,
            float partialTick
    ) {
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

        if (!telemetry.shipDetected()) {
            return new F35DisplayState(
                    telemetry,
                    "NO-SHIP",
                    List.of(),
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

        F35RadarTrackView selected =
                tracks.stream()
                        .min(
                                Comparator.comparingDouble(
                                        track ->
                                                track.position()
                                                        .distanceTo(
                                                                telemetry.position()
                                                        )
                                )
                        )
                        .orElse(
                                null
                        );

        double farthest =
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

        double radarRange =
                selectRange(
                        farthest
                );

        return new F35DisplayState(
                telemetry,
                radar.networkId(),
                tracks,
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

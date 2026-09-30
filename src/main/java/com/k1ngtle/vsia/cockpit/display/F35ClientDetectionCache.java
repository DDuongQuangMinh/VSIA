package com.k1ngtle.vsia.cockpit.display;

import com.k1ngtle.vsia.cockpit.detection.F35DetectionContact;
import com.k1ngtle.vsia.cockpit.detection.F35ShipSilhouette;
import java.util.List;

public final class F35ClientDetectionCache {
    private static volatile Snapshot snapshot =
            new Snapshot(
                    0L,
                    List.of(),
                    F35ShipSilhouette.empty()
            );

    private F35ClientDetectionCache() {
    }

    public static Snapshot snapshot() {
        return snapshot;
    }

    public static void accept(
            long serverTick,
            List<F35DetectionContact> contacts,
            F35ShipSilhouette silhouette
    ) {
        snapshot =
                new Snapshot(
                        serverTick,
                        contacts,
                        silhouette
                );
    }

    public record Snapshot(
            long serverTick,
            List<F35DetectionContact> contacts,
            F35ShipSilhouette silhouette
    ) {
        public Snapshot {
            contacts =
                    List.copyOf(
                            contacts
                    );

            silhouette =
                    silhouette == null
                            ? F35ShipSilhouette.empty()
                            : silhouette;
        }
    }
}

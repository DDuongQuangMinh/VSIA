package com.k1ngtle.vsia.cockpit.display;

import java.util.List;

public final class F35ClientRadarCache {
    private static volatile Snapshot snapshot =
            new Snapshot(
                    "default",
                    0L,
                    List.of()
            );

    private F35ClientRadarCache() {
    }

    public static Snapshot snapshot() {
        return snapshot;
    }

    public static void accept(
            String networkId,
            long serverTick,
            List<F35RadarTrackView> tracks
    ) {
        snapshot =
                new Snapshot(
                        networkId,
                        serverTick,
                        List.copyOf(
                                tracks
                        )
                );
    }

    public record Snapshot(
            String networkId,
            long serverTick,
            List<F35RadarTrackView> tracks
    ) {
        public Snapshot {
            tracks =
                    List.copyOf(
                            tracks
                    );
        }
    }
}

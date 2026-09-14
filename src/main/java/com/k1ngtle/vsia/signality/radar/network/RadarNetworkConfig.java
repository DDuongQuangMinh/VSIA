package com.k1ngtle.vsia.signality.radar.network;

public record RadarNetworkConfig(
        int baseLatencyTicks,
        int jitterTicks,
        double packetLossProbability,
        int confirmHits,
        int coastAfterTicks,
        int dropAfterTicks,
        int maxReportsPerTick,
        int maxQueuedReports,
        double baseGateMeters
) {
    public RadarNetworkConfig {
        baseLatencyTicks = Math.max(0, baseLatencyTicks);
        jitterTicks = Math.max(0, jitterTicks);
        packetLossProbability =
                Math.max(
                        0.0,
                        Math.min(
                                0.95,
                                packetLossProbability
                        )
                );

        confirmHits = Math.max(1, confirmHits);
        coastAfterTicks = Math.max(1, coastAfterTicks);
        dropAfterTicks =
                Math.max(
                        coastAfterTicks + 1,
                        dropAfterTicks
                );

        maxReportsPerTick =
                Math.max(
                        1,
                        maxReportsPerTick
                );

        maxQueuedReports =
                Math.max(
                        maxReportsPerTick,
                        maxQueuedReports
                );

        baseGateMeters =
                Math.max(
                        1.0,
                        baseGateMeters
                );
    }

    public static RadarNetworkConfig realisticDefault() {
        return new RadarNetworkConfig(
                1,
                2,
                0.005,
                3,
                20,
                160,
                512,
                4096,
                18.0
        );
    }
}

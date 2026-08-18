package com.k1ngtle.vsia.signality.engineering.wifi.phy;

public record WifiPhyConfiguration(
        WifiPhyGeneration generation,
        WifiChannelWidth channelWidth,
        WifiGuardInterval guardInterval,
        int spatialStreams,
        int transmitAntennas,
        int receiveAntennas,
        double spatialCorrelation,
        double activeBandwidthFraction
) {
    public WifiPhyConfiguration {
        if (generation == null
                || channelWidth == null
                || guardInterval == null) {
            throw new IllegalArgumentException(
                    "generation/channelWidth/guardInterval"
            );
        }

        channelWidth =
                WifiOfdmNumerologyTable.normalizeWidth(
                        generation,
                        channelWidth
                );

        if (spatialStreams < 1
                || transmitAntennas < 1
                || receiveAntennas < 1) {
            throw new IllegalArgumentException(
                    "antenna/stream count must be >= 1"
            );
        }

        spatialCorrelation =
                clamp01(
                        spatialCorrelation
                );

        activeBandwidthFraction =
                Math.max(
                        0.0,
                        Math.min(
                                1.0,
                                activeBandwidthFraction
                        )
                );
    }

    public static WifiPhyConfiguration from(
            String protocol,
            double bandwidthHz,
            int spatialStreams
    ) {
        WifiPhyGeneration generation =
                WifiPhyGeneration.fromProtocol(
                        protocol
                );

        WifiChannelWidth width =
                WifiOfdmNumerologyTable.normalizeWidth(
                        generation,
                        WifiChannelWidth.nearest(
                                bandwidthHz
                        )
                );

        WifiGuardInterval gi =
                generation == WifiPhyGeneration.HE
                        || generation == WifiPhyGeneration.EHT
                        ? WifiGuardInterval.GI_0_8_US
                        : WifiGuardInterval.GI_0_8_US;

        int antennas =
                Math.max(
                        1,
                        spatialStreams
                );

        return new WifiPhyConfiguration(
                generation,
                width,
                gi,
                spatialStreams,
                antennas,
                antennas,
                0.15,
                1.0
        );
    }

    private static double clamp01(
            double value
    ) {
        return Math.max(
                0.0,
                Math.min(
                        1.0,
                        value
                )
        );
    }
}

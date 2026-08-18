package com.k1ngtle.vsia.signality.engineering.wifi.phy;

import com.k1ngtle.vsia.signality.engineering.wifi.WifiMcs;

public final class WifiPhyRateCalculator {
    private WifiPhyRateCalculator() {
    }

    public static WifiPhyRateResult calculate(
            WifiPhyConfiguration configuration,
            WifiMcs mcs,
            WifiResourceUnit resourceUnit,
            WifiPuncturingPattern puncturing,
            double snrDb
    ) {
        WifiOfdmNumerology numerology =
                WifiOfdmNumerologyTable.resolve(
                        configuration.generation(),
                        configuration.channelWidth(),
                        configuration.guardInterval()
                );

        WifiResourceUnit ru =
                resourceUnit == null
                        ? fullBandRu(
                        numerology.channelWidth()
                )
                        : resourceUnit;

        int dataTones =
                configuration.generation() == WifiPhyGeneration.HE
                        || configuration.generation() == WifiPhyGeneration.EHT
                        ? ru.dataTones()
                        : numerology.dataTones();

        WifiMimoAssessment mimo =
                WifiMimoModel.assess(
                        configuration,
                        snrDb
                );

        double activeFraction =
                configuration.activeBandwidthFraction();

        if (puncturing != null) {
            activeFraction *=
                    puncturing.activeFraction();
        }

        int bitsPerSubcarrier =
                bitsPerSubcarrier(
                        mcs.modulation().bitsPerSymbol()
                );

        double bitsPerSymbol =
                dataTones
                        * bitsPerSubcarrier
                        * mcs.codingRate()
                        * mimo.usableSpatialStreams();

        double grossRate =
                bitsPerSymbol
                        * numerology.symbolRateHz();

        double effectiveRate =
                grossRate
                        * mimo.spatialEfficiency()
                        * activeFraction;

        return new WifiPhyRateResult(
                grossRate,
                effectiveRate,
                numerology.symbolRateHz(),
                dataTones,
                bitsPerSubcarrier,
                mcs.codingRate(),
                mimo.usableSpatialStreams(),
                activeFraction
        );
    }

    public static WifiResourceUnit fullBandRu(
            WifiChannelWidth width
    ) {
        return switch (width) {
            case MHZ_20 -> WifiResourceUnit.RU_242;
            case MHZ_40 -> WifiResourceUnit.RU_484;
            case MHZ_80 -> WifiResourceUnit.RU_996;
            case MHZ_160 -> WifiResourceUnit.RU_2X996;
            case MHZ_320 -> WifiResourceUnit.RU_4X996;
        };
    }

    private static int bitsPerSubcarrier(
            int bitsPerSymbol
    ) {
        return Math.max(
                1,
                bitsPerSymbol
        );
    }
}

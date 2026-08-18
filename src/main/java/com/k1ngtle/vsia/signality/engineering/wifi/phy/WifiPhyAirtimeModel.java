package com.k1ngtle.vsia.signality.engineering.wifi.phy;

import com.k1ngtle.vsia.signality.engineering.wifi.WifiMcs;

public final class WifiPhyAirtimeModel {
    private WifiPhyAirtimeModel() {
    }

    public static WifiPpduEstimate estimate(
            WifiPhyConfiguration configuration,
            WifiMcs mcs,
            long psduBits,
            WifiPuncturingPattern puncturing,
            double snrDb
    ) {
        WifiOfdmNumerology numerology =
                WifiOfdmNumerologyTable.resolve(
                        configuration.generation(),
                        configuration.channelWidth(),
                        configuration.guardInterval()
                );

        WifiPhyRateResult rate =
                WifiPhyRateCalculator.calculate(
                        configuration,
                        mcs,
                        null,
                        puncturing,
                        snrDb
                );

        double bitsPerSymbol =
                rate.grossPhyRateBps()
                        / numerology.symbolRateHz();

        long codedInputBits =
                Math.max(
                        1L,
                        psduBits + serviceAndTailBits(
                                configuration.generation()
                        )
                );

        int symbols =
                Math.max(
                        1,
                        (int) Math.ceil(
                                codedInputBits
                                        / Math.max(
                                        1.0,
                                        bitsPerSymbol
                                )
                        )
                );

        double dataTimeUs =
                symbols
                        * numerology.symbolTimeUs();

        double preambleUs =
                approximatePreambleUs(
                        configuration.generation(),
                        rate.spatialStreams()
                );

        return new WifiPpduEstimate(
                psduBits,
                symbols,
                preambleUs,
                dataTimeUs,
                preambleUs + dataTimeUs,
                rate.grossPhyRateBps()
        );
    }

    private static int serviceAndTailBits(
            WifiPhyGeneration generation
    ) {
        return switch (generation) {
            case LEGACY_OFDM,
                 HT,
                 VHT ->
                    22;

            case HE,
                 EHT ->
                    16;
        };
    }

    private static double approximatePreambleUs(
            WifiPhyGeneration generation,
            int spatialStreams
    ) {
        int streams =
                Math.max(
                        1,
                        spatialStreams
                );

        return switch (generation) {
            case LEGACY_OFDM ->
                    20.0;

            case HT ->
                    32.0
                            + 4.0
                            * streams;

            case VHT ->
                    36.0
                            + 4.0
                            * streams;

            case HE ->
                    40.0
                            + 8.0
                            * streams;

            case EHT ->
                    48.0
                            + 8.0
                            * streams;
        };
    }
}

package com.k1ngtle.vsia.signality.engineering.wifi.phy;

public final class WifiOfdmNumerologyTable {
    private WifiOfdmNumerologyTable() {
    }

    public static WifiOfdmNumerology resolve(
            WifiPhyGeneration generation,
            WifiChannelWidth width,
            WifiGuardInterval guardInterval
    ) {
        if (generation == WifiPhyGeneration.HE
                || generation == WifiPhyGeneration.EHT) {
            return heEht(
                    generation,
                    width,
                    normalizeHeGi(
                            guardInterval
                    )
            );
        }

        return legacyFamily(
                generation,
                width,
                normalizeLegacyGi(
                        guardInterval
                )
        );
    }

    private static WifiOfdmNumerology heEht(
            WifiPhyGeneration generation,
            WifiChannelWidth width,
            WifiGuardInterval gi
    ) {
        int fft =
                switch (width) {
                    case MHZ_20 -> 256;
                    case MHZ_40 -> 512;
                    case MHZ_80 -> 1024;
                    case MHZ_160 -> 2048;
                    case MHZ_320 -> 4096;
                };

        WifiResourceUnit fullBandRu =
                switch (width) {
                    case MHZ_20 -> WifiResourceUnit.RU_242;
                    case MHZ_40 -> WifiResourceUnit.RU_484;
                    case MHZ_80 -> WifiResourceUnit.RU_996;
                    case MHZ_160 -> WifiResourceUnit.RU_2X996;
                    case MHZ_320 -> WifiResourceUnit.RU_4X996;
                };

        return new WifiOfdmNumerology(
                generation,
                width,
                fft,
                78_125.0,
                12.8,
                gi,
                fullBandRu.tones(),
                fullBandRu.dataTones(),
                fullBandRu.pilotTones()
        );
    }

    private static WifiOfdmNumerology legacyFamily(
            WifiPhyGeneration generation,
            WifiChannelWidth requestedWidth,
            WifiGuardInterval gi
    ) {
        WifiChannelWidth width =
                normalizeWidth(
                        generation,
                        requestedWidth
                );

        int fft =
                switch (width) {
                    case MHZ_20 -> 64;
                    case MHZ_40 -> 128;
                    case MHZ_80 -> 256;
                    case MHZ_160 -> 512;
                    case MHZ_320 -> 1024;
                };

        int dataTones;
        int pilotTones;

        if (generation == WifiPhyGeneration.LEGACY_OFDM) {
            dataTones =
                    48;
            pilotTones =
                    4;
        } else {
            switch (width) {
                case MHZ_20 -> {
                    dataTones = 52;
                    pilotTones = 4;
                }

                case MHZ_40 -> {
                    dataTones = 108;
                    pilotTones = 6;
                }

                case MHZ_80 -> {
                    dataTones = 234;
                    pilotTones = 8;
                }

                case MHZ_160 -> {
                    dataTones = 468;
                    pilotTones = 16;
                }

                default -> {
                    dataTones = 468;
                    pilotTones = 16;
                }
            }
        }

        return new WifiOfdmNumerology(
                generation,
                width,
                fft,
                312_500.0,
                3.2,
                gi,
                dataTones + pilotTones,
                dataTones,
                pilotTones
        );
    }

    public static WifiChannelWidth normalizeWidth(
            WifiPhyGeneration generation,
            WifiChannelWidth width
    ) {
        return switch (generation) {
            case LEGACY_OFDM ->
                    WifiChannelWidth.MHZ_20;

            case HT ->
                    width.mhz() > 40
                            ? WifiChannelWidth.MHZ_40
                            : width;

            case VHT,
                 HE ->
                    width.mhz() > 160
                            ? WifiChannelWidth.MHZ_160
                            : width;

            case EHT ->
                    width;
        };
    }

    private static WifiGuardInterval normalizeHeGi(
            WifiGuardInterval gi
    ) {
        if (gi == WifiGuardInterval.GI_0_4_US) {
            return WifiGuardInterval.GI_0_8_US;
        }

        return gi;
    }

    private static WifiGuardInterval normalizeLegacyGi(
            WifiGuardInterval gi
    ) {
        if (gi == WifiGuardInterval.GI_1_6_US
                || gi == WifiGuardInterval.GI_3_2_US) {
            return WifiGuardInterval.GI_0_8_US;
        }

        return gi;
    }
}

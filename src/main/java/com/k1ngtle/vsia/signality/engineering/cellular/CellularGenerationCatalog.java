package com.k1ngtle.vsia.signality.engineering.cellular;

import java.util.EnumMap;
import java.util.Map;

public final class CellularGenerationCatalog {
    private static final Map<CellularGeneration, CellularGenerationProfile> PROFILES =
            new EnumMap<>(CellularGeneration.class);

    static {
        PROFILES.put(
                CellularGeneration.G1_ANALOG,
                new CellularGenerationProfile(
                        CellularGeneration.G1_ANALOG,
                        "AMPS/TACS-like analog simulation",
                        "FDMA",
                        0.0,
                        1,
                        1,
                        6.0,
                        1_000L,
                        false,
                        false
                )
        );

        PROFILES.put(
                CellularGeneration.G2_GSM,
                new CellularGenerationProfile(
                        CellularGeneration.G2_GSM,
                        "GSM/GPRS concepts",
                        "FDMA/TDMA",
                        0.0,
                        1,
                        8,
                        5.0,
                        800L,
                        false,
                        true
                )
        );

        PROFILES.put(
                CellularGeneration.G3_UMTS,
                new CellularGenerationProfile(
                        CellularGeneration.G3_UMTS,
                        "UMTS/WCDMA concepts",
                        "W-CDMA",
                        0.0,
                        1,
                        15,
                        4.0,
                        500L,
                        true,
                        true
                )
        );

        PROFILES.put(
                CellularGeneration.G4_LTE,
                new CellularGenerationProfile(
                        CellularGeneration.G4_LTE,
                        "LTE E-UTRA concepts",
                        "OFDMA/SC-FDMA",
                        15_000.0,
                        4,
                        15,
                        3.0,
                        320L,
                        true,
                        true
                )
        );

        PROFILES.put(
                CellularGeneration.G5_NR,
                new CellularGenerationProfile(
                        CellularGeneration.G5_NR,
                        "5G NR concepts",
                        "CP-OFDM/DFT-s-OFDM",
                        30_000.0,
                        8,
                        15,
                        3.0,
                        160L,
                        true,
                        true
                )
        );
    }

    private CellularGenerationCatalog() {
    }

    public static CellularGenerationProfile profile(CellularGeneration generation) {
        CellularGeneration key = generation == null
                ? CellularGeneration.G5_NR
                : generation;
        return PROFILES.get(key);
    }

    public static CellularGenerationProfile fromProtocol(String protocol) {
        return profile(CellularGeneration.fromProtocol(protocol));
    }
}

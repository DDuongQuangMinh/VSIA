package com.k1ngtle.vsia.signality.engineering.cellular;

import java.util.List;

public final class CellularBandCatalog {
    private static final List<CellularBand> BANDS = List.of(
            new CellularBand(
                    "ANALOG-900",
                    CellularGeneration.G1_ANALOG,
                    869.0e6,
                    894.0e6,
                    824.0e6,
                    849.0e6,
                    false
            ),
            new CellularBand(
                    "GSM900",
                    CellularGeneration.G2_GSM,
                    935.0e6,
                    960.0e6,
                    890.0e6,
                    915.0e6,
                    false
            ),
            new CellularBand(
                    "UMTS-B1",
                    CellularGeneration.G3_UMTS,
                    2110.0e6,
                    2170.0e6,
                    1920.0e6,
                    1980.0e6,
                    false
            ),
            new CellularBand(
                    "LTE-B3",
                    CellularGeneration.G4_LTE,
                    1805.0e6,
                    1880.0e6,
                    1710.0e6,
                    1785.0e6,
                    false
            ),
            new CellularBand(
                    "LTE-B20",
                    CellularGeneration.G4_LTE,
                    791.0e6,
                    821.0e6,
                    832.0e6,
                    862.0e6,
                    false
            ),
            new CellularBand(
                    "NR-n28",
                    CellularGeneration.G5_NR,
                    758.0e6,
                    803.0e6,
                    703.0e6,
                    748.0e6,
                    false
            ),
            new CellularBand(
                    "NR-n41",
                    CellularGeneration.G5_NR,
                    2496.0e6,
                    2690.0e6,
                    2496.0e6,
                    2690.0e6,
                    true
            ),
            new CellularBand(
                    "NR-n78",
                    CellularGeneration.G5_NR,
                    3300.0e6,
                    3800.0e6,
                    3300.0e6,
                    3800.0e6,
                    true
            )
    );

    private CellularBandCatalog() {
    }

    public static List<CellularBand> all() {
        return BANDS;
    }

    public static CellularBand bestMatch(
            CellularGeneration generation,
            double downlinkFrequencyHz
    ) {
        CellularBand exact = BANDS.stream()
                .filter(band -> band.generation() == generation)
                .filter(band -> band.containsDownlink(downlinkFrequencyHz))
                .findFirst()
                .orElse(null);

        if (exact != null) {
            return exact;
        }

        return BANDS.stream()
                .filter(band -> band.generation() == generation)
                .min((a, b) -> Double.compare(
                        Math.abs(a.centerDownlinkHz() - downlinkFrequencyHz),
                        Math.abs(b.centerDownlinkHz() - downlinkFrequencyHz)
                ))
                .orElse(null);
    }
}

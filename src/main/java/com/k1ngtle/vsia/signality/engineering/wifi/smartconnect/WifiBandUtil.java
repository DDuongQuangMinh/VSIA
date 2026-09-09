package com.k1ngtle.vsia.signality.engineering.wifi.smartconnect;

public final class WifiBandUtil {
    private WifiBandUtil() {
    }

    public static WifiBand bandForFrequency(
            double frequencyHz
    ) {
        if (!Double.isFinite(
                frequencyHz
        )
                || frequencyHz <= 0.0D) {
            return WifiBand.UNKNOWN;
        }

        if (frequencyHz >= 2_400_000_000.0D
                && frequencyHz < 2_500_000_000.0D) {
            return WifiBand.TWO_FOUR_GHZ;
        }

        if (frequencyHz >= 4_900_000_000.0D
                && frequencyHz < 5_925_000_000.0D) {
            return WifiBand.FIVE_GHZ;
        }

        if (frequencyHz >= 5_925_000_000.0D
                && frequencyHz <= 7_125_000_000.0D) {
            return WifiBand.SIX_GHZ;
        }

        return WifiBand.UNKNOWN;
    }

    public static boolean mb1DualBandEligible(
            double frequencyHz
    ) {
        WifiBand band =
                bandForFrequency(
                        frequencyHz
                );

        return band == WifiBand.TWO_FOUR_GHZ
                || band == WifiBand.FIVE_GHZ;
    }

    public static String displayName(
            WifiBand band
    ) {
        if (band == null) {
            return "Unknown";
        }

        return switch (band) {
            case TWO_FOUR_GHZ -> "2.4 GHz";
            case FIVE_GHZ -> "5 GHz";
            case SIX_GHZ -> "6 GHz";
            case UNKNOWN -> "Unknown";
        };
    }
}

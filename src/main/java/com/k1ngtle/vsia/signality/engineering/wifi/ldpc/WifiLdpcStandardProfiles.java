package com.k1ngtle.vsia.signality.engineering.wifi.ldpc;

import java.util.List;

public final class WifiLdpcStandardProfiles {
    public static final String IEEE_80211_REFERENCE_R34 =
            "MathWorks ldpcDecode documentation: WLAN IEEE 802.11 rate-3/4 prototype matrix";

    public static final String CROSSCHECK_REFERENCE =
            "simgunz/802.11n-ldpc protoH tables, commit 615b59463386574bf266228ab70f0dddbc8ce227";

    private WifiLdpcStandardProfiles() {
    }

    public static List<WifiLdpcStandardProfile> allPinned() {
        return List.of(
                n648RateOneHalf(),
                n648RateTwoThirds(),
                n648RateThreeQuarter(),
                n648RateFiveSixths(),
                n1296RateOneHalf(),
                n1296RateTwoThirds(),
                n1296RateThreeQuarter(),
                n1296RateFiveSixths()
        );
    }

    public static List<WifiLdpcStandardProfile> n648Family() {
        return List.of(
                n648RateOneHalf(),
                n648RateTwoThirds(),
                n648RateThreeQuarter(),
                n648RateFiveSixths()
        );
    }

    public static List<WifiLdpcStandardProfile> n1296Family() {
        return List.of(
                n1296RateOneHalf(),
                n1296RateTwoThirds(),
                n1296RateThreeQuarter(),
                n1296RateFiveSixths()
        );
    }

    public static WifiLdpcStandardProfile n648RateOneHalf() {
        return profile(
                "ieee-80211-ldpc-n648-r12",
                WifiLdpcCodewordLength.N_648,
                WifiLdpcTargetRate.RATE_1_2,
                27,
                new int[][] {
                        {0, -1, -1, -1, 0, 0, -1, -1, 0, -1, -1, 0, 1, 0, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
                        {22, 0, -1, -1, 17, -1, 0, 0, 12, -1, -1, -1, -1, 0, 0, -1, -1, -1, -1, -1, -1, -1, -1, -1},
                        {6, -1, 0, -1, 10, -1, -1, -1, 24, -1, 0, -1, -1, -1, 0, 0, -1, -1, -1, -1, -1, -1, -1, -1},
                        {2, -1, -1, 0, 20, -1, -1, -1, 25, 0, -1, -1, -1, -1, -1, 0, 0, -1, -1, -1, -1, -1, -1, -1},
                        {23, -1, -1, -1, 3, -1, -1, -1, 0, -1, 9, 11, -1, -1, -1, -1, 0, 0, -1, -1, -1, -1, -1, -1},
                        {24, -1, 23, 1, 17, -1, 3, -1, 10, -1, -1, -1, -1, -1, -1, -1, -1, 0, 0, -1, -1, -1, -1, -1},
                        {25, -1, -1, -1, 8, -1, -1, -1, 7, 18, -1, -1, 0, -1, -1, -1, -1, -1, 0, 0, -1, -1, -1, -1},
                        {13, 24, -1, -1, 0, -1, 8, -1, 6, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 0, 0, -1, -1, -1},
                        {7, 20, -1, 16, 22, 10, -1, -1, 23, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 0, 0, -1, -1},
                        {11, -1, -1, -1, 19, -1, -1, -1, 13, -1, 3, 17, -1, -1, -1, -1, -1, -1, -1, -1, -1, 0, 0, -1},
                        {25, -1, 8, -1, 23, 18, -1, 14, 9, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 0, 0},
                        {3, -1, -1, -1, 16, -1, -1, 2, 25, 5, -1, -1, 1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 0}
                },
                CROSSCHECK_REFERENCE
        );
    }

    public static WifiLdpcStandardProfile n648RateTwoThirds() {
        return profile(
                "ieee-80211-ldpc-n648-r23",
                WifiLdpcCodewordLength.N_648,
                WifiLdpcTargetRate.RATE_2_3,
                27,
                new int[][] {
                        {25, 26, 14, -1, 20, -1, 2, -1, 4, -1, -1, 8, -1, 16, -1, 18, 1, 0, -1, -1, -1, -1, -1, -1},
                        {10, 9, 15, 11, -1, 0, -1, 1, -1, -1, 18, -1, 8, -1, 10, -1, -1, 0, 0, -1, -1, -1, -1, -1},
                        {16, 2, 20, 26, 21, -1, 6, -1, 1, 26, -1, 7, -1, -1, -1, -1, -1, -1, 0, 0, -1, -1, -1, -1},
                        {10, 13, 5, 0, -1, 3, -1, 7, -1, -1, 26, -1, -1, 13, -1, 16, -1, -1, -1, 0, 0, -1, -1, -1},
                        {23, 14, 24, -1, 12, -1, 19, -1, 17, -1, -1, -1, 20, -1, 21, -1, 0, -1, -1, -1, 0, 0, -1, -1},
                        {6, 22, 9, 20, -1, 25, -1, 17, -1, 8, -1, 14, -1, 18, -1, -1, -1, -1, -1, -1, -1, 0, 0, -1},
                        {14, 23, 21, 11, 20, -1, 24, -1, 18, -1, 19, -1, -1, -1, -1, 22, -1, -1, -1, -1, -1, -1, 0, 0},
                        {17, 11, 11, 20, -1, 21, -1, 26, -1, 3, -1, -1, 18, -1, 26, -1, 1, -1, -1, -1, -1, -1, -1, 0}
                },
                CROSSCHECK_REFERENCE
        );
    }

    public static WifiLdpcStandardProfile n648RateThreeQuarter() {
        return profile(
                "ieee-80211-ldpc-n648-r34",
                WifiLdpcCodewordLength.N_648,
                WifiLdpcTargetRate.RATE_3_4,
                27,
                new int[][] {
                        {16, 17, 22, 24, 9, 3, 14, -1, 4, 2, 7, -1, 26, -1, 2, -1, 21, -1, 1, 0, -1, -1, -1, -1},
                        {25, 12, 12, 3, 3, 26, 6, 21, -1, 15, 22, -1, 15, -1, 4, -1, -1, 16, -1, 0, 0, -1, -1, -1},
                        {25, 18, 26, 16, 22, 23, 9, -1, 0, -1, 4, -1, 4, -1, 8, 23, 11, -1, -1, -1, 0, 0, -1, -1},
                        {9, 7, 0, 1, 17, -1, -1, 7, 3, -1, 3, 23, -1, 16, -1, -1, 21, -1, 0, -1, -1, 0, 0, -1},
                        {24, 5, 26, 7, 1, -1, -1, 15, 24, 15, -1, 8, -1, 13, -1, 13, -1, 11, -1, -1, -1, -1, 0, 0},
                        {2, 2, 19, 14, 24, 1, 15, 19, -1, 21, -1, 2, -1, 24, -1, 3, -1, 2, 1, -1, -1, -1, -1, 0}
                },
                IEEE_80211_REFERENCE_R34 + "; " + CROSSCHECK_REFERENCE
        );
    }

    public static WifiLdpcStandardProfile n648RateFiveSixths() {
        return profile(
                "ieee-80211-ldpc-n648-r56",
                WifiLdpcCodewordLength.N_648,
                WifiLdpcTargetRate.RATE_5_6,
                27,
                new int[][] {
                        {17, 13, 8, 21, 9, 3, 18, 12, 10, 0, 4, 15, 19, 2, 5, 10, 26, 19, 13, 13, 1, 0, -1, -1},
                        {3, 12, 11, 14, 11, 25, 5, 18, 0, 9, 2, 26, 26, 10, 24, 7, 14, 20, 4, 2, -1, 0, 0, -1},
                        {22, 16, 4, 3, 10, 21, 12, 5, 21, 14, 19, 5, -1, 8, 5, 18, 11, 5, 5, 15, 0, -1, 0, 0},
                        {7, 7, 14, 14, 4, 16, 16, 24, 24, 10, 1, 7, 15, 6, 10, 26, 8, 18, 21, 14, 1, -1, -1, 0}
                },
                CROSSCHECK_REFERENCE
        );
    }

    public static WifiLdpcStandardProfile n1296RateOneHalf() {
        return profile(
                "ieee-80211-ldpc-n1296-r12",
                WifiLdpcCodewordLength.N_1296,
                WifiLdpcTargetRate.RATE_1_2,
                54,
                new int[][] {
                        {40, -1, -1, -1, 22, -1, 49, 23, 43, -1, -1, -1, 1, 0, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
                        {50, 1, -1, -1, 48, 35, -1, -1, 13, -1, 30, -1, -1, 0, 0, -1, -1, -1, -1, -1, -1, -1, -1, -1},
                        {39, 50, -1, -1, 4, -1, 2, -1, -1, -1, -1, 49, -1, -1, 0, 0, -1, -1, -1, -1, -1, -1, -1, -1},
                        {33, -1, -1, 38, 37, -1, -1, 4, 1, -1, -1, -1, -1, -1, -1, 0, 0, -1, -1, -1, -1, -1, -1, -1},
                        {45, -1, -1, -1, 0, 22, -1, -1, 20, 42, -1, -1, -1, -1, -1, -1, 0, 0, -1, -1, -1, -1, -1, -1},
                        {51, -1, -1, 48, 35, -1, -1, -1, 44, -1, 18, -1, -1, -1, -1, -1, -1, 0, 0, -1, -1, -1, -1, -1},
                        {47, 11, -1, -1, -1, 17, -1, -1, 51, -1, -1, -1, 0, -1, -1, -1, -1, -1, 0, 0, -1, -1, -1, -1},
                        {5, -1, 25, -1, 6, -1, 45, -1, 13, 40, -1, -1, -1, -1, -1, -1, -1, -1, -1, 0, 0, -1, -1, -1},
                        {33, -1, -1, 34, 24, -1, -1, -1, 23, -1, -1, 46, -1, -1, -1, -1, -1, -1, -1, -1, 0, 0, -1, -1},
                        {1, -1, 27, -1, 1, -1, -1, -1, 38, -1, 44, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 0, 0, -1},
                        {-1, 18, -1, -1, 23, -1, -1, 8, 0, 35, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 0, 0},
                        {49, -1, 17, -1, 30, -1, -1, -1, 34, -1, -1, 19, 1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 0}
                },
                CROSSCHECK_REFERENCE
        );
    }

    public static WifiLdpcStandardProfile n1296RateTwoThirds() {
        return profile(
                "ieee-80211-ldpc-n1296-r23",
                WifiLdpcCodewordLength.N_1296,
                WifiLdpcTargetRate.RATE_2_3,
                54,
                new int[][] {
                        {39, 31, 22, 43, -1, 40, 4, -1, 11, -1, -1, 50, -1, -1, -1, 6, 1, 0, -1, -1, -1, -1, -1, -1},
                        {25, 52, 41, 2, 6, -1, 14, -1, 34, -1, -1, -1, 24, -1, 37, -1, -1, 0, 0, -1, -1, -1, -1, -1},
                        {43, 31, 29, 0, 21, -1, 28, -1, -1, 2, -1, -1, 7, -1, 17, -1, -1, -1, 0, 0, -1, -1, -1, -1},
                        {20, 33, 48, -1, 4, 13, -1, 26, -1, -1, 22, -1, -1, 46, 42, -1, -1, -1, -1, 0, 0, -1, -1, -1},
                        {45, 7, 18, 51, 12, 25, -1, -1, -1, 50, -1, -1, 5, -1, -1, -1, 0, -1, -1, -1, 0, 0, -1, -1},
                        {35, 40, 32, 16, 5, -1, -1, 18, -1, -1, 43, 51, -1, 32, -1, -1, -1, -1, -1, -1, -1, 0, 0, -1},
                        {9, 24, 13, 22, 28, -1, -1, 37, -1, -1, 25, -1, -1, 52, -1, 13, -1, -1, -1, -1, -1, -1, 0, 0},
                        {32, 22, 4, 21, 16, -1, -1, -1, 27, 28, -1, 38, -1, -1, -1, 8, 1, -1, -1, -1, -1, -1, -1, 0}
                },
                CROSSCHECK_REFERENCE
        );
    }

    public static WifiLdpcStandardProfile n1296RateThreeQuarter() {
        return profile(
                "ieee-80211-ldpc-n1296-r34",
                WifiLdpcCodewordLength.N_1296,
                WifiLdpcTargetRate.RATE_3_4,
                54,
                new int[][] {
                        {39, 40, 51, 41, 3, 29, 8, 36, -1, 14, -1, 6, -1, 33, -1, 11, -1, 4, 1, 0, -1, -1, -1, -1},
                        {48, 21, 47, 9, 48, 35, 51, -1, 38, -1, 28, -1, 34, -1, 50, -1, 50, -1, -1, 0, 0, -1, -1, -1},
                        {30, 39, 28, 42, 50, 39, 5, 17, -1, 6, -1, 18, -1, 20, -1, 15, -1, 40, -1, -1, 0, 0, -1, -1},
                        {29, 0, 1, 43, 36, 30, 47, -1, 49, -1, 47, -1, 3, -1, 35, -1, 34, -1, 0, -1, -1, 0, 0, -1},
                        {1, 32, 11, 23, 10, 44, 12, 7, -1, 48, -1, 4, -1, 9, -1, 17, -1, 16, -1, -1, -1, -1, 0, 0},
                        {13, 7, 15, 47, 23, 16, 47, -1, 43, -1, 29, -1, 52, -1, 2, -1, 53, -1, 1, -1, -1, -1, -1, 0}
                },
                CROSSCHECK_REFERENCE
        );
    }

    public static WifiLdpcStandardProfile n1296RateFiveSixths() {
        return profile(
                "ieee-80211-ldpc-n1296-r56",
                WifiLdpcCodewordLength.N_1296,
                WifiLdpcTargetRate.RATE_5_6,
                54,
                new int[][] {
                        {48, 29, 37, 52, 2, 16, 6, 14, 53, 31, 34, 5, 18, 42, 53, 31, 45, -1, 46, 52, 1, 0, -1, -1},
                        {17, 4, 30, 7, 43, 11, 24, 6, 14, 21, 6, 39, 17, 40, 47, 7, 15, 41, 19, -1, -1, 0, 0, -1},
                        {7, 2, 51, 31, 46, 23, 16, 11, 53, 40, 10, 7, 46, 53, 33, 35, -1, 25, 35, 38, 0, -1, 0, 0},
                        {19, 48, 41, 1, 10, 7, 36, 47, 5, 29, 52, 52, 31, 10, 26, 6, 3, 2, -1, 51, 1, -1, -1, 0}
                },
                CROSSCHECK_REFERENCE
        );
    }

    private static WifiLdpcStandardProfile profile(
            String id,
            WifiLdpcCodewordLength length,
            WifiLdpcTargetRate rate,
            int expansionFactor,
            int[][] shifts,
            String reference
    ) {
        QcLdpcBaseMatrix matrix =
                new QcLdpcBaseMatrix(
                        id,
                        expansionFactor,
                        shifts,
                        true
                );

        return new WifiLdpcStandardProfile(
                new WifiLdpcStandardProfileMetadata(
                        id,
                        "IEEE 802.11 WLAN LDPC",
                        reference,
                        length,
                        rate,
                        expansionFactor
                ),
                matrix
        );
    }
}

package com.k1ngtle.vsia.signality.engineering.wifi.ldpc;

import java.util.List;

public final class WifiLdpcStandardProfiles {
    public static final String IEEE_80211_REFERENCE_R34 =
            "MathWorks ldpcDecode documentation: WLAN IEEE 802.11 rate-3/4 prototype matrix";

    public static final String CROSSCHECK_REFERENCE =
            "simgunz/802.11n-ldpc protoH tables, commit 615b59463386574bf266228ab70f0dddbc8ce227";

    private WifiLdpcStandardProfiles() {
    }

    public static List<WifiLdpcStandardProfile> n648Family() {
        return List.of(
                n648RateOneHalf(),
                n648RateTwoThirds(),
                n648RateThreeQuarter(),
                n648RateFiveSixths()
        );
    }

    public static WifiLdpcStandardProfile n648RateOneHalf() {
        return profile(
                "ieee-80211-ldpc-n648-r12",
                WifiLdpcTargetRate.RATE_1_2,
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
                WifiLdpcTargetRate.RATE_2_3,
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
                WifiLdpcTargetRate.RATE_3_4,
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
                WifiLdpcTargetRate.RATE_5_6,
                new int[][] {
                        {17, 13, 8, 21, 9, 3, 18, 12, 10, 0, 4, 15, 19, 2, 5, 10, 26, 19, 13, 13, 1, 0, -1, -1},
                        {3, 12, 11, 14, 11, 25, 5, 18, 0, 9, 2, 26, 26, 10, 24, 7, 14, 20, 4, 2, -1, 0, 0, -1},
                        {22, 16, 4, 3, 10, 21, 12, 5, 21, 14, 19, 5, -1, 8, 5, 18, 11, 5, 5, 15, 0, -1, 0, 0},
                        {7, 7, 14, 14, 4, 16, 16, 24, 24, 10, 1, 7, 15, 6, 10, 26, 8, 18, 21, 14, 1, -1, -1, 0}
                },
                CROSSCHECK_REFERENCE
        );
    }

    private static WifiLdpcStandardProfile profile(
            String id,
            WifiLdpcTargetRate rate,
            int[][] shifts,
            String reference
    ) {
        QcLdpcBaseMatrix matrix =
                new QcLdpcBaseMatrix(
                        id,
                        27,
                        shifts,
                        true
                );

        return new WifiLdpcStandardProfile(
                new WifiLdpcStandardProfileMetadata(
                        id,
                        "IEEE 802.11 WLAN LDPC",
                        reference,
                        WifiLdpcCodewordLength.N_648,
                        rate,
                        27
                ),
                matrix
        );
    }
}

package com.k1ngtle.vsia.signality.engineering.wifi.ldpc;

public final class WifiLdpcLabProfiles {
    private WifiLdpcLabProfiles() {
    }

    public static QcLdpcBaseMatrix referenceRateHalfZ27() {
        return new QcLdpcBaseMatrix(
                "vsia-reference-qc-ldpc-r12-z27",
                27,
                new int[][] {
                        {
                                0, 1, -1, 2,
                                0, -1, -1, -1
                        },
                        {
                                2, -1, 0, 1,
                                -1, 0, -1, -1
                        },
                        {
                                1, 2, 1, -1,
                                -1, -1, 0, -1
                        },
                        {
                                -1, 0, 2, 1,
                                -1, -1, -1, 0
                        }
                },
                false
        );
    }
}

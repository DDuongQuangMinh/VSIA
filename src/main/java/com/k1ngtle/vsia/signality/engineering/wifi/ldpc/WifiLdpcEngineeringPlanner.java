package com.k1ngtle.vsia.signality.engineering.wifi.ldpc;

public final class WifiLdpcEngineeringPlanner {
    private WifiLdpcEngineeringPlanner() {
    }

    public static WifiLdpcCodewordPlan plan(
            int payloadBits,
            WifiLdpcTargetRate targetRate
    ) {
        if (payloadBits < 1
                || targetRate == null) {
            throw new IllegalArgumentException(
                    "payloadBits/targetRate"
            );
        }

        for (WifiLdpcCodewordLength length
                : WifiLdpcCodewordLength.values()) {
            int information =
                    nominalInformationBits(
                            length,
                            targetRate
                    );

            if (payloadBits <= information) {
                return new WifiLdpcCodewordPlan(
                        length,
                        targetRate,
                        1,
                        payloadBits,
                        information,
                        information,
                        information
                                - payloadBits
                );
            }
        }

        WifiLdpcCodewordLength length =
                WifiLdpcCodewordLength.N_1944;

        int information =
                nominalInformationBits(
                        length,
                        targetRate
                );

        int count =
                (
                        payloadBits
                                + information
                                - 1
                )
                        / information;

        int total =
                count
                        * information;

        return new WifiLdpcCodewordPlan(
                length,
                targetRate,
                count,
                payloadBits,
                information,
                total,
                total - payloadBits
        );
    }

    public static int nominalInformationBits(
            WifiLdpcCodewordLength length,
            WifiLdpcTargetRate targetRate
    ) {
        return length.bits()
                * targetRate.numerator()
                / targetRate.denominator();
    }
}

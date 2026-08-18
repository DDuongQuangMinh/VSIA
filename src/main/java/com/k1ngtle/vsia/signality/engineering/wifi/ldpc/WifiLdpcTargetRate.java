package com.k1ngtle.vsia.signality.engineering.wifi.ldpc;

public enum WifiLdpcTargetRate {
    RATE_1_2(1, 2),
    RATE_2_3(2, 3),
    RATE_3_4(3, 4),
    RATE_5_6(5, 6);

    private final int numerator;
    private final int denominator;

    WifiLdpcTargetRate(
            int numerator,
            int denominator
    ) {
        this.numerator =
                numerator;
        this.denominator =
                denominator;
    }

    public int numerator() {
        return numerator;
    }

    public int denominator() {
        return denominator;
    }

    public double value() {
        return numerator
                / (double) denominator;
    }

    public static WifiLdpcTargetRate nearest(
            double value
    ) {
        WifiLdpcTargetRate best =
                RATE_1_2;

        double error =
                Double.POSITIVE_INFINITY;

        for (WifiLdpcTargetRate candidate
                : values()) {
            double candidateError =
                    Math.abs(
                            candidate.value()
                                    - value
                    );

            if (candidateError < error) {
                error =
                        candidateError;

                best =
                        candidate;
            }
        }

        return best;
    }
}

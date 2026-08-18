package com.k1ngtle.vsia.signality.engineering.wifi.baseband;

public enum BccCodeRate {
    RATE_1_2(
            1,
            2,
            new int[] {
                    1,
                    1
            }
    ),
    RATE_2_3(
            2,
            3,
            new int[] {
                    1,
                    1,
                    1,
                    0
            }
    ),
    RATE_3_4(
            3,
            4,
            new int[] {
                    1,
                    1,
                    1,
                    0,
                    0,
                    1
            }
    );

    private final int numerator;
    private final int denominator;
    private final int[] puncturePattern;

    BccCodeRate(
            int numerator,
            int denominator,
            int[] puncturePattern
    ) {
        this.numerator =
                numerator;
        this.denominator =
                denominator;
        this.puncturePattern =
                puncturePattern;
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

    public int[] puncturePattern() {
        return puncturePattern.clone();
    }
}

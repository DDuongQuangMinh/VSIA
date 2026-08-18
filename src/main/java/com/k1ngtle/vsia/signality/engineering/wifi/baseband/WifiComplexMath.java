package com.k1ngtle.vsia.signality.engineering.wifi.baseband;

import com.k1ngtle.vsia.signality.engineering.math.Complex;

public final class WifiComplexMath {
    private WifiComplexMath() {
    }

    public static Complex conjugate(
            Complex value
    ) {
        return new Complex(
                value.re(),
                -value.im()
        );
    }

    public static Complex divide(
            Complex numerator,
            Complex denominator
    ) {
        double power =
                denominator.magnitudeSquared();

        if (power < 1.0E-18) {
            return Complex.ZERO;
        }

        return numerator
                .multiply(
                        conjugate(
                                denominator
                        )
                )
                .scale(
                        1.0 / power
                );
    }

    public static double phase(
            Complex value
    ) {
        return Math.atan2(
                value.im(),
                value.re()
        );
    }

    public static double magnitude(
            Complex value
    ) {
        return Math.sqrt(
                value.magnitudeSquared()
        );
    }
}

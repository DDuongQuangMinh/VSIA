package com.k1ngtle.vsia.signality.engineering.phy;

public final class ErrorRateModel {
    private ErrorRateModel() {
    }

    public static double bitErrorRate(
            Modulation modulation,
            double snrDb,
            double codingGainDb
    ) {
        double effectiveSnr =
                Math.pow(
                        10.0,
                        (snrDb + codingGainDb) / 10.0
                );

        if (modulation == Modulation.BPSK
                || modulation == Modulation.QPSK) {
            return clamp(
                    0.5 * erfc(
                            Math.sqrt(effectiveSnr)
                    )
            );
        }

        int m = modulation.order();
        double k = Math.log(m) / Math.log(2.0);

        double qArgument =
                Math.sqrt(
                        (3.0 * k * effectiveSnr)
                                / (m - 1.0)
                );

        double q =
                0.5
                        * erfc(
                        qArgument / Math.sqrt(2.0)
                );

        double ber =
                (4.0 / k)
                        * (1.0 - 1.0 / Math.sqrt(m))
                        * q;

        return clamp(ber);
    }

    public static double frameErrorRate(
            double bitErrorRate,
            long frameBits
    ) {
        if (frameBits <= 0 || bitErrorRate <= 0.0) {
            return 0.0;
        }

        if (bitErrorRate >= 1.0) {
            return 1.0;
        }

        double success =
                Math.exp(
                        frameBits
                                * Math.log1p(
                                -bitErrorRate
                        )
                );

        return clamp(
                1.0 - success
        );
    }

    private static double clamp(double value) {
        return Math.max(
                0.0,
                Math.min(
                        1.0,
                        value
                )
        );
    }

    private static double erfc(double x) {
        return 1.0 - erf(x);
    }

    private static double erf(double x) {
        double sign = x < 0.0 ? -1.0 : 1.0;
        x = Math.abs(x);

        double a1 = 0.254829592;
        double a2 = -0.284496736;
        double a3 = 1.421413741;
        double a4 = -1.453152027;
        double a5 = 1.061405429;
        double p = 0.3275911;

        double t = 1.0 / (1.0 + p * x);

        double y =
                1.0
                        - (((((a5 * t + a4) * t + a3)
                        * t + a2)
                        * t + a1)
                        * t
                        * Math.exp(-x * x));

        return sign * y;
    }
}

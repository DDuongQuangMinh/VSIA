package com.k1ngtle.vsia.signality.engineering.radio;

public final class RadioSquelchEngine {
    private RadioSquelchEngine() {
    }

    public static RadioLinkQuality evaluate(
            double receivedPowerDbm,
            double snrDb,
            double squelchSnrThresholdDb
    ) {
        double intelligibility =
                logistic(
                        snrDb,
                        3.0,
                        0.35
                );

        double staticLevel =
                1.0 - intelligibility;

        double packetSuccess =
                logistic(
                        snrDb,
                        6.0,
                        0.45
                );

        boolean squelchOpen =
                snrDb >= squelchSnrThresholdDb;

        return new RadioLinkQuality(
                receivedPowerDbm,
                snrDb,
                clamp01(intelligibility),
                clamp01(staticLevel),
                clamp01(packetSuccess),
                squelchOpen
        );
    }

    private static double logistic(
            double x,
            double midpoint,
            double slope
    ) {
        return 1.0
                / (1.0 + Math.exp(
                -slope * (x - midpoint)
        ));
    }

    private static double clamp01(
            double value
    ) {
        return Math.max(
                0.0,
                Math.min(
                        1.0,
                        value
                )
        );
    }
}

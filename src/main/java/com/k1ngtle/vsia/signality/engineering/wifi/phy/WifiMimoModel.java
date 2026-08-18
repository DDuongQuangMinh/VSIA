package com.k1ngtle.vsia.signality.engineering.wifi.phy;

public final class WifiMimoModel {
    private WifiMimoModel() {
    }

    public static WifiMimoAssessment assess(
            WifiPhyConfiguration configuration,
            double snrDb
    ) {
        int maximumRank =
                Math.min(
                        configuration.transmitAntennas(),
                        configuration.receiveAntennas()
                );

        int requested =
                Math.min(
                        configuration.spatialStreams(),
                        maximumRank
                );

        double snrFactor =
                clamp01(
                        (snrDb + 5.0)
                                / 30.0
                );

        double decorrelation =
                1.0
                        - configuration.spatialCorrelation();

        int usable =
                Math.max(
                        1,
                        (int) Math.floor(
                                1.0
                                        + (
                                        requested - 1
                                )
                                        * snrFactor
                                        * decorrelation
                                        + 1.0E-9
                        )
                );

        usable =
                Math.min(
                        requested,
                        usable
                );

        double efficiency =
                Math.max(
                        0.35,
                        1.0
                                - 0.35
                                * configuration.spatialCorrelation()
                );

        double arrayGainDb =
                10.0
                        * Math.log10(
                        Math.max(
                                1,
                                configuration.receiveAntennas()
                        )
                );

        return new WifiMimoAssessment(
                maximumRank,
                usable,
                efficiency,
                arrayGainDb
        );
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

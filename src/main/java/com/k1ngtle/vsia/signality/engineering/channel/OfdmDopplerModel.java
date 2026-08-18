package com.k1ngtle.vsia.signality.engineering.channel;

public final class OfdmDopplerModel {
    private OfdmDopplerModel() {
    }

    public static OfdmDopplerAssessment assess(
            double dopplerHz,
            double subcarrierSpacingHz
    ) {
        if (subcarrierSpacingHz <= 0.0) {
            throw new IllegalArgumentException(
                    "subcarrierSpacingHz"
            );
        }

        double epsilon =
                dopplerHz
                        / subcarrierSpacingHz;

        double desiredPower =
                sincSquared(
                        epsilon
                );

        desiredPower =
                Math.max(
                        0.0,
                        Math.min(
                                1.0,
                                desiredPower
                        )
                );

        double iciPower =
                1.0
                        - desiredPower;

        double lossDb =
                desiredPower <= 0.0
                        ? Double.POSITIVE_INFINITY
                        : -10.0
                        * Math.log10(
                        desiredPower
                );

        return new OfdmDopplerAssessment(
                epsilon,
                desiredPower,
                iciPower,
                lossDb
        );
    }

    private static double sincSquared(
            double x
    ) {
        if (Math.abs(
                x
        ) < 1.0E-12) {
            return 1.0;
        }

        double value =
                Math.sin(
                        Math.PI
                                * x
                )
                        / (
                        Math.PI
                                * x
                );

        return value * value;
    }
}

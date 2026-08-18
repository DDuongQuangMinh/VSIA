package com.k1ngtle.vsia.signality.engineering.channel;

import java.util.UUID;

public final class SmallScaleFading {
    private SmallScaleFading() {
    }

    public static double fadingDb(
            UUID transmissionId,
            UUID receiverId,
            long tick,
            FadingModel model,
            double ricianK
    ) {
        if (!RfChannelSettings.ENABLE_SMALL_SCALE_FADING
                || model == FadingModel.NONE) {
            return 0.0;
        }

        long timeBin =
                tick
                        / Math.max(
                        1,
                        RfChannelSettings.FADING_TIME_BIN_TICKS
                );

        long seed =
                transmissionId
                        .getMostSignificantBits()
                        ^ transmissionId
                        .getLeastSignificantBits()
                        ^ receiverId
                        .getMostSignificantBits()
                        ^ receiverId
                        .getLeastSignificantBits()
                        ^ timeBin
                        * 0x9E3779B97F4A7C15L;

        double g1 =
                gaussian(
                        seed
                );

        double g2 =
                gaussian(
                        seed
                                ^ 0xD1B54A32D192ED03L
                );

        double power;

        if (model == FadingModel.RAYLEIGH) {
            double i =
                    g1 / Math.sqrt(
                            2.0
                    );

            double q =
                    g2 / Math.sqrt(
                            2.0
                    );

            power =
                    i * i
                            + q * q;
        } else {
            double k =
                    Math.max(
                            0.0,
                            ricianK
                    );

            double los =
                    Math.sqrt(
                            k
                                    / (
                                    k + 1.0
                            )
                    );

            double scatterScale =
                    Math.sqrt(
                            1.0
                                    / (
                                    2.0
                                            * (
                                            k + 1.0
                                    )
                            )
                    );

            double i =
                    los
                            + scatterScale
                            * g1;

            double q =
                    scatterScale
                            * g2;

            power =
                    i * i
                            + q * q;
        }

        power =
                Math.max(
                        power,
                        1.0E-9
                );

        double db =
                10.0
                        * Math.log10(
                        power
                );

        return Math.max(
                -RfChannelSettings.MAX_ABS_FADING_DB,
                Math.min(
                        RfChannelSettings.MAX_ABS_FADING_DB,
                        db
                )
        );
    }

    private static double gaussian(
            long seed
    ) {
        double u1 =
                uniform01(
                        mix(
                                seed
                        )
                );

        double u2 =
                uniform01(
                        mix(
                                seed
                                        ^ 0xA0761D6478BD642FL
                        )
                );

        u1 =
                Math.max(
                        u1,
                        1.0E-12
                );

        return Math.sqrt(
                -2.0
                        * Math.log(
                        u1
                )
        )
                * Math.cos(
                2.0
                        * Math.PI
                        * u2
        );
    }

    private static long mix(
            long value
    ) {
        long z =
                value;

        z ^=
                z >>> 33;

        z *=
                0xff51afd7ed558ccdL;

        z ^=
                z >>> 33;

        z *=
                0xc4ceb9fe1a85ec53L;

        z ^=
                z >>> 33;

        return z;
    }

    private static double uniform01(
            long value
    ) {
        return (
                value >>> 11
        )
                * 0x1.0p-53;
    }
}

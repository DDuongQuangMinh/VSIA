package com.k1ngtle.vsia.signality.engineering.channel;

import net.minecraft.world.phys.Vec3;

public final class StableShadowing {
    private StableShadowing() {
    }

    public static double offsetDb(
            String dimensionId,
            Vec3 transmitter,
            Vec3 receiver,
            double frequencyHz
    ) {
        if (!RfChannelSettings.ENABLE_SHADOWING
                || RfChannelSettings.SHADOWING_SIGMA_DB <= 0.0) {
            return 0.0;
        }

        long hash =
                0x9E3779B97F4A7C15L;

        hash =
                mix(
                        hash,
                        dimensionId == null
                                ? 0
                                : dimensionId.hashCode()
                );

        hash =
                mix(
                        hash,
                        quantize(
                                (transmitter.x + receiver.x) * 0.5,
                                8.0
                        )
                );

        hash =
                mix(
                        hash,
                        quantize(
                                (transmitter.y + receiver.y) * 0.5,
                                8.0
                        )
                );

        hash =
                mix(
                        hash,
                        quantize(
                                (transmitter.z + receiver.z) * 0.5,
                                8.0
                        )
                );

        hash =
                mix(
                        hash,
                        (long) Math.floor(
                                frequencyHz / 100_000_000.0
                        )
                );

        double u1 =
                uniform01(
                        hash
                );

        double u2 =
                uniform01(
                        mix(
                                hash,
                                0x6A09E667F3BCC909L
                        )
                );

        u1 =
                Math.max(
                        u1,
                        1.0E-12
                );

        double gaussian =
                Math.sqrt(
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

        double db =
                gaussian
                        * RfChannelSettings.SHADOWING_SIGMA_DB;

        return clamp(
                db,
                -RfChannelSettings.MAX_ABS_SHADOWING_DB,
                RfChannelSettings.MAX_ABS_SHADOWING_DB
        );
    }

    private static long quantize(
            double value,
            double cellSize
    ) {
        return (long) Math.floor(
                value / cellSize
        );
    }

    private static long mix(
            long seed,
            long value
    ) {
        long z =
                seed
                        ^ (
                        value
                                + 0x9E3779B97F4A7C15L
                                + (seed << 6)
                                + (seed >>> 2)
                );

        z ^=
                z >>> 30;

        z *=
                0xBF58476D1CE4E5B9L;

        z ^=
                z >>> 27;

        z *=
                0x94D049BB133111EBL;

        z ^=
                z >>> 31;

        return z;
    }

    private static double uniform01(
            long value
    ) {
        long bits =
                value
                        >>> 11;

        return bits
                * 0x1.0p-53;
    }

    private static double clamp(
            double value,
            double min,
            double max
    ) {
        return Math.max(
                min,
                Math.min(
                        max,
                        value
                )
        );
    }
}

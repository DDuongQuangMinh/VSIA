package com.k1ngtle.vsia.signality.engineering.channel;

import net.minecraft.world.phys.Vec3;

public record RfAntennaState(
        RfAntennaPattern pattern,
        RfPolarization polarization,
        Vec3 boresight,
        double peakGainDbi,
        double horizontalBeamwidthDeg,
        double verticalBeamwidthDeg,
        double frontToBackRatioDb
) {
    public RfAntennaState {
        pattern =
                pattern == null
                        ? RfAntennaPattern.ISOTROPIC
                        : pattern;

        polarization =
                polarization == null
                        ? RfPolarization.UNKNOWN
                        : polarization;

        boresight =
                boresight == null
                        ? new Vec3(
                        0.0,
                        0.0,
                        1.0
                )
                        : safeNormalize(
                        boresight
                );

        horizontalBeamwidthDeg =
                clamp(
                        horizontalBeamwidthDeg,
                        1.0,
                        360.0
                );

        verticalBeamwidthDeg =
                clamp(
                        verticalBeamwidthDeg,
                        1.0,
                        360.0
                );

        frontToBackRatioDb =
                Math.max(
                        0.0,
                        frontToBackRatioDb
                );
    }

    public static RfAntennaState isotropic() {
        return new RfAntennaState(
                RfAntennaPattern.ISOTROPIC,
                RfPolarization.UNKNOWN,
                new Vec3(
                        0.0,
                        0.0,
                        1.0
                ),
                0.0,
                360.0,
                360.0,
                0.0
        );
    }

    public static RfAntennaState omniVertical(
            double peakGainDbi
    ) {
        return new RfAntennaState(
                RfAntennaPattern.OMNIDIRECTIONAL,
                RfPolarization.VERTICAL,
                new Vec3(
                        0.0,
                        1.0,
                        0.0
                ),
                peakGainDbi,
                360.0,
                78.0,
                20.0
        );
    }

    private static Vec3 safeNormalize(
            Vec3 value
    ) {
        double length =
                value.length();

        if (!Double.isFinite(
                length
        )
                || length < 1.0E-9) {
            return new Vec3(
                    0.0,
                    0.0,
                    1.0
            );
        }

        return value.scale(
                1.0 / length
        );
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

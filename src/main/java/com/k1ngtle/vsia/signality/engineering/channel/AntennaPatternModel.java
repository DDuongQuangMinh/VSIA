package com.k1ngtle.vsia.signality.engineering.channel;

import net.minecraft.world.phys.Vec3;

public final class AntennaPatternModel {
    private AntennaPatternModel() {
    }

    public static double gainTowardDbi(
            RfAntennaState antenna,
            Vec3 directionFromAntenna
    ) {
        if (antenna == null) {
            return 0.0;
        }

        Vec3 direction =
                safeNormalize(
                        directionFromAntenna
                );

        return switch (antenna.pattern()) {
            case ISOTROPIC ->
                    antenna.peakGainDbi();

            case OMNIDIRECTIONAL ->
                    omnidirectionalGain(
                            antenna,
                            direction
                    );

            case SECTOR,
                 YAGI,
                 DISH,
                 PHASED_ARRAY ->
                    directionalGain(
                            antenna,
                            direction
                    );
        };
    }

    private static double omnidirectionalGain(
            RfAntennaState antenna,
            Vec3 direction
    ) {
        Vec3 axis =
                safeNormalize(
                        antenna.boresight()
                );

        double cos =
                clamp(
                        Math.abs(
                                axis.dot(
                                        direction
                                )
                        ),
                        0.0,
                        1.0
                );

        double sinSquared =
                Math.max(
                        1.0E-6,
                        1.0
                                - cos * cos
                );

        double patternDb =
                10.0
                        * Math.log10(
                        sinSquared
                );

        return antenna.peakGainDbi()
                + Math.max(
                -antenna.frontToBackRatioDb(),
                patternDb
        );
    }

    private static double directionalGain(
            RfAntennaState antenna,
            Vec3 direction
    ) {
        Vec3 boresight =
                safeNormalize(
                        antenna.boresight()
                );

        double dot =
                clamp(
                        boresight.dot(
                                direction
                        ),
                        -1.0,
                        1.0
                );

        double angleDeg =
                Math.toDegrees(
                        Math.acos(
                                dot
                        )
                );

        double beamwidth =
                Math.max(
                        1.0,
                        Math.min(
                                antenna.horizontalBeamwidthDeg(),
                                antenna.verticalBeamwidthDeg()
                        )
                );

        double attenuationDb =
                12.0
                        * Math.pow(
                        angleDeg
                                / beamwidth,
                        2.0
                );

        attenuationDb =
                Math.min(
                        attenuationDb,
                        antenna.frontToBackRatioDb()
                );

        return antenna.peakGainDbi()
                - attenuationDb;
    }

    private static Vec3 safeNormalize(
            Vec3 value
    ) {
        if (value == null
                || value.lengthSqr()
                < 1.0E-18) {
            return new Vec3(
                    0.0,
                    0.0,
                    1.0
            );
        }

        return value.normalize();
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

package com.k1ngtle.vsia.signality.radar.network;

import com.k1ngtle.vsia.signality.api.radar.IRadarEmitter;
import com.k1ngtle.vsia.signality.api.radar.RadarContact;
import com.k1ngtle.vsia.signality.api.radar.RadarProfile;
import java.util.SplittableRandom;
import java.util.UUID;
import net.minecraft.world.phys.Vec3;

public final class RadarMeasurementModel {
    private static final double MIN_ANGLE_SIGMA =
            Math.toRadians(0.03);

    private RadarMeasurementModel() {
    }

    public static RadarMeasurement fromContact(
            IRadarEmitter emitter,
            RadarContact contact,
            long gameTime
    ) {
        RadarProfile profile =
                emitter.profile();

        RadarWaveformProfile waveform =
                RadarWaveformRegistry.forEmitter(
                        emitter
                );

        double threshold =
                Math.max(
                        1.0E-12,
                        profile.minDetectableSnr()
                );

        double snr =
                Math.max(
                        1.0E-12,
                        contact.signalToNoiseRatio()
                );

        double margin =
                snr / threshold;

        double pd =
                probabilityOfDetection(
                        margin,
                        contact.trackQuality()
                );

        SplittableRandom random =
                new SplittableRandom(
                        seed(
                                emitter.id(),
                                contact.targetId(),
                                gameTime
                        )
                );

        if (random.nextDouble() > pd) {
            return null;
        }

        Vec3 sensor =
                emitter.originWorld();

        Vec3 truth =
                contact.positionWorld();

        Vec3 delta =
                truth.subtract(
                        sensor
                );

        double range =
                Math.max(
                        0.001,
                        delta.length()
                );

        Vec3 los =
                delta.scale(
                        1.0 / range
                );

        Vec3 right =
                horizontalRight(
                        los
                );

        Vec3 up =
                right.cross(
                        los
                );

        if (up.lengthSqr()
                < 1.0E-10) {
            up =
                    new Vec3(
                            0.0,
                            1.0,
                            0.0
                    );
        } else {
            up =
                    up.normalize();
        }

        double sqrtMargin =
                Math.sqrt(
                        Math.max(
                                1.0,
                                margin
                        )
                );

        double rangeResolutionSigma =
                waveform.rangeResolutionMeters()
                        / Math.sqrt(
                        12.0
                );

        double rangeSigma =
                clamp(
                        rangeResolutionSigma
                                / sqrtMargin,
                        0.35,
                        60.0
                );

        double angularSigma =
                clamp(
                        profile.halfBeamWidthRad()
                                / (
                                2.0
                                        * sqrtMargin
                        ),
                        MIN_ANGLE_SIGMA,
                        Math.max(
                                MIN_ANGLE_SIGMA,
                                profile.halfBeamWidthRad()
                                        / 1.5
                        )
                );

        double dopplerResolutionSigma =
                waveform.radialVelocityResolutionMps()
                        / Math.sqrt(
                        12.0
                );

        double dopplerSigma =
                clamp(
                        dopplerResolutionSigma
                                / sqrtMargin
                                + 0.10,
                        0.10,
                        12.0
                );

        double resolvedRange =
                waveform.resolveRange(
                        range
                );

        double rangeNoise =
                gaussian(
                        random
                )
                        * rangeSigma;

        double azimuthNoise =
                gaussian(
                        random
                )
                        * angularSigma;

        double elevationNoise =
                gaussian(
                        random
                )
                        * angularSigma;

        double crossRangeAz =
                range
                        * azimuthNoise;

        double crossRangeEl =
                range
                        * elevationNoise;

        Vec3 measuredPosition =
                sensor
                        .add(
                                los.scale(
                                        resolvedRange
                                                + rangeNoise
                                )
                        )
                        .add(
                                right.scale(
                                        crossRangeAz
                                )
                        )
                        .add(
                                up.scale(
                                        crossRangeEl
                                )
                        );

        double measuredRange =
                Math.max(
                        0.0,
                        measuredPosition.distanceTo(
                                sensor
                        )
                );

        double measuredClosure =
                waveform.resolveRadialVelocity(
                        contact.closureRateMps()
                )
                        + gaussian(
                        random
                )
                        * dopplerSigma;

        Vec3 radialVelocityEstimate =
                emitter.velocityWorld()
                        .add(
                                los.scale(
                                        -measuredClosure
                                )
                        );

        double positionVariance =
                rangeSigma
                        * rangeSigma
                        + (
                        range
                                * angularSigma
                )
                        * (
                        range
                                * angularSigma
                );

        return new RadarMeasurement(
                emitter.id(),
                contact.targetId(),
                gameTime,
                sensor,
                measuredPosition,
                radialVelocityEstimate,
                measuredRange,
                contact.bearingRad()
                        + azimuthNoise,
                contact.elevationRad()
                        + elevationNoise,
                measuredClosure,
                snr,
                contact.signalToClutterRatio(),
                positionVariance,
                dopplerSigma
                        * dopplerSigma,
                contact.trackQuality()
        );
    }

    public static double probabilityOfDetection(
            double snrMarginLinear,
            boolean trackQuality
    ) {
        double x =
                Math.max(
                        0.0,
                        snrMarginLinear - 1.0
                );

        double pd =
                0.50
                        + 0.50
                        * (
                        1.0
                                - Math.exp(
                                -0.75
                                        * x
                        )
                );

        if (trackQuality) {
            pd =
                    Math.min(
                            0.9995,
                            pd + 0.035
                    );
        }

        return clamp(
                pd,
                0.50,
                0.9995
        );
    }

    private static Vec3 horizontalRight(
            Vec3 los
    ) {
        Vec3 right =
                new Vec3(
                        -los.z,
                        0.0,
                        los.x
                );

        if (right.lengthSqr()
                < 1.0E-10) {
            return new Vec3(
                    1.0,
                    0.0,
                    0.0
            );
        }

        return right.normalize();
    }

    private static long seed(
            UUID emitter,
            UUID target,
            long tick
    ) {
        long x =
                emitter
                        .getMostSignificantBits()
                        ^ emitter
                        .getLeastSignificantBits();

        x =
                mix64(
                        x
                                ^ target
                                .getMostSignificantBits()
                );

        x =
                mix64(
                        x
                                ^ target
                                .getLeastSignificantBits()
                );

        return mix64(
                x ^ tick
        );
    }

    private static long mix64(
            long z
    ) {
        z =
                (
                        z
                                ^ (
                                z >>> 30
                        )
                )
                        * 0xbf58476d1ce4e5b9L;

        z =
                (
                        z
                                ^ (
                                z >>> 27
                        )
                )
                        * 0x94d049bb133111ebL;

        return z
                ^ (
                z >>> 31
        );
    }

    private static double gaussian(
            SplittableRandom random
    ) {
        double u1 =
                Math.max(
                        1.0E-12,
                        random.nextDouble()
                );

        double u2 =
                random.nextDouble();

        return Math.sqrt(
                -2.0
                        * Math.log(
                        u1
                )
        )
                * Math.cos(
                Math.PI
                        * 2.0
                        * u2
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

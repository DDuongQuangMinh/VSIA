package com.k1ngtle.vsia.signality.engineering.reality;

import com.k1ngtle.vsia.signality.engineering.channel.RfChannelAssessment;
import com.k1ngtle.vsia.signality.engineering.channel.SpectralOverlap;

import java.util.Collection;

public final class NetworkRealityEngine {
    private NetworkRealityEngine() {
    }

    public static Result apply(
            RfChannelAssessment base,
            RfMicroTiming desired,
            Collection<RfMicroTiming> timings,
            double propagationDistanceMeters
    ) {
        if (base == null
                || desired == null) {
            return new Result(
                    base,
                    new NetworkRealityAssessment(
                            RfPropagationDelayModel.delayMicros(
                                    propagationDistanceMeters
                            ),
                            desired == null
                                    ? 0L
                                    : desired.durationMicros(),
                            1.0,
                            base == null
                                    ? 0.0
                                    : base.interferencePowerWatts(),
                            base == null
                                    ? Double.NEGATIVE_INFINITY
                                    : base.sinrDb(),
                            Double.POSITIVE_INFINITY,
                            true
                    )
            );
        }

        double overlapSum =
                0.0;

        int overlapping =
                0;

        long earliestInterfererStart =
                Long.MAX_VALUE;

        if (timings != null) {
            for (RfMicroTiming candidate : timings) {
                if (candidate.transmissionId()
                        .equals(
                                desired.transmissionId()
                        )) {
                    continue;
                }

                double spectral =
                        SpectralOverlap.fractionOfReceiverBandwidth(
                                desired.centerFrequencyHz(),
                                desired.bandwidthHz(),
                                candidate.centerFrequencyHz(),
                                candidate.bandwidthHz()
                        );

                if (spectral <= 0.0) {
                    continue;
                }

                double temporal =
                        MicroTemporalOverlap.fractionOfDesired(
                                desired,
                                candidate
                        );

                if (temporal <= 0.0) {
                    continue;
                }

                overlapSum +=
                        temporal;

                overlapping++;

                earliestInterfererStart =
                        Math.min(
                                earliestInterfererStart,
                                candidate.startMicros()
                        );
            }
        }

        double temporalFactor =
                overlapping == 0
                        ? 0.0
                        : Math.max(
                        0.0,
                        Math.min(
                                1.0,
                                overlapSum
                                        / overlapping
                        )
                );

        if (!NetworkRealitySettings
                .ENABLE_MICROSECOND_OVERLAP) {
            temporalFactor =
                    base.interferencePowerWatts() > 0.0
                            ? 1.0
                            : 0.0;
        }

        double correctedInterference =
                base.interferencePowerWatts()
                        * temporalFactor;

        double correctedSinr =
                ratioDb(
                        base.effectiveDesiredPowerWatts(),
                        base.noisePowerWatts()
                                + correctedInterference
                );

        double captureMargin =
                ReceiverCaptureModel.captureMarginDb(
                        base.effectiveDesiredPowerWatts(),
                        correctedInterference
                );

        boolean lockedBeforeInterference =
                earliestInterfererStart
                        != Long.MAX_VALUE
                        && earliestInterfererStart
                        - desired.startMicros()
                        >= NetworkRealitySettings
                        .PREAMBLE_LOCK_MICROS;

        double captureThreshold =
                lockedBeforeInterference
                        ? NetworkRealitySettings
                        .LOCKED_RECEIVER_CAPTURE_THRESHOLD_DB
                        : NetworkRealitySettings
                        .SIMULTANEOUS_CAPTURE_THRESHOLD_DB;

        boolean captured =
                !NetworkRealitySettings
                        .ENABLE_CAPTURE_MODEL
                        || correctedInterference <= 0.0
                        || ReceiverCaptureModel.desiredCanCapture(
                        base.effectiveDesiredPowerWatts(),
                        correctedInterference,
                        captureThreshold
                );

        RfChannelAssessment corrected =
                new RfChannelAssessment(
                        base.rawDesiredPowerWatts(),
                        base.effectiveDesiredPowerWatts(),
                        correctedInterference,
                        base.noisePowerWatts(),
                        base.rawSnrDb(),
                        correctedSinr,
                        base.materialLossDb(),
                        base.shadowingDb(),
                        base.fadingDb(),
                        base.transmitDirectionalGainDbi(),
                        base.receiveDirectionalGainDbi(),
                        base.polarizationMismatchLossDb(),
                        base.radialRelativeVelocityMetersPerSecond(),
                        base.dopplerHz(),
                        overlapping
                );

        NetworkRealityAssessment reality =
                new NetworkRealityAssessment(
                        RfPropagationDelayModel.delayMicros(
                                propagationDistanceMeters
                        ),
                        desired.durationMicros(),
                        temporalFactor,
                        correctedInterference,
                        correctedSinr,
                        captureMargin,
                        captured
                );

        return new Result(
                corrected,
                reality
        );
    }

    private static double ratioDb(
            double numerator,
            double denominator
    ) {
        if (numerator <= 0.0) {
            return Double.NEGATIVE_INFINITY;
        }

        if (denominator <= 0.0) {
            return Double.POSITIVE_INFINITY;
        }

        return 10.0
                * Math.log10(
                numerator
                        / denominator
        );
    }

    public record Result(
            RfChannelAssessment channel,
            NetworkRealityAssessment reality
    ) {
    }
}

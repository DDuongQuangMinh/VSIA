package com.k1ngtle.vsia.signality.engineering.reality;

import com.k1ngtle.vsia.signality.engineering.channel.RfChannelAssessment;

import java.util.List;
import java.util.UUID;

public final class RealityTestSuite {
    private RealityTestSuite() {
    }

    public static List<RealityTestResult> runAll() {
        return List.of(
                propagationOneMicrosecond(),
                temporalHalfOverlap(),
                genericAirtime(),
                captureThreshold(),
                correctedInterference()
        );
    }

    private static RealityTestResult propagationOneMicrosecond() {
        double distance =
                RfPropagationDelayModel.SPEED_OF_LIGHT_MPS
                        / 1_000_000.0;

        double delay =
                RfPropagationDelayModel.delayMicros(
                        distance
                );

        return result(
                "reality-propagation-one-microsecond",
                Math.abs(
                        delay - 1.0
                ) < 1.0E-12,
                "c * 1 us of distance must produce 1 us free-space propagation delay"
        );
    }

    private static RealityTestResult temporalHalfOverlap() {
        RfMicroTiming desired =
                timing(
                        100L,
                        199L
                );

        RfMicroTiming interferer =
                timing(
                        150L,
                        249L
                );

        double overlap =
                MicroTemporalOverlap.fractionOfDesired(
                        desired,
                        interferer
                );

        return result(
                "reality-micro-overlap-half",
                Math.abs(
                        overlap - 0.5
                ) < 1.0E-12,
                "50 us overlap across a 100 us desired frame must equal 0.5"
        );
    }

    private static RealityTestResult genericAirtime() {
        long airtime =
                GeneralRfAirtimeModel.estimateMicros(
                        12_000L,
                        20_000_000.0
                );

        return result(
                "reality-generic-airtime",
                airtime == 1_200L,
                "12,000 bits at the 10 Mbit/s conservative 20 MHz fallback must take 1,200 us"
        );
    }

    private static RealityTestResult captureThreshold() {
        boolean capture =
                ReceiverCaptureModel.desiredCanCapture(
                        10.0,
                        1.0,
                        10.0
                );

        boolean reject =
                ReceiverCaptureModel.desiredCanCapture(
                        2.0,
                        1.0,
                        10.0
                );

        return result(
                "reality-capture-threshold",
                capture && !reject,
                "10 dB desired/interferer margin captures; roughly 3 dB does not"
        );
    }

    private static RealityTestResult correctedInterference() {
        UUID desiredId =
                UUID.nameUUIDFromBytes(
                        "desired".getBytes(
                                java.nio.charset.StandardCharsets.UTF_8
                        )
                );

        UUID interfererId =
                UUID.nameUUIDFromBytes(
                        "interferer".getBytes(
                                java.nio.charset.StandardCharsets.UTF_8
                        )
                );

        RfMicroTiming desired =
                new RfMicroTiming(
                        desiredId,
                        "test",
                        2.4E9,
                        20.0E6,
                        100L,
                        199L
                );

        RfMicroTiming interferer =
                new RfMicroTiming(
                        interfererId,
                        "test",
                        2.4E9,
                        20.0E6,
                        150L,
                        249L
                );

        RfChannelAssessment base =
                new RfChannelAssessment(
                        10.0,
                        10.0,
                        2.0,
                        1.0,
                        10.0,
                        5.2287874528,
                        0.0,
                        0.0,
                        0.0,
                        0.0,
                        0
                );

        NetworkRealityEngine.Result realityResult =
                NetworkRealityEngine.apply(
                        base,
                        desired,
                        List.of(
                                desired,
                                interferer
                        ),
                        0.0
                );

        boolean passed =
                Math.abs(
                        realityResult.channel()
                                .interferencePowerWatts()
                                - 1.0
                ) < 1.0E-12
                        && Math.abs(
                        realityResult.reality()
                                .microTemporalInterferenceFactor()
                                - 0.5
                ) < 1.0E-12;

        return result(
                "reality-interference-corrected-by-airtime",
                passed,
                "Half-frame temporal overlap must halve the tick-level interference estimate"
        );
    }

    private static RfMicroTiming timing(
            long start,
            long end
    ) {
        return new RfMicroTiming(
                UUID.randomUUID(),
                "test",
                2.4E9,
                20.0E6,
                start,
                end
        );
    }

    private static RealityTestResult result(
            String id,
            boolean passed,
            String detail
    ) {
        return new RealityTestResult(
                id,
                passed,
                detail
        );
    }
}

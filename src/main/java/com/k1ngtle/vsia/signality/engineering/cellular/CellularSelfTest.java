package com.k1ngtle.vsia.signality.engineering.cellular;

import com.k1ngtle.vsia.signality.engineering.cellular.core.PduSession;
import com.k1ngtle.vsia.signality.engineering.cellular.nas.NasState;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class CellularSelfTest {
    private CellularSelfTest() {
    }

    public static Report run() {
        List<Result> results =
                new ArrayList<>();

        test(
                results,
                "cell-generation-map",
                () -> require(
                        CellularGeneration.fromProtocol(
                                "signality:5g_nr"
                        )
                                == CellularGeneration.G5_NR,
                        "5G NR protocol mapping failed"
                )
        );

        test(
                results,
                "cell-band-n78",
                () -> require(
                        CellularBandCatalog.bestMatch(
                                CellularGeneration.G5_NR,
                                3.5e9
                        ).id().equals("NR-n78"),
                        "3.5 GHz did not resolve to n78"
                )
        );

        test(
                results,
                "cell-fspl-distance",
                () -> {
                    double near =
                            CellularMeasurementEngine
                                    .freeSpacePathLossDb(
                                            3.5e9,
                                            10.0
                                    );

                    double far =
                            CellularMeasurementEngine
                                    .freeSpacePathLossDb(
                                            3.5e9,
                                            100.0
                                    );

                    require(
                            far > near,
                            "FSPL must increase with distance"
                    );
                }
        );

        test(
                results,
                "cell-rsrp-distance",
                () -> {
                    CellularMeasurement near =
                            sampleMeasurement(
                                    20.0
                            );

                    CellularMeasurement far =
                            sampleMeasurement(
                                    200.0
                            );

                    require(
                            near.rsrpDbm()
                                    > far.rsrpDbm(),
                            "RSRP must fall with distance"
                    );
                }
        );

        test(
                results,
                "cell-sinr-interference",
                () -> {
                    CellularMeasurement clean =
                            CellularMeasurementEngine.evaluate(
                                    3.5e9,
                                    100.0,
                                    46.0,
                                    15.0,
                                    0.0,
                                    100.0e6,
                                    273,
                                    -120.0,
                                    5.0,
                                    0.0
                            );

                    CellularMeasurement noisy =
                            CellularMeasurementEngine.evaluate(
                                    3.5e9,
                                    100.0,
                                    46.0,
                                    15.0,
                                    0.0,
                                    100.0e6,
                                    273,
                                    -70.0,
                                    5.0,
                                    0.0
                            );

                    require(
                            clean.sinrDb()
                                    > noisy.sinrDb(),
                            "SINR must decrease with interference"
                    );
                }
        );

        test(
                results,
                "cell-cqi-range",
                () -> {
                    for (int snr = -50;
                         snr <= 60;
                         snr++) {
                        int cqi =
                                CellularMeasurementEngine
                                        .cqiFromSinrDb(
                                                snr
                                        );

                        require(
                                cqi >= 1
                                        && cqi <= 15,
                                "CQI outside 1..15"
                        );
                    }
                }
        );

        test(
                results,
                "cell-shannon-capacity",
                () -> require(
                        sampleMeasurement(
                                100.0
                        ).shannonCapacityBps()
                                > 0.0,
                        "capacity must be positive"
                )
        );

        test(
                results,
                "cell-timing-advance",
                () -> require(
                        CellularMeasurementEngine
                                .timingAdvanceUnits(
                                        1_000.0
                                )
                                > CellularMeasurementEngine
                                .timingAdvanceUnits(
                                        10.0
                                ),
                        "timing advance must grow with distance"
                )
        );

        test(
                results,
                "cell-a3-hysteresis",
                () -> {
                    CellularMobilityPolicy policy =
                            new CellularMobilityPolicy(
                                    3.0,
                                    0L
                            );

                    UUID serving =
                            UUID.randomUUID();

                    UUID target =
                            UUID.randomUUID();

                    CellularMobilityPolicy.Decision weak =
                            policy.evaluate(
                                    serving,
                                    -80.0,
                                    target,
                                    -78.5,
                                    1L
                            );

                    CellularMobilityPolicy.Decision strong =
                            policy.evaluate(
                                    serving,
                                    -80.0,
                                    target,
                                    -74.0,
                                    2L
                            );

                    require(
                            !weak.handover()
                                    && strong.handover(),
                            "A3 hysteresis behavior failed"
                    );
                }
        );

        test(
                results,
                "cell-a3-time-to-trigger",
                () -> {
                    CellularMobilityPolicy policy =
                            new CellularMobilityPolicy(
                                    3.0,
                                    200L
                            );

                    UUID serving =
                            UUID.randomUUID();

                    UUID target =
                            UUID.randomUUID();

                    require(
                            !policy.evaluate(
                                    serving,
                                    -90.0,
                                    target,
                                    -80.0,
                                    100L
                            ).handover(),
                            "handover occurred before TTT"
                    );

                    require(
                            policy.evaluate(
                                    serving,
                                    -90.0,
                                    target,
                                    -80.0,
                                    301L
                            ).handover(),
                            "handover did not occur after TTT"
                    );
                }
        );

        test(
                results,
                "cell-5qi-catalog",
                () -> require(
                        CellularQosCatalog
                                .forFiveQi(1)
                                .schedulerWeight()
                                > CellularQosCatalog
                                .forFiveQi(9)
                                .schedulerWeight(),
                        "5QI 1 should be weighted above best effort"
                )
        );

        test(
                results,
                "cell-qos-scheduler-budget",
                () -> {
                    UeContext first =
                            new UeContext(
                                    UUID.randomUUID(),
                                    100
                            );

                    UeContext second =
                            new UeContext(
                                    UUID.randomUUID(),
                                    101
                            );

                    first.setRequestedResourceBlocks(
                            20
                    );

                    second.setRequestedResourceBlocks(
                            20
                    );

                    CellularQosScheduler scheduler =
                            new CellularQosScheduler();

                    List<ResourceBlockAllocation> allocations =
                            scheduler.schedule(
                                    List.of(
                                            first,
                                            second
                                    ),
                                    ignored ->
                                            new PduSession(
                                                    1,
                                                    ignored,
                                                    "internet",
                                                    "10.0.0.2",
                                                    9,
                                                    true
                                            ),
                                    12
                            );

                    int total =
                            allocations.stream()
                                    .mapToInt(
                                            ResourceBlockAllocation::resourceBlockCount
                                    )
                                    .sum();

                    require(
                            total <= 12,
                            "scheduler exceeded RB budget"
                    );
                }
        );

        test(
                results,
                "cell-sim-key-clone",
                () -> {
                    byte[] key =
                            new byte[32];

                    key[0] = 7;

                    CellularSimProfile sim =
                            new CellularSimProfile(
                                    "89001010000000000001",
                                    "001011234567890",
                                    "00101",
                                    key,
                                    "internet",
                                    9
                            );

                    byte[] exported =
                            sim.subscriberKey();

                    exported[0] = 99;

                    require(
                            sim.subscriberKey()[0]
                                    == 7,
                            "SIM leaked mutable key array"
                    );
                }
        );

        test(
                results,
                "cell-sim-fingerprint",
                () -> {
                    CellularSimProfile sim =
                            new CellularSimProfile(
                                    "89001010000000000001",
                                    "001011234567890",
                                    "00101",
                                    new byte[32],
                                    "internet",
                                    9
                            );

                    require(
                            sim.fingerprint().length()
                                    == 16,
                            "unexpected fingerprint length"
                    );
                }
        );

        test(
                results,
                "cell-auto-base-station",
                () -> {
                    CellularAutomationController controller =
                            new CellularAutomationController();

                    require(
                            controller.nextAction(
                                    0L,
                                    CellularMode.BASE_STATION,
                                    UeRanState.DETACHED,
                                    NasState.DEREGISTERED,
                                    false,
                                    null
                            )
                                    == CellularAutomationController.Action
                                    .BROADCAST_SYSTEM_INFORMATION,
                            "base station did not schedule SSB"
                    );
                }
        );

        test(
                results,
                "cell-auto-ue-search",
                () -> {
                    CellularAutomationController controller =
                            new CellularAutomationController();

                    require(
                            controller.nextAction(
                                    0L,
                                    CellularMode.UE,
                                    UeRanState.DETACHED,
                                    NasState.DEREGISTERED,
                                    false,
                                    null
                            )
                                    == CellularAutomationController.Action
                                    .START_CELL_SEARCH,
                            "detached UE did not start cell search"
                    );
                }
        );

        test(
                results,
                "cell-auto-pdu",
                () -> {
                    CellularAutomationController controller =
                            new CellularAutomationController();

                    require(
                            controller.nextAction(
                                    2_000_000L,
                                    CellularMode.UE,
                                    UeRanState.REGISTERED,
                                    NasState.REGISTERED,
                                    true,
                                    null
                            )
                                    == CellularAutomationController.Action
                                    .REQUEST_PDU_SESSION,
                            "registered UE did not request a PDU session"
                    );
                }
        );

        test(
                results,
                "cell-1g-fdma",
                () -> require(
                        CellularAirInterfaceMath
                                .analogFdmaChannels(
                                        1_000_000.0,
                                        25_000.0
                                )
                                == 40,
                        "1G FDMA channel count mismatch"
                )
        );

        test(
                results,
                "cell-2g-tdma",
                () -> requireNear(
                        CellularAirInterfaceMath
                                .gsmTimeslotDurationSeconds(),
                        CellularAirInterfaceMath
                                .GSM_FRAME_DURATION_SECONDS
                                / 8.0,
                        1.0e-12,
                        "GSM timeslot duration"
                )
        );

        test(
                results,
                "cell-3g-processing-gain",
                () -> require(
                        CellularAirInterfaceMath
                                .wcdmaProcessingGainDb(
                                        64_000.0
                                )
                                > 10.0,
                        "WCDMA processing gain is too small"
                )
        );

        test(
                results,
                "cell-4g-resource-blocks",
                () -> require(
                        CellularAirInterfaceMath
                                .lteResourceBlocks(
                                        20_000_000.0
                                )
                                == 100,
                        "20 MHz LTE should expose 100 RB"
                )
        );

        test(
                results,
                "cell-5g-numerology",
                () -> requireNear(
                        CellularAirInterfaceMath
                                .nrSubcarrierSpacingHz(
                                        1
                                ),
                        30_000.0,
                        1.0e-9,
                        "NR mu=1 subcarrier spacing"
                )
        );

        test(
                results,
                "cell-generation-peak-rate",
                () -> require(
                        CellularAirInterfaceMath
                                .estimatedPeakUserRateBps(
                                        CellularGeneration.G5_NR,
                                        100_000_000.0,
                                        15,
                                        4
                                )
                                > CellularAirInterfaceMath
                                .estimatedPeakUserRateBps(
                                        CellularGeneration.G4_LTE,
                                        20_000_000.0,
                                        15,
                                        2
                                ),
                        "expected 5G example peak rate to exceed LTE example"
                )
        );

        test(
                results,
                "cell-2g-protocol-concepts",
                () -> require(
                        CellularProtocolConceptCatalog
                                .forGeneration(
                                        CellularGeneration.G2_GSM
                                )
                                .simplifiedAttachSequence()
                                .contains(
                                        "RACH access"
                                ),
                        "2G concept flow is missing GSM RACH"
                )
        );

        test(
                results,
                "cell-5g-protocol-concepts",
                () -> require(
                        CellularProtocolConceptCatalog
                                .forGeneration(
                                        CellularGeneration.G5_NR
                                )
                                .simplifiedAttachSequence()
                                .contains(
                                        "PDU session"
                                ),
                        "5G concept flow is missing PDU session"
                )
        );

        int passed =
                (int) results.stream()
                        .filter(
                                Result::passed
                        )
                        .count();

        return new Report(
                List.copyOf(results),
                passed,
                results.size() - passed
        );
    }

    private static CellularMeasurement sampleMeasurement(
            double distanceMeters
    ) {
        return CellularMeasurementEngine.evaluate(
                3.5e9,
                distanceMeters,
                46.0,
                15.0,
                0.0,
                100.0e6,
                273,
                -110.0,
                5.0,
                0.0
        );
    }

    private static void test(
            List<Result> output,
            String id,
            Check check
    ) {
        try {
            check.run();
            output.add(
                    new Result(
                            id,
                            true,
                            ""
                    )
            );
        } catch (Throwable throwable) {
            output.add(
                    new Result(
                            id,
                            false,
                            throwable.getMessage() == null
                                    ? throwable.getClass()
                                    .getSimpleName()
                                    : throwable.getMessage()
                    )
            );
        }
    }

    private static void requireNear(
            double actual,
            double expected,
            double tolerance,
            String label
    ) {
        if (Math.abs(
                actual - expected
        ) > tolerance) {
            throw new IllegalStateException(
                    label
                            + " expected="
                            + expected
                            + " actual="
                            + actual
            );
        }
    }

    private static void require(
            boolean value,
            String message
    ) {
        if (!value) {
            throw new IllegalStateException(
                    message
            );
        }
    }

    public record Result(
            String id,
            boolean passed,
            String detail
    ) {
    }

    public record Report(
            List<Result> results,
            int passed,
            int failed
    ) {
    }

    @FunctionalInterface
    private interface Check {
        void run() throws Exception;
    }
}

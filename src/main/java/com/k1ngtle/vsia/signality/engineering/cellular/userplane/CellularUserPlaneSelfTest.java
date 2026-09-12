package com.k1ngtle.vsia.signality.engineering.cellular.userplane;

import com.k1ngtle.vsia.signality.engineering.cellular.core.PduSession;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

// CELLULAR_USER_PLANE_V1
public final class CellularUserPlaneSelfTest {
    private CellularUserPlaneSelfTest() {
    }

    public static Report run() {
        List<Result> results =
                new ArrayList<>();

        UUID deviceId =
                UUID.fromString(
                        "2e5e2af5-7e32-4ff4-91e8-68fbbf241301"
                );

        PduSession session =
                new PduSession(
                        1,
                        deviceId,
                        "internet",
                        "10.0.0.2",
                        9,
                        true
                );

        CellularUserPlaneService.reset(deviceId);

        test(
                results,
                "cell-up-ipv4-parser",
                () -> {
                    require(
                            CellularIcmpEchoPacket.validIpv4("10.0.0.1"),
                            "valid IPv4 address rejected"
                    );

                    require(
                            !CellularIcmpEchoPacket.validIpv4("10.0.0.999"),
                            "invalid IPv4 address accepted"
                    );
                }
        );

        test(
                results,
                "cell-up-icmp-checksum",
                () -> {
                    CellularIcmpEchoPacket packet =
                            CellularIcmpEchoPacket.request(
                                    "10.0.0.2",
                                    "10.0.0.1",
                                    1,
                                    1,
                                    32
                            );

                    require(
                            packet.checksum() != 0,
                            "ICMP checksum was zero"
                    );

                    require(
                            packet.ipv4Bytes() == 60,
                            "unexpected IPv4/ICMP wire size"
                    );
                }
        );

        test(
                results,
                "cell-up-gtpu-encapsulation",
                () -> {
                    CellularGtpuPacket packet =
                            CellularGtpuPacket.forSession(
                                    session,
                                    1,
                                    60
                            );

                    require(
                            packet.gtpuBytes() == 68,
                            "GTP-U header accounting failed"
                    );

                    require(
                            packet.n3WireBytes() == 96,
                            "N3 outer IPv4/UDP/GTP-U accounting failed"
                    );
                }
        );

        final CellularUserPlaneTrace[] successful =
                new CellularUserPlaneTrace[1];

        test(
                results,
                "cell-up-gateway-echo",
                () -> {
                    successful[0] =
                            CellularUserPlaneService.ping(
                                    deviceId,
                                    session,
                                    CellularUserPlaneService.DEFAULT_GATEWAY
                            );

                    require(
                            successful[0].success(),
                            "UPF gateway echo failed: "
                                    + successful[0].failureReason()
                    );

                    require(
                            successful[0].roundTripMicros() > 0L,
                            "RTT was not accumulated"
                    );
                }
        );

        test(
                results,
                "cell-up-protocol-path",
                () -> {
                    CellularUserPlaneTrace trace = successful[0];

                    require(
                            trace != null && trace.success(),
                            "successful trace missing"
                    );

                    for (CellularUserPlaneLayer layer
                            : CellularUserPlaneLayer.values()) {
                        boolean present =
                                trace.hops()
                                        .stream()
                                        .anyMatch(
                                                hop -> hop.layer() == layer
                                        );

                        require(
                                present,
                                "layer missing from trace: " + layer
                        );
                    }
                }
        );

        test(
                results,
                "cell-up-counters",
                () -> {
                    CellularUserPlaneStatsSnapshot snapshot =
                            CellularUserPlaneService.snapshot(deviceId);

                    require(
                            snapshot.txPackets() == 1L,
                            "TX packet counter mismatch"
                    );

                    require(
                            snapshot.rxPackets() == 1L,
                            "RX packet counter mismatch"
                    );

                    require(
                            snapshot.txRadioBytes() > 0L,
                            "radio byte accounting missing"
                    );
                }
        );

        test(
                results,
                "cell-up-dnn-echo",
                () -> {
                    CellularUserPlaneTrace trace =
                            CellularUserPlaneService.ping(
                                    deviceId,
                                    session,
                                    CellularUserPlaneService.DNN_TEST_ECHO
                            );

                    require(
                            trace.success(),
                            "DNN test endpoint was not routed"
                    );

                    require(
                            "DNN_TEST_ECHO".equals(trace.route()),
                            "unexpected DNN route label"
                    );
                }
        );

        test(
                results,
                "cell-up-no-route",
                () -> {
                    CellularUserPlaneTrace trace =
                            CellularUserPlaneService.ping(
                                    deviceId,
                                    session,
                                    "203.0.113.77"
                            );

                    require(
                            !trace.success(),
                            "unknown destination unexpectedly routed"
                    );

                    require(
                            "NO_ROUTE".equals(trace.failureReason()),
                            "wrong no-route failure reason"
                    );
                }
        );

        test(
                results,
                "cell-up-pdu-required",
                () -> {
                    CellularUserPlaneTrace trace =
                            CellularUserPlaneService.ping(
                                    UUID.randomUUID(),
                                    null,
                                    CellularUserPlaneService.DEFAULT_GATEWAY
                            );

                    require(
                            !trace.success(),
                            "ping succeeded without PDU session"
                    );

                    require(
                            "NO_PDU_SESSION".equals(trace.failureReason()),
                            "wrong missing-PDU failure reason"
                    );
                }
        );

        test(
                results,
                "cell-up-inactive-pdu",
                () -> {
                    UUID inactiveId = UUID.randomUUID();

                    PduSession inactive =
                            new PduSession(
                                    2,
                                    inactiveId,
                                    "internet",
                                    "10.0.0.3",
                                    9,
                                    false
                            );

                    CellularUserPlaneTrace trace =
                            CellularUserPlaneService.ping(
                                    inactiveId,
                                    inactive,
                                    CellularUserPlaneService.DEFAULT_GATEWAY
                            );

                    require(
                            !trace.success(),
                            "inactive PDU session transmitted data"
                    );

                    require(
                            "PDU_SESSION_INACTIVE".equals(
                                    trace.failureReason()
                            ),
                            "wrong inactive-PDU failure reason"
                    );
                }
        );

        CellularUserPlaneService.reset(deviceId);

        return new Report(results);
    }

    private static void test(
            List<Result> results,
            String id,
            CheckedTest body
    ) {
        try {
            body.run();

            results.add(
                    new Result(
                            id,
                            true,
                            ""
                    )
            );
        } catch (Throwable throwable) {
            String detail = throwable.getMessage();

            if (detail == null || detail.isBlank()) {
                detail =
                        throwable.getClass()
                                .getSimpleName();
            }

            results.add(
                    new Result(
                            id,
                            false,
                            detail
                    )
            );
        }
    }

    private static void require(
            boolean condition,
            String message
    ) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }

    @FunctionalInterface
    private interface CheckedTest {
        void run() throws Exception;
    }

    public record Result(
            String id,
            boolean passed,
            String detail
    ) {
    }

    public record Report(
            List<Result> results
    ) {
        public Report {
            results = List.copyOf(results);
        }

        public int passed() {
            return (int) results.stream()
                    .filter(Result::passed)
                    .count();
        }

        public int failed() {
            return results.size() - passed();
        }
    }
}

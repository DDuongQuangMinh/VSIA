package com.k1ngtle.vsia.signality.engineering.wifi.trace;

import java.util.List;

public final class WifiPacketTraceTestSuite {
    private WifiPacketTraceTestSuite() {
    }

    public static List<WifiPacketTraceTestResult> runAll() {
        return List.of(
                boundedCapacity(),
                chronologicalOrder(),
                clearWorks(),
                formatterContainsProtocolFields(),
                dropOutcomePreserved()
        );
    }

    private static WifiPacketTraceTestResult boundedCapacity() {
        WifiPacketTraceBuffer buffer =
                new WifiPacketTraceBuffer(
                        8
                );

        for (int i = 0; i < 12; i++) {
            append(
                    buffer,
                    i,
                    WifiPacketOutcome.DELIVERED
            );
        }

        List<WifiPacketTraceEvent> events =
                buffer.snapshot();

        return result(
                "wifi-w17-trace-capacity",
                events.size() == 8
                        && events.get(0)
                        .sequence() == 4
                        && events.get(7)
                        .sequence() == 11,
                "Packet trace must evict the oldest entries while preserving sequence numbers"
        );
    }

    private static WifiPacketTraceTestResult chronologicalOrder() {
        WifiPacketTraceBuffer buffer =
                new WifiPacketTraceBuffer(
                        8
                );

        append(
                buffer,
                100,
                WifiPacketOutcome.QUEUED
        );
        append(
                buffer,
                200,
                WifiPacketOutcome.DELIVERED
        );

        List<WifiPacketTraceEvent> events =
                buffer.snapshot();

        return result(
                "wifi-w17-trace-order",
                events.get(0)
                        .timestampMicros() == 100
                        && events.get(1)
                        .timestampMicros() == 200,
                "Packet trace snapshot must preserve capture order"
        );
    }

    private static WifiPacketTraceTestResult clearWorks() {
        WifiPacketTraceBuffer buffer =
                new WifiPacketTraceBuffer(
                        8
                );

        append(
                buffer,
                1,
                WifiPacketOutcome.DELIVERED
        );

        buffer.clear();

        return result(
                "wifi-w17-trace-clear",
                buffer.snapshot()
                        .isEmpty(),
                "Clear must remove all captured packet events"
        );
    }

    private static WifiPacketTraceTestResult formatterContainsProtocolFields() {
        WifiPacketTraceBuffer buffer =
                new WifiPacketTraceBuffer(
                        8
                );

        WifiPacketTraceEvent event =
                buffer.append(
                        1000L,
                        WifiPacketDirection.RX,
                        "DATA",
                        0,
                        "00:11:22:33:44:55",
                        "FF:FF:FF:FF:FF:FF",
                        8,
                        "EHT",
                        512,
                        120L,
                        -58.2,
                        23.6,
                        22.9,
                        false,
                        "STANDARD_LDPC_FEC",
                        WifiPacketOutcome.DELIVERED,
                        "Recovered"
                );

        String compact =
                WifiPacketTraceFormatter.compact(
                        event
                );

        return result(
                "wifi-w17-trace-formatter",
                compact.contains(
                        "RX"
                )
                        && compact.contains(
                        "DATA"
                        )
                        && compact.contains(
                        "MCS8"
                        )
                        && compact.contains(
                        "DELIVERED"
                        ),
                "Compact formatter must expose direction, frame type, MCS and outcome"
        );
    }

    private static WifiPacketTraceTestResult dropOutcomePreserved() {
        WifiPacketTraceBuffer buffer =
                new WifiPacketTraceBuffer(
                        8
                );

        append(
                buffer,
                5,
                WifiPacketOutcome.DETAILED_PHY_DROP
        );

        return result(
                "wifi-w17-trace-drop-outcome",
                buffer.snapshot()
                        .get(0)
                        .outcome()
                        == WifiPacketOutcome.DETAILED_PHY_DROP,
                "Detailed PHY rejection must remain distinguishable from analytical/capture drops"
        );
    }

    private static void append(
            WifiPacketTraceBuffer buffer,
            long timestamp,
            WifiPacketOutcome outcome
    ) {
        buffer.append(
                timestamp,
                WifiPacketDirection.RX,
                "DATA",
                0,
                "00:11:22:33:44:55",
                "66:77:88:99:AA:BB",
                4,
                "HE",
                128,
                80,
                -60.0,
                20.0,
                19.0,
                false,
                "STANDARD_LDPC_FEC",
                outcome,
                outcome.name()
        );
    }

    private static WifiPacketTraceTestResult result(
            String id,
            boolean passed,
            String detail
    ) {
        return new WifiPacketTraceTestResult(
                id,
                passed,
                detail
        );
    }
}

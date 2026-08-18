package com.k1ngtle.vsia.signality.engineering.wifi.tcp;

import com.k1ngtle.vsia.signality.engineering.wifi.ip.InternetChecksum;

import java.util.ArrayList;
import java.util.List;

public final class TcpLabTestSuite {
    private TcpLabTestSuite() {
    }

    public static List<TcpLabTestResult> runAll() {
        return List.of(
                sequenceWrap(),
                rtoEstimator(),
                congestionSlowStart(),
                fastRetransmit(),
                threeWayHandshake(),
                dataAndAck(),
                rtoRetransmission(),
                gracefulClose(),
                tcpHeaderChecksum()
        );
    }

    private static TcpLabTestResult sequenceWrap() {
        long value =
                TcpSequence.add(
                        0xFFFF_FFFEL,
                        4L
                );

        return result(
                "wifi-w191-sequence-wrap",
                value == 2L
                        && TcpSequence.before(
                        0xFFFF_FFFEL,
                        2L
                ),
                "TCP sequence arithmetic must operate modulo 2^32"
        );
    }

    private static TcpLabTestResult rtoEstimator() {
        TcpRtoEstimator estimator =
                new TcpRtoEstimator(
                        0.001
                );

        boolean initial =
                Math.abs(
                        estimator.rtoSeconds()
                                - 1.0
                ) < 1.0E-12;

        estimator.observeRttSeconds(
                0.100
        );

        boolean first =
                Math.abs(
                        estimator.srttSeconds()
                                - 0.100
                ) < 1.0E-12
                        && Math.abs(
                        estimator.rttvarSeconds()
                                - 0.050
                ) < 1.0E-12
                        && Math.abs(
                        estimator.rtoSeconds()
                                - 1.0
                ) < 1.0E-12;

        estimator.backoff();

        return result(
                "wifi-w191-rto-estimator",
                initial
                        && first
                        && Math.abs(
                        estimator.rtoSeconds()
                                - 2.0
                ) < 1.0E-12,
                "RFC 6298-style estimator must start at 1 s, initialize SRTT/RTTVAR, enforce 1 s floor, and back off x2"
        );
    }

    private static TcpLabTestResult congestionSlowStart() {
        TcpCongestionController controller =
                new TcpCongestionController(
                        1460
                );

        long initial =
                controller.cwndBytes();

        controller.onNewAcknowledgement(
                1460,
                1000L
        );

        return result(
                "wifi-w191-slow-start",
                initial == 4380L
                        && controller.cwndBytes()
                        == 5840L,
                "RFC 5681 classic initial window for SMSS 1460 is 3*SMSS; slow start grows by at most one SMSS per new ACK"
        );
    }

    private static TcpLabTestResult fastRetransmit() {
        TcpCongestionController controller =
                new TcpCongestionController(
                        1460
                );

        boolean first =
                controller.onDuplicateAcknowledgement(
                        10_000L
                );

        boolean second =
                controller.onDuplicateAcknowledgement(
                        10_000L
                );

        boolean third =
                controller.onDuplicateAcknowledgement(
                        10_000L
                );

        return result(
                "wifi-w191-fast-retransmit",
                !first
                        && !second
                        && third
                        && controller.fastRecovery()
                        && controller.duplicateAckCount()
                        == 3,
                "Third duplicate ACK must trigger fast retransmit / fast recovery"
        );
    }

    private static TcpLabTestResult threeWayHandshake() {
        Pair pair =
                establish();

        return result(
                "wifi-w191-three-way-handshake",
                pair.client()
                        .state()
                        == TcpState.ESTABLISHED
                        && pair.server()
                        .state()
                        == TcpState.ESTABLISHED,
                "SYN -> SYN-ACK -> ACK must establish both endpoints"
        );
    }

    private static TcpLabTestResult dataAndAck() {
        Pair pair =
                establish();

        long now =
                100_000L;

        TcpConnectionAction sent =
                pair.client()
                        .sendData(
                                1460,
                                now
                        );

        if (sent.outbound()
                .size() != 1) {
            return result(
                    "wifi-w191-data-ack",
                    false,
                    "Expected one 1460-byte segment"
            );
        }

        TcpConnectionAction serverAction =
                pair.server()
                        .onSegment(
                                sent.outbound()
                                        .get(0),
                                now + 20_000L
                        );

        if (serverAction.outbound()
                .isEmpty()) {
            return result(
                    "wifi-w191-data-ack",
                    false,
                    "Receiver did not emit ACK"
            );
        }

        pair.client()
                .onSegment(
                        serverAction.outbound()
                                .get(0),
                        now + 40_000L
                );

        TcpConnectionSnapshot snapshot =
                pair.client()
                        .snapshot();

        return result(
                "wifi-w191-data-ack",
                snapshot.bytesInFlight() == 0L
                        && Double.isFinite(
                        snapshot.srttMs()
                )
                        && snapshot.congestionWindowBytes()
                        > 4380L,
                "New DATA ACK must advance SND.UNA, clear flight bytes, sample RTT, and grow cwnd"
        );
    }

    private static TcpLabTestResult rtoRetransmission() {
        Pair pair =
                establish();

        long now =
                500_000L;

        TcpConnectionAction sent =
                pair.client()
                        .sendData(
                                1000,
                                now
                        );

        if (sent.outbound()
                .isEmpty()) {
            return result(
                    "wifi-w191-rto-retransmission",
                    false,
                    "Could not queue data"
            );
        }

        TcpConnectionAction early =
                pair.client()
                        .onTimer(
                                now + 500_000L
                        );

        TcpConnectionAction expired =
                pair.client()
                        .onTimer(
                                now + 2_000_000L
                        );

        return result(
                "wifi-w191-rto-retransmission",
                early.outbound()
                        .isEmpty()
                        && expired.retransmitEarliest()
                        && expired.outbound()
                        .size() == 1
                        && expired.outbound()
                        .get(0)
                        .retransmission()
                        && pair.client()
                        .snapshot()
                        .retransmissions() == 1,
                "RTO must not retransmit early and must retransmit the earliest unacknowledged segment after expiration"
        );
    }

    private static TcpLabTestResult gracefulClose() {
        Pair pair =
                establish();

        long now =
                1_000_000L;

        TcpConnectionAction fin =
                pair.client()
                        .close(
                                now
                        );

        TcpConnectionAction serverAck =
                pair.server()
                        .onSegment(
                                fin.outbound()
                                        .get(0),
                                now + 10_000L
                        );

        pair.client()
                .onSegment(
                        serverAck.outbound()
                                .get(0),
                        now + 20_000L
                );

        TcpConnectionAction serverFin =
                pair.server()
                        .close(
                                now + 30_000L
                        );

        TcpConnectionAction clientAck =
                pair.client()
                        .onSegment(
                                serverFin.outbound()
                                        .get(0),
                                now + 40_000L
                        );

        pair.server()
                .onSegment(
                        clientAck.outbound()
                                .get(0),
                        now + 50_000L
                );

        return result(
                "wifi-w191-graceful-close",
                pair.client()
                        .state()
                        == TcpState.TIME_WAIT
                        && pair.server()
                        .state()
                        == TcpState.CLOSED,
                "FIN/ACK close sequence must reach TIME_WAIT on active closer and CLOSED on passive closer"
        );
    }

    private static TcpLabTestResult tcpHeaderChecksum() {
        TcpWireHeader header =
                new TcpWireHeader(
                        49152,
                        80,
                        0x12345678L,
                        0xABCDEF01L,
                        TcpFlags.data(),
                        65535,
                        0
                );

        byte[] payload =
                new byte[] {
                        1,
                        2,
                        3,
                        4,
                        5
                };

        byte[] encoded =
                header.encode(
                        "192.168.1.100",
                        "192.168.1.2",
                        payload
                );

        byte[] pseudo =
                pseudo(
                        "192.168.1.100",
                        "192.168.1.2",
                        encoded,
                        payload
                );

        return result(
                "wifi-w191-tcp-checksum",
                encoded.length == 20
                        && InternetChecksum.compute(
                        pseudo
                ) == 0,
                "20-byte TCP header checksum must validate over the IPv4 pseudo-header and payload"
        );
    }

    private static Pair establish() {
        TcpConnection client =
                new TcpConnection(
                        50000,
                        80,
                        1460,
                        1000L
                );

        TcpConnection server =
                new TcpConnection(
                        80,
                        50000,
                        1460,
                        5000L
                );

        server.listen();

        TcpSegment syn =
                client.activeOpen(
                        0L
                )
                        .outbound()
                        .get(0);

        TcpSegment synAck =
                server.onSegment(
                                syn,
                                10_000L
                        )
                        .outbound()
                        .get(0);

        TcpSegment ack =
                client.onSegment(
                                synAck,
                                20_000L
                        )
                        .outbound()
                        .get(0);

        server.onSegment(
                ack,
                30_000L
        );

        return new Pair(
                client,
                server
        );
    }

    private static byte[] pseudo(
            String source,
            String destination,
            byte[] header,
            byte[] payload
    ) {
        byte[] src =
                com.k1ngtle.vsia.signality.engineering.wifi.ip.Ipv4Address
                        .parse(
                                source
                        );

        byte[] dst =
                com.k1ngtle.vsia.signality.engineering.wifi.ip.Ipv4Address
                        .parse(
                                destination
                        );

        int tcpLength =
                header.length
                        + payload.length;

        byte[] out =
                new byte[
                        12
                                + tcpLength
                                + (
                                tcpLength
                                        % 2
                        )
                ];

        System.arraycopy(
                src,
                0,
                out,
                0,
                4
        );

        System.arraycopy(
                dst,
                0,
                out,
                4,
                4
        );

        out[9] =
                6;

        out[10] =
                (
                        byte
                ) (
                tcpLength
                        >>> 8
        );

        out[11] =
                (
                        byte
                ) tcpLength;

        System.arraycopy(
                header,
                0,
                out,
                12,
                header.length
        );

        System.arraycopy(
                payload,
                0,
                out,
                12 + header.length,
                payload.length
        );

        return out;
    }

    private static TcpLabTestResult result(
            String id,
            boolean passed,
            String detail
    ) {
        return new TcpLabTestResult(
                id,
                passed,
                detail
        );
    }

    private record Pair(
            TcpConnection client,
            TcpConnection server
    ) {
    }
}

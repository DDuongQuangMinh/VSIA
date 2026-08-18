package com.k1ngtle.vsia.signality.engineering.wifi.tcp.options;

import com.k1ngtle.vsia.signality.engineering.wifi.tcp.TcpConnection;
import com.k1ngtle.vsia.signality.engineering.wifi.tcp.TcpConnectionAction;
import com.k1ngtle.vsia.signality.engineering.wifi.tcp.TcpFlags;
import com.k1ngtle.vsia.signality.engineering.wifi.tcp.TcpSegment;
import com.k1ngtle.vsia.signality.engineering.wifi.tcp.TcpWireHeader;
import com.k1ngtle.vsia.signality.engineering.wifi.tcp.live.TcpLivePacketCodec;
import com.k1ngtle.vsia.signality.engineering.wifi.tcp.live.TcpLiveController;
import com.k1ngtle.vsia.signality.engineering.wifi.tcp.stream.TcpSackBlock;
import com.k1ngtle.vsia.signality.internet.OSINetworkPacket;

import java.util.ArrayList;
import java.util.List;

public final class TcpLiveOptionsTestSuite {
    private TcpLiveOptionsTestSuite() {
    }

    public static List<TcpLiveOptionsTestResult> runAll() {
        return List.of(
                optionRoundTrip(),
                variableHeaderLength(),
                liveChecksumWithOptions(),
                liveSynNegotiation(),
                liveSackTargetedRetransmit(),
                sackScoreboard(),
                windowScaling(),
                persistProbe(),
                targetedRetransmit()
        );
    }

    private static TcpLiveOptionsTestResult optionRoundTrip() {
        TcpOptionSet source =
                new TcpOptionSet(
                        1200,
                        2,
                        true,
                        List.of(
                                new TcpSackBlock(
                                        2000L,
                                        2400L
                                ),
                                new TcpSackBlock(
                                        3000L,
                                        3600L
                                )
                        ),
                        123456L,
                        120000L
                );

        byte[] encoded =
                TcpOptionCodec.encode(
                        source
                );

        TcpOptionSet decoded =
                TcpOptionCodec.decode(
                        encoded
                );

        return result(
                "wifi-w194-option-roundtrip",
                encoded.length <= 40
                        && (encoded.length & 3) == 0
                        && decoded.mss() == 1200
                        && decoded.windowScale() == 2
                        && decoded.sackPermitted()
                        && decoded.sackBlocks().size() == 2
                        && decoded.timestampValue() == 123456L
                        && decoded.timestampEchoReply() == 120000L,
                "MSS/WS/SACK/Timestamp options must round-trip through standard TCP option kinds"
        );
    }

    private static TcpLiveOptionsTestResult variableHeaderLength() {
        TcpOptionSet options =
                TcpOptionSet.synOffer(
                        1200,
                        2,
                        true,
                        1000L
                );

        byte[] optionBytes =
                TcpOptionCodec.encode(
                        options
                );

        TcpWireHeader header =
                new TcpWireHeader(
                        50000,
                        80,
                        100L,
                        0L,
                        TcpFlags.synOnly(),
                        65535,
                        0
                );

        byte[] encoded =
                header.encode(
                        "192.168.1.100",
                        "192.168.1.2",
                        new byte[0],
                        optionBytes
                );

        int dataOffsetWords =
                (
                        encoded[12]
                                >>> 4
                )
                        & 0x0F;

        return result(
                "wifi-w194-variable-header",
                encoded.length == 20 + optionBytes.length
                        && dataOffsetWords * 4 == encoded.length,
                "TCP Data Offset must reflect the real 20..60-byte variable header length"
        );
    }

    private static TcpLiveOptionsTestResult liveChecksumWithOptions() {
        TcpSegment segment =
                new TcpSegment(
                        50000,
                        80,
                        1000L,
                        5000L,
                        TcpFlags.data(),
                        32768,
                        4,
                        1_000_000L,
                        false
                );

        TcpOptionSet options =
                new TcpOptionSet(
                        TcpOptionSet.ABSENT,
                        TcpOptionSet.ABSENT,
                        false,
                        List.of(
                                new TcpSackBlock(
                                        3000L,
                                        3600L
                                )
                        ),
                        1000L,
                        900L
                );

        OSINetworkPacket packet =
                TcpLivePacketCodec.encode(
                        "tcp-test",
                        "AA:BB:CC:00:00:01",
                        "AA:BB:CC:00:00:02",
                        "192.168.1.100",
                        "192.168.1.2",
                        segment,
                        new byte[] {
                                1,
                                2,
                                3,
                                4
                        },
                        0,
                        4,
                        options
                );

        TcpOptionSet decoded =
                TcpLivePacketCodec.decodeOptions(
                        packet
                );

        return result(
                "wifi-w194-live-option-checksum",
                TcpLivePacketCodec.checksumValid(
                        packet
                )
                        && decoded.hasSackBlocks()
                        && decoded.hasTimestamp(),
                "Live TCP checksum must cover the encoded option bytes as part of the TCP header"
        );
    }

    private static TcpLiveOptionsTestResult liveSynNegotiation() {
        TcpLiveController client =
                new TcpLiveController();

        List<OSINetworkPacket> outbound =
                new ArrayList<>();

        client.startApplication(
                applicationRequest(
                        64
                ),
                1_000_000L,
                outbound::add
        );

        if (outbound.isEmpty()) {
            return result(
                    "wifi-w194-live-syn-options",
                    false,
                    "No SYN packet emitted"
            );
        }

        OSINetworkPacket syn =
                outbound.get(0);

        TcpOptionSet options =
                TcpLivePacketCodec.decodeOptions(
                        syn
                );

        TcpSegment segment =
                TcpLivePacketCodec.decode(
                        syn
                );

        return result(
                "wifi-w194-live-syn-options",
                segment.flags().syn()
                        && options.mss() == TcpLiveController.DEFAULT_SMSS
                        && options.windowScale()
                        == TcpLiveController.DEFAULT_WINDOW_SCALE
                        && options.sackPermitted()
                        && options.hasTimestamp(),
                "Live SYN must advertise MSS, Window Scale, SACK-Permitted and Timestamp options"
        );
    }

    private static TcpLiveOptionsTestResult liveSackTargetedRetransmit() {
        TcpLiveController client =
                new TcpLiveController();

        TcpLiveController server =
                new TcpLiveController();

        List<OSINetworkPacket> clientOut =
                new ArrayList<>();

        List<OSINetworkPacket> serverOut =
                new ArrayList<>();

        client.startApplication(
                applicationRequest(
                        5000
                ),
                0L,
                clientOut::add
        );

        if (clientOut.isEmpty()) {
            return result(
                    "wifi-w194-live-sack-retransmit",
                    false,
                    "Client did not emit SYN"
            );
        }

        OSINetworkPacket syn =
                clientOut.remove(0);

        server.handleIncoming(
                SERVER_MAC,
                SERVER_IP,
                syn,
                10_000L,
                serverOut::add,
                packet -> {
                }
        );

        if (serverOut.isEmpty()) {
            return result(
                    "wifi-w194-live-sack-retransmit",
                    false,
                    "Server did not emit SYN-ACK"
            );
        }

        OSINetworkPacket synAck =
                serverOut.remove(0);

        client.handleIncoming(
                CLIENT_MAC,
                CLIENT_IP,
                synAck,
                20_000L,
                clientOut::add,
                packet -> {
                }
        );

        OSINetworkPacket pureAck =
                clientOut.stream()
                        .filter(
                                packet -> {
                                    TcpSegment segment =
                                            TcpLivePacketCodec.decode(
                                                    packet
                                            );

                                    return segment.flags().ack()
                                            && !segment.flags().syn()
                                            && segment.payloadBytes() == 0;
                                }
                        )
                        .findFirst()
                        .orElse(
                                null
                        );

        List<OSINetworkPacket> data =
                clientOut.stream()
                        .filter(
                                packet ->
                                        TcpLivePacketCodec.decode(
                                                packet
                                        ).payloadBytes() > 0
                        )
                        .toList();

        if (pureAck == null
                || data.size() < 2) {
            return result(
                    "wifi-w194-live-sack-retransmit",
                    false,
                    "Handshake did not produce ACK plus multiple DATA segments"
            );
        }

        server.handleIncoming(
                SERVER_MAC,
                SERVER_IP,
                pureAck,
                30_000L,
                serverOut::add,
                packet -> {
                }
        );

        OSINetworkPacket firstData =
                data.get(0);

        OSINetworkPacket secondData =
                data.get(1);

        long firstSequence =
                TcpLivePacketCodec.decode(
                        firstData
                ).sequenceNumber();

        serverOut.clear();

        server.handleIncoming(
                SERVER_MAC,
                SERVER_IP,
                secondData,
                40_000L,
                serverOut::add,
                packet -> {
                }
        );

        OSINetworkPacket sackAck =
                serverOut.stream()
                        .filter(
                                packet ->
                                        TcpLivePacketCodec.decodeOptions(
                                                packet
                                        ).hasSackBlocks()
                        )
                        .findFirst()
                        .orElse(
                                null
                        );

        if (sackAck == null) {
            return result(
                    "wifi-w194-live-sack-retransmit",
                    false,
                    "Out-of-order DATA did not produce a SACK-bearing ACK"
            );
        }

        clientOut.clear();

        client.handleIncoming(
                CLIENT_MAC,
                CLIENT_IP,
                sackAck,
                50_000L,
                clientOut::add,
                packet -> {
                }
        );

        OSINetworkPacket retransmission =
                clientOut.stream()
                        .filter(
                                packet ->
                                        packet.payload.getBoolean(
                                                "tcp_retransmission"
                                        )
                                                && TcpLivePacketCodec.decode(
                                                packet
                                        ).sequenceNumber()
                                        == firstSequence
                        )
                        .findFirst()
                        .orElse(
                                null
                        );

        return result(
                "wifi-w194-live-sack-retransmit",
                retransmission != null,
                "A live SACK-bearing duplicate ACK must trigger targeted retransmission of the missing sequence range"
        );
    }

    private static TcpLiveOptionsTestResult sackScoreboard() {
        TcpSackScoreboard scoreboard =
                new TcpSackScoreboard();

        scoreboard.update(
                1000L,
                List.of(
                        new TcpSackBlock(
                                1400L,
                                1800L
                        ),
                        new TcpSackBlock(
                                1800L,
                                2200L
                        )
                )
        );

        return result(
                "wifi-w194-sack-scoreboard",
                scoreboard.isSacked(
                        1500L,
                        2100L
                )
                        && !scoreboard.isSacked(
                        1000L,
                        1200L
                ),
                "Sender SACK scoreboard must merge contiguous blocks and distinguish missing holes"
        );
    }

    private static TcpLiveOptionsTestResult windowScaling() {
        Pair pair =
                establish();

        pair.client().setPeerWindowScale(
                2
        );

        TcpSegment ack =
                new TcpSegment(
                        80,
                        50000,
                        pair.client().snapshot().receiveNext(),
                        pair.client().snapshot().sendUnacknowledged(),
                        TcpFlags.ackOnly(),
                        1000,
                        0,
                        50_000L,
                        false
                );

        pair.client().onSegment(
                ack,
                60_000L
        );

        return result(
                "wifi-w194-window-scale",
                pair.client().snapshot().receiverWindow() == 4000
                        && pair.client().localWindowScale() == 2,
                "Post-SYN advertised windows must be shifted by the negotiated peer Window Scale"
        );
    }

    private static TcpLiveOptionsTestResult persistProbe() {
        Pair pair =
                establish();

        TcpSegment zeroWindow =
                new TcpSegment(
                        80,
                        50000,
                        pair.client().snapshot().receiveNext(),
                        pair.client().snapshot().sendUnacknowledged(),
                        TcpFlags.ackOnly(),
                        0,
                        0,
                        100_000L,
                        false
                );

        pair.client().onSegment(
                zeroWindow,
                100_000L
        );

        boolean armed =
                pair.client().persistTimerArmed();

        TcpConnectionAction early =
                pair.client().createPersistProbe(
                        1_000_000L
                );

        TcpConnectionAction due =
                pair.client().createPersistProbe(
                        1_100_000L
                );

        return result(
                "wifi-w194-persist-probe",
                armed
                        && early.outbound().isEmpty()
                        && due.outbound().size() == 1
                        && due.outbound().get(0).payloadBytes() == 1
                        && due.outbound().get(0).retransmission(),
                "A zero receive window must drive an actual one-byte persist probe when its timer expires"
        );
    }

    private static TcpLiveOptionsTestResult targetedRetransmit() {
        Pair pair =
                establish();

        TcpConnectionAction data =
                pair.client().sendData(
                        2400,
                        100_000L
                );

        if (data.outbound().size() < 2) {
            return result(
                    "wifi-w194-targeted-retransmit",
                    false,
                    "Expected at least two outstanding segments"
            );
        }

        long firstSeq =
                data.outbound()
                        .get(0)
                        .sequenceNumber();

        TcpConnectionAction retransmit =
                pair.client().retransmitSequence(
                        firstSeq,
                        200_000L
                );

        return result(
                "wifi-w194-targeted-retransmit",
                retransmit.outbound().size() == 1
                        && retransmit.outbound().get(0).sequenceNumber() == firstSeq
                        && retransmit.outbound().get(0).retransmission(),
                "SACK logic must be able to retransmit one specific outstanding sequence range"
        );
    }

    private static final String CLIENT_MAC =
            "AA:BB:CC:00:00:01";

    private static final String SERVER_MAC =
            "AA:BB:CC:00:00:02";

    private static final String CLIENT_IP =
            "192.168.1.100";

    private static final String SERVER_IP =
            "192.168.1.2";

    private static OSINetworkPacket applicationRequest(
            int payloadBytes
    ) {
        OSINetworkPacket request =
                new OSINetworkPacket();

        request.sourceMac =
                CLIENT_MAC;

        request.targetMac =
                SERVER_MAC;

        request.sourceIp =
                CLIENT_IP;

        request.targetIp =
                SERVER_IP;

        request.sourcePort =
                50000;

        request.targetPort =
                80;

        request.ipProtocol =
                6;

        request.applicationProtocol =
                "HTTP";

        request.payload.putString(
                "method",
                "GET"
        );

        request.payload.putString(
                "path",
                "/"
        );

        request.payload.putByteArray(
                "blob",
                new byte[
                        Math.max(
                                0,
                                payloadBytes
                        )
                ]
        );

        return request;
    }

    private static Pair establish() {
        TcpConnection client =
                new TcpConnection(
                        50000,
                        80,
                        1200,
                        1000L
                );

        TcpConnection server =
                new TcpConnection(
                        80,
                        50000,
                        1200,
                        5000L
                );

        client.setLocalWindowScale(
                2
        );

        server.setLocalWindowScale(
                2
        );

        server.listen();

        TcpSegment syn =
                client.activeOpen(
                        0L
                ).outbound().get(0);

        TcpSegment synAck =
                server.onSegment(
                        syn,
                        10_000L
                ).outbound().get(0);

        TcpSegment ack =
                client.onSegment(
                        synAck,
                        20_000L
                ).outbound().get(0);

        server.onSegment(
                ack,
                30_000L
        );

        return new Pair(
                client,
                server
        );
    }

    private static TcpLiveOptionsTestResult result(
            String id,
            boolean passed,
            String detail
    ) {
        return new TcpLiveOptionsTestResult(
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

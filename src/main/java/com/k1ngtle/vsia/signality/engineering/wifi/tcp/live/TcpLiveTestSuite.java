package com.k1ngtle.vsia.signality.engineering.wifi.tcp.live;

import com.k1ngtle.vsia.signality.engineering.wifi.tcp.TcpFlags;
import com.k1ngtle.vsia.signality.engineering.wifi.tcp.TcpSegment;
import com.k1ngtle.vsia.signality.internet.OSINetworkPacket;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.function.Consumer;

public final class TcpLiveTestSuite {
    private static final String CLIENT_MAC =
            "AA:BB:CC:00:00:01";

    private static final String SERVER_MAC =
            "AA:BB:CC:00:00:02";

    private static final String CLIENT_IP =
            "192.168.1.100";

    private static final String SERVER_IP =
            "192.168.1.2";

    private TcpLiveTestSuite() {
    }

    public static List<TcpLiveTestResult> runAll() {
        return List.of(
                packetCodecRoundTrip(),
                liveHttpRoundTrip(),
                rtoRetransmission(),
                multiSegmentResponse(),
                automaticTeardown()
        );
    }

    private static TcpLiveTestResult packetCodecRoundTrip() {
        TcpSegment segment =
                new TcpSegment(
                        50000,
                        80,
                        1234L,
                        4321L,
                        TcpFlags.data(),
                        65535,
                        4,
                        1000L,
                        false
                );

        byte[] chunk =
                new byte[] {
                        1,
                        2,
                        3,
                        4
                };

        OSINetworkPacket packet =
                TcpLivePacketCodec.encode(
                        "tcp-test",
                        CLIENT_MAC,
                        SERVER_MAC,
                        CLIENT_IP,
                        SERVER_IP,
                        segment,
                        chunk,
                        0,
                        4
                );

        TcpSegment decoded =
                TcpLivePacketCodec.decode(
                        packet
                );

        return result(
                "wifi-w192-packet-codec",
                TcpLivePacketCodec.checksumValid(
                        packet
                )
                        && decoded.sequenceNumber()
                        == 1234L
                        && decoded.acknowledgementNumber()
                        == 4321L
                        && decoded.payloadBytes() == 4
                        && decoded.flags().ack()
                        && decoded.flags().psh(),
                "Live TCP segment metadata/checksum must survive OSINetworkPacket encoding"
        );
    }

    private static TcpLiveTestResult liveHttpRoundTrip() {
        Simulation simulation =
                new Simulation(
                        "hello"
                );

        simulation.start();

        simulation.pump(
                200
        );

        return result(
                "wifi-w192-live-http-roundtrip",
                simulation.clientResponses.size()
                        == 1
                        && simulation.clientResponses
                        .get(0)
                        .payload.getInt(
                                "status"
                        )
                        == 200
                        && "hello".equals(
                        simulation.clientResponses
                                .get(0)
                                .payload.getString(
                                        "content"
                                )
                ),
                "SYN/SYN-ACK/ACK + TCP DATA must carry an HTTP request and response end-to-end"
        );
    }

    private static TcpLiveTestResult rtoRetransmission() {
        TcpLiveController client =
                new TcpLiveController();

        Deque<OSINetworkPacket> wire =
                new ArrayDeque<>();

        OSINetworkPacket request =
                request();

        client.startApplication(
                request,
                0L,
                wire::add
        );

        wire.clear();

        client.tick(
                1_100_000L,
                wire::add
        );

        OSINetworkPacket retransmitted =
                wire.peekFirst();

        return result(
                "wifi-w192-live-rto",
                retransmitted != null
                        && retransmitted.payload
                        .getBoolean(
                                "tcp_retransmission"
                        )
                        && client.snapshot()
                        .retransmissions() == 1,
                "A lost live SYN must be retransmitted after the RFC 6298 timer expires"
        );
    }

    private static TcpLiveTestResult multiSegmentResponse() {
        String content =
                "X".repeat(
                        5000
                );

        Simulation simulation =
                new Simulation(
                        content
                );

        simulation.start();

        simulation.pump(
                500
        );

        return result(
                "wifi-w192-live-segmentation",
                simulation.clientResponses.size()
                        == 1
                        && content.equals(
                        simulation.clientResponses
                                .get(0)
                                .payload.getString(
                                        "content"
                                )
                ),
                "Application payloads larger than SMSS/cwnd must be segmented, windowed, ACKed and reassembled"
        );
    }

    private static TcpLiveTestResult automaticTeardown() {
        Simulation simulation =
                new Simulation(
                        "done"
                );

        simulation.start();

        simulation.pump(
                300
        );

        String clientState =
                simulation.client
                        .snapshot()
                        .state();

        String serverState =
                simulation.server
                        .snapshot()
                        .state();

        boolean clientFinished =
                clientState.equals(
                        "CLOSED"
                )
                        || clientState.equals(
                        "LAST_ACK"
                )
                        || clientState.equals(
                        "TIME_WAIT"
                );

        boolean serverFinished =
                serverState.equals(
                        "CLOSED"
                )
                        || serverState.equals(
                        "LAST_ACK"
                )
                        || serverState.equals(
                        "TIME_WAIT"
                );

        return result(
                "wifi-w192-live-fin-teardown",
                clientFinished
                        && serverFinished,
                "HTTP response completion must progress the live session into FIN teardown"
        );
    }

    private static OSINetworkPacket request() {
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

        return request;
    }

    private static TcpLiveTestResult result(
            String id,
            boolean passed,
            String detail
    ) {
        return new TcpLiveTestResult(
                id,
                passed,
                detail
        );
    }

    private static final class Simulation {
        private final TcpLiveController client =
                new TcpLiveController();

        private final TcpLiveController server =
                new TcpLiveController();

        private final Deque<OSINetworkPacket> wire =
                new ArrayDeque<>();

        private final List<OSINetworkPacket> clientResponses =
                new ArrayList<>();

        private final String responseContent;

        private long nowMicros;

        private Simulation(
                String responseContent
        ) {
            this.responseContent =
                    responseContent;
        }

        private void start() {
            client.startApplication(
                    request(),
                    nowMicros,
                    wire::add
            );
        }

        private void pump(
                int limit
        ) {
            int count =
                    0;

            while (!wire.isEmpty()
                    && count < limit) {
                OSINetworkPacket packet =
                        wire.removeFirst();

                nowMicros +=
                        10_000L;

                if (SERVER_MAC.equals(
                        packet.targetMac
                )) {
                    server.handleIncoming(
                            SERVER_MAC,
                            SERVER_IP,
                            packet,
                            nowMicros,
                            wire::add,
                            this::serverApplication
                    );
                } else if (CLIENT_MAC.equals(
                        packet.targetMac
                )) {
                    client.handleIncoming(
                            CLIENT_MAC,
                            CLIENT_IP,
                            packet,
                            nowMicros,
                            wire::add,
                            clientResponses::add
                    );
                }

                count++;
            }
        }

        private void serverApplication(
                OSINetworkPacket request
        ) {
            OSINetworkPacket response =
                    new OSINetworkPacket();

            response.sourceMac =
                    SERVER_MAC;

            response.targetMac =
                    CLIENT_MAC;

            response.sourceIp =
                    SERVER_IP;

            response.targetIp =
                    CLIENT_IP;

            response.sourcePort =
                    80;

            response.targetPort =
                    request.sourcePort;

            response.ipProtocol =
                    6;

            response.applicationProtocol =
                    "HTTP";

            response.sessionId =
                    request.sessionId;

            response.isResponse =
                    true;

            response.payload.putInt(
                    "status",
                    200
            );

            response.payload.putString(
                    "content",
                    responseContent
            );

            server.interceptApplicationResponse(
                    response,
                    nowMicros,
                    wire::add
            );
        }
    }
}

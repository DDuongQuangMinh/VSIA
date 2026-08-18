package com.k1ngtle.vsia.signality.engineering.wifi.tcp.stream;

import com.k1ngtle.vsia.signality.engineering.wifi.tcp.TcpConnection;
import com.k1ngtle.vsia.signality.engineering.wifi.tcp.TcpConnectionAction;
import com.k1ngtle.vsia.signality.engineering.wifi.tcp.TcpSegment;
import com.k1ngtle.vsia.signality.engineering.wifi.tcp.TcpState;

import java.util.List;

public final class TcpStreamTestSuite {
    private TcpStreamTestSuite() {
    }

    public static List<TcpStreamTestResult> runAll() {
        return List.of(
                outOfOrderQueue(),
                duplicateSuppression(),
                cumulativeAdvance(),
                sackBlocks(),
                receiveWindowPressure(),
                persistTimer(),
                optionNegotiation(),
                connectionOutOfOrderIntegration()
        );
    }

    private static TcpStreamTestResult outOfOrderQueue() {
        TcpReceiveQueue queue =
                new TcpReceiveQueue(
                        1000L,
                        4096
                );

        TcpReceiveResult result =
                queue.accept(
                        1200L,
                        200
                );

        return result(
                "wifi-w193-out-of-order-queue",
                result.cumulativeAck() == 1000L
                        && result.outOfOrder()
                        && result.bufferedOutOfOrderBytes() == 200,
                "Out-of-order data must be retained without advancing the cumulative ACK"
        );
    }

    private static TcpStreamTestResult duplicateSuppression() {
        TcpReceiveQueue queue =
                new TcpReceiveQueue(
                        1000L,
                        4096
                );

        queue.accept(
                1200L,
                200
        );

        TcpReceiveResult duplicate =
                queue.accept(
                        1200L,
                        200
                );

        return result(
                "wifi-w193-duplicate-suppression",
                duplicate.newlyAcceptedBytes() == 0
                        && duplicate.duplicateBytes() == 200
                        && duplicate.bufferedOutOfOrderBytes() == 200,
                "Duplicate TCP bytes must not consume receive-buffer capacity twice"
        );
    }

    private static TcpStreamTestResult cumulativeAdvance() {
        TcpReceiveQueue queue =
                new TcpReceiveQueue(
                        1000L,
                        4096
                );

        queue.accept(
                1200L,
                200
        );

        TcpReceiveResult fill =
                queue.accept(
                        1000L,
                        200
                );

        return result(
                "wifi-w193-cumulative-ack",
                fill.cumulativeAck() == 1400L
                        && fill.bufferedOutOfOrderBytes() == 0,
                "Filling a sequence hole must advance the cumulative ACK across already buffered contiguous data"
        );
    }

    private static TcpStreamTestResult sackBlocks() {
        TcpReceiveQueue queue =
                new TcpReceiveQueue(
                        1000L,
                        8192
                );

        queue.accept(
                1400L,
                200
        );

        queue.accept(
                1800L,
                300
        );

        List<TcpSackBlock> blocks =
                queue.sackBlocks(
                        4
                );

        return result(
                "wifi-w193-sack-blocks",
                blocks.size() == 2
                        && blocks.stream()
                        .anyMatch(
                                block ->
                                        block.leftEdge() == 1400L
                                                && block.rightEdge() == 1600L
                        )
                        && blocks.stream()
                        .anyMatch(
                                block ->
                                        block.leftEdge() == 1800L
                                                && block.rightEdge() == 2100L
                        ),
                "Receive holes must produce SACK block ranges without moving RCV.NXT"
        );
    }

    private static TcpStreamTestResult receiveWindowPressure() {
        TcpReceiveQueue queue =
                new TcpReceiveQueue(
                        1000L,
                        300
                );

        TcpReceiveResult accepted =
                queue.accept(
                        1300L,
                        500
                );

        return result(
                "wifi-w193-receive-window",
                accepted.newlyAcceptedBytes() == 300
                        && accepted.advertisedWindowBytes() == 0,
                "Receive buffering must be bounded and advertise a zero window when capacity is exhausted"
        );
    }

    private static TcpStreamTestResult persistTimer() {
        TcpPersistTimer timer =
                new TcpPersistTimer();

        timer.observeWindow(
                0,
                0L
        );

        boolean before =
                timer.shouldProbe(
                        999_999L
                );

        boolean at =
                timer.shouldProbe(
                        1_000_000L
                );

        timer.onProbeSent(
                1_000_000L
        );

        boolean backedOff =
                timer.intervalMicros()
                        == 2_000_000L;

        timer.observeWindow(
                4096,
                1_500_000L
        );

        return result(
                "wifi-w193-persist-timer",
                !before
                        && at
                        && backedOff
                        && !timer.armed(),
                "Zero-window persist timer must arm, probe, back off, and disarm when the peer window reopens"
        );
    }

    private static TcpStreamTestResult optionNegotiation() {
        TcpNegotiatedOptions options =
                new TcpNegotiatedOptions(
                        1460,
                        1200,
                        true,
                        7,
                        true
                );

        return result(
                "wifi-w193-option-negotiation",
                options.effectiveMss() == 1200
                        && options.scaledWindow(
                        65535
                ) == 8_388_480L
                        && options.sackPermitted()
                        && options.timestamps(),
                "MSS/window-scale/SACK/timestamp option state must produce deterministic negotiated values"
        );
    }

    private static TcpStreamTestResult connectionOutOfOrderIntegration() {
        Pair pair =
                establish();

        long clientSeq =
                pair.client()
                        .snapshot()
                        .sendNext();

        long serverSeq =
                pair.server()
                        .snapshot()
                        .sendNext();

        TcpSegment late =
                new TcpSegment(
                        50000,
                        80,
                        clientSeq + 200,
                        serverSeq,
                        com.k1ngtle.vsia.signality.engineering.wifi.tcp.TcpFlags.data(),
                        65535,
                        200,
                        50_000L,
                        false
                );

        pair.server()
                .onSegment(
                        late,
                        60_000L
                );

        long ackBeforeHole =
                pair.server()
                        .snapshot()
                        .receiveNext();

        TcpSegment hole =
                new TcpSegment(
                        50000,
                        80,
                        clientSeq,
                        serverSeq,
                        com.k1ngtle.vsia.signality.engineering.wifi.tcp.TcpFlags.data(),
                        65535,
                        200,
                        70_000L,
                        false
                );

        pair.server()
                .onSegment(
                        hole,
                        80_000L
                );

        return result(
                "wifi-w193-connection-reordering",
                ackBeforeHole == clientSeq
                        && pair.server()
                        .snapshot()
                        .receiveNext() == clientSeq + 400
                        && pair.server()
                        .receiveBufferedBytes() == 0,
                "TcpConnection must retain out-of-order sequence ranges and advance RCV.NXT when the missing hole arrives"
        );
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

    private static TcpStreamTestResult result(
            String id,
            boolean passed,
            String detail
    ) {
        return new TcpStreamTestResult(
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

package com.k1ngtle.vsia.signality.engineering.wifi.tcp;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.k1ngtle.vsia.signality.engineering.wifi.tcp.stream.TcpReceiveQueue;
import com.k1ngtle.vsia.signality.engineering.wifi.tcp.stream.TcpReceiveResult;
import com.k1ngtle.vsia.signality.engineering.wifi.tcp.stream.TcpPersistTimer;
import com.k1ngtle.vsia.signality.engineering.wifi.tcp.stream.TcpSackBlock;

public final class TcpConnection {
    private static final int DEFAULT_WINDOW =
            65_535;

    private final int localPort;
    private final int remotePort;
    private final int smssBytes;

    private final TcpRtoEstimator rto =
            new TcpRtoEstimator();

    private final TcpCongestionController congestion;

    private final Map<Long, Outstanding> outstanding =
            new LinkedHashMap<>();

    private TcpState state =
            TcpState.CLOSED;

    private long initialSendSequence;

    private long sendUnacknowledged;

    private long sendNext;

    private long receiveNext;

    private TcpReceiveQueue receiveQueue;

    private TcpReceiveResult lastReceiveResult;

    private final TcpPersistTimer persistTimer =
            new TcpPersistTimer();

    private int receiverWindow =
            DEFAULT_WINDOW;

    private int retransmissions;

    private String lastEvent =
            "CLOSED";

    public TcpConnection(
            int localPort,
            int remotePort,
            int smssBytes,
            long initialSendSequence
    ) {
        validatePort(
                localPort
        );

        validatePort(
                remotePort
        );

        if (smssBytes <= 0) {
            throw new IllegalArgumentException(
                    "smssBytes"
            );
        }

        this.localPort =
                localPort;

        this.remotePort =
                remotePort;

        this.smssBytes =
                smssBytes;

        this.initialSendSequence =
                TcpSequence.normalize(
                        initialSendSequence
                );

        this.sendUnacknowledged =
                this.initialSendSequence;

        this.sendNext =
                this.initialSendSequence;

        this.congestion =
                new TcpCongestionController(
                        smssBytes
                );

        this.receiveQueue =
                new TcpReceiveQueue(
                        0L,
                        DEFAULT_WINDOW
                );
    }

    public TcpState state() {
        return state;
    }

    public int receiveBufferedBytes() {
        return receiveQueue.bufferedBytes();
    }

    public int advertisedReceiveWindow() {
        return receiveQueue.advertisedWindowBytes();
    }

    public java.util.List<TcpSackBlock> sackBlocks() {
        return receiveQueue.sackBlocks(
                4
        );
    }

    public boolean persistTimerArmed() {
        return persistTimer.armed();
    }

    public long persistIntervalMicros() {
        return persistTimer.intervalMicros();
    }

    public TcpConnectionAction listen() {
        if (state != TcpState.CLOSED) {
            return TcpConnectionAction.none(
                    "LISTEN rejected from "
                            + state
            );
        }

        state =
                TcpState.LISTEN;

        lastEvent =
                "LISTEN";

        return TcpConnectionAction.none(
                lastEvent
        );
    }

    public TcpConnectionAction activeOpen(
            long nowMicros
    ) {
        if (state != TcpState.CLOSED) {
            return TcpConnectionAction.none(
                    "Active open rejected from "
                            + state
            );
        }

        TcpSegment syn =
                segment(
                        sendNext,
                        0L,
                        TcpFlags.synOnly(),
                        0,
                        nowMicros,
                        false
                );

        trackOutstanding(
                syn,
                nowMicros
        );

        sendNext =
                syn.endSequenceExclusive();

        state =
                TcpState.SYN_SENT;

        lastEvent =
                "SYN sent";

        return new TcpConnectionAction(
                List.of(
                        syn
                ),
                false,
                false,
                lastEvent
        );
    }

    public TcpConnectionAction sendData(
            int requestedBytes,
            long nowMicros
    ) {
        if (state != TcpState.ESTABLISHED
                || requestedBytes <= 0) {
            return TcpConnectionAction.none(
                    "DATA rejected from "
                            + state
            );
        }

        long usable =
                congestion.usableWindowBytes(
                        receiverWindow,
                        bytesInFlight()
                );

        int remaining =
                (
                        int
                ) Math.min(
                        requestedBytes,
                        Math.min(
                                Integer.MAX_VALUE,
                                usable
                        )
                );

        if (remaining <= 0) {
            return TcpConnectionAction.none(
                    "Congestion/receive window closed"
            );
        }

        List<TcpSegment> outbound =
                new ArrayList<>();

        while (remaining > 0) {
            int bytes =
                    Math.min(
                            smssBytes,
                            remaining
                    );

            TcpSegment data =
                    segment(
                            sendNext,
                            receiveNext,
                            TcpFlags.data(),
                            bytes,
                            nowMicros,
                            false
                    );

            trackOutstanding(
                    data,
                    nowMicros
            );

            outbound.add(
                    data
            );

            sendNext =
                    data.endSequenceExclusive();

            remaining -=
                    bytes;
        }

        lastEvent =
                "DATA sent "
                        + outbound.size()
                        + " segment(s)";

        return new TcpConnectionAction(
                outbound,
                false,
                false,
                lastEvent
        );
    }

    public TcpConnectionAction close(
            long nowMicros
    ) {
        if (state == TcpState.ESTABLISHED) {
            TcpSegment fin =
                    segment(
                            sendNext,
                            receiveNext,
                            TcpFlags.finAck(),
                            0,
                            nowMicros,
                            false
                    );

            trackOutstanding(
                    fin,
                    nowMicros
            );

            sendNext =
                    fin.endSequenceExclusive();

            state =
                    TcpState.FIN_WAIT_1;

            lastEvent =
                    "FIN sent";

            return new TcpConnectionAction(
                    List.of(
                            fin
                    ),
                    false,
                    false,
                    lastEvent
            );
        }

        if (state == TcpState.CLOSE_WAIT) {
            TcpSegment fin =
                    segment(
                            sendNext,
                            receiveNext,
                            TcpFlags.finAck(),
                            0,
                            nowMicros,
                            false
                    );

            trackOutstanding(
                    fin,
                    nowMicros
            );

            sendNext =
                    fin.endSequenceExclusive();

            state =
                    TcpState.LAST_ACK;

            lastEvent =
                    "LAST_ACK FIN sent";

            return new TcpConnectionAction(
                    List.of(
                            fin
                    ),
                    false,
                    false,
                    lastEvent
            );
        }

        return TcpConnectionAction.none(
                "Close ignored from "
                        + state
        );
    }

    public TcpConnectionAction onSegment(
            TcpSegment incoming,
            long nowMicros
    ) {
        if (incoming == null) {
            return TcpConnectionAction.none(
                    "Null segment"
            );
        }

        receiverWindow =
                incoming.window();

        persistTimer.observeWindow(
                receiverWindow,
                nowMicros
        );

        if (incoming.flags()
                .rst()) {
            state =
                    TcpState.CLOSED;

            outstanding.clear();

            lastEvent =
                    "RST received";

            return new TcpConnectionAction(
                    List.of(),
                    false,
                    true,
                    lastEvent
            );
        }

        return switch (state) {
            case CLOSED ->
                    TcpConnectionAction.none(
                            "Segment ignored while CLOSED"
                    );

            case LISTEN ->
                    onListenSegment(
                            incoming,
                            nowMicros
                    );

            case SYN_SENT ->
                    onSynSentSegment(
                            incoming,
                            nowMicros
                    );

            case SYN_RECEIVED ->
                    onSynReceivedSegment(
                            incoming,
                            nowMicros
                    );

            case ESTABLISHED ->
                    onEstablishedSegment(
                            incoming,
                            nowMicros
                    );

            case FIN_WAIT_1 ->
                    onFinWait1Segment(
                            incoming,
                            nowMicros
                    );

            case FIN_WAIT_2 ->
                    onFinWait2Segment(
                            incoming,
                            nowMicros
                    );

            case CLOSE_WAIT ->
                    acknowledgeDataOrDuplicate(
                            incoming,
                            nowMicros
                    );

            case CLOSING ->
                    onClosingSegment(
                            incoming,
                            nowMicros
                    );

            case LAST_ACK ->
                    onLastAckSegment(
                            incoming,
                            nowMicros
                    );

            case TIME_WAIT ->
                    onTimeWaitSegment(
                            incoming,
                            nowMicros
                    );
        };
    }

    public TcpConnectionAction onTimer(
            long nowMicros
    ) {
        Outstanding earliest =
                earliestOutstanding();

        if (earliest == null) {
            return TcpConnectionAction.none(
                    "No retransmission timer"
            );
        }

        if (nowMicros
                - earliest.lastSentMicros()
                < rto.rtoMicros()) {
            return TcpConnectionAction.none(
                    "RTO not expired"
            );
        }

        rto.backoff();

        congestion.onRetransmissionTimeout(
                bytesInFlight()
        );

        retransmissions++;

        TcpSegment retransmitted =
                new TcpSegment(
                        earliest.segment()
                                .sourcePort(),
                        earliest.segment()
                                .destinationPort(),
                        earliest.segment()
                                .sequenceNumber(),
                        earliest.segment()
                                .acknowledgementNumber(),
                        earliest.segment()
                                .flags(),
                        earliest.segment()
                                .window(),
                        earliest.segment()
                                .payloadBytes(),
                        nowMicros,
                        true
                );

        outstanding.put(
                retransmitted.sequenceNumber(),
                new Outstanding(
                        retransmitted,
                        earliest.firstSentMicros(),
                        nowMicros,
                        true
                )
        );

        lastEvent =
                "RTO retransmit seq="
                        + retransmitted.sequenceNumber();

        return new TcpConnectionAction(
                List.of(
                        retransmitted
                ),
                true,
                false,
                lastEvent
        );
    }

    public TcpConnectionSnapshot snapshot() {
        return new TcpConnectionSnapshot(
                state,
                sendUnacknowledged,
                sendNext,
                receiveNext,
                receiverWindow,
                congestion.cwndBytes(),
                congestion.ssthreshBytes(),
                bytesInFlight(),
                toMillis(
                        rto.srttSeconds()
                ),
                toMillis(
                        rto.rttvarSeconds()
                ),
                toMillis(
                        rto.rtoSeconds()
                ),
                congestion.duplicateAckCount(),
                congestion.fastRecovery(),
                retransmissions,
                lastEvent
        );
    }

    private TcpConnectionAction onListenSegment(
            TcpSegment incoming,
            long nowMicros
    ) {
        if (!incoming.flags()
                .syn()) {
            return TcpConnectionAction.none(
                    "LISTEN ignored non-SYN"
            );
        }

        receiveNext =
                TcpSequence.add(
                        incoming.sequenceNumber(),
                        1
                );

        receiveQueue.resetReceiveNext(
                receiveNext
        );

        TcpSegment synAck =
                segment(
                        sendNext,
                        receiveNext,
                        TcpFlags.synAck(),
                        0,
                        nowMicros,
                        false
                );

        trackOutstanding(
                synAck,
                nowMicros
        );

        sendNext =
                synAck.endSequenceExclusive();

        state =
                TcpState.SYN_RECEIVED;

        lastEvent =
                "SYN received; SYN-ACK sent";

        return new TcpConnectionAction(
                List.of(
                        synAck
                ),
                false,
                false,
                lastEvent
        );
    }

    private TcpConnectionAction onSynSentSegment(
            TcpSegment incoming,
            long nowMicros
    ) {
        if (incoming.flags()
                .syn()
                && incoming.flags()
                .ack()
                && incoming.acknowledgementNumber()
                == sendNext) {
            acknowledgeUpTo(
                    incoming.acknowledgementNumber(),
                    nowMicros
            );

            receiveNext =
                    TcpSequence.add(
                            incoming.sequenceNumber(),
                            1
                    );

            receiveQueue.resetReceiveNext(
                    receiveNext
            );


            TcpSegment ack =
                    segment(
                            sendNext,
                            receiveNext,
                            TcpFlags.ackOnly(),
                            0,
                            nowMicros,
                            false
                    );

            state =
                    TcpState.ESTABLISHED;

            lastEvent =
                    "SYN-ACK received; ESTABLISHED";

            return new TcpConnectionAction(
                    List.of(
                            ack
                    ),
                    false,
                    false,
                    lastEvent
            );
        }

        if (incoming.flags()
                .syn()
                && !incoming.flags()
                .ack()) {
            receiveNext =
                    TcpSequence.add(
                            incoming.sequenceNumber(),
                            1
                    );

            TcpSegment synAck =
                    segment(
                            sendUnacknowledged,
                            receiveNext,
                            TcpFlags.synAck(),
                            0,
                            nowMicros,
                            false
                    );

            state =
                    TcpState.SYN_RECEIVED;

            lastEvent =
                    "Simultaneous open";

            return new TcpConnectionAction(
                    List.of(
                            synAck
                    ),
                    false,
                    false,
                    lastEvent
            );
        }

        return TcpConnectionAction.none(
                "SYN_SENT ignored segment"
        );
    }

    private TcpConnectionAction onSynReceivedSegment(
            TcpSegment incoming,
            long nowMicros
    ) {
        if (incoming.flags()
                .ack()
                && incoming.acknowledgementNumber()
                == sendNext) {
            acknowledgeUpTo(
                    incoming.acknowledgementNumber(),
                    nowMicros
            );

            state =
                    TcpState.ESTABLISHED;

            lastEvent =
                    "ACK received; ESTABLISHED";

            return TcpConnectionAction.none(
                    lastEvent
            );
        }

        return TcpConnectionAction.none(
                "SYN_RECEIVED awaiting ACK"
        );
    }

    private TcpConnectionAction onEstablishedSegment(
            TcpSegment incoming,
            long nowMicros
    ) {
        List<TcpSegment> outbound =
                new ArrayList<>();

        boolean fastRetransmit =
                processAcknowledgement(
                        incoming,
                        nowMicros
                );

        if (fastRetransmit) {
            Outstanding earliest =
                    earliestOutstanding();

            if (earliest != null) {
                TcpSegment retransmitted =
                        cloneRetransmission(
                                earliest.segment(),
                                nowMicros
                        );

                outstanding.put(
                        retransmitted.sequenceNumber(),
                        new Outstanding(
                                retransmitted,
                                earliest.firstSentMicros(),
                                nowMicros,
                                true
                        )
                );

                retransmissions++;

                outbound.add(
                        retransmitted
                );
            }
        }

        if (incoming.payloadBytes() > 0) {
            lastReceiveResult =
                    receiveQueue.accept(
                            incoming.sequenceNumber(),
                            incoming.payloadBytes()
                    );

            receiveNext =
                    lastReceiveResult.cumulativeAck();

            outbound.add(
                    pureAck(
                            nowMicros
                    )
            );

            if (lastReceiveResult.outOfOrder()) {
                lastEvent =
                        "Out-of-order segment buffered; cumulative ACK "
                                + receiveNext;
            } else if (lastReceiveResult.duplicateOnly()) {
                lastEvent =
                        "Duplicate segment suppressed; ACK "
                                + receiveNext;
            }
        }

        if (incoming.flags()
                .fin()) {
            long finSequence =
                    TcpSequence.add(
                            incoming.sequenceNumber(),
                            incoming.payloadBytes()
                    );

            if (finSequence
                    == receiveNext) {
                receiveNext =
                        TcpSequence.add(
                                receiveNext,
                                1
                        );
            }

            outbound.add(
                    pureAck(
                            nowMicros
                    )
            );

            state =
                    TcpState.CLOSE_WAIT;

            lastEvent =
                    "FIN received; CLOSE_WAIT";

            return new TcpConnectionAction(
                    outbound,
                    fastRetransmit,
                    false,
                    lastEvent
            );
        }

        if (!outbound.isEmpty()) {
            lastEvent =
                    fastRetransmit
                            ? "Fast retransmit"
                            : "Segment processed";
        }

        return new TcpConnectionAction(
                outbound,
                fastRetransmit,
                false,
                lastEvent
        );
    }

    private TcpConnectionAction onFinWait1Segment(
            TcpSegment incoming,
            long nowMicros
    ) {
        boolean finAcked =
                incoming.flags()
                        .ack()
                        && TcpSequence.beforeOrEqual(
                        sendNext,
                        incoming.acknowledgementNumber()
                );

        if (finAcked) {
            acknowledgeUpTo(
                    incoming.acknowledgementNumber(),
                    nowMicros
            );
        }

        if (incoming.flags()
                .fin()) {
            receiveNext =
                    TcpSequence.add(
                            incoming.sequenceNumber(),
                            incoming.payloadBytes()
                                    + 1L
                    );

            TcpSegment ack =
                    pureAck(
                            nowMicros
                    );

            state =
                    finAcked
                            ? TcpState.TIME_WAIT
                            : TcpState.CLOSING;

            lastEvent =
                    "Peer FIN in FIN_WAIT_1";

            return new TcpConnectionAction(
                    List.of(
                            ack
                    ),
                    false,
                    false,
                    lastEvent
            );
        }

        if (finAcked) {
            state =
                    TcpState.FIN_WAIT_2;

            lastEvent =
                    "Our FIN acknowledged";
        }

        return TcpConnectionAction.none(
                lastEvent
        );
    }

    private TcpConnectionAction onFinWait2Segment(
            TcpSegment incoming,
            long nowMicros
    ) {
        if (incoming.flags()
                .fin()) {
            receiveNext =
                    TcpSequence.add(
                            incoming.sequenceNumber(),
                            incoming.payloadBytes()
                                    + 1L
                    );

            state =
                    TcpState.TIME_WAIT;

            lastEvent =
                    "Peer FIN received; TIME_WAIT";

            return new TcpConnectionAction(
                    List.of(
                            pureAck(
                                    nowMicros
                            )
                    ),
                    false,
                    false,
                    lastEvent
            );
        }

        return acknowledgeDataOrDuplicate(
                incoming,
                nowMicros
        );
    }

    private TcpConnectionAction onClosingSegment(
            TcpSegment incoming,
            long nowMicros
    ) {
        if (incoming.flags()
                .ack()
                && TcpSequence.beforeOrEqual(
                sendNext,
                incoming.acknowledgementNumber()
        )) {
            acknowledgeUpTo(
                    incoming.acknowledgementNumber(),
                    nowMicros
            );

            state =
                    TcpState.TIME_WAIT;

            lastEvent =
                    "Closing ACK received; TIME_WAIT";
        }

        return TcpConnectionAction.none(
                lastEvent
        );
    }

    private TcpConnectionAction onLastAckSegment(
            TcpSegment incoming,
            long nowMicros
    ) {
        if (incoming.flags()
                .ack()
                && TcpSequence.beforeOrEqual(
                sendNext,
                incoming.acknowledgementNumber()
        )) {
            acknowledgeUpTo(
                    incoming.acknowledgementNumber(),
                    nowMicros
            );

            state =
                    TcpState.CLOSED;

            lastEvent =
                    "Final ACK received; CLOSED";
        }

        return TcpConnectionAction.none(
                lastEvent
        );
    }

    private TcpConnectionAction onTimeWaitSegment(
            TcpSegment incoming,
            long nowMicros
    ) {
        if (incoming.flags()
                .fin()) {
            lastEvent =
                    "Duplicate FIN in TIME_WAIT";

            return new TcpConnectionAction(
                    List.of(
                            pureAck(
                                    nowMicros
                            )
                    ),
                    false,
                    false,
                    lastEvent
            );
        }

        return TcpConnectionAction.none(
                "TIME_WAIT"
        );
    }

    private TcpConnectionAction acknowledgeDataOrDuplicate(
            TcpSegment incoming,
            long nowMicros
    ) {
        boolean retransmit =
                processAcknowledgement(
                        incoming,
                        nowMicros
                );

        if (retransmit) {
            Outstanding earliest =
                    earliestOutstanding();

            if (earliest != null) {
                TcpSegment segment =
                        cloneRetransmission(
                                earliest.segment(),
                                nowMicros
                        );

                retransmissions++;

                return new TcpConnectionAction(
                        List.of(
                                segment
                        ),
                        true,
                        false,
                        "Fast retransmit"
                );
            }
        }

        return TcpConnectionAction.none(
                "ACK processed"
        );
    }

    private boolean processAcknowledgement(
            TcpSegment incoming,
            long nowMicros
    ) {
        if (!incoming.flags()
                .ack()) {
            return false;
        }

        long acknowledgement =
                incoming.acknowledgementNumber();

        if (acknowledgement
                == sendUnacknowledged) {
            return congestion.onDuplicateAcknowledgement(
                    sendNext
            );
        }

        if (TcpSequence.after(
                acknowledgement,
                sendUnacknowledged
        )
                && TcpSequence.beforeOrEqual(
                acknowledgement,
                sendNext
        )) {
            long acknowledgedBytes =
                    TcpSequence.distance(
                            sendUnacknowledged,
                            acknowledgement
                    );

            acknowledgeUpTo(
                    acknowledgement,
                    nowMicros
            );

            congestion.onNewAcknowledgement(
                    (
                            int
                    ) Math.min(
                            Integer.MAX_VALUE,
                            acknowledgedBytes
                    ),
                    acknowledgement
            );
        }

        return false;
    }

    private void acknowledgeUpTo(
            long acknowledgement,
            long nowMicros
    ) {
        sendUnacknowledged =
                acknowledgement;

        Iterator<Map.Entry<Long, Outstanding>> iterator =
                outstanding.entrySet()
                        .iterator();

        while (iterator.hasNext()) {
            Outstanding item =
                    iterator.next()
                            .getValue();

            if (TcpSequence.beforeOrEqual(
                    item.segment()
                            .endSequenceExclusive(),
                    acknowledgement
            )) {
                if (!item.retransmitted()) {
                    long sampleMicros =
                            Math.max(
                                    1L,
                                    nowMicros
                                            - item.firstSentMicros()
                            );

                    rto.observeRttSeconds(
                            sampleMicros
                                    / 1_000_000.0
                    );
                }

                iterator.remove();
            }
        }
    }

    private void trackOutstanding(
            TcpSegment segment,
            long nowMicros
    ) {
        if (segment.sequenceSpaceLength()
                <= 0L) {
            return;
        }

        outstanding.put(
                segment.sequenceNumber(),
                new Outstanding(
                        segment,
                        nowMicros,
                        nowMicros,
                        false
                )
        );
    }

    private Outstanding earliestOutstanding() {
        return outstanding.values()
                .stream()
                .findFirst()
                .orElse(
                        null
                );
    }

    private long bytesInFlight() {
        return TcpSequence.distance(
                sendUnacknowledged,
                sendNext
        );
    }

    private TcpSegment pureAck(
            long nowMicros
    ) {
        return segment(
                sendNext,
                receiveNext,
                TcpFlags.ackOnly(),
                0,
                nowMicros,
                false
        );
    }

    private TcpSegment segment(
            long sequence,
            long acknowledgement,
            TcpFlags flags,
            int payloadBytes,
            long nowMicros,
            boolean retransmission
    ) {
        return new TcpSegment(
                localPort,
                remotePort,
                sequence,
                acknowledgement,
                flags,
                Math.min(
                        65_535,
                        Math.max(
                                0,
                                receiveQueue.advertisedWindowBytes()
                        )
                ),
                payloadBytes,
                nowMicros,
                retransmission
        );
    }

    private TcpSegment cloneRetransmission(
            TcpSegment original,
            long nowMicros
    ) {
        return new TcpSegment(
                original.sourcePort(),
                original.destinationPort(),
                original.sequenceNumber(),
                original.acknowledgementNumber(),
                original.flags(),
                original.window(),
                original.payloadBytes(),
                nowMicros,
                true
        );
    }

    private static double toMillis(
            double seconds
    ) {
        return Double.isFinite(
                seconds
        )
                ? seconds
                * 1000.0
                : Double.NaN;
    }

    private static void validatePort(
            int value
    ) {
        if (value < 0
                || value > 65535) {
            throw new IllegalArgumentException(
                    "TCP port out of range"
            );
        }
    }

    private record Outstanding(
            TcpSegment segment,
            long firstSentMicros,
            long lastSentMicros,
            boolean retransmitted
    ) {
    }
}

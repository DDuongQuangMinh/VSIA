package com.k1ngtle.vsia.signality.engineering.wifi.tcp.live;

import com.k1ngtle.vsia.signality.engineering.wifi.tcp.TcpConnection;
import com.k1ngtle.vsia.signality.engineering.wifi.tcp.TcpConnectionAction;
import com.k1ngtle.vsia.signality.engineering.wifi.tcp.TcpConnectionSnapshot;
import com.k1ngtle.vsia.signality.engineering.wifi.tcp.TcpSegment;
import com.k1ngtle.vsia.signality.engineering.wifi.tcp.TcpState;
import com.k1ngtle.vsia.signality.internet.OSINetworkPacket;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

public final class TcpLiveController {
    public static final int DEFAULT_SMSS =
            1200;

    private final Map<String, Session> sessions =
            new LinkedHashMap<>();

    private String activeSessionId =
            "";

    private String status =
            "IDLE";

    public boolean startApplication(
            OSINetworkPacket application,
            long nowMicros,
            Consumer<OSINetworkPacket> transmitter
    ) {
        if (application == null
                || application.targetMac.isBlank()
                || application.targetIp.isBlank()) {
            status =
                    "TCP start rejected: invalid application target";

            return false;
        }

        String sessionId =
                "tcp-"
                        + UUID.randomUUID();

        int localPort =
                application.sourcePort > 0
                        ? application.sourcePort
                        : 49_152
                        + Math.floorMod(
                        sessionId.hashCode(),
                        10_000
                );

        int remotePort =
                application.targetPort > 0
                        ? application.targetPort
                        : 80;

        TcpConnection connection =
                new TcpConnection(
                        localPort,
                        remotePort,
                        DEFAULT_SMSS,
                        initialSequence(
                                sessionId,
                                application.sourceMac
                        )
                );

        application.sessionId =
                sessionId;

        Session session =
                new Session(
                        sessionId,
                        application.sourceMac,
                        application.targetMac,
                        application.sourceIp,
                        application.targetIp,
                        localPort,
                        remotePort,
                        false,
                        connection
                );

        session.pendingApplication =
                TcpLiveApplicationCodec.encode(
                        application
                );

        sessions.put(
                sessionId,
                session
        );

        activeSessionId =
                sessionId;

        TcpConnectionAction action =
                connection.activeOpen(
                        nowMicros
                );

        sendAction(
                session,
                action,
                nowMicros,
                transmitter
        );

        status =
                "TCP SYN queued to "
                        + application.targetIp
                        + ":"
                        + remotePort;

        return true;
    }

    public boolean interceptApplicationResponse(
            OSINetworkPacket application,
            long nowMicros,
            Consumer<OSINetworkPacket> transmitter
    ) {
        if (application == null
                || !application.isResponse
                || application.sessionId == null
                || application.sessionId.isBlank()) {
            return false;
        }

        Session session =
                sessions.get(
                        application.sessionId
                );

        if (session == null
                || !session.serverSide
                || session.connection.state()
                != TcpState.ESTABLISHED) {
            return false;
        }

        byte[] bytes =
                TcpLiveApplicationCodec.encode(
                        application
                );

        boolean queued =
                sendApplicationBytes(
                        session,
                        bytes,
                        0,
                        bytes.length,
                        nowMicros,
                        transmitter
                );

        if (queued) {
            session.closeAfterApplicationAck =
                    true;

            status =
                    "TCP application response queued";
        }

        return queued;
    }

    public boolean handleIncoming(
            String localMac,
            String localIp,
            OSINetworkPacket packet,
            long nowMicros,
            Consumer<OSINetworkPacket> transmitter,
            Consumer<OSINetworkPacket> applicationSink
    ) {
        if (packet == null
                || !"TCP".equalsIgnoreCase(
                packet.applicationProtocol
        )) {
            return false;
        }

        if (!TcpLivePacketCodec.checksumValid(
                packet
        )) {
            status =
                    "TCP checksum drop";

            return true;
        }

        TcpSegment incoming =
                TcpLivePacketCodec.decode(
                        packet
                );

        Session session =
                sessions.get(
                        packet.sessionId
                );

        if (session == null) {
            if (!incoming.flags()
                    .syn()) {
                status =
                        "TCP segment dropped: unknown session";

                return true;
            }

            TcpConnection connection =
                    new TcpConnection(
                            packet.targetPort,
                            packet.sourcePort,
                            DEFAULT_SMSS,
                            initialSequence(
                                    packet.sessionId,
                                    localMac
                            )
                    );

            connection.listen();

            session =
                    new Session(
                            packet.sessionId,
                            localMac,
                            packet.sourceMac,
                            localIp,
                            packet.sourceIp,
                            packet.targetPort,
                            packet.sourcePort,
                            true,
                            connection
                    );

            sessions.put(
                    packet.sessionId,
                    session
            );

            activeSessionId =
                    packet.sessionId;
        }

        TcpConnectionAction action =
                session.connection.onSegment(
                        incoming,
                        nowMicros
                );

        sendAction(
                session,
                action,
                nowMicros,
                transmitter
        );

        if (incoming.payloadBytes() > 0) {
            acceptApplicationChunk(
                    session,
                    packet,
                    applicationSink
            );
        }

        if (!session.serverSide
                && session.connection.state()
                == TcpState.ESTABLISHED
                && session.pendingApplication != null) {
            byte[] pending =
                    session.pendingApplication;

            session.pendingApplication =
                    null;

            sendApplicationBytes(
                    session,
                    pending,
                    0,
                    pending.length,
                    nowMicros,
                    transmitter
            );
        }

        TcpConnectionSnapshot snapshot =
                session.connection.snapshot();

        if (session.closeAfterApplicationAck
                && session.connection.state()
                == TcpState.ESTABLISHED
                && snapshot.bytesInFlight() == 0L) {
            session.closeAfterApplicationAck =
                    false;

            sendAction(
                    session,
                    session.connection.close(
                            nowMicros
                    ),
                    nowMicros,
                    transmitter
            );
        }

        if (session.connection.state()
                == TcpState.CLOSE_WAIT) {
            sendAction(
                    session,
                    session.connection.close(
                            nowMicros
                    ),
                    nowMicros,
                    transmitter
            );
        }

        updateStatus(
                session
        );

        return true;
    }

    public void tick(
            long nowMicros,
            Consumer<OSINetworkPacket> transmitter
    ) {
        List<Session> copy =
                new ArrayList<>(
                        sessions.values()
                );

        for (Session session : copy) {
            TcpConnectionAction action =
                    session.connection.onTimer(
                            nowMicros
                    );

            if (!action.outbound()
                    .isEmpty()) {
                sendAction(
                        session,
                        action,
                        nowMicros,
                        transmitter
                );

                updateStatus(
                        session
                );
            }
        }

        sessions.entrySet()
                .removeIf(
                        entry ->
                                entry.getValue()
                                        .connection
                                        .state()
                                        == TcpState.CLOSED
                                        && !entry.getKey()
                                        .equals(
                                                activeSessionId
                                        )
                );
    }

    public boolean closeActive(
            long nowMicros,
            Consumer<OSINetworkPacket> transmitter
    ) {
        Session session =
                sessions.get(
                        activeSessionId
                );

        if (session == null) {
            status =
                    "No active TCP session";

            return false;
        }

        TcpConnectionAction action =
                session.connection.close(
                        nowMicros
                );

        if (action.outbound()
                .isEmpty()) {
            status =
                    action.event();

            return false;
        }

        sendAction(
                session,
                action,
                nowMicros,
                transmitter
        );

        updateStatus(
                session
        );

        return true;
    }

    public TcpLiveSnapshot snapshot() {
        Session session =
                sessions.get(
                        activeSessionId
                );

        if (session == null) {
            TcpLiveSnapshot idle =
                    TcpLiveSnapshot.idle();

            return new TcpLiveSnapshot(
                    idle.sessionId(),
                    idle.state(),
                    idle.peerIp(),
                    idle.peerMac(),
                    idle.localPort(),
                    idle.remotePort(),
                    idle.congestionWindowBytes(),
                    idle.slowStartThresholdBytes(),
                    idle.bytesInFlight(),
                    idle.srttMs(),
                    idle.rtoMs(),
                    idle.retransmissions(),
                    status
            );
        }

        TcpConnectionSnapshot tcp =
                session.connection.snapshot();

        return new TcpLiveSnapshot(
                session.sessionId,
                tcp.state()
                        .name(),
                session.remoteIp,
                session.remoteMac,
                session.localPort,
                session.remotePort,
                tcp.congestionWindowBytes(),
                tcp.slowStartThresholdBytes(),
                tcp.bytesInFlight(),
                tcp.srttMs(),
                tcp.rtoMs(),
                tcp.retransmissions(),
                status
        );
    }

    public void clear() {
        sessions.clear();
        activeSessionId =
                "";
        status =
                "CLEARED";
    }

    private boolean sendApplicationBytes(
            Session session,
            byte[] applicationBytes,
            int baseOffset,
            int applicationTotalBytes,
            long nowMicros,
            Consumer<OSINetworkPacket> transmitter
    ) {
        if (applicationBytes == null
                || applicationBytes.length == 0) {
            return false;
        }

        int offset =
                0;

        while (offset < applicationBytes.length) {
            int request =
                    applicationBytes.length
                            - offset;

            TcpConnectionAction action =
                    session.connection.sendData(
                            request,
                            nowMicros
                    );

            if (action.outbound()
                    .isEmpty()) {
                break;
            }

            for (TcpSegment segment
                    : action.outbound()) {
                int length =
                        Math.min(
                                segment.payloadBytes(),
                                applicationBytes.length
                                        - offset
                        );

                byte[] chunk =
                        new byte[
                                length
                        ];

                System.arraycopy(
                        applicationBytes,
                        offset,
                        chunk,
                        0,
                        length
                );

                transmitSegment(
                        session,
                        segment,
                        chunk,
                        baseOffset + offset,
                        applicationTotalBytes,
                        transmitter
                );

                offset +=
                        length;
            }
        }

        if (offset < applicationBytes.length) {
            session.pendingRemainder =
                    new byte[
                            applicationBytes.length
                                    - offset
                    ];

            System.arraycopy(
                    applicationBytes,
                    offset,
                    session.pendingRemainder,
                    0,
                    session.pendingRemainder.length
            );

            session.pendingRemainderOffset =
                    baseOffset
                            + offset;

            session.pendingRemainderTotal =
                    applicationTotalBytes;
        }

        return offset > 0;
    }

    private void sendAction(
            Session session,
            TcpConnectionAction action,
            long nowMicros,
            Consumer<OSINetworkPacket> transmitter
    ) {
        for (TcpSegment segment
                : action.outbound()) {
            byte[] chunk =
                    session.outboundChunkBySequence.get(
                            segment.sequenceNumber()
                    );

            int offset =
                    session.outboundOffsetBySequence.getOrDefault(
                            segment.sequenceNumber(),
                            0
                    );

            int total =
                    session.outboundTotalBySequence.getOrDefault(
                            segment.sequenceNumber(),
                            chunk == null
                                    ? 0
                                    : chunk.length
                    );

            transmitSegment(
                    session,
                    segment,
                    chunk,
                    offset,
                    total,
                    transmitter
            );
        }

        if (session.pendingRemainder != null
                && session.connection.state()
                == TcpState.ESTABLISHED
                && session.connection.snapshot()
                .bytesInFlight()
                == 0L) {
            byte[] remainder =
                    session.pendingRemainder;

            int remainderOffset =
                    session.pendingRemainderOffset;

            int remainderTotal =
                    session.pendingRemainderTotal;

            session.pendingRemainder =
                    null;

            session.pendingRemainderOffset =
                    0;

            session.pendingRemainderTotal =
                    0;

            sendApplicationBytes(
                    session,
                    remainder,
                    remainderOffset,
                    remainderTotal,
                    nowMicros,
                    transmitter
            );
        }
    }

    private void transmitSegment(
            Session session,
            TcpSegment segment,
            byte[] chunk,
            int offset,
            int total,
            Consumer<OSINetworkPacket> transmitter
    ) {
        byte[] safeChunk =
                chunk == null
                        ? new byte[0]
                        : chunk.clone();

        if (safeChunk.length > 0) {
            session.outboundChunkBySequence.put(
                    segment.sequenceNumber(),
                    safeChunk
            );

            session.outboundOffsetBySequence.put(
                    segment.sequenceNumber(),
                    offset
            );

            session.outboundTotalBySequence.put(
                    segment.sequenceNumber(),
                    total
            );
        }

        transmitter.accept(
                TcpLivePacketCodec.encode(
                        session.sessionId,
                        session.localMac,
                        session.remoteMac,
                        session.localIp,
                        session.remoteIp,
                        segment,
                        safeChunk,
                        offset,
                        total
                )
        );
    }

    private void acceptApplicationChunk(
            Session session,
            OSINetworkPacket packet,
            Consumer<OSINetworkPacket> applicationSink
    ) {
        byte[] chunk =
                packet.payload.getByteArray(
                        "tcp_app_chunk"
                );

        int offset =
                packet.payload.getInt(
                        "tcp_app_offset"
                );

        int total =
                packet.payload.getInt(
                        "tcp_app_total"
                );

        if (chunk.length == 0
                || total <= 0
                || offset < 0
                || offset + chunk.length > total) {
            return;
        }

        if (session.incomingTotal != total) {
            session.incomingTotal =
                    total;

            session.incomingChunks.clear();
        }

        session.incomingChunks.put(
                offset,
                chunk
        );

        int received =
                session.incomingChunks.values()
                        .stream()
                        .mapToInt(
                                value ->
                                        value.length
                        )
                        .sum();

        if (received < total) {
            return;
        }

        ByteArrayOutputStream bytes =
                new ByteArrayOutputStream(
                        total
                );

        int expectedOffset =
                0;

        for (Map.Entry<Integer, byte[]> entry
                : session.incomingChunks.entrySet()
                .stream()
                .sorted(
                        Comparator.comparingInt(
                                Map.Entry::getKey
                        )
                )
                .toList()) {
            if (entry.getKey()
                    != expectedOffset) {
                return;
            }

            bytes.writeBytes(
                    entry.getValue()
            );

            expectedOffset +=
                    entry.getValue()
                            .length;
        }

        if (expectedOffset != total) {
            return;
        }

        OSINetworkPacket application =
                TcpLiveApplicationCodec.decode(
                        bytes.toByteArray()
                );

        application.sessionId =
                session.sessionId;

        session.incomingChunks.clear();
        session.incomingTotal =
                0;

        applicationSink.accept(
                application
        );

        status =
                "TCP delivered "
                        + application.applicationProtocol
                        + " application payload";
    }

    private void updateStatus(
            Session session
    ) {
        TcpConnectionSnapshot snapshot =
                session.connection.snapshot();

        status =
                snapshot.state()
                        + " | cwnd "
                        + snapshot.congestionWindowBytes()
                        + " B | in-flight "
                        + snapshot.bytesInFlight()
                        + " B | RTO "
                        + String.format(
                        java.util.Locale.ROOT,
                        "%.1f ms",
                        snapshot.rtoMs()
                );
    }

    private long initialSequence(
            String sessionId,
            String endpoint
    ) {
        long mixed =
                (
                        (
                                long
                        ) sessionId.hashCode()
                                << 32
                )
                        ^ endpoint.hashCode();

        return mixed
                & 0xFFFF_FFFFL;
    }

    private static final class Session {
        private final String sessionId;
        private final String localMac;
        private final String remoteMac;
        private final String localIp;
        private final String remoteIp;
        private final int localPort;
        private final int remotePort;
        private final boolean serverSide;
        private final TcpConnection connection;

        private byte[] pendingApplication;
        private byte[] pendingRemainder;
        private int pendingRemainderOffset;
        private int pendingRemainderTotal;
        private boolean closeAfterApplicationAck;

        private int incomingTotal;

        private final Map<Integer, byte[]> incomingChunks =
                new LinkedHashMap<>();

        private final Map<Long, byte[]> outboundChunkBySequence =
                new LinkedHashMap<>();

        private final Map<Long, Integer> outboundOffsetBySequence =
                new LinkedHashMap<>();

        private final Map<Long, Integer> outboundTotalBySequence =
                new LinkedHashMap<>();

        private Session(
                String sessionId,
                String localMac,
                String remoteMac,
                String localIp,
                String remoteIp,
                int localPort,
                int remotePort,
                boolean serverSide,
                TcpConnection connection
        ) {
            this.sessionId =
                    sessionId;

            this.localMac =
                    localMac;

            this.remoteMac =
                    remoteMac;

            this.localIp =
                    localIp;

            this.remoteIp =
                    remoteIp;

            this.localPort =
                    localPort;

            this.remotePort =
                    remotePort;

            this.serverSide =
                    serverSide;

            this.connection =
                    connection;
        }
    }
}

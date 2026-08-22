package com.k1ngtle.vsia.signality.engineering.wifi.ip;

import com.k1ngtle.vsia.signality.internet.OSINetworkPacket;
import net.minecraft.nbt.CompoundTag;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

public final class WifiIpApplicationEngine {
    public static final int UDP_ECHO_PORT =
            40001;

    public static final long FLOW_TIMEOUT_MICROS =
            5_000_000L;

    private final Map<String, WifiIpNeighbor> neighbors =
            new LinkedHashMap<>();

    private final WifiIpFlowTracker flows =
            new WifiIpFlowTracker();

    private int nextSequence =
            1;

    private String peerIp =
            "";

    private String peerMac =
            "";

    private String lastStatus =
            "IDLE";

    private long pendingHttpRequestId =
            -1L;

    public void configurePeer(
            String ip,
            String mac
    ) {
        peerIp =
                ip == null
                        ? ""
                        : ip;

        peerMac =
                mac == null
                        ? ""
                        : mac;

        if (Ipv4Address.isUsableUnicast(
                peerIp
        )
                && !peerMac.isBlank()) {
            neighbors.put(
                    peerIp,
                    new WifiIpNeighbor(
                            peerIp,
                            peerMac,
                            0L
                    )
            );
        }
    }

    public String peerIp() {
        return peerIp;
    }

    public String peerMac() {
        return peerMac;
    }

    public Map<String, WifiIpNeighbor> neighbors() {
        return Map.copyOf(
                neighbors
        );
    }

    public String neighborMac(
            String ip
    ) {
        WifiIpNeighbor neighbor =
                neighbors.get(
                        ip
                );

        return neighbor == null
                ? ""
                : neighbor.macAddress();
    }

    public WifiIpFlowSnapshot snapshot(
            String localIp,
            long nowMicros
    ) {
        flows.expire(
                nowMicros,
                FLOW_TIMEOUT_MICROS
        );

        WifiIpFlowSnapshot base =
                flows.snapshot(
                        localIp,
                        peerIp,
                        peerMac
                );

        return new WifiIpFlowSnapshot(
                base.localIp(),
                base.peerIp(),
                base.peerMac(),
                base.txPackets(),
                base.rxPackets(),
                base.txBytes(),
                base.rxBytes(),
                base.lostPackets(),
                base.lastRttMs(),
                base.averageRttMs(),
                base.jitterMs(),
                base.goodputKbps(),
                base.lastProtocol(),
                lastStatus.isBlank()
                        ? base.lastStatus()
                        : lastStatus
        );
    }

    public void clearMetrics() {
        flows.clear();
        lastStatus =
                "Metrics cleared";
    }

    public void setStatus(
            String status
    ) {
        lastStatus =
                status == null
                        ? ""
                        : status;
    }

    public OSINetworkPacket createArpRequest(
            String localMac,
            String localIp,
            String targetIp,
            long nowMicros
    ) {
        OSINetworkPacket packet =
                basePacket(
                        localMac,
                        "FF:FF:FF:FF:FF:FF",
                        validSourceIp(
                                localIp
                        ),
                        targetIp,
                        0,
                        0,
                        "ARP"
                );

        packet.ipProtocol =
                0;

        packet.payload.putString(
                "operation",
                "REQUEST"
        );
        packet.payload.putString(
                "arp_op",
                "REQUEST"
        );

        packet.payload.putString(
                "sender_ip",
                validSourceIp(
                        localIp
                )
        );

        packet.payload.putString(
                "sender_mac",
                localMac
        );

        packet.payload.putString(
                "target_ip",
                targetIp
        );

        packet.payload.putLong(
                "sent_us",
                nowMicros
        );

        long id =
                nextId();

        packet.payload.putLong(
                "w1_request_id",
                id
        );

        flows.recordTx(
                id,
                nowMicros,
                28,
                "ARP"
        );

        lastStatus =
                "ARP request queued for "
                        + targetIp;

        return packet;
    }

    public OSINetworkPacket createIcmpEcho(
            String localMac,
            String localIp,
            String targetMac,
            String targetIp,
            int payloadBytes,
            long nowMicros
    ) {
        int sequence =
                nextSequence++;

        int bytes =
                Math.max(
                        8,
                        Math.min(
                                1400,
                                payloadBytes
                        )
                );

        byte[] payload =
                new byte[
                        bytes
                ];

        for (int i = 0; i < payload.length; i++) {
            payload[i] =
                    (
                            byte
                    ) (
                    sequence
                            + i * 13
            );
        }

        IcmpEchoMessage icmp =
                new IcmpEchoMessage(
                        false,
                        0x5653,
                        sequence,
                        payload
                );

        byte[] wire =
                icmp.encode();

        OSINetworkPacket packet =
                basePacket(
                        localMac,
                        targetMac,
                        validSourceIp(
                                localIp
                        ),
                        targetIp,
                        0,
                        0,
                        "ICMP"
                );

        packet.ipProtocol =
                1;

        packet.transportChecksum =
                icmp.checksum();

        fillIpv4Metadata(
                packet,
                wire.length,
                sequence
        );

        long id =
                nextId();

        packet.payload.putString(
                "type",
                "ECHO_REQUEST"
        );

        packet.payload.putInt(
                "identifier",
                0x5653
        );

        packet.payload.putInt(
                "sequence",
                sequence
        );

        packet.payload.putByteArray(
                "data",
                payload
        );

        packet.payload.putLong(
                "sent_us",
                nowMicros
        );

        packet.payload.putLong(
                "w1_request_id",
                id
        );

        packet.payload.putInt(
                "icmp_checksum",
                icmp.checksum()
        );

        flows.recordTx(
                id,
                nowMicros,
                wire.length,
                "ICMP"
        );

        lastStatus =
                "ICMP echo request queued";

        return packet;
    }

    public OSINetworkPacket createUdpEcho(
            String localMac,
            String localIp,
            String targetMac,
            String targetIp,
            int payloadBytes,
            long nowMicros
    ) {
        int sequence =
                nextSequence++;

        int bytes =
                Math.max(
                        16,
                        Math.min(
                                1400,
                                payloadBytes
                        )
                );

        byte[] payload =
                new byte[
                        bytes
                ];

        for (int i = 0; i < payload.length; i++) {
            payload[i] =
                    (
                            byte
                    ) (
                    sequence * 7
                            + i * 19
            );
        }

        int sourcePort =
                49152
                        + (
                        sequence
                                % 10000
                );

        UdpDatagram udp =
                new UdpDatagram(
                        sourcePort,
                        UDP_ECHO_PORT,
                        payload
                );

        byte[] wire =
                udp.encode(
                        validSourceIp(
                                localIp
                        ),
                        targetIp
                );

        OSINetworkPacket packet =
                basePacket(
                        localMac,
                        targetMac,
                        validSourceIp(
                                localIp
                        ),
                        targetIp,
                        sourcePort,
                        UDP_ECHO_PORT,
                        "UDP"
                );

        packet.ipProtocol =
                17;

        packet.transportChecksum =
                udp.checksum(
                        validSourceIp(
                                localIp
                        ),
                        targetIp
                );

        fillIpv4Metadata(
                packet,
                wire.length,
                sequence
        );

        long id =
                nextId();

        packet.payload.putString(
                "service",
                "ECHO"
        );

        packet.payload.putString(
                "type",
                "REQUEST"
        );

        packet.payload.putInt(
                "sequence",
                sequence
        );

        packet.payload.putByteArray(
                "data",
                payload
        );

        packet.payload.putLong(
                "sent_us",
                nowMicros
        );

        packet.payload.putLong(
                "w1_request_id",
                id
        );

        packet.payload.putInt(
                "udp_checksum",
                packet.transportChecksum
        );

        flows.recordTx(
                id,
                nowMicros,
                wire.length,
                "UDP"
        );

        lastStatus =
                "UDP echo request queued";

        return packet;
    }

    public OSINetworkPacket createHttpGet(
            String localMac,
            String localIp,
            String targetMac,
            String targetIp,
            String path,
            long nowMicros
    ) {
        int sourcePort =
                50000
                        + (
                        nextSequence
                                % 10000
                );

        OSINetworkPacket packet =
                basePacket(
                        localMac,
                        targetMac,
                        validSourceIp(
                                localIp
                        ),
                        targetIp,
                        sourcePort,
                        80,
                        "HTTP"
                );

        packet.ipProtocol =
                6;

        byte[] requestBytes =
                (
                        "GET "
                                + (
                                path == null
                                        || path.isBlank()
                                ? "/"
                                : path
                        )
                                + " HTTP/1.1\\r\\nHost: "
                                + targetIp
                                + "\\r\\nConnection: close\\r\\n\\r\\n"
                ).getBytes(
                        java.nio.charset.StandardCharsets.US_ASCII
                );

        fillIpv4Metadata(
                packet,
                20
                        + requestBytes.length,
                nextSequence
        );

        long id =
                nextId();

        pendingHttpRequestId =
                id;

        packet.payload.putString(
                "method",
                "GET"
        );

        packet.payload.putString(
                "path",
                path == null
                        || path.isBlank()
                        ? "/"
                        : path
        );

        packet.payload.putLong(
                "sent_us",
                nowMicros
        );

        packet.payload.putLong(
                "w1_request_id",
                id
        );

        packet.payload.putByteArray(
                "request_wire",
                requestBytes
        );

        flows.recordTx(
                id,
                nowMicros,
                requestBytes.length,
                "HTTP"
        );

        lastStatus =
                "HTTP GET queued for "
                        + targetIp;

        return packet;
    }

    public boolean handleIncoming(
            String localMac,
            String localIp,
            OSINetworkPacket packet,
            long nowMicros,
            Consumer<OSINetworkPacket> transmitter
    ) {
        if (packet == null) {
            return false;
        }

        if ("ARP".equals(
                packet.applicationProtocol
        )) {
            return handleArp(
                    localMac,
                    localIp,
                    packet,
                    nowMicros,
                    transmitter
            );
        }

        if ("ICMP".equals(
                packet.applicationProtocol
        )) {
            return handleIcmp(
                    localMac,
                    localIp,
                    packet,
                    nowMicros,
                    transmitter
            );
        }

        if ("UDP".equals(
                packet.applicationProtocol
        )
                && packet.targetPort
                == UDP_ECHO_PORT) {
            return handleUdpEcho(
                    localMac,
                    localIp,
                    packet,
                    nowMicros,
                    transmitter
            );
        }

        if (packet.isResponse) {
            if (packet.payload.contains(
                    "w1_request_id"
            )) {
                flows.recordRx(
                        packet.payload.getLong(
                                "w1_request_id"
                        ),
                        nowMicros,
                        estimatePayloadBytes(
                                packet.payload
                        ),
                        packet.applicationProtocol
                );
            }

            if ("HTTP".equalsIgnoreCase(
                    packet.applicationProtocol
            )) {
                if (pendingHttpRequestId >= 0L) {
                    flows.recordRx(
                            pendingHttpRequestId,
                            nowMicros,
                            packet.payload.getString(
                                    "content"
                            ).getBytes(
                                    java.nio.charset.StandardCharsets.UTF_8
                            ).length,
                            "HTTP"
                    );

                    pendingHttpRequestId =
                            -1L;
                }

                lastStatus =
                        "HTTP response "
                                + packet.payload.getInt(
                                "status"
                        )
                                + " received";
            } else {
                lastStatus =
                        packet.applicationProtocol
                                + " response received";
            }
        }

        return false;
    }

    private boolean handleArp(
            String localMac,
            String localIp,
            OSINetworkPacket packet,
            long nowMicros,
            Consumer<OSINetworkPacket> transmitter
    ) {
        // W1.20.3 ARP SCHEMA INTEROPERABILITY
        String operation =
                packet.payload.getString(
                        "operation"
                );

        if (operation == null
                || operation.isBlank()) {
            operation =
                    packet.payload.getString(
                            "arp_op"
                    );
        }

        String senderIp =
                packet.payload.getString(
                        "sender_ip"
                );

        String senderMac =
                packet.payload.getString(
                        "sender_mac"
                );

        if (Ipv4Address.isUsableUnicast(
                senderIp
        )
                && !senderMac.isBlank()) {
            neighbors.put(
                    senderIp,
                    new WifiIpNeighbor(
                            senderIp,
                            senderMac,
                            nowMicros
                    )
            );
        }

        if ("REQUEST".equals(
                operation
        )
                && packet.payload
                .getString(
                        "target_ip"
                )
                .equals(
                        localIp
                )) {
            OSINetworkPacket response =
                    basePacket(
                            localMac,
                            packet.sourceMac,
                            localIp,
                            packet.sourceIp,
                            0,
                            0,
                            "ARP"
                    );

            response.isResponse =
                    true;

            response.payload.putString(
                    "operation",
                    "REPLY"
            );
            response.payload.putString(
                    "arp_op",
                    "REPLY"
            );

            response.payload.putString(
                    "sender_ip",
                    localIp
            );

            response.payload.putString(
                    "sender_mac",
                    localMac
            );

            response.payload.putString(
                    "target_ip",
                    packet.sourceIp
            );

            response.payload.putLong(
                    "w1_request_id",
                    packet.payload.getLong(
                            "w1_request_id"
                    )
            );

            if (packet.payload.getBoolean(
                    "traceroute_probe"
            )) {
                response.payload.putBoolean(
                        "traceroute_probe",
                        true
                );

                response.payload.putLong(
                        "traceroute_id",
                        packet.payload.getLong(
                                "traceroute_id"
                        )
                );

                response.payload.putInt(
                        "traceroute_ttl",
                        packet.payload.getInt(
                                "traceroute_ttl"
                        )
                );

                response.payload.putInt(
                        "traceroute_attempt",
                        packet.payload.getInt(
                                "traceroute_attempt"
                        )
                );
            }

            transmitter.accept(
                    response
            );

            lastStatus =
                    "ARP reply sent to "
                            + packet.sourceIp;

            return true;
        }

        if ("REPLY".equals(
                operation
        )) {
            flows.recordRx(
                    packet.payload.getLong(
                            "w1_request_id"
                    ),
                    nowMicros,
                    28,
                    "ARP"
            );

            peerIp =
                    senderIp;

            peerMac =
                    senderMac;

            lastStatus =
                    "ARP resolved "
                            + senderIp
                            + " -> "
                            + senderMac;

            return true;
        }

        return true;
    }

    private boolean handleIcmp(
            String localMac,
            String localIp,
            OSINetworkPacket packet,
            long nowMicros,
            Consumer<OSINetworkPacket> transmitter
    ) {
        // W1.20.8 UNIFIED ICMP ECHO SCHEMA
        String type =
                packet.payload.getString(
                        "type"
                );

        if ((type == null || type.isBlank())
                && packet.payload.getBoolean(
                "w117_echo_request"
        )) {
            type = "ECHO_REQUEST";
        }

        if ((type == null || type.isBlank())
                && packet.payload.getBoolean(
                "w117_echo_reply"
        )) {
            type = "ECHO_REPLY";
        }

        if ("ECHO_REQUEST".equals(
                type
        )) {
            byte[] data =
                    packet.payload.getByteArray(
                            "data"
                    );

            int identifier =
                    packet.payload.contains(
                            "identifier"
                    )
                            ? packet.payload.getInt(
                            "identifier"
                    )
                            : (
                            packet.sessionId == null
                                    ? 0
                                    : packet.sessionId.hashCode()
                    ) & 0xFFFF;

            int sequence =
                    packet.payload.contains(
                            "sequence"
                    )
                            ? packet.payload.getInt(
                            "sequence"
                    )
                            : 1;

            IcmpEchoMessage reply =
                    new IcmpEchoMessage(
                            true,
                            identifier,
                            sequence,
                            data
                    );

            OSINetworkPacket response =
                    basePacket(
                            localMac,
                            packet.sourceMac,
                            localIp,
                            packet.sourceIp,
                            0,
                            0,
                            "ICMP"
                    );

            response.ipProtocol =
                    1;

            response.isResponse =
                    true;

            response.transportChecksum =
                    reply.checksum();

            fillIpv4Metadata(
                    response,
                    reply.encode().length,
                    sequence
            );

            response.payload.putString(
                    "type",
                    "ECHO_REPLY"
            );

            response.payload.putBoolean(
                    "w117_echo_request",
                    false
            );

            response.payload.putBoolean(
                    "w117_echo_reply",
                    true
            );

            response.payload.putInt(
                    "identifier",
                    identifier
            );

            response.payload.putInt(
                    "sequence",
                    sequence
            );

            response.payload.putByteArray(
                    "data",
                    data
            );

            response.payload.putLong(
                    "w1_request_id",
                    packet.payload.getLong(
                            "w1_request_id"
                    )
            );

            if (packet.payload.getBoolean(
                    "traceroute_probe"
            )) {
                response.payload.putBoolean(
                        "traceroute_probe",
                        true
                );

                response.payload.putLong(
                        "traceroute_id",
                        packet.payload.getLong(
                                "traceroute_id"
                        )
                );

                response.payload.putInt(
                        "traceroute_ttl",
                        packet.payload.getInt(
                                "traceroute_ttl"
                        )
                );

                response.payload.putInt(
                        "traceroute_attempt",
                        packet.payload.getInt(
                                "traceroute_attempt"
                        )
                );
            }

            if (packet.payload.getBoolean(
                    "pmtu_probe"
            )) {
                response.payload.putBoolean(
                        "pmtu_probe",
                        true
                );

                response.payload.putLong(
                        "pmtu_session_id",
                        packet.payload.getLong(
                                "pmtu_session_id"
                        )
                );

                response.payload.putInt(
                        "pmtu_probe_bytes",
                        packet.payload.getInt(
                                "pmtu_probe_bytes"
                        )
                );
            }

            transmitter.accept(
                    response
            );

            lastStatus =
                    "ICMP echo reply sent";

            return true;
        }

        if ("ECHO_REPLY".equals(
                type
        )) {
            flows.recordRx(
                    packet.payload.getLong(
                            "w1_request_id"
                    ),
                    nowMicros,
                    packet.payload.getByteArray(
                            "data"
                    ).length
                            + 8,
                    "ICMP"
            );

            lastStatus =
                    "ICMP echo reply received";

            return true;
        }

        return true;
    }

    private boolean handleUdpEcho(
            String localMac,
            String localIp,
            OSINetworkPacket packet,
            long nowMicros,
            Consumer<OSINetworkPacket> transmitter
    ) {
        String type =
                packet.payload.getString(
                        "type"
                );

        if ("REQUEST".equals(
                type
        )) {
            byte[] data =
                    packet.payload.getByteArray(
                            "data"
                    );

            UdpDatagram reply =
                    new UdpDatagram(
                            UDP_ECHO_PORT,
                            packet.sourcePort,
                            data
                    );

            OSINetworkPacket response =
                    basePacket(
                            localMac,
                            packet.sourceMac,
                            localIp,
                            packet.sourceIp,
                            UDP_ECHO_PORT,
                            packet.sourcePort,
                            "UDP"
                    );

            response.ipProtocol =
                    17;

            response.isResponse =
                    true;

            response.transportChecksum =
                    reply.checksum(
                            localIp,
                            packet.sourceIp
                    );

            fillIpv4Metadata(
                    response,
                    reply.encode(
                            localIp,
                            packet.sourceIp
                    ).length,
                    packet.payload.getInt(
                            "sequence"
                    )
            );

            response.payload.putString(
                    "service",
                    "ECHO"
            );

            response.payload.putString(
                    "type",
                    "REPLY"
            );

            response.payload.putInt(
                    "sequence",
                    packet.payload.getInt(
                            "sequence"
                    )
            );

            response.payload.putByteArray(
                    "data",
                    data
            );

            response.payload.putLong(
                    "w1_request_id",
                    packet.payload.getLong(
                            "w1_request_id"
                    )
            );

            if (packet.payload.getBoolean(
                    "traceroute_probe"
            )) {
                response.payload.putBoolean(
                        "traceroute_probe",
                        true
                );

                response.payload.putLong(
                        "traceroute_id",
                        packet.payload.getLong(
                                "traceroute_id"
                        )
                );

                response.payload.putInt(
                        "traceroute_ttl",
                        packet.payload.getInt(
                                "traceroute_ttl"
                        )
                );

                response.payload.putInt(
                        "traceroute_attempt",
                        packet.payload.getInt(
                                "traceroute_attempt"
                        )
                );
            }

            transmitter.accept(
                    response
            );

            lastStatus =
                    "UDP echo reply sent";

            return true;
        }

        if ("REPLY".equals(
                type
        )) {
            flows.recordRx(
                    packet.payload.getLong(
                            "w1_request_id"
                    ),
                    nowMicros,
                    packet.payload.getByteArray(
                            "data"
                    ).length
                            + 8,
                    "UDP"
            );

            lastStatus =
                    "UDP echo reply received";

            return true;
        }

        return true;
    }

    private void fillIpv4Metadata(
            OSINetworkPacket packet,
            int payloadLength,
            int identification
    ) {
        Ipv4Header header =
                new Ipv4Header(
                        packet.sourceIp,
                        packet.targetIp,
                        packet.ipProtocol,
                        packet.ttl,
                        identification,
                        payloadLength,
                        true
                );

        packet.ipv4HeaderChecksum =
                header.headerChecksum();

        packet.ipPacketLength =
                Ipv4Header.HEADER_BYTES
                        + payloadLength;

        packet.dontFragment =
                true;
    }

    private OSINetworkPacket basePacket(
            String sourceMac,
            String targetMac,
            String sourceIp,
            String targetIp,
            int sourcePort,
            int targetPort,
            String protocol
    ) {
        OSINetworkPacket packet =
                new OSINetworkPacket();

        packet.sourceMac =
                sourceMac;

        packet.targetMac =
                targetMac;

        packet.sourceIp =
                sourceIp;

        packet.targetIp =
                targetIp;

        packet.sourcePort =
                sourcePort;

        packet.targetPort =
                targetPort;

        packet.applicationProtocol =
                protocol;

        packet.sessionId =
                "w1-"
                        + nextSequence;

        return packet;
    }

    private long nextId() {
        return (
                (
                        long
                ) nextSequence++
                        << 32
        )
                ^ System.nanoTime();
    }

    private String validSourceIp(
            String localIp
    ) {
        return Ipv4Address.isUsableUnicast(
                localIp
        )
                ? localIp
                : "0.0.0.0";
    }

    private int estimatePayloadBytes(
            CompoundTag payload
    ) {
        if (payload.contains(
                "data"
        )) {
            return payload.getByteArray(
                    "data"
            ).length;
        }

        return 64;
    }
}

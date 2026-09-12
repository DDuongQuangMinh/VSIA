package com.k1ngtle.vsia.signality.engineering.cellular.userplane;

import com.k1ngtle.vsia.signality.engineering.cellular.core.PduSession;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

// CELLULAR_USER_PLANE_V1
public final class CellularUserPlaneService {
    public static final String DEFAULT_GATEWAY = "10.0.0.1";
    public static final String DNN_TEST_ECHO = "198.51.100.1";
    public static final int DEFAULT_PAYLOAD_BYTES = 32;

    private static final ConcurrentMap<UUID, DeviceState> STATES =
            new ConcurrentHashMap<>();

    private CellularUserPlaneService() {
    }

    public static CellularUserPlaneTrace ping(
            UUID deviceId,
            PduSession session,
            String targetAddress
    ) {
        return ping(
                deviceId,
                session,
                targetAddress,
                DEFAULT_PAYLOAD_BYTES
        );
    }

    public static CellularUserPlaneTrace ping(
            UUID deviceId,
            PduSession session,
            String targetAddress,
            int payloadBytes
    ) {
        if (deviceId == null) {
            throw new IllegalArgumentException("deviceId cannot be null");
        }

        DeviceState state =
                STATES.computeIfAbsent(
                        deviceId,
                        ignored -> new DeviceState()
                );

        synchronized (state) {
            int sequence = ++state.sequence;
            state.lastSequence = sequence;
            state.lastTarget =
                    targetAddress == null
                            ? ""
                            : targetAddress;

            if (session == null) {
                return fail(
                        state,
                        sequence,
                        "",
                        targetAddress,
                        payloadBytes,
                        "NO_PDU_SESSION"
                );
            }

            if (!session.active()) {
                return fail(
                        state,
                        sequence,
                        session.ipAddress(),
                        targetAddress,
                        payloadBytes,
                        "PDU_SESSION_INACTIVE"
                );
            }

            if (!CellularIcmpEchoPacket.validIpv4(session.ipAddress())) {
                return fail(
                        state,
                        sequence,
                        session.ipAddress(),
                        targetAddress,
                        payloadBytes,
                        "INVALID_UE_ADDRESS"
                );
            }

            if (!CellularIcmpEchoPacket.validIpv4(targetAddress)) {
                return fail(
                        state,
                        sequence,
                        session.ipAddress(),
                        targetAddress,
                        payloadBytes,
                        "INVALID_TARGET_IPV4"
                );
            }

            if (payloadBytes < 0 || payloadBytes > 1400) {
                return fail(
                        state,
                        sequence,
                        session.ipAddress(),
                        targetAddress,
                        payloadBytes,
                        "PAYLOAD_REQUIRES_FRAGMENTATION"
                );
            }

            CellularIcmpEchoPacket request =
                    CellularIcmpEchoPacket.request(
                            session.ipAddress(),
                            targetAddress,
                            session.sessionId(),
                            sequence,
                            payloadBytes
                    );

            state.txPackets++;
            state.txIpBytes += request.ipv4Bytes();

            String route =
                    resolveRoute(
                            session,
                            targetAddress
                    );

            if (route == null) {
                state.droppedPackets++;
                state.lastResult = "NO_ROUTE";
                state.lastRoundTripMicros = 0L;

                return new CellularUserPlaneTrace(
                        false,
                        sequence,
                        session.ipAddress(),
                        targetAddress,
                        payloadBytes,
                        0L,
                        0,
                        "",
                        "NO_ROUTE",
                        List.of()
                );
            }

            List<CellularUserPlaneHop> hops =
                    new ArrayList<>();

            int ipBytes = request.ipv4Bytes();
            int sdapBytes = ipBytes + 1;
            int pdcpBytes = sdapBytes + 2;
            int rlcBytes = pdcpBytes + 2;
            int macBytes = rlcBytes + 3;

            CellularGtpuPacket gtpu =
                    CellularGtpuPacket.forSession(
                            session,
                            sequence,
                            ipBytes
                    );

            long jitter =
                    deterministicJitterMicros(
                            deviceId,
                            targetAddress,
                            sequence
                    );

            long radioPenalty =
                    qosRadioPenaltyMicros(
                            session.fiveQi()
                    );

            add(
                    hops,
                    CellularUserPlaneLayer.ICMP,
                    CellularUserPlaneDirection.UPLINK,
                    20L,
                    request.icmpBytes(),
                    "echo-request checksum=0x"
                            + Integer.toHexString(request.checksum())
            );

            add(
                    hops,
                    CellularUserPlaneLayer.IPV4,
                    CellularUserPlaneDirection.UPLINK,
                    25L,
                    ipBytes,
                    "src="
                            + request.sourceAddress()
                            + " dst="
                            + request.destinationAddress()
                            + " ttl=64"
            );

            add(
                    hops,
                    CellularUserPlaneLayer.SDAP,
                    CellularUserPlaneDirection.UPLINK,
                    12L,
                    sdapBytes,
                    "5QI=" + session.fiveQi()
            );

            add(
                    hops,
                    CellularUserPlaneLayer.PDCP,
                    CellularUserPlaneDirection.UPLINK,
                    35L,
                    pdcpBytes,
                    "data-radio-bearer"
            );

            add(
                    hops,
                    CellularUserPlaneLayer.RLC,
                    CellularUserPlaneDirection.UPLINK,
                    45L,
                    rlcBytes,
                    "AM data PDU"
            );

            add(
                    hops,
                    CellularUserPlaneLayer.MAC,
                    CellularUserPlaneDirection.UPLINK,
                    70L,
                    macBytes,
                    "UL-SCH transport block"
            );

            add(
                    hops,
                    CellularUserPlaneLayer.NR_PHY,
                    CellularUserPlaneDirection.UPLINK,
                    700L + radioPenalty + jitter / 2L,
                    macBytes,
                    "PUSCH"
            );

            add(
                    hops,
                    CellularUserPlaneLayer.GNB,
                    CellularUserPlaneDirection.UPLINK,
                    120L,
                    macBytes,
                    "gNB user-plane termination"
            );

            add(
                    hops,
                    CellularUserPlaneLayer.GTP_U_N3,
                    CellularUserPlaneDirection.UPLINK,
                    160L,
                    gtpu.n3WireBytes(),
                    "TEID=" + Long.toUnsignedString(gtpu.teid())
            );

            add(
                    hops,
                    CellularUserPlaneLayer.UPF,
                    CellularUserPlaneDirection.UPLINK,
                    190L,
                    ipBytes,
                    "PDR/FAR route lookup"
            );

            add(
                    hops,
                    CellularUserPlaneLayer.DNN,
                    CellularUserPlaneDirection.LOCAL,
                    route.equals("UPF_GATEWAY") ? 120L : 650L,
                    ipBytes,
                    route
            );

            CellularIcmpEchoPacket reply =
                    request.toReply();

            add(
                    hops,
                    CellularUserPlaneLayer.UPF,
                    CellularUserPlaneDirection.DOWNLINK,
                    190L,
                    reply.ipv4Bytes(),
                    "downlink PDR/FAR"
            );

            add(
                    hops,
                    CellularUserPlaneLayer.GTP_U_N3,
                    CellularUserPlaneDirection.DOWNLINK,
                    160L,
                    gtpu.n3WireBytes(),
                    "N3 tunnel"
            );

            add(
                    hops,
                    CellularUserPlaneLayer.GNB,
                    CellularUserPlaneDirection.DOWNLINK,
                    120L,
                    macBytes,
                    "gNB scheduler"
            );

            add(
                    hops,
                    CellularUserPlaneLayer.NR_PHY,
                    CellularUserPlaneDirection.DOWNLINK,
                    700L + radioPenalty + jitter / 2L,
                    macBytes,
                    "PDSCH"
            );

            add(
                    hops,
                    CellularUserPlaneLayer.MAC,
                    CellularUserPlaneDirection.DOWNLINK,
                    70L,
                    macBytes,
                    "DL-SCH transport block"
            );

            add(
                    hops,
                    CellularUserPlaneLayer.RLC,
                    CellularUserPlaneDirection.DOWNLINK,
                    45L,
                    rlcBytes,
                    "RLC reassembly"
            );

            add(
                    hops,
                    CellularUserPlaneLayer.PDCP,
                    CellularUserPlaneDirection.DOWNLINK,
                    35L,
                    pdcpBytes,
                    "PDCP delivery"
            );

            add(
                    hops,
                    CellularUserPlaneLayer.SDAP,
                    CellularUserPlaneDirection.DOWNLINK,
                    12L,
                    sdapBytes,
                    "QoS-flow mapping"
            );

            add(
                    hops,
                    CellularUserPlaneLayer.IPV4,
                    CellularUserPlaneDirection.DOWNLINK,
                    25L,
                    reply.ipv4Bytes(),
                    "dst=" + reply.destinationAddress() + " ttl=64"
            );

            add(
                    hops,
                    CellularUserPlaneLayer.ICMP,
                    CellularUserPlaneDirection.DOWNLINK,
                    20L,
                    reply.icmpBytes(),
                    "echo-reply checksum=0x"
                            + Integer.toHexString(reply.checksum())
            );

            long roundTripMicros =
                    hops.stream()
                            .mapToLong(
                                    CellularUserPlaneHop::processingMicros
                            )
                            .sum();

            state.rxPackets++;
            state.rxIpBytes += reply.ipv4Bytes();
            state.txRadioBytes += macBytes;
            state.rxRadioBytes += macBytes;
            state.lastResult = "ECHO_REPLY";
            state.lastRoundTripMicros = roundTripMicros;

            return new CellularUserPlaneTrace(
                    true,
                    sequence,
                    session.ipAddress(),
                    targetAddress,
                    payloadBytes,
                    roundTripMicros,
                    64,
                    route,
                    "",
                    hops
            );
        }
    }

    public static CellularUserPlaneStatsSnapshot snapshot(
            UUID deviceId
    ) {
        if (deviceId == null) {
            return emptySnapshot();
        }

        DeviceState state = STATES.get(deviceId);

        if (state == null) {
            return emptySnapshot();
        }

        synchronized (state) {
            return new CellularUserPlaneStatsSnapshot(
                    state.txPackets,
                    state.rxPackets,
                    state.droppedPackets,
                    state.txIpBytes,
                    state.rxIpBytes,
                    state.txRadioBytes,
                    state.rxRadioBytes,
                    state.lastSequence,
                    state.lastTarget,
                    state.lastResult,
                    state.lastRoundTripMicros
            );
        }
    }

    public static void reset(UUID deviceId) {
        if (deviceId != null) {
            STATES.remove(deviceId);
        }
    }

    public static boolean isSupportedEchoTarget(
            String targetAddress
    ) {
        return DEFAULT_GATEWAY.equals(targetAddress)
                || DNN_TEST_ECHO.equals(targetAddress);
    }

    private static CellularUserPlaneTrace fail(
            DeviceState state,
            int sequence,
            String sourceAddress,
            String targetAddress,
            int payloadBytes,
            String reason
    ) {
        state.droppedPackets++;
        state.lastResult = reason;
        state.lastRoundTripMicros = 0L;

        return new CellularUserPlaneTrace(
                false,
                sequence,
                sourceAddress,
                targetAddress,
                payloadBytes,
                0L,
                0,
                "",
                reason,
                List.of()
        );
    }

    private static String resolveRoute(
            PduSession session,
            String targetAddress
    ) {
        if (DEFAULT_GATEWAY.equals(targetAddress)) {
            return "UPF_GATEWAY";
        }

        if (DNN_TEST_ECHO.equals(targetAddress)
                && "internet".equalsIgnoreCase(session.dnn())) {
            return "DNN_TEST_ECHO";
        }

        return null;
    }

    private static long deterministicJitterMicros(
            UUID deviceId,
            String targetAddress,
            int sequence
    ) {
        int hash = deviceId.hashCode();
        hash = 31 * hash + targetAddress.hashCode();
        hash = 31 * hash + sequence;

        return Math.floorMod(hash, 401);
    }

    private static long qosRadioPenaltyMicros(
            int fiveQi
    ) {
        if (fiveQi <= 4) {
            return 150L;
        }

        if (fiveQi <= 8) {
            return 350L;
        }

        return 650L;
    }

    private static void add(
            List<CellularUserPlaneHop> hops,
            CellularUserPlaneLayer layer,
            CellularUserPlaneDirection direction,
            long processingMicros,
            int wireBytes,
            String note
    ) {
        hops.add(
                new CellularUserPlaneHop(
                        layer,
                        direction,
                        processingMicros,
                        wireBytes,
                        note
                )
        );
    }

    private static CellularUserPlaneStatsSnapshot emptySnapshot() {
        return new CellularUserPlaneStatsSnapshot(
                0L,
                0L,
                0L,
                0L,
                0L,
                0L,
                0L,
                0,
                "",
                "NONE",
                0L
        );
    }

    private static final class DeviceState {
        private int sequence;

        private long txPackets;
        private long rxPackets;
        private long droppedPackets;

        private long txIpBytes;
        private long rxIpBytes;

        private long txRadioBytes;
        private long rxRadioBytes;

        private int lastSequence;
        private String lastTarget = "";
        private String lastResult = "NONE";
        private long lastRoundTripMicros;
    }
}

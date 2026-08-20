package com.k1ngtle.vsia.signality.engineering.wifi.ip.raw;

import com.k1ngtle.vsia.signality.engineering.ExecutionMode;
import com.k1ngtle.vsia.signality.engineering.wifi.link.EtherType;
import com.k1ngtle.vsia.signality.engineering.wifi.link.LlcSnapCodec;
import com.k1ngtle.vsia.signality.engineering.wifi.link.LlcSnapFrame;
import com.k1ngtle.vsia.signality.internet.OSINetworkPacket;
import net.minecraft.nbt.CompoundTag;

import java.util.ArrayList;
import java.util.List;

public final class RawIpv4LiveCarrierCodec {
    public static final String CONTROL_KEY =
            "vsia_raw_network_control";

    public static final String CONTROL_VALUE =
            "LLC_SNAP_IPV4_FRAGMENT_V1";

    public static final String RAW_MSDU_KEY =
            "raw_llc_snap_msdu";

    private RawIpv4LiveCarrierCodec() {
    }

    public static boolean isRawFragmentCarrier(
            CompoundTag body
    ) {
        return body != null
                && CONTROL_VALUE.equals(
                body.getString(
                        CONTROL_KEY
                )
        )
                && body.contains(
                RAW_MSDU_KEY
        );
    }

    public static List<CompoundTag> encodeUdpFragments(
            OSINetworkPacket logical,
            int mtu,
            int identification
    ) {
        if (logical == null
                || !"UDP".equalsIgnoreCase(
                logical.applicationProtocol
        )) {
            throw new IllegalArgumentException(
                    "UDP OSINetworkPacket required"
            );
        }

        byte[] udp =
                RawUdpCodec.encode(
                        logical.sourceIp,
                        logical.targetIp,
                        logical.sourcePort,
                        logical.targetPort,
                        logical.payload.getByteArray(
                                "data"
                        )
                );

        byte[] raw =
                RawIpv4Encoder.encode(
                        logical.sourceIp,
                        logical.targetIp,
                        0,
                        identification,
                        logical.dontFragment,
                        false,
                        0,
                        logical.ttl <= 0
                                ? 64
                                : logical.ttl,
                        17,
                        udp
                );

        return wrapFragments(
                logical,
                RawIpv4Fragmenter.fragment(
                        raw,
                        mtu
                )
        );
    }

    public static List<CompoundTag> encodeIcmpEchoFragments(
            OSINetworkPacket logical,
            int mtu,
            int identification
    ) {
        if (logical == null
                || !"ICMP".equalsIgnoreCase(
                logical.applicationProtocol
        )) {
            throw new IllegalArgumentException(
                    "ICMP OSINetworkPacket required"
            );
        }

        String type =
                logical.payload.getString(
                        "type"
                );

        boolean reply =
                "ECHO_REPLY".equalsIgnoreCase(
                        type
                );

        if (!reply
                && !"ECHO_REQUEST".equalsIgnoreCase(
                type
        )) {
            throw new IllegalArgumentException(
                    "W1.11.1 live carrier supports ICMP Echo here; router ICMP errors keep the existing raw ICMP carrier"
            );
        }

        byte[] icmp =
                RawIcmpCodec.encodeEcho(
                        reply,
                        logical.payload.getInt(
                                "identifier"
                        ),
                        logical.payload.getInt(
                                "sequence"
                        ),
                        logical.payload.getByteArray(
                                "data"
                        )
                );

        byte[] raw =
                RawIpv4Encoder.encode(
                        logical.sourceIp,
                        logical.targetIp,
                        0,
                        identification,
                        logical.dontFragment,
                        false,
                        0,
                        logical.ttl <= 0
                                ? 64
                                : logical.ttl,
                        1,
                        icmp
                );

        return wrapFragments(
                logical,
                RawIpv4Fragmenter.fragment(
                        raw,
                        mtu
                )
        );
    }

    public static DecodedFragment decodeFragment(
            CompoundTag body
    ) {
        if (!isRawFragmentCarrier(
                body
        )) {
            throw new IllegalArgumentException(
                    "Not a W1.11 raw IPv4 fragment carrier"
            );
        }

        LlcSnapFrame frame =
                LlcSnapCodec.decodeRfc1042(
                        body.getByteArray(
                                RAW_MSDU_KEY
                        )
                );

        if (frame.etherType()
                != EtherType.IPV4.value()) {
            throw new IllegalArgumentException(
                    "Expected IPv4 EtherType"
            );
        }

        return new DecodedFragment(
                frame.payload(),
                body.getString(
                        "src_mac"
                ),
                body.getString(
                        "dst_mac"
                ),
                body.getCompound(
                        "logical_meta"
                )
        );
    }

    public static OSINetworkPacket toLogical(
            byte[] reassembledRawIpv4,
            String sourceMac,
            String targetMac,
            CompoundTag metadata
    ) {
        RawIpv4Transport.Decoded decoded =
                RawIpv4Transport.decode(
                        reassembledRawIpv4
                );

        OSINetworkPacket logical =
                new OSINetworkPacket();

        logical.sourceMac =
                sourceMac == null
                        ? ""
                        : sourceMac;

        logical.targetMac =
                targetMac == null
                        ? ""
                        : targetMac;

        logical.sourceIp =
                decoded.ipv4()
                        .sourceAddress();

        logical.targetIp =
                decoded.ipv4()
                        .destinationAddress();

        logical.ttl =
                decoded.ipv4()
                        .ttl();

        logical.ipProtocol =
                decoded.ipv4()
                        .protocol();

        logical.ipPacketLength =
                decoded.ipv4()
                        .totalLength();

        logical.ipv4HeaderChecksum =
                decoded.ipv4()
                        .headerChecksum();

        logical.dontFragment =
                decoded.ipv4()
                        .dontFragment();

        CompoundTag meta =
                metadata == null
                        ? new CompoundTag()
                        : metadata;

        logical.isResponse =
                meta.getBoolean(
                        "is_response"
                );

        logical.sessionId =
                meta.getString(
                        "session_id"
                );

        if (decoded.udp() != null) {
            logical.applicationProtocol =
                    "UDP";

            logical.sourcePort =
                    decoded.udp()
                            .sourcePort();

            logical.targetPort =
                    decoded.udp()
                            .destinationPort();

            logical.transportChecksum =
                    decoded.udp()
                            .checksum();

            logical.payload.putByteArray(
                    "data",
                    decoded.udp()
                            .payload()
            );

            copyMetadata(
                    meta,
                    logical.payload
            );

            return logical;
        }

        if (decoded.icmp() != null) {
            logical.applicationProtocol =
                    "ICMP";

            logical.transportChecksum =
                    decoded.icmp()
                            .checksum();

            int type =
                    decoded.icmp()
                            .type();

            int code =
                    decoded.icmp()
                            .code();

            logical.payload.putInt(
                    "icmp_type",
                    type
            );

            logical.payload.putInt(
                    "icmp_code",
                    code
            );

            if (type == 0
                    || type == 8) {
                logical.payload.putString(
                        "type",
                        type == 0
                                ? "ECHO_REPLY"
                                : "ECHO_REQUEST"
                );

                logical.payload.putInt(
                        "identifier",
                        (decoded.icmp()
                                .restOfHeader()
                                >>> 16)
                                & 0xFFFF
                );

                logical.payload.putInt(
                        "sequence",
                        decoded.icmp()
                                .restOfHeader()
                                & 0xFFFF
                );

                logical.payload.putByteArray(
                        "data",
                        decoded.icmp()
                                .payload()
                );
            }

            copyMetadata(
                    meta,
                    logical.payload
            );

            return logical;
        }

        logical.applicationProtocol =
                "RAW_IPV4";

        logical.payload.putByteArray(
                "raw_payload",
                decoded.ipv4()
                        .payload()
        );

        return logical;
    }

    private static List<CompoundTag> wrapFragments(
            OSINetworkPacket logical,
            List<byte[]> rawFragments
    ) {
        List<CompoundTag> out =
                new ArrayList<>();

        CompoundTag metadata =
                metadata(
                        logical
                );

        for (int index = 0;
             index < rawFragments.size();
             index++) {
            byte[] raw =
                    rawFragments.get(
                            index
                    );

            CompoundTag body =
                    new CompoundTag();

            body.putString(
                    CONTROL_KEY,
                    CONTROL_VALUE
            );

            body.putString(
                    "execution_mode",
                    ExecutionMode.CONFORMANCE.name()
            );

            body.putByteArray(
                    RAW_MSDU_KEY,
                    LlcSnapCodec.encodeRfc1042(
                            EtherType.IPV4,
                            raw
                    )
            );

            body.putString(
                    "src_mac",
                    logical.sourceMac
            );

            body.putString(
                    "dst_mac",
                    logical.targetMac
            );

            body.putInt(
                    "fragment_index",
                    index
            );

            body.putInt(
                    "fragment_count",
                    rawFragments.size()
            );

            body.put(
                    "logical_meta",
                    metadata.copy()
            );

            out.add(
                    body
            );
        }

        return List.copyOf(
                out
        );
    }

    private static CompoundTag metadata(
            OSINetworkPacket logical
    ) {
        CompoundTag meta =
                new CompoundTag();

        meta.putBoolean(
                "is_response",
                logical.isResponse
        );

        meta.putString(
                "session_id",
                logical.sessionId == null
                        ? ""
                        : logical.sessionId
        );

        CompoundTag payload =
                logical.payload;

        copyIfPresent(
                payload,
                meta,
                "service"
        );
        copyIfPresent(
                payload,
                meta,
                "type"
        );
        copyIfPresent(
                payload,
                meta,
                "sequence"
        );
        copyIfPresent(
                payload,
                meta,
                "sent_us"
        );
        copyIfPresent(
                payload,
                meta,
                "w1_request_id"
        );
        copyIfPresent(
                payload,
                meta,
                "traceroute_probe"
        );
        copyIfPresent(
                payload,
                meta,
                "traceroute_id"
        );
        copyIfPresent(
                payload,
                meta,
                "traceroute_ttl"
        );
        copyIfPresent(
                payload,
                meta,
                "traceroute_attempt"
        );
        copyIfPresent(
                payload,
                meta,
                "pmtu_probe"
        );
        copyIfPresent(
                payload,
                meta,
                "pmtu_session_id"
        );
        copyIfPresent(
                payload,
                meta,
                "pmtu_probe_bytes"
        );

        return meta;
    }

    private static void copyMetadata(
            CompoundTag from,
            CompoundTag to
    ) {
        for (String key
                : from.getAllKeys()) {
            if ("is_response".equals(
                    key
            )
                    || "session_id".equals(
                    key
            )) {
                continue;
            }

            if (from.get(
                    key
            ) != null) {
                to.put(
                        key,
                        from.get(
                                key
                        ).copy()
                );
            }
        }
    }

    private static void copyIfPresent(
            CompoundTag from,
            CompoundTag to,
            String key
    ) {
        if (from.contains(
                key
        )
                && from.get(
                key
        ) != null) {
            to.put(
                    key,
                    from.get(
                            key
                    ).copy()
            );
        }
    }

    public record DecodedFragment(
            byte[] rawIpv4,
            String sourceMac,
            String targetMac,
            CompoundTag metadata
    ) {
        public DecodedFragment {
            rawIpv4 =
                    rawIpv4 == null
                            ? new byte[0]
                            : rawIpv4.clone();

            metadata =
                    metadata == null
                            ? new CompoundTag()
                            : metadata.copy();
        }

        @Override
        public byte[] rawIpv4() {
            return rawIpv4.clone();
        }

        @Override
        public CompoundTag metadata() {
            return metadata.copy();
        }
    }
}

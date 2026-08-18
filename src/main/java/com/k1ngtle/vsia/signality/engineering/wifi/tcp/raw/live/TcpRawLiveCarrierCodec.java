package com.k1ngtle.vsia.signality.engineering.wifi.tcp.raw.live;

import com.k1ngtle.vsia.signality.engineering.ExecutionMode;
import com.k1ngtle.vsia.signality.engineering.wifi.tcp.TcpSegment;
import com.k1ngtle.vsia.signality.engineering.wifi.tcp.live.TcpLivePacketCodec;
import com.k1ngtle.vsia.signality.engineering.wifi.tcp.options.TcpOptionSet;
import com.k1ngtle.vsia.signality.engineering.wifi.tcp.raw.RawIpv4TcpCodec;
import com.k1ngtle.vsia.signality.engineering.wifi.tcp.raw.RawIpv4TcpPacket;
import com.k1ngtle.vsia.signality.internet.OSINetworkPacket;
import net.minecraft.nbt.CompoundTag;

public final class TcpRawLiveCarrierCodec {
    public static final String CONTROL_KEY =
            "vsia_raw_network_control";

    public static final String CONTROL_VALUE =
            "IPV4_TCP_V1";

    public static final String RAW_PACKET_KEY =
            "raw_ipv4_tcp";

    private TcpRawLiveCarrierCodec() {
    }

    public static boolean isRawCarrier(
            CompoundTag body
    ) {
        return body != null
                && CONTROL_VALUE.equals(
                body.getString(
                        CONTROL_KEY
                )
        )
                && body.contains(
                RAW_PACKET_KEY
        );
    }

    public static CompoundTag encode(
            OSINetworkPacket logical
    ) {
        if (logical == null
                || !"TCP".equalsIgnoreCase(
                logical.applicationProtocol
        )) {
            throw new IllegalArgumentException(
                    "Raw live carrier requires a TCP OSINetworkPacket"
            );
        }

        TcpSegment segment =
                TcpLivePacketCodec.decode(
                        logical
                );

        TcpOptionSet options =
                TcpLivePacketCodec.decodeOptions(
                        logical
                );

        byte[] chunk =
                logical.payload.getByteArray(
                        "tcp_app_chunk"
                );

        int identification =
                (
                        int
                ) (
                segment.sequenceNumber()
                        & 0xFFFFL
        );

        byte[] raw =
                RawIpv4TcpCodec.encode(
                        logical.sourceIp,
                        logical.targetIp,
                        segment.sourcePort(),
                        segment.destinationPort(),
                        segment.sequenceNumber(),
                        segment.acknowledgementNumber(),
                        segment.flags(),
                        segment.window(),
                        logical.ttl <= 0
                                ? 64
                                : logical.ttl,
                        identification,
                        logical.dontFragment,
                        options,
                        chunk
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
                RAW_PACKET_KEY,
                raw
        );

        body.putString(
                "src_mac",
                logical.sourceMac
        );

        body.putString(
                "dst_mac",
                logical.targetMac
        );

        body.putString(
                "tcp_session_id",
                logical.sessionId
        );

        body.putBoolean(
                "tcp_is_response",
                logical.isResponse
        );

        body.putInt(
                "tcp_app_offset",
                logical.payload.getInt(
                        "tcp_app_offset"
                )
        );

        body.putInt(
                "tcp_app_total",
                logical.payload.getInt(
                        "tcp_app_total"
                )
        );

        body.putLong(
                "tcp_sent_us",
                logical.payload.getLong(
                        "tcp_sent_us"
                )
        );

        body.putBoolean(
                "tcp_retransmission",
                logical.payload.getBoolean(
                        "tcp_retransmission"
                )
        );

        return body;
    }

    public static OSINetworkPacket decode(
            CompoundTag body
    ) {
        if (!isRawCarrier(
                body
        )) {
            throw new IllegalArgumentException(
                    "Not a VSIA raw IPv4/TCP carrier"
            );
        }

        RawIpv4TcpPacket decoded =
                RawIpv4TcpCodec.decode(
                        body.getByteArray(
                                RAW_PACKET_KEY
                        )
                );

        if (!decoded.valid()) {
            throw new IllegalArgumentException(
                    "Raw IPv4/TCP checksum validation failed"
            );
        }

        OSINetworkPacket logical =
                new OSINetworkPacket();

        logical.sourceMac =
                body.getString(
                        "src_mac"
                );

        logical.targetMac =
                body.getString(
                        "dst_mac"
                );

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

        logical.ipv4HeaderChecksum =
                decoded.ipv4()
                        .headerChecksum();

        logical.ipPacketLength =
                decoded.ipv4()
                        .totalLength();

        logical.dontFragment =
                decoded.ipv4()
                        .dontFragment();

        logical.sourcePort =
                decoded.tcp()
                        .sourcePort();

        logical.targetPort =
                decoded.tcp()
                        .destinationPort();

        logical.transportChecksum =
                decoded.tcp()
                        .checksum();

        logical.sessionId =
                body.getString(
                        "tcp_session_id"
                );

        logical.isResponse =
                body.getBoolean(
                        "tcp_is_response"
                );

        logical.applicationProtocol =
                "TCP";

        CompoundTag payload =
                logical.payload;

        payload.putLong(
                "tcp_seq",
                decoded.tcp()
                        .sequenceNumber()
        );

        payload.putLong(
                "tcp_ack",
                decoded.tcp()
                        .acknowledgementNumber()
        );

        payload.putInt(
                "tcp_window",
                decoded.tcp()
                        .window()
        );

        payload.putBoolean(
                "tcp_fin",
                decoded.tcp()
                        .flags()
                        .fin()
        );

        payload.putBoolean(
                "tcp_syn",
                decoded.tcp()
                        .flags()
                        .syn()
        );

        payload.putBoolean(
                "tcp_rst",
                decoded.tcp()
                        .flags()
                        .rst()
        );

        payload.putBoolean(
                "tcp_psh",
                decoded.tcp()
                        .flags()
                        .psh()
        );

        payload.putBoolean(
                "tcp_ack_flag",
                decoded.tcp()
                        .flags()
                        .ack()
        );

        payload.putLong(
                "tcp_sent_us",
                body.getLong(
                        "tcp_sent_us"
                )
        );

        payload.putBoolean(
                "tcp_retransmission",
                body.getBoolean(
                        "tcp_retransmission"
                )
        );

        byte[] options =
                decoded.tcp()
                        .optionBytes();

        if (options.length > 0) {
            payload.putByteArray(
                    "tcp_options",
                    options
            );
        }

        byte[] chunk =
                decoded.tcp()
                        .payload();

        if (chunk.length > 0) {
            payload.putByteArray(
                    "tcp_app_chunk",
                    chunk
            );

            payload.putInt(
                    "tcp_app_offset",
                    body.getInt(
                            "tcp_app_offset"
                    )
            );

            payload.putInt(
                    "tcp_app_total",
                    body.getInt(
                            "tcp_app_total"
                    )
            );
        }

        return logical;
    }
}

package com.k1ngtle.vsia.signality.engineering.wifi.ip.router.live;

import com.k1ngtle.vsia.signality.engineering.ExecutionMode;
import com.k1ngtle.vsia.signality.engineering.wifi.ip.Ipv4Header;
import com.k1ngtle.vsia.signality.engineering.wifi.ip.router.IcmpErrorModel;
import com.k1ngtle.vsia.signality.engineering.wifi.link.EtherType;
import com.k1ngtle.vsia.signality.engineering.wifi.link.LlcSnapCodec;
import com.k1ngtle.vsia.signality.engineering.wifi.link.LlcSnapFrame;
import com.k1ngtle.vsia.signality.engineering.wifi.tcp.raw.RawIpv4Decoder;
import com.k1ngtle.vsia.signality.engineering.wifi.tcp.raw.RawIpv4Packet;
import com.k1ngtle.vsia.signality.internet.OSINetworkPacket;
import net.minecraft.nbt.CompoundTag;

public final class IcmpRawLiveCarrierCodec {
    public static final String CONTROL_KEY =
            "vsia_raw_network_control";

    public static final String CONTROL_VALUE =
            "LLC_SNAP_IPV4_ICMP_ERROR_V1";

    public static final String RAW_MSDU_KEY =
            "raw_llc_snap_msdu";

    private IcmpRawLiveCarrierCodec() {
    }

    public static boolean isRawCarrier(
            CompoundTag body
    ) {
        return body != null
                && CONTROL_VALUE.equals(
                body.getString(CONTROL_KEY)
        )
                && body.contains(RAW_MSDU_KEY);
    }

    public static boolean canEncode(
            OSINetworkPacket packet
    ) {
        if (packet == null
                || !"ICMP".equalsIgnoreCase(
                packet.applicationProtocol
        )) {
            return false;
        }

        String type =
                packet.payload.getString(
                        "type"
                );

        return "TIME_EXCEEDED".equalsIgnoreCase(type)
                || "DESTINATION_UNREACHABLE".equalsIgnoreCase(type);
    }

    public static CompoundTag encode(
            OSINetworkPacket logical
    ) {
        if (!canEncode(logical)) {
            throw new IllegalArgumentException(
                    "Raw ICMP error carrier requires Time Exceeded or Destination Unreachable"
            );
        }

        int type =
                logical.payload.getInt(
                        "icmp_type"
                );

        int code =
                logical.payload.getInt(
                        "icmp_code"
                );

        byte[] quote =
                buildQuote(logical);

        byte[] icmp =
                IcmpErrorModel.encode(
                        type,
                        code,
                        quote
                );

        Ipv4Header header =
                new Ipv4Header(
                        logical.sourceIp,
                        logical.targetIp,
                        1,
                        logical.ttl <= 0
                                ? 64
                                : logical.ttl,
                        logical.payload.getInt(
                                "icmp_error_id"
                        ) & 0xFFFF,
                        icmp.length,
                        true
                );

        byte[] ipHeader =
                header.encode();

        byte[] raw =
                new byte[
                        ipHeader.length
                                + icmp.length
                ];

        System.arraycopy(
                ipHeader,
                0,
                raw,
                0,
                ipHeader.length
        );

        System.arraycopy(
                icmp,
                0,
                raw,
                ipHeader.length,
                icmp.length
        );

        byte[] msdu =
                LlcSnapCodec.encodeRfc1042(
                        EtherType.IPV4,
                        raw
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
                msdu
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
                "icmp_error_name",
                logical.payload.getString(
                        "type"
                )
        );

        return body;
    }

    public static OSINetworkPacket decode(
            CompoundTag body
    ) {
        if (!isRawCarrier(body)) {
            throw new IllegalArgumentException(
                    "Not a VSIA raw IPv4/ICMP error carrier"
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
                    "ICMP error carrier requires IPv4 EtherType"
            );
        }

        RawIpv4Packet ipv4 =
                RawIpv4Decoder.decode(
                        frame.payload()
                );

        if (!ipv4.checksumValid()
                || ipv4.protocol() != 1) {
            throw new IllegalArgumentException(
                    "Invalid IPv4/ICMP error packet"
            );
        }

        byte[] icmp =
                ipv4.payload();

        if (icmp.length < 8
                || IcmpErrorModel.internetChecksum(
                icmp
        ) != 0) {
            throw new IllegalArgumentException(
                    "Invalid ICMP error checksum/length"
            );
        }

        int type =
                icmp[0] & 0xFF;

        int code =
                icmp[1] & 0xFF;

        if (type != IcmpErrorModel.TIME_EXCEEDED
                && type != IcmpErrorModel.DESTINATION_UNREACHABLE) {
            throw new IllegalArgumentException(
                    "Unsupported ICMP error type "
                            + type
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
                ipv4.sourceAddress();

        logical.targetIp =
                ipv4.destinationAddress();

        logical.ttl =
                ipv4.ttl();

        logical.ipProtocol =
                1;

        logical.ipv4HeaderChecksum =
                ipv4.headerChecksum();

        logical.ipPacketLength =
                ipv4.totalLength();

        logical.applicationProtocol =
                "ICMP";

        logical.isResponse =
                true;

        logical.payload.putString(
                "type",
                type == IcmpErrorModel.TIME_EXCEEDED
                        ? "TIME_EXCEEDED"
                        : "DESTINATION_UNREACHABLE"
        );

        logical.payload.putInt(
                "icmp_type",
                type
        );

        logical.payload.putInt(
                "icmp_code",
                code
        );

        byte[] quote =
                java.util.Arrays.copyOfRange(
                        icmp,
                        8,
                        icmp.length
                );

        logical.payload.putByteArray(
                "icmp_quote",
                quote
        );

        populateQuotedIpv4Metadata(
                logical,
                quote
        );

        return logical;
    }

    private static void populateQuotedIpv4Metadata(
            OSINetworkPacket logical,
            byte[] quote
    ) {
        if (logical == null
                || quote == null
                || quote.length < 20) {
            return;
        }

        try {
            RawIpv4Packet quoted =
                    RawIpv4Decoder.decode(
                            quote
                    );

            logical.payload.putString(
                    "quoted_source_ip",
                    quoted.sourceAddress()
            );

            logical.payload.putString(
                    "quoted_target_ip",
                    quoted.destinationAddress()
            );

            logical.payload.putInt(
                    "quoted_protocol",
                    quoted.protocol()
            );

            logical.payload.putInt(
                    "quoted_ttl",
                    quoted.ttl()
            );

            logical.payload.putInt(
                    "quoted_identification",
                    quoted.identification()
            );

            byte[] quotedPayload =
                    quoted.payload();

            if (quotedPayload.length >= 4) {
                int sourcePort =
                        (
                                (
                                        quotedPayload[0]
                                                & 0xFF
                                )
                                        << 8
                        )
                                | (
                                quotedPayload[1]
                                        & 0xFF
                        );

                int targetPort =
                        (
                                (
                                        quotedPayload[2]
                                                & 0xFF
                                )
                                        << 8
                        )
                                | (
                                quotedPayload[3]
                                        & 0xFF
                        );

                logical.payload.putInt(
                        "quoted_source_port",
                        sourcePort
                );

                logical.payload.putInt(
                        "quoted_target_port",
                        targetPort
                );
            }
        } catch (IllegalArgumentException ignored) {
        }
    }

    private static byte[] buildQuote(
            OSINetworkPacket logical
    ) {
        String quotedSource =
                logical.payload.getString(
                        "quoted_source_ip"
                );

        String quotedTarget =
                logical.payload.getString(
                        "quoted_target_ip"
                );

        int quotedProtocol =
                logical.payload.getInt(
                        "quoted_protocol"
                );

        int quotedTtl =
                logical.payload.getInt(
                        "quoted_ttl"
                );

        Ipv4Header quotedHeader =
                new Ipv4Header(
                        quotedSource,
                        quotedTarget,
                        quotedProtocol,
                        quotedTtl <= 0
                                ? 1
                                : quotedTtl,
                        logical.payload.getInt(
                                "quoted_identification"
                        ) & 0xFFFF,
                        8,
                        false
                );

        byte[] header =
                quotedHeader.encode();

        byte[] quote =
                new byte[
                        header.length + 8
                ];

        System.arraycopy(
                header,
                0,
                quote,
                0,
                header.length
        );

        int sourcePort =
                logical.payload.getInt(
                        "quoted_source_port"
                );

        int targetPort =
                logical.payload.getInt(
                        "quoted_target_port"
                );

        quote[20] =
                (byte) (
                        sourcePort >>> 8
                );

        quote[21] =
                (byte) sourcePort;

        quote[22] =
                (byte) (
                        targetPort >>> 8
                );

        quote[23] =
                (byte) targetPort;

        return quote;
    }
}

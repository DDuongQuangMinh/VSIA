package com.k1ngtle.vsia.signality.engineering.wifi.link;

import com.k1ngtle.vsia.signality.engineering.wifi.tcp.TcpFlags;
import com.k1ngtle.vsia.signality.engineering.wifi.tcp.TcpSegment;
import com.k1ngtle.vsia.signality.engineering.wifi.tcp.live.TcpLivePacketCodec;
import com.k1ngtle.vsia.signality.engineering.wifi.tcp.options.TcpOptionSet;
import com.k1ngtle.vsia.signality.engineering.wifi.tcp.raw.RawIpv4TcpCodec;
import com.k1ngtle.vsia.signality.engineering.wifi.tcp.raw.RawPacketHex;
import com.k1ngtle.vsia.signality.engineering.wifi.tcp.raw.live.TcpRawLiveCarrierCodec;
import com.k1ngtle.vsia.signality.internet.OSINetworkPacket;
import net.minecraft.nbt.CompoundTag;

import java.util.Arrays;
import java.util.List;

public final class LlcSnapTestSuite {
    private static final String IPV4_SYN_HEX =
            "45000040BEEF400040068F76C000020AC6336414"
                    + "C35000501234567800000000B002FFFFEF2B0000"
                    + "020404B00402010303020101080A00003039000000000000";

    private static final String LLC_SNAP_PREFIX_HEX =
            "AAAA030000000800";

    private LlcSnapTestSuite() {
    }

    public static List<LlcSnapTestResult> runAll() {
        return List.of(
                exactRfc1042Prefix(),
                decodeIpv4EtherType(),
                liveCarrierRoundTrip(),
                wrongEtherTypeRejected(),
                malformedSnapRejected(),
                legacyCarrierCompatibility(),
                simulationUnaffected()
        );
    }

    private static LlcSnapTestResult exactRfc1042Prefix() {
        byte[] ipv4 =
                RawPacketHex.decode(
                        IPV4_SYN_HEX
                );

        byte[] msdu =
                LlcSnapCodec.encodeRfc1042(
                        EtherType.IPV4,
                        ipv4
                );

        String actual =
                RawPacketHex.encode(
                        msdu
                );

        return result(
                "wifi-w197-exact-rfc1042-prefix",
                actual.equals(
                        LLC_SNAP_PREFIX_HEX
                                + IPV4_SYN_HEX
                ),
                "RFC 1042-style IPv4 MSDU must begin AA AA 03 00 00 00 08 00 before the raw IPv4 packet"
        );
    }

    private static LlcSnapTestResult decodeIpv4EtherType() {
        byte[] msdu =
                RawPacketHex.decode(
                        LLC_SNAP_PREFIX_HEX
                                + IPV4_SYN_HEX
                );

        LlcSnapFrame frame =
                LlcSnapCodec.decodeRfc1042(
                        msdu
                );

        return result(
                "wifi-w197-decode-ipv4",
                frame.isRfc1042Snap()
                        && frame.etherType()
                        == EtherType.IPV4.value()
                        && frame.etherTypeKind()
                        == EtherType.IPV4
                        && Arrays.equals(
                        frame.payload(),
                        RawPacketHex.decode(
                                IPV4_SYN_HEX
                        )
                ),
                "Decoder must recover RFC1042 OUI, EtherType 0x0800, and the exact IPv4 payload"
        );
    }

    private static LlcSnapTestResult liveCarrierRoundTrip() {
        OSINetworkPacket logical =
                TcpLivePacketCodec.encode(
                        "tcp-w197",
                        "AA:BB:CC:00:00:01",
                        "AA:BB:CC:00:00:02",
                        "192.0.2.10",
                        "198.51.100.20",
                        new TcpSegment(
                                50000,
                                80,
                                0x12345678L,
                                0L,
                                TcpFlags.synOnly(),
                                65535,
                                0,
                                1_000_000L,
                                false
                        ),
                        new byte[0],
                        0,
                        0,
                        TcpOptionSet.synOffer(
                                1200,
                                2,
                                true,
                                1000L
                        )
                );

        CompoundTag carrier =
                TcpRawLiveCarrierCodec.encode(
                        logical
                );

        byte[] msdu =
                carrier.getByteArray(
                        TcpRawLiveCarrierCodec.RAW_MSDU_KEY
                );

        LlcSnapFrame frame =
                LlcSnapCodec.decodeRfc1042(
                        msdu
                );

        OSINetworkPacket decoded =
                TcpRawLiveCarrierCodec.decode(
                        carrier
                );

        TcpSegment tcp =
                TcpLivePacketCodec.decode(
                        decoded
                );

        return result(
                "wifi-w197-live-carrier",
                TcpRawLiveCarrierCodec.CONTROL_VALUE.equals(
                        carrier.getString(
                                TcpRawLiveCarrierCodec.CONTROL_KEY
                        )
                )
                        && "RFC1042_LLC_SNAP".equals(
                        carrier.getString(
                                "network_framing"
                        )
                )
                        && frame.etherType()
                        == 0x0800
                        && RawIpv4TcpCodec.decode(
                        frame.payload()
                ).valid()
                        && tcp.flags().syn()
                        && tcp.sequenceNumber()
                        == 0x12345678L,
                "W1.9.6 live raw carrier must now place the literal IPv4/TCP packet behind LLC/SNAP EtherType framing"
        );
    }

    private static LlcSnapTestResult wrongEtherTypeRejected() {
        OSINetworkPacket logical =
                sampleLogical();

        CompoundTag carrier =
                TcpRawLiveCarrierCodec.encode(
                        logical
                );

        byte[] msdu =
                carrier.getByteArray(
                        TcpRawLiveCarrierCodec.RAW_MSDU_KEY
                );

        msdu[6] =
                (
                        byte
                ) 0x86;

        msdu[7] =
                (
                        byte
                ) 0xDD;

        carrier.putByteArray(
                TcpRawLiveCarrierCodec.RAW_MSDU_KEY,
                msdu
        );

        boolean rejected =
                false;

        try {
            TcpRawLiveCarrierCodec.decode(
                    carrier
            );
        } catch (IllegalArgumentException expected) {
            rejected =
                    true;
        }

        return result(
                "wifi-w197-ethertype-reject",
                rejected,
                "TCP/IPv4 carrier must reject LLC/SNAP payloads whose EtherType is not 0x0800"
        );
    }

    private static LlcSnapTestResult malformedSnapRejected() {
        byte[] malformed =
                RawPacketHex.decode(
                        "ABAA030000000800"
                                + IPV4_SYN_HEX
                );

        boolean rejected =
                false;

        try {
            LlcSnapCodec.decodeRfc1042(
                    malformed
            );
        } catch (IllegalArgumentException expected) {
            rejected =
                    true;
        }

        return result(
                "wifi-w197-malformed-snap",
                rejected,
                "RFC1042 decoder must reject an invalid SNAP DSAP/SSAP/Control/OUI header"
        );
    }

    private static LlcSnapTestResult legacyCarrierCompatibility() {
        OSINetworkPacket logical =
                sampleLogical();

        CompoundTag modern =
                TcpRawLiveCarrierCodec.encode(
                        logical
                );

        LlcSnapFrame frame =
                LlcSnapCodec.decodeRfc1042(
                        modern.getByteArray(
                                TcpRawLiveCarrierCodec.RAW_MSDU_KEY
                        )
                );

        CompoundTag legacy =
                new CompoundTag();

        legacy.putString(
                TcpRawLiveCarrierCodec.CONTROL_KEY,
                TcpRawLiveCarrierCodec.LEGACY_CONTROL_VALUE
        );

        legacy.putByteArray(
                TcpRawLiveCarrierCodec.LEGACY_RAW_PACKET_KEY,
                frame.payload()
        );

        legacy.putString(
                "src_mac",
                logical.sourceMac
        );

        legacy.putString(
                "dst_mac",
                logical.targetMac
        );

        legacy.putString(
                "tcp_session_id",
                logical.sessionId
        );

        OSINetworkPacket decoded =
                TcpRawLiveCarrierCodec.decode(
                        legacy
                );

        return result(
                "wifi-w197-w196-compat",
                TcpRawLiveCarrierCodec.isRawCarrier(
                        legacy
                )
                        && decoded.sourceIp.equals(
                        logical.sourceIp
                )
                        && decoded.targetIp.equals(
                        logical.targetIp
                )
                        && TcpLivePacketCodec.decode(
                        decoded
                ).sequenceNumber()
                        == TcpLivePacketCodec.decode(
                        logical
                ).sequenceNumber(),
                "Decoder must accept the W1.9.6 IPV4_TCP_V1 carrier during migration"
        );
    }

    private static LlcSnapTestResult simulationUnaffected() {
        CompoundTag simulation =
                new CompoundTag();

        simulation.put(
                "osi_packet",
                new CompoundTag()
        );

        return result(
                "wifi-w197-simulation-unaffected",
                !TcpRawLiveCarrierCodec.isRawCarrier(
                        simulation
                ),
                "The normal SIMULATION osi_packet path must remain distinct from LLC/SNAP conformance traffic"
        );
    }

    private static OSINetworkPacket sampleLogical() {
        return TcpLivePacketCodec.encode(
                "tcp-w197-sample",
                "AA:BB:CC:00:00:01",
                "AA:BB:CC:00:00:02",
                "192.0.2.10",
                "198.51.100.20",
                new TcpSegment(
                        50000,
                        80,
                        1000L,
                        5000L,
                        TcpFlags.ackOnly(),
                        32768,
                        0,
                        2_000_000L,
                        false
                ),
                new byte[0],
                0,
                0,
                TcpOptionSet.none()
        );
    }

    private static LlcSnapTestResult result(
            String id,
            boolean passed,
            String detail
    ) {
        return new LlcSnapTestResult(
                id,
                passed,
                detail
        );
    }
}

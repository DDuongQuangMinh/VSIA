package com.k1ngtle.vsia.signality.engineering.wifi.arp;

import com.k1ngtle.vsia.signality.engineering.wifi.arp.live.ArpRawLiveCarrierCodec;
import com.k1ngtle.vsia.signality.engineering.wifi.link.EtherType;
import com.k1ngtle.vsia.signality.engineering.wifi.link.LlcSnapCodec;
import com.k1ngtle.vsia.signality.engineering.wifi.link.LlcSnapFrame;
import com.k1ngtle.vsia.signality.engineering.wifi.tcp.raw.RawPacketHex;
import com.k1ngtle.vsia.signality.internet.OSINetworkPacket;
import net.minecraft.nbt.CompoundTag;

import java.util.List;

public final class ArpRawTestSuite {
    private static final String REQUEST_HEX =
            "0001080006040001"
                    + "001122334455"
                    + "C000020A"
                    + "000000000000"
                    + "C6336414";

    private ArpRawTestSuite() {
    }

    public static List<ArpRawTestResult> runAll() {
        return List.of(
                exactRequestVector(),
                requestDecode(),
                replyRoundTrip(),
                llcSnapArpFraming(),
                liveCarrierRoundTrip(),
                wrongEtherTypeRejected(),
                malformedAddressFormatRejected(),
                macNormalization(),
                carrierRequestMacPreserved(),
                carrierReplyMacPreserved()
        );
    }

    private static ArpRawTestResult exactRequestVector() {
        ArpPacket request =
                ArpCodec.request(
                        "00:11:22:33:44:55",
                        "192.0.2.10",
                        "198.51.100.20"
                );

        return result(
                "wifi-w1101-arp-request-kat",
                REQUEST_HEX.equals(
                        RawPacketHex.encode(
                                ArpCodec.encode(
                                        request
                                )
                        )
                ),
                "Ethernet/IPv4 ARP request must reproduce the pinned 28-byte RFC 826 field layout exactly"
        );
    }

    private static ArpRawTestResult requestDecode() {
        ArpPacket decoded =
                ArpCodec.decode(
                        RawPacketHex.decode(
                                REQUEST_HEX
                        )
                );

        return result(
                "wifi-w1101-arp-request-decode",
                decoded.operation()
                        == ArpOperation.REQUEST
                        && decoded.hardwareType() == 1
                        && decoded.protocolType() == 0x0800
                        && decoded.hardwareLength() == 6
                        && decoded.protocolLength() == 4
                        && "00:11:22:33:44:55".equals(
                        decoded.senderMac()
                )
                        && "192.0.2.10".equals(
                        decoded.senderIp()
                )
                        && MacAddressBytes.zero()
                        .equals(
                                decoded.targetMac()
                        )
                        && "198.51.100.20".equals(
                        decoded.targetIp()
                ),
                "ARP decoder must recover hardware/protocol types, operation, and sender/target addresses"
        );
    }

    private static ArpRawTestResult replyRoundTrip() {
        ArpPacket reply =
                ArpCodec.reply(
                        "66:77:88:99:AA:BB",
                        "198.51.100.20",
                        "00:11:22:33:44:55",
                        "192.0.2.10"
                );

        ArpPacket decoded =
                ArpCodec.decode(
                        ArpCodec.encode(
                                reply
                        )
                );

        return result(
                "wifi-w1101-arp-reply-roundtrip",
                decoded.operation()
                        == ArpOperation.REPLY
                        && decoded.senderMac()
                        .equals(
                                "66:77:88:99:AA:BB"
                        )
                        && decoded.targetMac()
                        .equals(
                                "00:11:22:33:44:55"
                        )
                        && decoded.senderIp()
                        .equals(
                                "198.51.100.20"
                        )
                        && decoded.targetIp()
                        .equals(
                                "192.0.2.10"
                        ),
                "ARP reply must preserve both hardware and protocol address pairs"
        );
    }

    private static ArpRawTestResult llcSnapArpFraming() {
        byte[] msdu =
                LlcSnapCodec.encodeRfc1042(
                        EtherType.ARP,
                        RawPacketHex.decode(
                                REQUEST_HEX
                        )
                );

        String exact =
                "AAAA030000000806"
                        + REQUEST_HEX;

        LlcSnapFrame decoded =
                LlcSnapCodec.decodeRfc1042(
                        msdu
                );

        return result(
                "wifi-w1101-llc-snap-arp",
                exact.equals(
                        RawPacketHex.encode(
                                msdu
                        )
                )
                        && decoded.etherType()
                        == 0x0806
                        && decoded.payload().length
                        == 28,
                "ARP must be carried behind AA AA 03 00 00 00 08 06 LLC/SNAP framing"
        );
    }

    private static ArpRawTestResult liveCarrierRoundTrip() {
        OSINetworkPacket request =
                logicalRequest();

        CompoundTag body =
                ArpRawLiveCarrierCodec.encode(
                        request
                );

        OSINetworkPacket decoded =
                ArpRawLiveCarrierCodec.decode(
                        body
                );

        LlcSnapFrame frame =
                LlcSnapCodec.decodeRfc1042(
                        body.getByteArray(
                                ArpRawLiveCarrierCodec.RAW_MSDU_KEY
                        )
                );

        return result(
                "wifi-w1101-live-arp-carrier",
                ArpRawLiveCarrierCodec.isRawArpCarrier(
                        body
                )
                        && frame.etherType()
                        == 0x0806
                        && decoded.applicationProtocol
                        .equals(
                                "ARP"
                        )
                        && decoded.targetMac
                        .equals(
                                "FF:FF:FF:FF:FF:FF"
                        )
                        && decoded.payload.getString(
                        "operation"
                ).equals(
                        "REQUEST"
                )
                        && decoded.payload.getString(
                        "sender_ip"
                ).equals(
                        "192.0.2.10"
                )
                        && decoded.payload.getString(
                        "target_ip"
                ).equals(
                        "198.51.100.20"
                ),
                "Live conformance ARP must cross the Wi-Fi carrier as LLC/SNAP EtherType 0x0806 and reconstruct the existing neighbor-engine input"
        );
    }

    private static ArpRawTestResult wrongEtherTypeRejected() {
        CompoundTag body =
                ArpRawLiveCarrierCodec.encode(
                        logicalRequest()
                );

        byte[] msdu =
                body.getByteArray(
                        ArpRawLiveCarrierCodec.RAW_MSDU_KEY
                );

        msdu[6] =
                0x08;

        msdu[7] =
                0x00;

        body.putByteArray(
                ArpRawLiveCarrierCodec.RAW_MSDU_KEY,
                msdu
        );

        boolean rejected =
                false;

        try {
            ArpRawLiveCarrierCodec.decode(
                    body
            );
        } catch (IllegalArgumentException expected) {
            rejected =
                    true;
        }

        return result(
                "wifi-w1101-arp-ethertype-reject",
                rejected,
                "Raw ARP carrier must reject an LLC/SNAP frame whose EtherType is not 0x0806"
        );
    }

    private static ArpRawTestResult malformedAddressFormatRejected() {
        byte[] malformed =
                RawPacketHex.decode(
                        REQUEST_HEX
                );

        malformed[4] =
                5;

        boolean rejected =
                false;

        try {
            ArpCodec.decode(
                    malformed
            );
        } catch (IllegalArgumentException expected) {
            rejected =
                    true;
        }

        return result(
                "wifi-w1101-arp-format-reject",
                rejected,
                "Ethernet/IPv4 ARP decoder must reject HLEN/PLEN/type combinations outside the supported 6/4 format"
        );
    }

    private static ArpRawTestResult macNormalization() {
        byte[] compact =
                MacAddressBytes.parse(
                        "001122334455"
                );

        byte[] hyphen =
                MacAddressBytes.parse(
                        "00-11-22-33-44-55"
                );

        return result(
                "wifi-w1101-mac-normalization",
                java.util.Arrays.equals(
                        compact,
                        hyphen
                )
                        && MacAddressBytes.format(
                        compact,
                        0
                ).equals(
                        "00:11:22:33:44:55"
                ),
                "VSIA compact MAC IDs and conventional colon/hyphen MAC notation must map to the same 48-bit address bytes"
        );
    }


    private static ArpRawTestResult carrierRequestMacPreserved() {
        OSINetworkPacket request =
                logicalRequest();

        request.sourceMac =
                "3ade93b46caf";

        request.payload.putString(
                "sender_mac",
                request.sourceMac
        );

        CompoundTag body =
                ArpRawLiveCarrierCodec.encode(
                        request
                );

        OSINetworkPacket decoded =
                ArpRawLiveCarrierCodec.decode(
                        body
                );

        return result(
                "wifi-w1104-arp-request-link-mac",
                "3ade93b46caf".equals(
                        decoded.sourceMac
                )
                        && "3ade93b46caf".equals(
                        decoded.payload.getString(
                                "sender_mac"
                        )
                )
                        && "3A:DE:93:B4:6C:AF".equals(
                        decoded.payload.getString(
                                "arp_sender_hardware_mac"
                        )
                ),
                "ARP decode must preserve the compact VSIA link MAC while retaining the protocol-formatted SHA separately"
        );
    }

    private static ArpRawTestResult carrierReplyMacPreserved() {
        OSINetworkPacket reply =
                new OSINetworkPacket();

        reply.sourceMac =
                "c72ec34fc58c";

        reply.targetMac =
                "3ade93b46caf";

        reply.sourceIp =
                "192.168.1.2";

        reply.targetIp =
                "192.168.1.101";

        reply.applicationProtocol =
                "ARP";

        reply.isResponse =
                true;

        reply.payload.putString(
                "operation",
                "REPLY"
        );

        reply.payload.putString(
                "sender_ip",
                reply.sourceIp
        );

        reply.payload.putString(
                "sender_mac",
                reply.sourceMac
        );

        reply.payload.putString(
                "target_ip",
                reply.targetIp
        );

        CompoundTag body =
                ArpRawLiveCarrierCodec.encode(
                        reply
                );

        OSINetworkPacket decoded =
                ArpRawLiveCarrierCodec.decode(
                        body
                );

        return result(
                "wifi-w1104-arp-reply-link-mac",
                "c72ec34fc58c".equals(
                        decoded.sourceMac
                )
                        && "3ade93b46caf".equals(
                        decoded.targetMac
                )
                        && "c72ec34fc58c".equals(
                        decoded.payload.getString(
                                "sender_mac"
                        )
                )
                        && "C7:2E:C3:4F:C5:8C".equals(
                        decoded.payload.getString(
                                "arp_sender_hardware_mac"
                        )
                ),
                "ARP reply must route to the actual compact station MAC rather than the colon-formatted ARP THA"
        );
    }

    private static OSINetworkPacket logicalRequest() {
        OSINetworkPacket packet =
                new OSINetworkPacket();

        packet.sourceMac =
                "001122334455";

        packet.targetMac =
                "FF:FF:FF:FF:FF:FF";

        packet.sourceIp =
                "192.0.2.10";

        packet.targetIp =
                "198.51.100.20";

        packet.applicationProtocol =
                "ARP";

        packet.payload.putString(
                "operation",
                "REQUEST"
        );

        packet.payload.putString(
                "sender_ip",
                packet.sourceIp
        );

        packet.payload.putString(
                "sender_mac",
                packet.sourceMac
        );

        packet.payload.putString(
                "target_ip",
                packet.targetIp
        );

        packet.payload.putLong(
                "w1_request_id",
                42L
        );

        packet.payload.putLong(
                "sent_us",
                1000L
        );

        return packet;
    }

    private static ArpRawTestResult result(
            String id,
            boolean passed,
            String detail
    ) {
        return new ArpRawTestResult(
                id,
                passed,
                detail
        );
    }
}

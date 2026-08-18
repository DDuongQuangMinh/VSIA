package com.k1ngtle.vsia.signality.engineering.wifi.dhcp.live;

import com.k1ngtle.vsia.signality.engineering.ExecutionMode;
import com.k1ngtle.vsia.signality.engineering.wifi.arp.MacAddressBytes;
import com.k1ngtle.vsia.signality.engineering.wifi.dhcp.DhcpCodec;
import com.k1ngtle.vsia.signality.engineering.wifi.dhcp.DhcpMessageType;
import com.k1ngtle.vsia.signality.engineering.wifi.dhcp.DhcpOption;
import com.k1ngtle.vsia.signality.engineering.wifi.dhcp.DhcpPacket;
import com.k1ngtle.vsia.signality.engineering.wifi.dhcp.RawUdpCodec;
import com.k1ngtle.vsia.signality.engineering.wifi.dhcp.RawUdpPacket;
import com.k1ngtle.vsia.signality.engineering.wifi.ip.Ipv4Header;
import com.k1ngtle.vsia.signality.engineering.wifi.link.EtherType;
import com.k1ngtle.vsia.signality.engineering.wifi.link.LlcSnapCodec;
import com.k1ngtle.vsia.signality.engineering.wifi.link.LlcSnapFrame;
import com.k1ngtle.vsia.signality.engineering.wifi.tcp.raw.RawIpv4Decoder;
import com.k1ngtle.vsia.signality.engineering.wifi.tcp.raw.RawIpv4Packet;
import com.k1ngtle.vsia.signality.internet.OSINetworkPacket;
import net.minecraft.nbt.CompoundTag;

import java.util.Map;

public final class DhcpRawLiveCarrierCodec {
    public static final String CONTROL_KEY =
            "vsia_raw_network_control";

    public static final String CONTROL_VALUE =
            "LLC_SNAP_DHCP_V1";

    public static final String RAW_MSDU_KEY =
            "raw_llc_snap_msdu";

    private DhcpRawLiveCarrierCodec() {
    }

    public static boolean isRawDhcpCarrier(
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

    public static CompoundTag encode(
            OSINetworkPacket logical
    ) {
        if (logical == null
                || !"DHCP".equalsIgnoreCase(
                logical.applicationProtocol
        )) {
            throw new IllegalArgumentException(
                    "Raw DHCP carrier requires DHCP"
            );
        }

        DhcpMessageType type =
                DhcpMessageType.valueOf(
                        logical.payload
                                .getString(
                                        "type"
                                )
                                .toUpperCase(
                                        java.util.Locale.ROOT
                                )
                );

        long xid =
                logical.payload.contains(
                        "xid"
                )
                        ? Integer.toUnsignedLong(
                        logical.payload.getInt(
                                "xid"
                        )
                )
                        : 0L;

        boolean request =
                type == DhcpMessageType.DISCOVER
                        || type == DhcpMessageType.REQUEST
                        || type == DhcpMessageType.RELEASE
                        || type == DhcpMessageType.INFORM;

        String clientMac =
                request
                        ? logical.sourceMac
                        : logical.targetMac;

        String serverIdentifier =
                logical.payload.contains(
                        "server_identifier"
                )
                        ? logical.payload.getString(
                        "server_identifier"
                )
                        : request
                        ? "0.0.0.0"
                        : logical.sourceIp;

        Map<Integer, byte[]> options =
                switch (type) {
                    case DISCOVER ->
                            DhcpCodec.discoverOptions(
                                    clientMac
                            );

                    case REQUEST ->
                            DhcpCodec.requestOptions(
                                    clientMac,
                                    logical.payload.getString(
                                            "requested_ip"
                                    ),
                                    serverIdentifier
                            );

                    case OFFER, ACK, NAK ->
                            DhcpCodec.replyOptions(
                                    type,
                                    logical.payload.getString(
                                            "subnet_mask"
                                    ),
                                    logical.payload.getString(
                                            "router_ip"
                                    ),
                                    logical.payload.getString(
                                            "dns_server"
                                    ),
                                    logical.payload.getInt(
                                            "lease_seconds"
                                    ),
                                    serverIdentifier
                            );

                    default ->
                            Map.of(
                                    DhcpOption.MESSAGE_TYPE,
                                    new byte[] {
                                            (byte) type.code()
                                    }
                            );
                };

        DhcpPacket dhcp =
                new DhcpPacket(
                        request
                                ? DhcpPacket.BOOTREQUEST
                                : DhcpPacket.BOOTREPLY,
                        DhcpPacket.HTYPE_ETHERNET,
                        DhcpPacket.HLEN_ETHERNET,
                        0,
                        xid,
                        0,
                        DhcpPacket.BROADCAST_FLAG,
                        "0.0.0.0",
                        request
                                ? "0.0.0.0"
                                : logical.payload.getString(
                                "assigned_ip"
                        ),
                        request
                                ? "0.0.0.0"
                                : serverIdentifier,
                        "0.0.0.0",
                        clientMac,
                        options
                );

        byte[] dhcpPayload =
                DhcpCodec.encode(
                        dhcp
                );

        String sourceIp =
                request
                        ? "0.0.0.0"
                        : logical.sourceIp;

        String destinationIp =
                "255.255.255.255";

        int sourcePort =
                request
                        ? 68
                        : 67;

        int destinationPort =
                request
                        ? 67
                        : 68;

        byte[] udp =
                RawUdpCodec.encode(
                        sourceIp,
                        destinationIp,
                        sourcePort,
                        destinationPort,
                        dhcpPayload
                );

        Ipv4Header ipv4 =
                new Ipv4Header(
                        sourceIp,
                        destinationIp,
                        17,
                        64,
                        (
                                int
                        ) (
                                xid
                                        & 0xFFFFL
                        ),
                        udp.length,
                        true
                );

        byte[] ipHeader =
                ipv4.encode();

        byte[] rawIpv4 =
                new byte[
                        ipHeader.length
                                + udp.length
                ];

        System.arraycopy(
                ipHeader,
                0,
                rawIpv4,
                0,
                ipHeader.length
        );

        System.arraycopy(
                udp,
                0,
                rawIpv4,
                ipHeader.length,
                udp.length
        );

        byte[] msdu =
                LlcSnapCodec.encodeRfc1042(
                        EtherType.IPV4,
                        rawIpv4
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

        return body;
    }

    public static OSINetworkPacket decode(
            CompoundTag body
    ) {
        if (!isRawDhcpCarrier(
                body
        )) {
            throw new IllegalArgumentException(
                    "Not a VSIA raw DHCP carrier"
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
                    "DHCP carrier requires IPv4 EtherType"
            );
        }

        RawIpv4Packet ipv4 =
                RawIpv4Decoder.decode(
                        frame.payload()
                );

        if (!ipv4.checksumValid()
                || ipv4.protocol() != 17) {
            throw new IllegalArgumentException(
                    "Invalid DHCP IPv4/UDP packet"
            );
        }

        RawUdpPacket udp =
                RawUdpCodec.decode(
                        ipv4.sourceAddress(),
                        ipv4.destinationAddress(),
                        ipv4.payload()
                );

        if (!udp.checksumValid()
                || !(
                (
                        udp.sourcePort() == 68
                                && udp.destinationPort() == 67
                )
                        || (
                        udp.sourcePort() == 67
                                && udp.destinationPort() == 68
                )
        )) {
            throw new IllegalArgumentException(
                    "Invalid DHCP UDP ports/checksum"
            );
        }

        DhcpPacket dhcp =
                DhcpCodec.decode(
                        udp.payload()
                );

        DhcpMessageType type =
                dhcp.messageType();

        boolean request =
                dhcp.op()
                        == DhcpPacket.BOOTREQUEST;

        OSINetworkPacket logical =
                new OSINetworkPacket();

        String carrierSourceMac =
                body.getString(
                        "src_mac"
                );

        String carrierTargetMac =
                body.getString(
                        "dst_mac"
                );

        logical.sourceMac =
                carrierSourceMac == null
                        || carrierSourceMac.isBlank()
                        ? dhcp.clientMac()
                        : carrierSourceMac;

        logical.targetMac =
                carrierTargetMac == null
                        || carrierTargetMac.isBlank()
                        ? request
                        ? "FF:FF:FF:FF:FF:FF"
                        : dhcp.clientMac()
                        : carrierTargetMac;

        logical.sourceIp =
                ipv4.sourceAddress();

        logical.targetIp =
                ipv4.destinationAddress();

        logical.sourcePort =
                udp.sourcePort();

        logical.targetPort =
                udp.destinationPort();

        logical.ipProtocol =
                17;

        logical.applicationProtocol =
                "DHCP";

        logical.isResponse =
                !request;

        logical.payload.putString(
                "type",
                type.name()
        );

        logical.payload.putString(
                "client_hardware_mac",
                dhcp.clientMac()
        );

        logical.payload.putString(
                "carrier_source_mac",
                logical.sourceMac
        );

        logical.payload.putString(
                "carrier_target_mac",
                logical.targetMac
        );

        logical.payload.putInt(
                "xid",
                (
                        int
                ) dhcp.transactionId()
        );

        logical.payload.putString(
                "assigned_ip",
                dhcp.yourIp()
        );

        logical.payload.putString(
                "server_identifier",
                dhcp.optionIpv4(
                        DhcpOption.SERVER_IDENTIFIER,
                        dhcp.serverIp()
                )
        );

        logical.payload.putString(
                "requested_ip",
                dhcp.optionIpv4(
                        DhcpOption.REQUESTED_IP,
                        "0.0.0.0"
                )
        );

        logical.payload.putString(
                "subnet_mask",
                dhcp.optionIpv4(
                        DhcpOption.SUBNET_MASK,
                        ""
                )
        );

        logical.payload.putString(
                "router_ip",
                dhcp.optionIpv4(
                        DhcpOption.ROUTER,
                        ""
                )
        );

        logical.payload.putString(
                "dns_server",
                dhcp.optionIpv4(
                        DhcpOption.DNS,
                        ""
                )
        );

        logical.payload.putInt(
                "lease_seconds",
                (
                        int
                ) dhcp.optionUnsigned32(
                        DhcpOption.LEASE_TIME,
                        0L
                )
        );

        return logical;
    }
}

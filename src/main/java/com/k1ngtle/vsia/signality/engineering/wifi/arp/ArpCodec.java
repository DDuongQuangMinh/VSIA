package com.k1ngtle.vsia.signality.engineering.wifi.arp;

import com.k1ngtle.vsia.signality.engineering.wifi.ip.Ipv4Address;

public final class ArpCodec {
    public static final int ETHERNET_IPV4_ARP_BYTES =
            28;

    private ArpCodec() {
    }

    public static byte[] encode(
            ArpPacket packet
    ) {
        if (packet == null
                || !packet.ethernetIpv4()) {
            throw new IllegalArgumentException(
                    "Only Ethernet/IPv4 ARP is supported"
            );
        }

        byte[] senderMac =
                MacAddressBytes.parse(
                        packet.senderMac()
                );

        byte[] senderIp =
                Ipv4Address.parse(
                        packet.senderIp()
                );

        byte[] targetMac =
                MacAddressBytes.parse(
                        packet.targetMac()
                );

        byte[] targetIp =
                Ipv4Address.parse(
                        packet.targetIp()
                );

        byte[] out =
                new byte[
                        ETHERNET_IPV4_ARP_BYTES
                ];

        put16(
                out,
                0,
                packet.hardwareType()
        );

        put16(
                out,
                2,
                packet.protocolType()
        );

        out[4] =
                (
                        byte
                ) packet.hardwareLength();

        out[5] =
                (
                        byte
                ) packet.protocolLength();

        put16(
                out,
                6,
                packet.operation()
                        .code()
        );

        System.arraycopy(
                senderMac,
                0,
                out,
                8,
                6
        );

        System.arraycopy(
                senderIp,
                0,
                out,
                14,
                4
        );

        System.arraycopy(
                targetMac,
                0,
                out,
                18,
                6
        );

        System.arraycopy(
                targetIp,
                0,
                out,
                24,
                4
        );

        return out;
    }

    public static ArpPacket decode(
            byte[] bytes
    ) {
        if (bytes == null
                || bytes.length
                != ETHERNET_IPV4_ARP_BYTES) {
            throw new IllegalArgumentException(
                    "Ethernet/IPv4 ARP payload must be exactly 28 bytes"
            );
        }

        int hardwareType =
                read16(
                        bytes,
                        0
                );

        int protocolType =
                read16(
                        bytes,
                        2
                );

        int hardwareLength =
                bytes[4]
                        & 0xFF;

        int protocolLength =
                bytes[5]
                        & 0xFF;

        if (hardwareType
                != ArpPacket.ETHERNET_HARDWARE_TYPE
                || protocolType
                != ArpPacket.IPV4_PROTOCOL_TYPE
                || hardwareLength
                != ArpPacket.ETHERNET_ADDRESS_BYTES
                || protocolLength
                != ArpPacket.IPV4_ADDRESS_BYTES) {
            throw new IllegalArgumentException(
                    "Unsupported ARP address format"
            );
        }

        return new ArpPacket(
                hardwareType,
                protocolType,
                hardwareLength,
                protocolLength,
                ArpOperation.fromCode(
                        read16(
                                bytes,
                                6
                        )
                ),
                MacAddressBytes.format(
                        bytes,
                        8
                ),
                ipv4(
                        bytes,
                        14
                ),
                MacAddressBytes.format(
                        bytes,
                        18
                ),
                ipv4(
                        bytes,
                        24
                )
        );
    }

    public static ArpPacket request(
            String senderMac,
            String senderIp,
            String targetIp
    ) {
        return new ArpPacket(
                ArpPacket.ETHERNET_HARDWARE_TYPE,
                ArpPacket.IPV4_PROTOCOL_TYPE,
                ArpPacket.ETHERNET_ADDRESS_BYTES,
                ArpPacket.IPV4_ADDRESS_BYTES,
                ArpOperation.REQUEST,
                senderMac,
                senderIp,
                MacAddressBytes.zero(),
                targetIp
        );
    }

    public static ArpPacket reply(
            String senderMac,
            String senderIp,
            String targetMac,
            String targetIp
    ) {
        return new ArpPacket(
                ArpPacket.ETHERNET_HARDWARE_TYPE,
                ArpPacket.IPV4_PROTOCOL_TYPE,
                ArpPacket.ETHERNET_ADDRESS_BYTES,
                ArpPacket.IPV4_ADDRESS_BYTES,
                ArpOperation.REPLY,
                senderMac,
                senderIp,
                targetMac,
                targetIp
        );
    }

    private static void put16(
            byte[] bytes,
            int offset,
            int value
    ) {
        bytes[offset] =
                (
                        byte
                ) (
                value >>> 8
        );

        bytes[offset + 1] =
                (
                        byte
                ) value;
    }

    private static int read16(
            byte[] bytes,
            int offset
    ) {
        return (
                (
                        bytes[offset]
                                & 0xFF
                )
                        << 8
        )
                | (
                bytes[offset + 1]
                        & 0xFF
        );
    }

    private static String ipv4(
            byte[] bytes,
            int offset
    ) {
        return (
                bytes[offset]
                        & 0xFF
        )
                + "."
                + (
                bytes[offset + 1]
                        & 0xFF
        )
                + "."
                + (
                bytes[offset + 2]
                        & 0xFF
        )
                + "."
                + (
                bytes[offset + 3]
                        & 0xFF
        );
    }
}

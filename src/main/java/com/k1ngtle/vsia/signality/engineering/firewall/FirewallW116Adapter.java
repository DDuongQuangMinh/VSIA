package com.k1ngtle.vsia.signality.engineering.firewall;

import com.k1ngtle.vsia.signality.internet.OSINetworkPacket;

public final class FirewallW116Adapter {
    private FirewallW116Adapter() {
    }

    public static FirewallPacketView packetView(
            OSINetworkPacket packet,
            String ingressPortName,
            String egressPortName
    ) {
        IpFamily family =
                IpPrefixMatcher.family(
                        packet.sourceIp
                );

        String protocol =
                packet.applicationProtocol == null
                        || packet.applicationProtocol.isBlank()
                        ? protocolFromNumber(packet.ipProtocol)
                        : packet.applicationProtocol;

        boolean tcp =
                "TCP".equalsIgnoreCase(protocol);

        boolean syn =
                tcp && booleanPayload(
                        packet,
                        "tcp_syn"
                );

        boolean ack =
                tcp && booleanPayload(
                        packet,
                        "tcp_ack"
                );

        boolean fin =
                tcp && booleanPayload(
                        packet,
                        "tcp_fin"
                );

        boolean rst =
                tcp && booleanPayload(
                        packet,
                        "tcp_rst"
                );

        int fragmentId =
                intPayload(
                        packet,
                        "ipv4_identification",
                        intPayload(
                                packet,
                                "fragment_identification",
                                0
                        )
                );

        int fragmentOffset =
                intPayload(
                        packet,
                        "fragment_offset",
                        0
                );

        boolean mf =
                booleanPayload(
                        packet,
                        "more_fragments"
                );

        boolean icmpError =
                booleanPayload(
                        packet,
                        "icmp_error"
                )
                        || isIcmpError(
                        protocol,
                        intPayload(
                                packet,
                                "icmp_type",
                                -1
                        )
                );

        return new FirewallPacketView(
                family,
                protocol,
                packet.sourceIp,
                clampPort(packet.sourcePort),
                packet.targetIp,
                clampPort(packet.targetPort),
                normalizeInterface(
                        ingressPortName
                ),
                normalizeInterface(
                        egressPortName
                ),
                syn,
                ack,
                fin,
                rst,
                icmpError,
                null,
                Math.max(0, fragmentId),
                Math.max(0, fragmentOffset),
                mf
        );
    }

    public static void applyNat(
            OSINetworkPacket packet,
            FirewallDecision decision
    ) {
        Nat44Mapping mapping =
                decision.natMapping();

        if (mapping == null) {
            return;
        }

        if (decision.reverseNat()) {
            packet.targetIp =
                    mapping.insideLocalIp();

            packet.targetPort =
                    mapping.insideLocalPort();
        } else {
            packet.sourceIp =
                    mapping.insideGlobalIp();

            packet.sourcePort =
                    mapping.insideGlobalPort();
        }

        if (packet.payload != null) {
            packet.payload.putBoolean(
                    "w116_nat44",
                    true
            );

            packet.payload.putString(
                    "w116_nat_original_inside",
                    mapping.insideLocalIp()
                            + ":"
                            + mapping.insideLocalPort()
            );

            packet.payload.putString(
                    "w116_nat_inside_global",
                    mapping.insideGlobalIp()
                            + ":"
                            + mapping.insideGlobalPort()
            );
        }
    }

    private static boolean booleanPayload(
            OSINetworkPacket packet,
            String key
    ) {
        return packet.payload != null
                && packet.payload.contains(key)
                && packet.payload.getBoolean(key);
    }

    private static int intPayload(
            OSINetworkPacket packet,
            String key,
            int fallback
    ) {
        return packet.payload != null
                && packet.payload.contains(key)
                ? packet.payload.getInt(key)
                : fallback;
    }

    private static String protocolFromNumber(
            int protocol
    ) {
        return switch (protocol) {
            case 1 -> "ICMP";
            case 6 -> "TCP";
            case 17 -> "UDP";
            case 58 -> "ICMPV6";
            default -> "IP";
        };
    }

    private static boolean isIcmpError(
            String protocol,
            int type
    ) {
        if ("ICMP".equalsIgnoreCase(protocol)) {
            return type == 3
                    || type == 4
                    || type == 5
                    || type == 11
                    || type == 12;
        }

        if ("ICMPV6".equalsIgnoreCase(protocol)) {
            return type >= 1
                    && type <= 4;
        }

        return false;
    }

    private static int clampPort(int port) {
        return Math.max(
                0,
                Math.min(
                        65535,
                        port
                )
        );
    }

    private static String normalizeInterface(
            String port
    ) {
        if (port == null) {
            return "";
        }

        if (port.equalsIgnoreCase(
                "GigabitEthernet1/1"
        )) {
            return "LAN";
        }

        if (port.equalsIgnoreCase(
                "GigabitEthernet1/2"
        )) {
            return "WAN";
        }

        return port;
    }
}

package com.k1ngtle.vsia.signality.engineering.firewall.w1162;

import com.k1ngtle.vsia.signality.engineering.firewall.Nat44Mapping;
import com.k1ngtle.vsia.signality.internet.OSINetworkPacket;
import net.minecraft.nbt.CompoundTag;

public final class W1162FlowFactory {
    private W1162FlowFactory() {
    }

    public static OSINetworkPacket outboundUdp(
            W1162HostProfile host,
            String destinationIp,
            int sourcePort,
            int destinationPort,
            String nextHopMac,
            int sequence
    ) {
        return outbound(
                host,
                destinationIp,
                sourcePort,
                destinationPort,
                nextHopMac,
                sequence,
                "UDP"
        );
    }

    public static OSINetworkPacket outboundTcpSyn(
            W1162HostProfile host,
            String destinationIp,
            int sourcePort,
            int destinationPort,
            String nextHopMac,
            int sequence
    ) {
        OSINetworkPacket packet =
                outbound(
                        host,
                        destinationIp,
                        sourcePort,
                        destinationPort,
                        nextHopMac,
                        sequence,
                        "TCP"
                );

        packet.payload.putBoolean(
                "tcp_syn",
                true
        );

        return packet;
    }

    private static OSINetworkPacket outbound(
            W1162HostProfile host,
            String destinationIp,
            int sourcePort,
            int destinationPort,
            String nextHopMac,
            int sequence,
            String protocol
    ) {
        OSINetworkPacket packet =
                new OSINetworkPacket();

        packet.sourceMac =
                host.macAddress();

        packet.targetMac =
                nextHopMac == null
                        || nextHopMac.isBlank()
                        ? "FF:FF:FF:FF:FF:FF"
                        : nextHopMac;

        packet.sourceIp =
                host.ipv4();

        packet.targetIp =
                destinationIp;

        packet.sourcePort =
                sourcePort;

        packet.targetPort =
                destinationPort;

        packet.ttl = 64;
        packet.ipPacketLength = 64;

        packet.applicationProtocol =
                protocol;

        packet.ipProtocol =
                "TCP".equals(protocol)
                        ? 6
                        : 17;

        packet.sessionId =
                "W1.16.2-"
                        + protocol
                        + "-"
                        + sequence;

        CompoundTag payload =
                new CompoundTag();

        payload.putString(
                "kind",
                "W1.16.2_MULTI_SESSION"
        );

        payload.putInt(
                "sequence",
                sequence
        );

        payload.putString(
                "host_next_hop",
                host.nextHop(destinationIp)
        );

        packet.payload = payload;

        return packet;
    }

    public static OSINetworkPacket reply(
            Nat44Mapping mapping,
            int sequence
    ) {
        OSINetworkPacket packet =
                new OSINetworkPacket();

        packet.sourceMac =
                "02:16:02:00:00:20";

        packet.targetMac =
                "FF:FF:FF:FF:FF:FF";

        packet.sourceIp =
                mapping.outsideIp();

        packet.targetIp =
                mapping.insideGlobalIp();

        packet.sourcePort =
                mapping.outsidePort();

        packet.targetPort =
                mapping.insideGlobalPort();

        packet.ttl = 64;
        packet.ipPacketLength = 64;
        packet.isResponse = true;

        packet.applicationProtocol =
                mapping.protocol();

        packet.ipProtocol =
                "TCP".equalsIgnoreCase(
                        mapping.protocol()
                )
                        ? 6
                        : 17;

        packet.sessionId =
                "W1.16.2-RETURN-"
                        + sequence;

        CompoundTag payload =
                new CompoundTag();

        payload.putString(
                "kind",
                "W1.16.2_MULTI_SESSION_RETURN"
        );

        payload.putInt(
                "sequence",
                sequence
        );

        if ("TCP".equalsIgnoreCase(
                mapping.protocol()
        )) {
            payload.putBoolean(
                    "tcp_syn",
                    true
            );
            payload.putBoolean(
                    "tcp_ack",
                    true
            );
        }

        packet.payload = payload;

        return packet;
    }
}

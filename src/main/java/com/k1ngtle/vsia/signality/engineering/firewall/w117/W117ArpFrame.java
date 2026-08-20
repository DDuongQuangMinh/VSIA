package com.k1ngtle.vsia.signality.engineering.firewall.w117;

import com.k1ngtle.vsia.signality.internet.OSINetworkPacket;
import net.minecraft.nbt.CompoundTag;

public final class W117ArpFrame {
    public static final String PROTOCOL = "ARP";
    public static final String BROADCAST_MAC = "FF:FF:FF:FF:FF:FF";

    private W117ArpFrame() {
    }

    public static boolean isArp(OSINetworkPacket packet) {
        return packet != null
                && PROTOCOL.equalsIgnoreCase(packet.applicationProtocol)
                && packet.payload != null
                && packet.payload.contains("arp_op");
    }

    public static boolean isRequest(OSINetworkPacket packet) {
        return isArp(packet)
                && "REQUEST".equalsIgnoreCase(
                packet.payload.getString("arp_op")
        );
    }

    public static boolean isReply(OSINetworkPacket packet) {
        return isArp(packet)
                && "REPLY".equalsIgnoreCase(
                packet.payload.getString("arp_op")
        );
    }

    public static OSINetworkPacket request(
            String senderMac,
            String senderIp,
            String targetIp,
            String sessionId
    ) {
        OSINetworkPacket packet = new OSINetworkPacket();

        packet.sourceMac = senderMac;
        packet.targetMac = BROADCAST_MAC;
        packet.sourceIp = senderIp;
        packet.targetIp = targetIp;
        packet.applicationProtocol = PROTOCOL;
        packet.ipProtocol = 0;
        packet.ipPacketLength = 28;
        packet.ttl = 64;
        packet.sessionId = sessionId == null ? "" : sessionId;

        CompoundTag payload = new CompoundTag();
        payload.putString("arp_op", "REQUEST");
        payload.putString("sender_mac", senderMac);
        payload.putString("sender_ip", senderIp);
        payload.putString("target_mac", "00:00:00:00:00:00");
        payload.putString("target_ip", targetIp);
        packet.payload = payload;

        return packet;
    }

    public static OSINetworkPacket reply(
            String senderMac,
            String senderIp,
            String targetMac,
            String targetIp,
            String sessionId
    ) {
        OSINetworkPacket packet = new OSINetworkPacket();

        packet.sourceMac = senderMac;
        packet.targetMac = targetMac;
        packet.sourceIp = senderIp;
        packet.targetIp = targetIp;
        packet.applicationProtocol = PROTOCOL;
        packet.ipProtocol = 0;
        packet.ipPacketLength = 28;
        packet.ttl = 64;
        packet.isResponse = true;
        packet.sessionId = sessionId == null ? "" : sessionId;

        CompoundTag payload = new CompoundTag();
        payload.putString("arp_op", "REPLY");
        payload.putString("sender_mac", senderMac);
        payload.putString("sender_ip", senderIp);
        payload.putString("target_mac", targetMac);
        payload.putString("target_ip", targetIp);
        packet.payload = payload;

        return packet;
    }

    public static OSINetworkPacket gratuitous(
            String mac,
            String ip
    ) {
        OSINetworkPacket packet =
                request(
                        mac,
                        ip,
                        ip,
                        "W1.17-GARP"
                );

        packet.payload.putBoolean(
                "gratuitous",
                true
        );

        return packet;
    }

    public static String senderIp(OSINetworkPacket packet) {
        return packet.payload.getString("sender_ip");
    }

    public static String senderMac(OSINetworkPacket packet) {
        return packet.payload.getString("sender_mac");
    }

    public static String targetIp(OSINetworkPacket packet) {
        return packet.payload.getString("target_ip");
    }
}

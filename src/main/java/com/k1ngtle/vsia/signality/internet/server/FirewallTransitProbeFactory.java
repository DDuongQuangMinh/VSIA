package com.k1ngtle.vsia.signality.internet.server;

import com.k1ngtle.vsia.signality.internet.OSINetworkPacket;
import net.minecraft.nbt.CompoundTag;

public final class FirewallTransitProbeFactory {
    private FirewallTransitProbeFactory() {
    }

    public static OSINetworkPacket lanProbe() {
        OSINetworkPacket packet = new OSINetworkPacket();

        packet.sourceMac = "02:16:01:00:00:20";
        packet.targetMac = "FF:FF:FF:FF:FF:FF";

        packet.sourceIp = "192.168.10.20";
        packet.targetIp = "198.51.100.20";

        packet.ttl = 64;
        packet.ipProtocol = 17;
        packet.ipPacketLength = 64;
        packet.dontFragment = false;

        packet.sourcePort = 51000;
        packet.targetPort = 443;

        packet.sessionId = "W1.16.1-LAN-PROBE";
        packet.isResponse = false;
        packet.applicationProtocol = "UDP";

        CompoundTag payload = new CompoundTag();
        payload.putString("kind", "W1.16.1_LAN_TRANSIT_PROBE");
        payload.putString("message", "VSIA-W1.16.1-LAN");
        packet.payload = payload;

        return packet;
    }

    public static OSINetworkPacket wanProbe() {
        OSINetworkPacket packet = new OSINetworkPacket();

        packet.sourceMac = "02:16:01:00:00:30";
        packet.targetMac = "FF:FF:FF:FF:FF:FF";

        packet.sourceIp = "198.51.100.20";
        packet.targetIp = "203.0.113.10";

        packet.ttl = 64;
        packet.ipProtocol = 17;
        packet.ipPacketLength = 64;
        packet.dontFragment = false;

        packet.sourcePort = 443;
        packet.targetPort = 40000;

        packet.sessionId = "W1.16.1-WAN-PROBE";
        packet.isResponse = true;
        packet.applicationProtocol = "UDP";

        CompoundTag payload = new CompoundTag();
        payload.putString("kind", "W1.16.1_WAN_TRANSIT_PROBE");
        payload.putString("message", "VSIA-W1.16.1-WAN");
        packet.payload = payload;

        return packet;
    }
}

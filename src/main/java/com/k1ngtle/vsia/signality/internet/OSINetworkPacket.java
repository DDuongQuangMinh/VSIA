package com.k1ngtle.vsia.signality.internet;

import net.minecraft.nbt.CompoundTag;

/**
 * Represents Layers 2 through 7 of the OSI Model.
 * Layer 1 (Physical) is handled by the Signality SignalPacket broadcasting this data.
 */
public class OSINetworkPacket {

    // --- Layer 2: Data Link (Local Hops) ---
    public String sourceMac = "";
    public String targetMac = "";

    // --- Layer 3: Network (End-to-End Routing) ---
    public String sourceIp = "";
    public String targetIp = "";
    public int ttl = 64; // Time To Live for routing loops

    // --- Layer 4: Transport (Ports) ---
    public int sourcePort = 0;
    public int targetPort = 0; // e.g., 80 (HTTP), 53 (DNS), 25 (SMTP)

    // --- Layer 5 & 6: Session & Presentation ---
    public String sessionId = "";
    public boolean isResponse = false;

    // --- Layer 7: Application (Payload) ---
    public String applicationProtocol = ""; // "HTTP", "DNS", "SMTP"
    public CompoundTag payload = new CompoundTag();

    public OSINetworkPacket() {}

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString("srcMac", sourceMac);
        tag.putString("tgtMac", targetMac);
        tag.putString("srcIp", sourceIp);
        tag.putString("tgtIp", targetIp);
        tag.putInt("srcPort", sourcePort);
        tag.putInt("tgtPort", targetPort);
        tag.putString("sessionId", sessionId);
        tag.putBoolean("isResponse", isResponse);
        tag.putString("appProtocol", applicationProtocol);
        tag.put("payload", payload);
        return tag;
    }

    public static OSINetworkPacket deserializeNBT(CompoundTag tag) {
        OSINetworkPacket packet = new OSINetworkPacket();
        packet.sourceMac = tag.getString("srcMac");
        packet.targetMac = tag.getString("tgtMac");
        packet.sourceIp = tag.getString("srcIp");
        packet.targetIp = tag.getString("tgtIp");
        packet.sourcePort = tag.getInt("srcPort");
        packet.targetPort = tag.getInt("tgtPort");
        packet.sessionId = tag.getString("sessionId");
        packet.isResponse = tag.getBoolean("isResponse");
        packet.applicationProtocol = tag.getString("appProtocol");
        packet.payload = tag.getCompound("payload");
        return packet;
    }
}
package com.k1ngtle.vsia.signality.internet;

import net.minecraft.nbt.CompoundTag;

public class OSINetworkPacket {
    public String sourceMac = "";
    public String targetMac = "";

    public String sourceIp = "";
    public String targetIp = "";

    public int ttl = 64;
    public int ipProtocol = 0;
    public int ipv4HeaderChecksum = 0;
    public int ipPacketLength = 0;
    public boolean dontFragment;

    public int dscp = 0;
    public int ecn = 0;

    public int sourcePort = 0;
    public int targetPort = 0;
    public int transportChecksum = 0;

    public String sessionId = "";
    public boolean isResponse = false;

    public String applicationProtocol = "";
    public CompoundTag payload = new CompoundTag();

    public OSINetworkPacket() {
    }

    public int dscpEcnByte() {
        int normalizedDscp =
                Math.max(
                        0,
                        Math.min(
                                63,
                                dscp
                        )
                );

        int normalizedEcn =
                Math.max(
                        0,
                        Math.min(
                                3,
                                ecn
                        )
                );

        return (
                normalizedDscp << 2
        )
                | normalizedEcn;
    }

    public CompoundTag serializeNBT() {
        CompoundTag tag =
                new CompoundTag();

        tag.putString(
                "srcMac",
                sourceMac
        );

        tag.putString(
                "tgtMac",
                targetMac
        );

        tag.putString(
                "srcIp",
                sourceIp
        );

        tag.putString(
                "tgtIp",
                targetIp
        );

        tag.putInt(
                "ttl",
                ttl
        );

        tag.putInt(
                "ipProtocol",
                ipProtocol
        );

        tag.putInt(
                "ipv4HeaderChecksum",
                ipv4HeaderChecksum
        );

        tag.putInt(
                "ipPacketLength",
                ipPacketLength
        );

        tag.putBoolean(
                "dontFragment",
                dontFragment
        );

        tag.putInt(
                "dscp",
                Math.max(
                        0,
                        Math.min(
                                63,
                                dscp
                        )
                )
        );

        tag.putInt(
                "ecn",
                Math.max(
                        0,
                        Math.min(
                                3,
                                ecn
                        )
                )
        );

        tag.putInt(
                "srcPort",
                sourcePort
        );

        tag.putInt(
                "tgtPort",
                targetPort
        );

        tag.putInt(
                "transportChecksum",
                transportChecksum
        );

        tag.putString(
                "sessionId",
                sessionId
        );

        tag.putBoolean(
                "isResponse",
                isResponse
        );

        tag.putString(
                "appProtocol",
                applicationProtocol
        );

        tag.put(
                "payload",
                payload
        );

        return tag;
    }

    public static OSINetworkPacket deserializeNBT(
            CompoundTag tag
    ) {
        OSINetworkPacket packet =
                new OSINetworkPacket();

        packet.sourceMac =
                tag.getString(
                        "srcMac"
                );

        packet.targetMac =
                tag.getString(
                        "tgtMac"
                );

        packet.sourceIp =
                tag.getString(
                        "srcIp"
                );

        packet.targetIp =
                tag.getString(
                        "tgtIp"
                );

        packet.ttl =
                tag.contains(
                        "ttl"
                )
                        ? tag.getInt(
                        "ttl"
                )
                        : 64;

        packet.ipProtocol =
                tag.getInt(
                        "ipProtocol"
                );

        packet.ipv4HeaderChecksum =
                tag.getInt(
                        "ipv4HeaderChecksum"
                );

        packet.ipPacketLength =
                tag.getInt(
                        "ipPacketLength"
                );

        packet.dontFragment =
                tag.getBoolean(
                        "dontFragment"
                );

        packet.dscp =
                tag.contains(
                        "dscp"
                )
                        ? Math.max(
                        0,
                        Math.min(
                                63,
                                tag.getInt(
                                        "dscp"
                                )
                        )
                )
                        : 0;

        packet.ecn =
                tag.contains(
                        "ecn"
                )
                        ? Math.max(
                        0,
                        Math.min(
                                3,
                                tag.getInt(
                                        "ecn"
                                )
                        )
                )
                        : 0;

        packet.sourcePort =
                tag.getInt(
                        "srcPort"
                );

        packet.targetPort =
                tag.getInt(
                        "tgtPort"
                );

        packet.transportChecksum =
                tag.getInt(
                        "transportChecksum"
                );

        packet.sessionId =
                tag.getString(
                        "sessionId"
                );

        packet.isResponse =
                tag.getBoolean(
                        "isResponse"
                );

        packet.applicationProtocol =
                tag.getString(
                        "appProtocol"
                );

        packet.payload =
                tag.getCompound(
                        "payload"
                );

        return packet;
    }
}

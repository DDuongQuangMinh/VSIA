package com.k1ngtle.vsia.signality.engineering.firewall.w118;

import com.k1ngtle.vsia.signality.internet.OSINetworkPacket;
import net.minecraft.nbt.CompoundTag;

public final class W118DnsMessage {
    public static final String PROTOCOL = "DNS";

    private W118DnsMessage() {
    }

    public static boolean isDns(OSINetworkPacket packet) {
        return packet != null
                && PROTOCOL.equalsIgnoreCase(packet.applicationProtocol)
                && packet.payload != null
                && packet.payload.contains("dns_query");
    }

    public static boolean isResponse(OSINetworkPacket packet) {
        return isDns(packet)
                && packet.payload.getBoolean("dns_response");
    }

    public static String queryName(OSINetworkPacket packet) {
        return packet.payload.getString("dns_query");
    }

    public static int id(OSINetworkPacket packet) {
        return packet.payload.getInt("dns_id");
    }

    public static String answer(OSINetworkPacket packet) {
        return packet.payload.getString("dns_answer");
    }

    public static long ttlSeconds(OSINetworkPacket packet) {
        return packet.payload.getLong("dns_ttl");
    }

    public static int rcode(OSINetworkPacket packet) {
        return packet.payload.getInt("dns_rcode");
    }

    public static OSINetworkPacket query(
            String sourceIp,
            String dnsServer,
            int sourcePort,
            int id,
            String name
    ) {
        OSINetworkPacket packet =
                new OSINetworkPacket();

        packet.sourceIp = sourceIp;
        packet.targetIp = dnsServer;
        packet.sourcePort = sourcePort;
        packet.targetPort = 53;
        packet.ipProtocol = 17;
        packet.applicationProtocol = PROTOCOL;
        packet.ipPacketLength = 96;
        packet.ttl = 64;
        packet.sessionId = "W1.18-DNS-" + id;

        CompoundTag payload =
                new CompoundTag();

        payload.putString(
                "dns_query",
                normalize(name)
        );
        payload.putInt(
                "dns_id",
                id
        );
        payload.putBoolean(
                "dns_response",
                false
        );

        packet.payload = payload;

        return packet;
    }

    public static OSINetworkPacket response(
            OSINetworkPacket query,
            String serverIp,
            String answer,
            long ttlSeconds,
            int rcode
    ) {
        OSINetworkPacket packet =
                new OSINetworkPacket();

        packet.sourceIp = serverIp;
        packet.targetIp = query.sourceIp;
        packet.sourcePort = 53;
        packet.targetPort = query.sourcePort;
        packet.ipProtocol = 17;
        packet.applicationProtocol = PROTOCOL;
        packet.ipPacketLength = 112;
        packet.ttl = 64;
        packet.isResponse = true;
        packet.sessionId =
                query.sessionId + "-RESPONSE";

        CompoundTag payload =
                new CompoundTag();

        payload.putString(
                "dns_query",
                queryName(query)
        );
        payload.putInt(
                "dns_id",
                id(query)
        );
        payload.putBoolean(
                "dns_response",
                true
        );
        payload.putString(
                "dns_answer",
                answer == null ? "" : answer
        );
        payload.putLong(
                "dns_ttl",
                Math.max(0L, ttlSeconds)
        );
        payload.putInt(
                "dns_rcode",
                rcode
        );

        packet.payload = payload;

        return packet;
    }

    public static String normalize(String name) {
        if (name == null) {
            return "";
        }

        String n =
                name.trim().toLowerCase();

        while (n.endsWith(".")) {
            n = n.substring(
                    0,
                    n.length() - 1
            );
        }

        return n;
    }
}

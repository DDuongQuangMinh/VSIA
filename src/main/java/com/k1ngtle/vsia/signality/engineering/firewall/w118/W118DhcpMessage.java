package com.k1ngtle.vsia.signality.engineering.firewall.w118;

import com.k1ngtle.vsia.signality.engineering.firewall.w117.W117ArpFrame;
import com.k1ngtle.vsia.signality.internet.OSINetworkPacket;
import net.minecraft.nbt.CompoundTag;

public final class W118DhcpMessage {
    public static final String PROTOCOL = "DHCP";
    public static final String BROADCAST_IP = "255.255.255.255";

    private W118DhcpMessage() {
    }

    public enum Type {
        DISCOVER,
        OFFER,
        REQUEST,
        ACK,
        NAK,
        RELEASE
    }

    public static boolean isDhcp(OSINetworkPacket packet) {
        return packet != null
                && PROTOCOL.equalsIgnoreCase(packet.applicationProtocol)
                && packet.payload != null
                && packet.payload.contains("dhcp_type");
    }

    public static Type type(OSINetworkPacket packet) {
        if (!isDhcp(packet)) {
            throw new IllegalArgumentException("Not DHCP");
        }

        return Type.valueOf(
                packet.payload.getString("dhcp_type")
                        .toUpperCase()
        );
    }

    public static int xid(OSINetworkPacket packet) {
        return packet.payload.getInt("dhcp_xid");
    }

    public static String clientMac(OSINetworkPacket packet) {
        return packet.payload.getString("dhcp_client_mac");
    }

    public static String requestedIp(OSINetworkPacket packet) {
        return packet.payload.getString("dhcp_requested_ip");
    }

    public static String yourIp(OSINetworkPacket packet) {
        return packet.payload.getString("dhcp_yiaddr");
    }

    public static String serverId(OSINetworkPacket packet) {
        return packet.payload.getString("dhcp_server_id");
    }

    public static String subnetMask(OSINetworkPacket packet) {
        return packet.payload.getString("dhcp_mask");
    }

    public static String gateway(OSINetworkPacket packet) {
        return packet.payload.getString("dhcp_gateway");
    }

    public static String dnsServer(OSINetworkPacket packet) {
        return packet.payload.getString("dhcp_dns");
    }

    public static long leaseSeconds(OSINetworkPacket packet) {
        return packet.payload.getLong("dhcp_lease_seconds");
    }

    public static long t1Seconds(OSINetworkPacket packet) {
        return packet.payload.getLong("dhcp_t1_seconds");
    }

    public static long t2Seconds(OSINetworkPacket packet) {
        return packet.payload.getLong("dhcp_t2_seconds");
    }

    public static OSINetworkPacket discover(
            String clientMac,
            int xid
    ) {
        return base(
                Type.DISCOVER,
                clientMac,
                xid,
                "0.0.0.0",
                BROADCAST_IP,
                W117ArpFrame.BROADCAST_MAC
        );
    }

    public static OSINetworkPacket request(
            String clientMac,
            int xid,
            String requestedIp,
            String serverId,
            boolean broadcast
    ) {
        OSINetworkPacket packet =
                base(
                        Type.REQUEST,
                        clientMac,
                        xid,
                        "0.0.0.0",
                        broadcast
                                ? BROADCAST_IP
                                : serverId,
                        broadcast
                                ? W117ArpFrame.BROADCAST_MAC
                                : ""
                );

        packet.payload.putString(
                "dhcp_requested_ip",
                requestedIp == null ? "" : requestedIp
        );

        packet.payload.putString(
                "dhcp_server_id",
                serverId == null ? "" : serverId
        );

        packet.payload.putBoolean(
                "dhcp_broadcast",
                broadcast
        );

        return packet;
    }

    public static OSINetworkPacket offer(
            OSINetworkPacket discover,
            String serverMac,
            String serverIp,
            String offeredIp,
            String mask,
            String gateway,
            String dns,
            long leaseSeconds
    ) {
        OSINetworkPacket packet =
                base(
                        Type.OFFER,
                        clientMac(discover),
                        xid(discover),
                        serverIp,
                        BROADCAST_IP,
                        W117ArpFrame.BROADCAST_MAC
                );

        options(
                packet,
                serverIp,
                offeredIp,
                mask,
                gateway,
                dns,
                leaseSeconds
        );

        packet.sourceMac = serverMac;

        return packet;
    }

    public static OSINetworkPacket ack(
            OSINetworkPacket request,
            String serverMac,
            String serverIp,
            String clientMac,
            String leasedIp,
            String mask,
            String gateway,
            String dns,
            long leaseSeconds
    ) {
        OSINetworkPacket packet =
                base(
                        Type.ACK,
                        clientMac,
                        xid(request),
                        serverIp,
                        leasedIp,
                        clientMac
                );

        options(
                packet,
                serverIp,
                leasedIp,
                mask,
                gateway,
                dns,
                leaseSeconds
        );

        packet.sourceMac = serverMac;
        packet.isResponse = true;

        return packet;
    }

    public static OSINetworkPacket nak(
            OSINetworkPacket request,
            String serverMac,
            String serverIp
    ) {
        OSINetworkPacket packet =
                base(
                        Type.NAK,
                        clientMac(request),
                        xid(request),
                        serverIp,
                        BROADCAST_IP,
                        W117ArpFrame.BROADCAST_MAC
                );

        packet.sourceMac = serverMac;
        packet.payload.putString(
                "dhcp_server_id",
                serverIp
        );

        return packet;
    }

    private static OSINetworkPacket base(
            Type type,
            String clientMac,
            int xid,
            String sourceIp,
            String targetIp,
            String targetMac
    ) {
        OSINetworkPacket packet =
                new OSINetworkPacket();

        packet.sourceMac =
                clientMac == null ? "" : clientMac;

        packet.targetMac =
                targetMac == null ? "" : targetMac;

        packet.sourceIp = sourceIp;
        packet.targetIp = targetIp;
        packet.sourcePort =
                type == Type.DISCOVER
                        || type == Type.REQUEST
                        || type == Type.RELEASE
                        ? 68
                        : 67;
        packet.targetPort =
                packet.sourcePort == 68 ? 67 : 68;
        packet.ipProtocol = 17;
        packet.applicationProtocol = PROTOCOL;
        packet.ipPacketLength = 300;
        packet.ttl = 64;
        packet.sessionId = "W1.18-DHCP-" + xid;

        CompoundTag payload =
                new CompoundTag();

        payload.putString(
                "dhcp_type",
                type.name()
        );
        payload.putInt(
                "dhcp_xid",
                xid
        );
        payload.putString(
                "dhcp_client_mac",
                clientMac == null ? "" : clientMac
        );

        packet.payload = payload;

        return packet;
    }

    private static void options(
            OSINetworkPacket packet,
            String serverId,
            String yiaddr,
            String mask,
            String gateway,
            String dns,
            long leaseSeconds
    ) {
        long lease =
                Math.max(
                        60L,
                        leaseSeconds
                );

        packet.payload.putString(
                "dhcp_server_id",
                serverId
        );
        packet.payload.putString(
                "dhcp_yiaddr",
                yiaddr
        );
        packet.payload.putString(
                "dhcp_mask",
                mask
        );
        packet.payload.putString(
                "dhcp_gateway",
                gateway
        );
        packet.payload.putString(
                "dhcp_dns",
                dns
        );
        packet.payload.putLong(
                "dhcp_lease_seconds",
                lease
        );
        packet.payload.putLong(
                "dhcp_t1_seconds",
                lease / 2L
        );
        packet.payload.putLong(
                "dhcp_t2_seconds",
                Math.max(
                        lease / 2L + 1L,
                        (lease * 7L) / 8L
                )
        );
    }
}

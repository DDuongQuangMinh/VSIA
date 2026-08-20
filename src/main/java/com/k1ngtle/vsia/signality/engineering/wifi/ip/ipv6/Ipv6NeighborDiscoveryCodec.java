package com.k1ngtle.vsia.signality.engineering.wifi.ip.ipv6;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;

public final class Ipv6NeighborDiscoveryCodec {
    public static final int OPTION_SOURCE_LINK_LAYER = 1;
    public static final int OPTION_TARGET_LINK_LAYER = 2;
    public static final int OPTION_PREFIX_INFORMATION = 3;
    public static final int OPTION_MTU = 5;

    private Ipv6NeighborDiscoveryCodec() {
    }

    public static byte[] encodeNeighborSolicitation(
            Ipv6Address source,
            Ipv6Address destination,
            Ipv6Address target,
            String sourceMac
    ) {
        byte[] payload = new byte[16 + (sourceMac == null || sourceMac.isBlank() ? 0 : 8)];
        System.arraycopy(target.bytes(), 0, payload, 0, 16);

        if (payload.length > 16) {
            payload[16] = OPTION_SOURCE_LINK_LAYER;
            payload[17] = 1;
            System.arraycopy(parseMac(sourceMac), 0, payload, 18, 6);
        }

        return Icmpv6Codec.encode(
                source,
                destination,
                Icmpv6Codec.NEIGHBOR_SOLICITATION,
                0,
                0,
                payload
        );
    }

    public static byte[] encodeNeighborAdvertisement(
            Ipv6Address source,
            Ipv6Address destination,
            Ipv6Address target,
            boolean router,
            boolean solicited,
            boolean override,
            String targetMac
    ) {
        int flags = 0;
        if (router) flags |= 0x80000000;
        if (solicited) flags |= 0x40000000;
        if (override) flags |= 0x20000000;

        byte[] payload = new byte[24];
        System.arraycopy(target.bytes(), 0, payload, 0, 16);
        payload[16] = OPTION_TARGET_LINK_LAYER;
        payload[17] = 1;
        System.arraycopy(parseMac(targetMac), 0, payload, 18, 6);

        return Icmpv6Codec.encode(
                source,
                destination,
                Icmpv6Codec.NEIGHBOR_ADVERTISEMENT,
                0,
                flags,
                payload
        );
    }

    public static byte[] encodeRouterSolicitation(
            Ipv6Address source,
            Ipv6Address destination,
            String sourceMac
    ) {
        byte[] options;

        if (sourceMac == null || sourceMac.isBlank()) {
            options = new byte[0];
        } else {
            options = new byte[8];
            options[0] = OPTION_SOURCE_LINK_LAYER;
            options[1] = 1;
            System.arraycopy(parseMac(sourceMac), 0, options, 2, 6);
        }

        return Icmpv6Codec.encode(
                source,
                destination,
                Icmpv6Codec.ROUTER_SOLICITATION,
                0,
                0,
                options
        );
    }

    public static byte[] encodeRouterAdvertisement(
            Ipv6Address source,
            Ipv6Address destination,
            int currentHopLimit,
            int routerLifetimeSeconds,
            Ipv6Prefix prefix,
            int validLifetimeSeconds,
            int preferredLifetimeSeconds,
            int mtu,
            String sourceMac
    ) {
        if (prefix.length() != 64) {
            throw new IllegalArgumentException("W1.15 SLAAC RA requires /64 prefix");
        }
        if (mtu < 1280) {
            throw new IllegalArgumentException("IPv6 advertised MTU must be >=1280");
        }

        ByteArrayOutputStream payload = new ByteArrayOutputStream();

        payload.write(currentHopLimit & 0xFF);
        payload.write(0);
        write16(payload, routerLifetimeSeconds);
        write32(payload, 0);
        write32(payload, 0);

        byte[] mac = parseMac(sourceMac);
        payload.write(OPTION_SOURCE_LINK_LAYER);
        payload.write(1);
        payload.writeBytes(mac);

        payload.write(OPTION_PREFIX_INFORMATION);
        payload.write(4);
        payload.write(64);
        payload.write(0xC0);
        write32(payload, validLifetimeSeconds);
        write32(payload, preferredLifetimeSeconds);
        write32(payload, 0);
        payload.writeBytes(prefix.network().bytes());

        payload.write(OPTION_MTU);
        payload.write(1);
        write16(payload, 0);
        write32(payload, mtu);

        return Icmpv6Codec.encode(
                source,
                destination,
                Icmpv6Codec.ROUTER_ADVERTISEMENT,
                0,
                0,
                payload.toByteArray()
        );
    }

    private static byte[] parseMac(String text) {
        String normalized = text.replace("-", ":");
        if (!normalized.contains(":") && normalized.length() == 12) {
            normalized =
                    normalized.substring(0, 2) + ":"
                    + normalized.substring(2, 4) + ":"
                    + normalized.substring(4, 6) + ":"
                    + normalized.substring(6, 8) + ":"
                    + normalized.substring(8, 10) + ":"
                    + normalized.substring(10, 12);
        }

        String[] parts = normalized.split(":");
        if (parts.length != 6) throw new IllegalArgumentException("Invalid MAC");

        byte[] out = new byte[6];
        for (int i = 0; i < 6; i++) {
            out[i] = (byte) Integer.parseInt(parts[i], 16);
        }
        return out;
    }

    private static void write16(ByteArrayOutputStream out, int value) {
        out.write((value >>> 8) & 0xFF);
        out.write(value & 0xFF);
    }

    private static void write32(ByteArrayOutputStream out, long value) {
        out.write((int) (value >>> 24) & 0xFF);
        out.write((int) (value >>> 16) & 0xFF);
        out.write((int) (value >>> 8) & 0xFF);
        out.write((int) value & 0xFF);
    }
}

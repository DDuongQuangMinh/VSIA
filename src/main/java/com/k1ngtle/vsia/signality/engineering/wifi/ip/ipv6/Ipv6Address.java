package com.k1ngtle.vsia.signality.engineering.wifi.ip.ipv6;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;

public final class Ipv6Address implements Comparable<Ipv6Address> {
    public static final int BYTES = 16;

    private final byte[] bytes;

    private Ipv6Address(byte[] bytes) {
        if (bytes == null || bytes.length != BYTES) {
            throw new IllegalArgumentException("IPv6 address must be exactly 16 bytes");
        }
        this.bytes = bytes.clone();
    }

    public static Ipv6Address parse(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("IPv6 address is blank");
        }

        try {
            InetAddress parsed = InetAddress.getByName(text);
            if (!(parsed instanceof Inet6Address)) {
                throw new IllegalArgumentException("Not an IPv6 address: " + text);
            }
            return new Ipv6Address(parsed.getAddress());
        } catch (UnknownHostException exception) {
            throw new IllegalArgumentException("Invalid IPv6 address: " + text, exception);
        }
    }

    public static Ipv6Address fromBytes(byte[] bytes) {
        return new Ipv6Address(bytes);
    }

    public static Ipv6Address unspecified() {
        return new Ipv6Address(new byte[16]);
    }

    public static Ipv6Address allNodesLinkLocal() {
        return parse("ff02::1");
    }

    public static Ipv6Address allRoutersLinkLocal() {
        return parse("ff02::2");
    }

    public static Ipv6Address solicitedNodeMulticast(Ipv6Address target) {
        byte[] out = new byte[16];
        out[0] = (byte) 0xFF;
        out[1] = 0x02;
        out[11] = 0x01;
        out[12] = (byte) 0xFF;

        byte[] t = target.bytes;
        out[13] = t[13];
        out[14] = t[14];
        out[15] = t[15];

        return new Ipv6Address(out);
    }

    public static Ipv6Address linkLocalFromMac(String mac) {
        byte[] m = parseMac(mac);
        byte[] eui64 = new byte[8];

        eui64[0] = (byte) (m[0] ^ 0x02);
        eui64[1] = m[1];
        eui64[2] = m[2];
        eui64[3] = (byte) 0xFF;
        eui64[4] = (byte) 0xFE;
        eui64[5] = m[3];
        eui64[6] = m[4];
        eui64[7] = m[5];

        byte[] out = new byte[16];
        out[0] = (byte) 0xFE;
        out[1] = (byte) 0x80;
        System.arraycopy(eui64, 0, out, 8, 8);

        return new Ipv6Address(out);
    }

    public Ipv6Address withPrefix(Ipv6Prefix prefix) {
        if (prefix.length() != 64) {
            throw new IllegalArgumentException("SLAAC/EUI-64 requires /64 prefix");
        }

        byte[] out = prefix.network().bytes();
        System.arraycopy(bytes, 8, out, 8, 8);
        return new Ipv6Address(out);
    }

    public byte[] bytes() {
        return bytes.clone();
    }

    public boolean isUnspecified() {
        for (byte b : bytes) {
            if (b != 0) return false;
        }
        return true;
    }

    public boolean isMulticast() {
        return (bytes[0] & 0xFF) == 0xFF;
    }

    public boolean isLinkLocal() {
        return (bytes[0] & 0xFF) == 0xFE
                && ((bytes[1] & 0xC0) == 0x80);
    }

    public boolean isLoopback() {
        for (int i = 0; i < 15; i++) {
            if (bytes[i] != 0) return false;
        }
        return bytes[15] == 1;
    }

    public int scope() {
        if (!isMulticast()) return 0;
        return bytes[1] & 0x0F;
    }

    @Override
    public String toString() {
        try {
            return InetAddress.getByAddress(bytes).getHostAddress();
        } catch (UnknownHostException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Ipv6Address other
                && Arrays.equals(bytes, other.bytes);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(bytes);
    }

    @Override
    public int compareTo(Ipv6Address other) {
        for (int i = 0; i < bytes.length; i++) {
            int a = bytes[i] & 0xFF;
            int b = other.bytes[i] & 0xFF;
            if (a != b) return Integer.compare(a, b);
        }
        return 0;
    }

    private static byte[] parseMac(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("MAC address is blank");
        }

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
        if (parts.length != 6) {
            throw new IllegalArgumentException("Invalid MAC address: " + text);
        }

        byte[] out = new byte[6];
        try {
            for (int i = 0; i < 6; i++) {
                out[i] = (byte) Integer.parseInt(parts[i], 16);
            }
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Invalid MAC address: " + text, exception);
        }
        return out;
    }
}

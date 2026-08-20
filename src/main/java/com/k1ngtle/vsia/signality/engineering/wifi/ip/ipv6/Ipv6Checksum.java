package com.k1ngtle.vsia.signality.engineering.wifi.ip.ipv6;

public final class Ipv6Checksum {
    private Ipv6Checksum() {
    }

    public static int transportChecksum(
            Ipv6Address source,
            Ipv6Address destination,
            int nextHeader,
            byte[] transport
    ) {
        if (nextHeader < 0 || nextHeader > 255) {
            throw new IllegalArgumentException("nextHeader");
        }

        byte[] payload = transport == null ? new byte[0] : transport;

        long sum = 0;
        sum = add(sum, source.bytes());
        sum = add(sum, destination.bytes());

        long length = Integer.toUnsignedLong(payload.length);
        sum += (length >>> 16) & 0xFFFF;
        sum += length & 0xFFFF;
        sum += nextHeader & 0xFF;

        sum = add(sum, payload);

        while ((sum >>> 16) != 0) {
            sum = (sum & 0xFFFF) + (sum >>> 16);
        }

        return (int) (~sum) & 0xFFFF;
    }

    private static long add(long sum, byte[] bytes) {
        for (int i = 0; i < bytes.length; i += 2) {
            int word = (bytes[i] & 0xFF) << 8;
            if (i + 1 < bytes.length) {
                word |= bytes[i + 1] & 0xFF;
            }
            sum += word;
            while ((sum >>> 16) != 0) {
                sum = (sum & 0xFFFF) + (sum >>> 16);
            }
        }
        return sum;
    }
}

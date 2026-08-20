package com.k1ngtle.vsia.signality.engineering.wifi.ip.ipv6;

import java.util.Arrays;

public final class RawUdp6Codec {
    private RawUdp6Codec() {
    }

    public static byte[] encode(
            Ipv6Address source,
            Ipv6Address destination,
            int sourcePort,
            int destinationPort,
            byte[] payload
    ) {
        validatePort(sourcePort);
        validatePort(destinationPort);

        byte[] body = payload == null ? new byte[0] : payload.clone();
        int length = 8 + body.length;
        if (length > 65535) throw new IllegalArgumentException("UDP/IPv6 datagram too large");

        byte[] out = new byte[length];
        put16(out, 0, sourcePort);
        put16(out, 2, destinationPort);
        put16(out, 4, length);
        put16(out, 6, 0);
        System.arraycopy(body, 0, out, 8, body.length);

        int checksum = Ipv6Checksum.transportChecksum(
                source,
                destination,
                17,
                out
        );

        if (checksum == 0) checksum = 0xFFFF;
        put16(out, 6, checksum);

        return out;
    }

    public static Decoded decode(
            Ipv6Address source,
            Ipv6Address destination,
            byte[] raw
    ) {
        if (raw == null || raw.length < 8) {
            throw new IllegalArgumentException("Truncated UDP/IPv6 header");
        }

        int sourcePort = read16(raw, 0);
        int destinationPort = read16(raw, 2);
        int length = read16(raw, 4);
        int checksum = read16(raw, 6);

        if (length < 8 || length > raw.length) {
            throw new IllegalArgumentException("Invalid UDP/IPv6 length");
        }

        if (checksum == 0) {
            throw new IllegalArgumentException("UDP checksum is mandatory in IPv6");
        }

        byte[] datagram = Arrays.copyOf(raw, length);
        boolean checksumValid =
                Ipv6Checksum.transportChecksum(
                        source,
                        destination,
                        17,
                        datagram
                ) == 0;

        return new Decoded(
                sourcePort,
                destinationPort,
                length,
                checksum,
                checksumValid,
                Arrays.copyOfRange(raw, 8, length)
        );
    }

    private static void validatePort(int port) {
        if (port < 0 || port > 65535) throw new IllegalArgumentException("port");
    }

    private static void put16(byte[] data, int offset, int value) {
        data[offset] = (byte) (value >>> 8);
        data[offset + 1] = (byte) value;
    }

    private static int read16(byte[] data, int offset) {
        return ((data[offset] & 0xFF) << 8)
                | (data[offset + 1] & 0xFF);
    }

    public record Decoded(
            int sourcePort,
            int destinationPort,
            int length,
            int checksum,
            boolean checksumValid,
            byte[] payload
    ) {
        public Decoded {
            payload = payload == null ? new byte[0] : payload.clone();
        }

        @Override
        public byte[] payload() {
            return payload.clone();
        }
    }
}

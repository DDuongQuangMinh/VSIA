package com.k1ngtle.vsia.signality.engineering.wifi.ip.ipv6;

import java.util.Arrays;

public final class Icmpv6Codec {
    public static final int DESTINATION_UNREACHABLE = 1;
    public static final int PACKET_TOO_BIG = 2;
    public static final int TIME_EXCEEDED = 3;
    public static final int PARAMETER_PROBLEM = 4;
    public static final int ECHO_REQUEST = 128;
    public static final int ECHO_REPLY = 129;
    public static final int ROUTER_SOLICITATION = 133;
    public static final int ROUTER_ADVERTISEMENT = 134;
    public static final int NEIGHBOR_SOLICITATION = 135;
    public static final int NEIGHBOR_ADVERTISEMENT = 136;

    private Icmpv6Codec() {
    }

    public static byte[] encode(
            Ipv6Address source,
            Ipv6Address destination,
            int type,
            int code,
            int restOfHeader,
            byte[] payload
    ) {
        byte[] body = payload == null ? new byte[0] : payload.clone();
        byte[] out = new byte[8 + body.length];

        out[0] = (byte) type;
        out[1] = (byte) code;
        out[4] = (byte) (restOfHeader >>> 24);
        out[5] = (byte) (restOfHeader >>> 16);
        out[6] = (byte) (restOfHeader >>> 8);
        out[7] = (byte) restOfHeader;
        System.arraycopy(body, 0, out, 8, body.length);

        int checksum = Ipv6Checksum.transportChecksum(
                source,
                destination,
                58,
                out
        );

        put16(out, 2, checksum);
        return out;
    }

    public static byte[] encodePacketTooBig(
            Ipv6Address source,
            Ipv6Address destination,
            int mtu,
            byte[] quoted
    ) {
        if (mtu < 1280) {
            throw new IllegalArgumentException("IPv6 minimum MTU is 1280");
        }

        return encode(
                source,
                destination,
                PACKET_TOO_BIG,
                0,
                mtu,
                quoted
        );
    }

    public static Decoded decode(
            Ipv6Address source,
            Ipv6Address destination,
            byte[] raw
    ) {
        if (raw == null || raw.length < 8) {
            throw new IllegalArgumentException("Truncated ICMPv6");
        }

        byte[] copy = raw.clone();

        boolean checksumValid =
                Ipv6Checksum.transportChecksum(
                        source,
                        destination,
                        58,
                        copy
                ) == 0;

        int rest =
                ((raw[4] & 0xFF) << 24)
                        | ((raw[5] & 0xFF) << 16)
                        | ((raw[6] & 0xFF) << 8)
                        | (raw[7] & 0xFF);

        return new Decoded(
                raw[0] & 0xFF,
                raw[1] & 0xFF,
                read16(raw, 2),
                checksumValid,
                rest,
                Arrays.copyOfRange(raw, 8, raw.length)
        );
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
            int type,
            int code,
            int checksum,
            boolean checksumValid,
            int restOfHeader,
            byte[] payload
    ) {
        public Decoded {
            payload = payload == null ? new byte[0] : payload.clone();
        }

        @Override
        public byte[] payload() {
            return payload.clone();
        }

        public long unsignedRest() {
            return Integer.toUnsignedLong(restOfHeader);
        }
    }
}

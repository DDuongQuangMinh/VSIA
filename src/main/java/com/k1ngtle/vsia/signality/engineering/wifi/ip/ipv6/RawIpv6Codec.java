package com.k1ngtle.vsia.signality.engineering.wifi.ip.ipv6;

import java.util.Arrays;

public final class RawIpv6Codec {
    public static final int BASE_HEADER_BYTES = 40;

    private RawIpv6Codec() {
    }

    public static byte[] encode(
            Ipv6Address source,
            Ipv6Address destination,
            int trafficClass,
            int flowLabel,
            int nextHeader,
            int hopLimit,
            byte[] payload
    ) {
        byte[] body = payload == null ? new byte[0] : payload.clone();
        if (body.length > 65535) {
            throw new IllegalArgumentException("Jumbograms are outside W1.15 scope");
        }
        if (trafficClass < 0 || trafficClass > 255) throw new IllegalArgumentException("trafficClass");
        if (flowLabel < 0 || flowLabel > 0xFFFFF) throw new IllegalArgumentException("flowLabel");
        if (nextHeader < 0 || nextHeader > 255) throw new IllegalArgumentException("nextHeader");
        if (hopLimit < 0 || hopLimit > 255) throw new IllegalArgumentException("hopLimit");

        byte[] out = new byte[40 + body.length];

        int firstWord =
                (6 << 28)
                        | ((trafficClass & 0xFF) << 20)
                        | (flowLabel & 0xFFFFF);

        out[0] = (byte) (firstWord >>> 24);
        out[1] = (byte) (firstWord >>> 16);
        out[2] = (byte) (firstWord >>> 8);
        out[3] = (byte) firstWord;

        put16(out, 4, body.length);
        out[6] = (byte) nextHeader;
        out[7] = (byte) hopLimit;

        System.arraycopy(source.bytes(), 0, out, 8, 16);
        System.arraycopy(destination.bytes(), 0, out, 24, 16);
        System.arraycopy(body, 0, out, 40, body.length);

        return out;
    }

    public static RawIpv6Packet decode(byte[] raw) {
        if (raw == null || raw.length < 40) {
            throw new IllegalArgumentException("Truncated IPv6 base header");
        }

        int version = (raw[0] >>> 4) & 0x0F;
        if (version != 6) {
            throw new IllegalArgumentException("Not IPv6");
        }

        int trafficClass =
                ((raw[0] & 0x0F) << 4)
                        | ((raw[1] >>> 4) & 0x0F);

        int flowLabel =
                ((raw[1] & 0x0F) << 16)
                        | ((raw[2] & 0xFF) << 8)
                        | (raw[3] & 0xFF);

        int payloadLength = read16(raw, 4);
        if (40 + payloadLength > raw.length) {
            throw new IllegalArgumentException("IPv6 Payload Length exceeds received bytes");
        }

        int nextHeader = raw[6] & 0xFF;
        int hopLimit = raw[7] & 0xFF;

        Ipv6Address source = Ipv6Address.fromBytes(Arrays.copyOfRange(raw, 8, 24));
        Ipv6Address destination = Ipv6Address.fromBytes(Arrays.copyOfRange(raw, 24, 40));
        byte[] payload = Arrays.copyOfRange(raw, 40, 40 + payloadLength);

        return new RawIpv6Packet(
                trafficClass,
                flowLabel,
                payloadLength,
                nextHeader,
                hopLimit,
                source,
                destination,
                payload
        );
    }

    public static byte[] decrementHopLimit(byte[] raw) {
        RawIpv6Packet packet = decode(raw);
        if (packet.hopLimit() <= 1) {
            throw new HopLimitExceededException();
        }

        byte[] out = Arrays.copyOf(raw, packet.totalLength());
        out[7] = (byte) (packet.hopLimit() - 1);
        return out;
    }

    private static void put16(byte[] data, int offset, int value) {
        data[offset] = (byte) (value >>> 8);
        data[offset + 1] = (byte) value;
    }

    private static int read16(byte[] data, int offset) {
        return ((data[offset] & 0xFF) << 8)
                | (data[offset + 1] & 0xFF);
    }

    public static final class HopLimitExceededException extends IllegalStateException {
        public HopLimitExceededException() {
            super("IPv6 Hop Limit exceeded");
        }
    }
}

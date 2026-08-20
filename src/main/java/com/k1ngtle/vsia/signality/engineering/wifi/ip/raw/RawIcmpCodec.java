package com.k1ngtle.vsia.signality.engineering.wifi.ip.raw;

import com.k1ngtle.vsia.signality.engineering.wifi.ip.InternetChecksum;

public final class RawIcmpCodec {
    private RawIcmpCodec() {
    }

    public static byte[] encodeEcho(
            boolean reply,
            int identifier,
            int sequence,
            byte[] payload
    ) {
        byte[] body =
                payload == null
                        ? new byte[0]
                        : payload.clone();

        byte[] out =
                new byte[
                        8 + body.length
                ];

        out[0] =
                (byte) (
                        reply
                                ? 0
                                : 8
                );

        out[1] =
                0;

        put16(
                out,
                4,
                identifier
        );

        put16(
                out,
                6,
                sequence
        );

        System.arraycopy(
                body,
                0,
                out,
                8,
                body.length
        );

        put16(
                out,
                2,
                InternetChecksum.compute(
                        out
                )
        );

        return out;
    }

    public static byte[] encodeError(
            int type,
            int code,
            int restOfHeader,
            byte[] quotedPacket
    ) {
        byte[] quote =
                quotedPacket == null
                        ? new byte[0]
                        : quotedPacket.clone();

        byte[] out =
                new byte[
                        8 + quote.length
                ];

        out[0] =
                (byte) type;

        out[1] =
                (byte) code;

        put32(
                out,
                4,
                restOfHeader
        );

        System.arraycopy(
                quote,
                0,
                out,
                8,
                quote.length
        );

        put16(
                out,
                2,
                InternetChecksum.compute(
                        out
                )
        );

        return out;
    }

    public static RawIcmpPacket decode(
            byte[] packet
    ) {
        if (packet == null
                || packet.length < 8) {
            throw new IllegalArgumentException(
                    "ICMP packet must contain at least 8 bytes"
            );
        }

        int type =
                packet[0]
                        & 0xFF;

        int code =
                packet[1]
                        & 0xFF;

        int checksum =
                read16(
                        packet,
                        2
                );

        int rest =
                read32(
                        packet,
                        4
                );

        byte[] payload =
                java.util.Arrays.copyOfRange(
                        packet,
                        8,
                        packet.length
                );

        return new RawIcmpPacket(
                type,
                code,
                checksum,
                rest,
                payload,
                InternetChecksum.compute(
                        packet
                ) == 0
        );
    }

    private static int read16(
            byte[] data,
            int offset
    ) {
        return ((data[offset] & 0xFF) << 8)
                | (data[offset + 1] & 0xFF);
    }

    private static int read32(
            byte[] data,
            int offset
    ) {
        return ((data[offset] & 0xFF) << 24)
                | ((data[offset + 1] & 0xFF) << 16)
                | ((data[offset + 2] & 0xFF) << 8)
                | (data[offset + 3] & 0xFF);
    }

    private static void put16(
            byte[] data,
            int offset,
            int value
    ) {
        data[offset] =
                (byte) (
                        value >>> 8
                );

        data[offset + 1] =
                (byte) value;
    }

    private static void put32(
            byte[] data,
            int offset,
            int value
    ) {
        data[offset] =
                (byte) (
                        value >>> 24
                );

        data[offset + 1] =
                (byte) (
                        value >>> 16
                );

        data[offset + 2] =
                (byte) (
                        value >>> 8
                );

        data[offset + 3] =
                (byte) value;
    }
}

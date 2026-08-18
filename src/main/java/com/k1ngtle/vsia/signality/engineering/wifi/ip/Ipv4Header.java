package com.k1ngtle.vsia.signality.engineering.wifi.ip;

public record Ipv4Header(
        String sourceAddress,
        String destinationAddress,
        int protocol,
        int ttl,
        int identification,
        int payloadLength,
        boolean dontFragment
) {
    public static final int HEADER_BYTES =
            20;

    public byte[] encode() {
        if (protocol < 0
                || protocol > 255) {
            throw new IllegalArgumentException(
                    "protocol"
            );
        }

        if (ttl < 0
                || ttl > 255) {
            throw new IllegalArgumentException(
                    "ttl"
            );
        }

        if (payloadLength < 0
                || payloadLength > 65515) {
            throw new IllegalArgumentException(
                    "payloadLength"
            );
        }

        byte[] source =
                Ipv4Address.parse(
                        sourceAddress
                );

        byte[] destination =
                Ipv4Address.parse(
                        destinationAddress
                );

        byte[] header =
                new byte[
                        HEADER_BYTES
                ];

        header[0] =
                0x45;

        header[1] =
                0;

        int totalLength =
                HEADER_BYTES
                        + payloadLength;

        put16(
                header,
                2,
                totalLength
        );

        put16(
                header,
                4,
                identification
        );

        put16(
                header,
                6,
                dontFragment
                        ? 0x4000
                        : 0
        );

        header[8] =
                (
                        byte
                ) ttl;

        header[9] =
                (
                        byte
                ) protocol;

        System.arraycopy(
                source,
                0,
                header,
                12,
                4
        );

        System.arraycopy(
                destination,
                0,
                header,
                16,
                4
        );

        int checksum =
                InternetChecksum.compute(
                        header
                );

        put16(
                header,
                10,
                checksum
        );

        return header;
    }

    public int headerChecksum() {
        byte[] encoded =
                encode();

        return (
                (
                        encoded[10]
                                & 0xFF
                )
                        << 8
        )
                | (
                encoded[11]
                        & 0xFF
        );
    }

    private static void put16(
            byte[] data,
            int offset,
            int value
    ) {
        data[offset] =
                (
                        byte
                ) (
                value
                        >>> 8
        );

        data[offset + 1] =
                (
                        byte
                ) value;
    }
}

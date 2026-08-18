package com.k1ngtle.vsia.signality.engineering.wifi.ip;

public record IcmpEchoMessage(
        boolean reply,
        int identifier,
        int sequence,
        byte[] payload
) {
    public IcmpEchoMessage {
        payload =
                payload == null
                        ? new byte[0]
                        : payload.clone();
    }

    @Override
    public byte[] payload() {
        return payload.clone();
    }

    public byte[] encode() {
        byte[] out =
                new byte[
                        8
                                + payload.length
                ];

        out[0] =
                (
                        byte
                ) (
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
                payload,
                0,
                out,
                8,
                payload.length
        );

        int checksum =
                InternetChecksum.compute(
                        out
                );

        put16(
                out,
                2,
                checksum
        );

        return out;
    }

    public int checksum() {
        byte[] encoded =
                encode();

        return (
                (
                        encoded[2]
                                & 0xFF
                )
                        << 8
        )
                | (
                encoded[3]
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

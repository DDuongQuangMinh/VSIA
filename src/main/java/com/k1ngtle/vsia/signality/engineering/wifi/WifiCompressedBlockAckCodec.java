package com.k1ngtle.vsia.signality.engineering.wifi;

import java.io.ByteArrayOutputStream;

public final class WifiCompressedBlockAckCodec {
    private WifiCompressedBlockAckCodec() {
    }

    public static byte[] encode(
            WifiBlockAckBitmap bitmap,
            int tid
    ) {
        ByteArrayOutputStream out =
                new ByteArrayOutputStream(
                        12
                );

        int baControl =
                0x0004
                        | (
                        (tid & 0x0F)
                                << 12
                );

        writeU16Le(
                out,
                baControl
        );

        writeU16Le(
                out,
                (
                        bitmap.startingSequence()
                                & 0x0FFF
                )
                        << 4
        );

        long bits =
                bitmap.bitmap();

        for (int i = 0;
             i < 8;
             i++) {
            out.write(
                    (int) (
                            bits
                                    >>> (
                                    i * 8
                            )
                    )
                            & 0xFF
            );
        }

        return out.toByteArray();
    }

    public static WifiBlockAckBitmap decode(
            byte[] payload
    ) {
        if (payload == null
                || payload.length != 12) {
            throw new IllegalArgumentException(
                    "Compressed Block Ack payload must be 12 bytes"
            );
        }

        int startingSequence =
                readU16Le(
                        payload,
                        2
                )
                        >>> 4;

        WifiBlockAckBitmap result =
                new WifiBlockAckBitmap(
                        startingSequence
                );

        long bits =
                0L;

        for (int i = 0;
             i < 8;
             i++) {
            bits |=
                    (
                            payload[4 + i]
                                    & 0xFFL
                    )
                            << (
                            i * 8
                    );
        }

        for (int i = 0;
             i < 64;
             i++) {
            if ((
                    bits
                            & (
                            1L << i
                    )
            )
                    != 0L) {
                result.acknowledge(
                        (
                                startingSequence
                                        + i
                        )
                                & 0x0FFF
                );
            }
        }

        return result;
    }

    private static void writeU16Le(
            ByteArrayOutputStream out,
            int value
    ) {
        out.write(
                value
                        & 0xFF
        );

        out.write(
                (
                        value >>> 8
                )
                        & 0xFF
        );
    }

    private static int readU16Le(
            byte[] bytes,
            int offset
    ) {
        return (
                bytes[offset]
                        & 0xFF
        )
                | (
                (
                        bytes[offset + 1]
                                & 0xFF
                )
                        << 8
        );
    }
}

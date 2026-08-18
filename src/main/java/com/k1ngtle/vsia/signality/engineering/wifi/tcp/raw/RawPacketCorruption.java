package com.k1ngtle.vsia.signality.engineering.wifi.tcp.raw;

public final class RawPacketCorruption {
    private RawPacketCorruption() {
    }

    public static byte[] flipBit(
            byte[] packet,
            int byteIndex,
            int bitIndex
    ) {
        if (packet == null) {
            throw new IllegalArgumentException(
                    "packet"
            );
        }

        if (byteIndex < 0
                || byteIndex >= packet.length) {
            throw new IllegalArgumentException(
                    "byteIndex"
            );
        }

        if (bitIndex < 0
                || bitIndex > 7) {
            throw new IllegalArgumentException(
                    "bitIndex"
            );
        }

        byte[] copy =
                packet.clone();

        copy[byteIndex] =
                (
                        byte
                ) (
                copy[byteIndex]
                        ^ (
                        1
                                << bitIndex
                )
        );

        return copy;
    }
}

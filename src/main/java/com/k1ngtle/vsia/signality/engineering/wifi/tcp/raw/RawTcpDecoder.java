package com.k1ngtle.vsia.signality.engineering.wifi.tcp.raw;

import com.k1ngtle.vsia.signality.engineering.wifi.tcp.TcpFlags;
import com.k1ngtle.vsia.signality.engineering.wifi.tcp.options.TcpOptionCodec;
import com.k1ngtle.vsia.signality.engineering.wifi.tcp.options.TcpOptionSet;

import java.util.Arrays;

public final class RawTcpDecoder {
    private RawTcpDecoder() {
    }

    public static RawTcpPacket decode(
            String sourceIp,
            String destinationIp,
            byte[] segment
    ) {
        if (segment == null
                || segment.length < 20) {
            throw new IllegalArgumentException(
                    "TCP segment must contain at least 20 bytes"
            );
        }

        int dataOffsetWords =
                (
                        segment[12]
                                >>> 4
                )
                        & 0x0F;

        if (dataOffsetWords < 5) {
            throw new IllegalArgumentException(
                    "Invalid TCP Data Offset"
            );
        }

        int headerBytes =
                dataOffsetWords
                        * 4;

        if (headerBytes > segment.length) {
            throw new IllegalArgumentException(
                    "TCP header exceeds segment length"
            );
        }

        int bits =
                segment[13]
                        & 0xFF;

        TcpFlags flags =
                new TcpFlags(
                        (
                                bits
                                        & 0x01
                        )
                                != 0,
                        (
                                bits
                                        & 0x02
                        )
                                != 0,
                        (
                                bits
                                        & 0x04
                        )
                                != 0,
                        (
                                bits
                                        & 0x08
                        )
                                != 0,
                        (
                                bits
                                        & 0x10
                        )
                                != 0
                );

        byte[] optionBytes =
                headerBytes > 20
                        ? Arrays.copyOfRange(
                        segment,
                        20,
                        headerBytes
                )
                        : new byte[0];

        TcpOptionSet options =
                TcpOptionCodec.decode(
                        optionBytes
                );

        byte[] payload =
                Arrays.copyOfRange(
                        segment,
                        headerBytes,
                        segment.length
                );

        return new RawTcpPacket(
                read16(
                        segment,
                        0
                ),
                read16(
                        segment,
                        2
                ),
                read32(
                        segment,
                        4
                ),
                read32(
                        segment,
                        8
                ),
                dataOffsetWords,
                flags,
                read16(
                        segment,
                        14
                ),
                read16(
                        segment,
                        16
                ),
                read16(
                        segment,
                        18
                ),
                optionBytes,
                options,
                payload,
                RawTcpChecksum.valid(
                        sourceIp,
                        destinationIp,
                        segment
                )
        );
    }

    private static int read16(
            byte[] data,
            int offset
    ) {
        return (
                (
                        data[offset]
                                & 0xFF
                )
                        << 8
        )
                | (
                data[offset + 1]
                        & 0xFF
        );
    }

    private static long read32(
            byte[] data,
            int offset
    ) {
        return (
                (
                        long
                ) (
                        data[offset]
                                & 0xFF
                )
                        << 24
        )
                | (
                (
                        long
                ) (
                        data[offset + 1]
                                & 0xFF
                )
                        << 16
        )
                | (
                (
                        long
                ) (
                        data[offset + 2]
                                & 0xFF
                )
                        << 8
        )
                | (
                long
        ) (
                data[offset + 3]
                        & 0xFF
        );
    }
}

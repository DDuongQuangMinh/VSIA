package com.k1ngtle.vsia.signality.engineering.wifi.tcp.raw;

import com.k1ngtle.vsia.signality.engineering.wifi.ip.InternetChecksum;

import java.util.Arrays;

public final class RawIpv4Decoder {
    private RawIpv4Decoder() {
    }

    public static RawIpv4Packet decode(
            byte[] packet
    ) {
        if (packet == null
                || packet.length < 20) {
            throw new IllegalArgumentException(
                    "IPv4 packet must contain at least 20 bytes"
            );
        }

        int version =
                (
                        packet[0]
                                >>> 4
                )
                        & 0x0F;

        int ihlWords =
                packet[0]
                        & 0x0F;

        if (version != 4) {
            throw new IllegalArgumentException(
                    "Not an IPv4 packet"
            );
        }

        if (ihlWords < 5) {
            throw new IllegalArgumentException(
                    "Invalid IPv4 IHL"
            );
        }

        int headerBytes =
                ihlWords
                        * 4;

        if (headerBytes > packet.length) {
            throw new IllegalArgumentException(
                    "IPv4 header exceeds packet length"
            );
        }

        int totalLength =
                read16(
                        packet,
                        2
                );

        if (totalLength < headerBytes
                || totalLength > packet.length) {
            throw new IllegalArgumentException(
                    "Invalid IPv4 total length"
            );
        }

        int flagsAndOffset =
                read16(
                        packet,
                        6
                );

        boolean dontFragment =
                (
                        flagsAndOffset
                                & 0x4000
                )
                        != 0;

        boolean moreFragments =
                (
                        flagsAndOffset
                                & 0x2000
                )
                        != 0;

        int fragmentOffset =
                flagsAndOffset
                        & 0x1FFF;

        byte[] header =
                Arrays.copyOfRange(
                        packet,
                        0,
                        headerBytes
                );

        byte[] options =
                headerBytes > 20
                        ? Arrays.copyOfRange(
                        packet,
                        20,
                        headerBytes
                )
                        : new byte[0];

        byte[] payload =
                Arrays.copyOfRange(
                        packet,
                        headerBytes,
                        totalLength
                );

        return new RawIpv4Packet(
                version,
                ihlWords,
                packet[1]
                        & 0xFF,
                totalLength,
                read16(
                        packet,
                        4
                ),
                dontFragment,
                moreFragments,
                fragmentOffset,
                packet[8]
                        & 0xFF,
                packet[9]
                        & 0xFF,
                read16(
                        packet,
                        10
                ),
                address(
                        packet,
                        12
                ),
                address(
                        packet,
                        16
                ),
                options,
                payload,
                InternetChecksum.compute(
                        header
                )
                        == 0
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

    private static String address(
            byte[] data,
            int offset
    ) {
        return (
                data[offset]
                        & 0xFF
        )
                + "."
                + (
                data[offset + 1]
                        & 0xFF
        )
                + "."
                + (
                data[offset + 2]
                        & 0xFF
        )
                + "."
                + (
                data[offset + 3]
                        & 0xFF
        );
    }
}

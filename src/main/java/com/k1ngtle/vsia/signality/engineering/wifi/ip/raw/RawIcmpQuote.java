package com.k1ngtle.vsia.signality.engineering.wifi.ip.raw;

import com.k1ngtle.vsia.signality.engineering.wifi.tcp.raw.RawIpv4Decoder;
import com.k1ngtle.vsia.signality.engineering.wifi.tcp.raw.RawIpv4Packet;

import java.util.Arrays;

public final class RawIcmpQuote {
    public static final int MIN_TRANSPORT_QUOTE_BYTES = 8;

    private RawIcmpQuote() {
    }

    public static byte[] fromRawIpv4(byte[] rawIpv4) {
        RawIpv4Packet packet =
                RawIpv4Decoder.decode(rawIpv4);

        if (!packet.checksumValid()) {
            throw new IllegalArgumentException(
                    "Cannot quote IPv4 packet with invalid header checksum"
            );
        }

        int quoteLength =
                Math.min(
                        packet.totalLength(),
                        packet.headerBytes()
                                + MIN_TRANSPORT_QUOTE_BYTES
                );

        return Arrays.copyOf(
                rawIpv4,
                quoteLength
        );
    }

    public static byte[] fromRawIpv4(
            byte[] rawIpv4,
            int transportBytes
    ) {
        if (transportBytes < 0) {
            throw new IllegalArgumentException(
                    "transportBytes"
            );
        }

        RawIpv4Packet packet =
                RawIpv4Decoder.decode(rawIpv4);

        if (!packet.checksumValid()) {
            throw new IllegalArgumentException(
                    "Cannot quote IPv4 packet with invalid header checksum"
            );
        }

        int quoteLength =
                Math.min(
                        packet.totalLength(),
                        packet.headerBytes()
                                + transportBytes
                );

        return Arrays.copyOf(
                rawIpv4,
                quoteLength
        );
    }
}

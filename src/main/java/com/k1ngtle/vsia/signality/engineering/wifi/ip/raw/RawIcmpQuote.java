package com.k1ngtle.vsia.signality.engineering.wifi.ip.raw;

import com.k1ngtle.vsia.signality.engineering.wifi.tcp.raw.RawIpv4Decoder;
import com.k1ngtle.vsia.signality.engineering.wifi.tcp.raw.RawIpv4Packet;

import java.util.Arrays;

public final class RawIcmpQuote {
    public static final int MIN_TRANSPORT_QUOTE_BYTES = 8;

    private RawIcmpQuote() {
    }

    public static byte[] fromRawIpv4(
            byte[] rawIpv4
    ) {
        RawIpv4Packet packet =
                RawIpv4Decoder.decode(
                        rawIpv4
                );

        if (!packet.checksumValid()) {
            throw new IllegalArgumentException(
                    "Cannot quote IPv4 packet with invalid header checksum"
            );
        }

        return Arrays.copyOf(
                rawIpv4,
                Math.min(
                        packet.totalLength(),
                        packet.headerBytes()
                                + MIN_TRANSPORT_QUOTE_BYTES
                )
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
                RawIpv4Decoder.decode(
                        rawIpv4
                );

        if (!packet.checksumValid()) {
            throw new IllegalArgumentException(
                    "Cannot quote IPv4 packet with invalid header checksum"
            );
        }

        return Arrays.copyOf(
                rawIpv4,
                Math.min(
                        packet.totalLength(),
                        packet.headerBytes()
                                + transportBytes
                )
        );
    }

    public static byte[] fromPossiblyMalformedIpv4(
            byte[] rawIpv4
    ) {
        if (rawIpv4 == null
                || rawIpv4.length == 0) {
            return new byte[0];
        }

        int ihlWords =
                rawIpv4[0]
                        & 0x0F;

        int headerBytes =
                ihlWords >= 5
                        ? ihlWords * 4
                        : 20;

        headerBytes =
                Math.max(
                        20,
                        Math.min(
                                60,
                                headerBytes
                        )
                );

        int availableHeader =
                Math.min(
                        headerBytes,
                        rawIpv4.length
                );

        int quoteLength =
                Math.min(
                        rawIpv4.length,
                        availableHeader
                                + MIN_TRANSPORT_QUOTE_BYTES
                );

        return Arrays.copyOf(
                rawIpv4,
                quoteLength
        );
    }
}

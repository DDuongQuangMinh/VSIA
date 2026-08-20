package com.k1ngtle.vsia.signality.engineering.wifi.ip.raw;

import com.k1ngtle.vsia.signality.engineering.wifi.tcp.raw.RawIpv4Decoder;
import com.k1ngtle.vsia.signality.engineering.wifi.tcp.raw.RawIpv4Packet;

public final class RawIpv4Transport {
    private RawIpv4Transport() {
    }

    public static Decoded decode(
            byte[] rawIpv4
    ) {
        RawIpv4Packet ipv4 =
                RawIpv4Decoder.decode(
                        rawIpv4
                );

        if (!ipv4.checksumValid()) {
            throw new IllegalArgumentException(
                    "IPv4 checksum invalid"
            );
        }

        if (ipv4.moreFragments()
                || ipv4.fragmentOffset() != 0) {
            throw new IllegalArgumentException(
                    "IPv4 datagram must be reassembled before transport decode"
            );
        }

        if (ipv4.protocol() == 17) {
            RawUdpPacket udp =
                    RawUdpCodec.decode(
                            ipv4.sourceAddress(),
                            ipv4.destinationAddress(),
                            ipv4.payload()
                    );

            return new Decoded(
                    ipv4,
                    udp,
                    null
            );
        }

        if (ipv4.protocol() == 1) {
            RawIcmpPacket icmp =
                    RawIcmpCodec.decode(
                            ipv4.payload()
                    );

            return new Decoded(
                    ipv4,
                    null,
                    icmp
            );
        }

        return new Decoded(
                ipv4,
                null,
                null
        );
    }

    public record Decoded(
            RawIpv4Packet ipv4,
            RawUdpPacket udp,
            RawIcmpPacket icmp
    ) {
    }
}

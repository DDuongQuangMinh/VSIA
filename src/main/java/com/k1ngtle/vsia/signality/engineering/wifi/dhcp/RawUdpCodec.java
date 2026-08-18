package com.k1ngtle.vsia.signality.engineering.wifi.dhcp;

import com.k1ngtle.vsia.signality.engineering.wifi.ip.InternetChecksum;
import com.k1ngtle.vsia.signality.engineering.wifi.ip.Ipv4Address;
import com.k1ngtle.vsia.signality.engineering.wifi.ip.UdpDatagram;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;

public final class RawUdpCodec {
    private RawUdpCodec() {
    }

    public static byte[] encode(
            String sourceIp,
            String destinationIp,
            int sourcePort,
            int destinationPort,
            byte[] payload
    ) {
        return new UdpDatagram(
                sourcePort,
                destinationPort,
                payload
        ).encode(
                sourceIp,
                destinationIp
        );
    }

    public static RawUdpPacket decode(
            String sourceIp,
            String destinationIp,
            byte[] datagram
    ) {
        if (datagram == null
                || datagram.length < 8) {
            throw new IllegalArgumentException(
                    "UDP datagram must contain at least 8 bytes"
            );
        }

        int sourcePort =
                read16(
                        datagram,
                        0
                );

        int destinationPort =
                read16(
                        datagram,
                        2
                );

        int length =
                read16(
                        datagram,
                        4
                );

        int checksum =
                read16(
                        datagram,
                        6
                );

        if (length < 8
                || length > datagram.length) {
            throw new IllegalArgumentException(
                    "Invalid UDP length"
            );
        }

        byte[] exact =
                Arrays.copyOf(
                        datagram,
                        length
                );

        boolean valid =
                checksum == 0
                        || checksumValid(
                        sourceIp,
                        destinationIp,
                        exact
                );

        return new RawUdpPacket(
                sourcePort,
                destinationPort,
                length,
                checksum,
                Arrays.copyOfRange(
                        exact,
                        8,
                        exact.length
                ),
                valid
        );
    }

    private static boolean checksumValid(
            String sourceIp,
            String destinationIp,
            byte[] datagram
    ) {
        ByteArrayOutputStream pseudo =
                new ByteArrayOutputStream();

        pseudo.writeBytes(
                Ipv4Address.parse(
                        sourceIp
                )
        );

        pseudo.writeBytes(
                Ipv4Address.parse(
                        destinationIp
                )
        );

        pseudo.write(
                0
        );

        pseudo.write(
                17
        );

        pseudo.write(
                (
                        datagram.length
                                >>> 8
                )
                        & 0xFF
        );

        pseudo.write(
                datagram.length
                        & 0xFF
        );

        pseudo.writeBytes(
                datagram
        );

        if (
                (
                        pseudo.size()
                                & 1
                )
                        != 0
        ) {
            pseudo.write(
                    0
            );
        }

        return InternetChecksum.compute(
                pseudo.toByteArray()
        )
                == 0;
    }

    private static int read16(
            byte[] bytes,
            int offset
    ) {
        return (
                (
                        bytes[offset]
                                & 0xFF
                )
                        << 8
        )
                | (
                bytes[offset + 1]
                        & 0xFF
        );
    }
}

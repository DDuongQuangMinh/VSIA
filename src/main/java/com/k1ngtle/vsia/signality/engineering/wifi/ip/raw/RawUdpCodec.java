package com.k1ngtle.vsia.signality.engineering.wifi.ip.raw;

import com.k1ngtle.vsia.signality.engineering.wifi.ip.InternetChecksum;
import com.k1ngtle.vsia.signality.engineering.wifi.ip.Ipv4Address;
import com.k1ngtle.vsia.signality.engineering.wifi.ip.UdpDatagram;

import java.io.ByteArrayOutputStream;

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
                java.util.Arrays.copyOf(
                        datagram,
                        length
                );

        boolean valid =
                checksum == 0
                        || InternetChecksum.compute(
                        pseudoHeader(
                                sourceIp,
                                destinationIp,
                                exact
                        )
                ) == 0;

        byte[] payload =
                java.util.Arrays.copyOfRange(
                        exact,
                        8,
                        exact.length
                );

        return new RawUdpPacket(
                sourcePort,
                destinationPort,
                length,
                checksum,
                payload,
                valid
        );
    }

    private static byte[] pseudoHeader(
            String sourceIp,
            String destinationIp,
            byte[] datagram
    ) {
        ByteArrayOutputStream out =
                new ByteArrayOutputStream();

        out.writeBytes(
                Ipv4Address.parse(
                        sourceIp
                )
        );

        out.writeBytes(
                Ipv4Address.parse(
                        destinationIp
                )
        );

        out.write(0);
        out.write(17);
        out.write(
                (datagram.length >>> 8)
                        & 0xFF
        );
        out.write(
                datagram.length
                        & 0xFF
        );

        out.writeBytes(
                datagram
        );

        if ((out.size() & 1) != 0) {
            out.write(0);
        }

        return out.toByteArray();
    }

    private static int read16(
            byte[] data,
            int offset
    ) {
        return ((data[offset] & 0xFF) << 8)
                | (data[offset + 1] & 0xFF);
    }
}

package com.k1ngtle.vsia.signality.engineering.wifi.ip;

import java.io.ByteArrayOutputStream;

public record UdpDatagram(
        int sourcePort,
        int destinationPort,
        byte[] payload
) {
    public UdpDatagram {
        payload =
                payload == null
                        ? new byte[0]
                        : payload.clone();

        validatePort(
                sourcePort
        );

        validatePort(
                destinationPort
        );
    }

    @Override
    public byte[] payload() {
        return payload.clone();
    }

    public byte[] encode(
            String sourceIp,
            String destinationIp
    ) {
        int length =
                8
                        + payload.length;

        byte[] datagram =
                new byte[
                        length
                ];

        put16(
                datagram,
                0,
                sourcePort
        );

        put16(
                datagram,
                2,
                destinationPort
        );

        put16(
                datagram,
                4,
                length
        );

        System.arraycopy(
                payload,
                0,
                datagram,
                8,
                payload.length
        );

        byte[] pseudo =
                pseudoHeader(
                        sourceIp,
                        destinationIp,
                        length,
                        datagram
                );

        int checksum =
                InternetChecksum.compute(
                        pseudo
                );

        if (checksum == 0) {
            checksum =
                    0xFFFF;
        }

        put16(
                datagram,
                6,
                checksum
        );

        return datagram;
    }

    public int checksum(
            String sourceIp,
            String destinationIp
    ) {
        byte[] encoded =
                encode(
                        sourceIp,
                        destinationIp
                );

        return (
                (
                        encoded[6]
                                & 0xFF
                )
                        << 8
        )
                | (
                encoded[7]
                        & 0xFF
        );
    }

    private static byte[] pseudoHeader(
            String sourceIp,
            String destinationIp,
            int udpLength,
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

        out.write(
                0
        );

        out.write(
                17
        );

        out.write(
                (
                        udpLength
                                >>> 8
                )
                        & 0xFF
        );

        out.write(
                udpLength
                        & 0xFF
        );

        out.writeBytes(
                datagram
        );

        if (
                (
                        out.size()
                                & 1
                )
                        != 0
        ) {
            out.write(
                    0
            );
        }

        return out.toByteArray();
    }

    private static void validatePort(
            int value
    ) {
        if (value < 0
                || value > 65535) {
            throw new IllegalArgumentException(
                    "port"
            );
        }
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

package com.k1ngtle.vsia.signality.engineering.wifi.tcp;

import com.k1ngtle.vsia.signality.engineering.wifi.ip.InternetChecksum;
import com.k1ngtle.vsia.signality.engineering.wifi.ip.Ipv4Address;

import java.io.ByteArrayOutputStream;

public record TcpWireHeader(
        int sourcePort,
        int destinationPort,
        long sequenceNumber,
        long acknowledgementNumber,
        TcpFlags flags,
        int window,
        int urgentPointer
) {
    public static final int BASE_HEADER_BYTES =
            20;

    public static final int HEADER_BYTES =
            BASE_HEADER_BYTES;

    public byte[] encode(
            String sourceIp,
            String destinationIp,
            byte[] payload
    ) {
        return encode(
                sourceIp,
                destinationIp,
                payload,
                new byte[0]
        );
    }

    public byte[] encode(
            String sourceIp,
            String destinationIp,
            byte[] payload,
            byte[] options
    ) {
        byte[] body =
                payload == null
                        ? new byte[0]
                        : payload.clone();

        byte[] optionBytes =
                options == null
                        ? new byte[0]
                        : options.clone();

        if (optionBytes.length > 40
                || (
                optionBytes.length
                        & 3
        )
                != 0) {
            throw new IllegalArgumentException(
                    "TCP option bytes must be 0..40 and padded to a 32-bit boundary"
            );
        }

        int headerBytes =
                BASE_HEADER_BYTES
                        + optionBytes.length;

        byte[] header =
                new byte[
                        headerBytes
                ];

        put16(
                header,
                0,
                sourcePort
        );

        put16(
                header,
                2,
                destinationPort
        );

        put32(
                header,
                4,
                sequenceNumber
        );

        put32(
                header,
                8,
                acknowledgementNumber
        );

        header[12] =
                (
                        byte
                ) (
                (
                        headerBytes
                                / 4
                )
                        << 4
        );

        int flagBits =
                0;

        if (flags.fin()) {
            flagBits |=
                    0x01;
        }

        if (flags.syn()) {
            flagBits |=
                    0x02;
        }

        if (flags.rst()) {
            flagBits |=
                    0x04;
        }

        if (flags.psh()) {
            flagBits |=
                    0x08;
        }

        if (flags.ack()) {
            flagBits |=
                    0x10;
        }

        header[13] =
                (
                        byte
                ) flagBits;

        put16(
                header,
                14,
                window
        );

        put16(
                header,
                18,
                urgentPointer
        );

        if (optionBytes.length > 0) {
            System.arraycopy(
                    optionBytes,
                    0,
                    header,
                    BASE_HEADER_BYTES,
                    optionBytes.length
            );
        }

        byte[] pseudo =
                pseudoHeader(
                        sourceIp,
                        destinationIp,
                        header,
                        body
                );

        int checksum =
                InternetChecksum.compute(
                        pseudo
                );

        put16(
                header,
                16,
                checksum
        );

        return header;
    }

    public int checksum(
            String sourceIp,
            String destinationIp,
            byte[] payload
    ) {
        return checksum(
                sourceIp,
                destinationIp,
                payload,
                new byte[0]
        );
    }

    public int checksum(
            String sourceIp,
            String destinationIp,
            byte[] payload,
            byte[] options
    ) {
        byte[] encoded =
                encode(
                        sourceIp,
                        destinationIp,
                        payload,
                        options
                );

        return (
                (
                        encoded[16]
                                & 0xFF
                )
                        << 8
        )
                | (
                encoded[17]
                        & 0xFF
        );
    }

    private byte[] pseudoHeader(
            String sourceIp,
            String destinationIp,
            byte[] header,
            byte[] payload
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
                6
        );

        int tcpLength =
                header.length
                        + payload.length;

        out.write(
                (
                        tcpLength
                                >>> 8
                )
                        & 0xFF
        );

        out.write(
                tcpLength
                        & 0xFF
        );

        out.writeBytes(
                header
        );

        out.writeBytes(
                payload
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

    private static void put32(
            byte[] data,
            int offset,
            long value
    ) {
        long normalized =
                TcpSequence.normalize(
                        value
                );

        data[offset] =
                (
                        byte
                ) (
                normalized
                        >>> 24
        );

        data[offset + 1] =
                (
                        byte
                ) (
                normalized
                        >>> 16
        );

        data[offset + 2] =
                (
                        byte
                ) (
                normalized
                        >>> 8
        );

        data[offset + 3] =
                (
                        byte
                ) normalized;
    }
}

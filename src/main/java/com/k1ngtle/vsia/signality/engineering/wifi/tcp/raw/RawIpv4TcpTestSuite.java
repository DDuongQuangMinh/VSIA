package com.k1ngtle.vsia.signality.engineering.wifi.tcp.raw;

import com.k1ngtle.vsia.signality.engineering.wifi.tcp.TcpFlags;
import com.k1ngtle.vsia.signality.engineering.wifi.tcp.options.TcpOptionSet;

import java.nio.charset.StandardCharsets;
import java.util.List;

public final class RawIpv4TcpTestSuite {
    private static final String SYN_KAT_HEX =
            "45000040BEEF400040068F76C000020AC6336414"
                    + "C35000501234567800000000B002FFFFEF2B0000"
                    + "020404B00402010303020101080A00003039000000000000";

    private RawIpv4TcpTestSuite() {
    }

    public static List<RawIpv4TcpTestResult> runAll() {
        return List.of(
                exactSynKnownAnswer(),
                decodeKnownAnswer(),
                payloadRoundTrip(),
                ipv4CorruptionDetection(),
                tcpCorruptionDetection(),
                invalidProtocolRejected(),
                fragmentedPacketRejected(),
                hexRoundTrip()
        );
    }

    private static RawIpv4TcpTestResult exactSynKnownAnswer() {
        byte[] raw =
                RawIpv4TcpCodec.encode(
                        "192.0.2.10",
                        "198.51.100.20",
                        50000,
                        80,
                        0x12345678L,
                        0L,
                        TcpFlags.synOnly(),
                        65535,
                        64,
                        0xBEEF,
                        true,
                        TcpOptionSet.synOffer(
                                1200,
                                2,
                                true,
                                12345L
                        ),
                        new byte[0]
                );

        String actual =
                RawPacketHex.encode(
                        raw
                );

        return result(
                "wifi-w195-exact-syn-kat",
                SYN_KAT_HEX.equals(
                        actual
                ),
                "Raw encoder must reproduce the pinned 64-byte IPv4/TCP SYN known-answer vector exactly"
        );
    }

    private static RawIpv4TcpTestResult decodeKnownAnswer() {
        RawIpv4TcpPacket decoded =
                RawIpv4TcpCodec.decode(
                        RawPacketHex.decode(
                                SYN_KAT_HEX
                        )
                );

        return result(
                "wifi-w195-decode-syn-kat",
                decoded.valid()
                        && decoded.ipv4().version() == 4
                        && decoded.ipv4().ihlWords() == 5
                        && decoded.ipv4().totalLength() == 64
                        && decoded.ipv4().identification() == 0xBEEF
                        && decoded.ipv4().dontFragment()
                        && decoded.ipv4().ttl() == 64
                        && decoded.ipv4().protocol() == 6
                        && "192.0.2.10".equals(
                        decoded.ipv4().sourceAddress()
                )
                        && "198.51.100.20".equals(
                        decoded.ipv4().destinationAddress()
                )
                        && decoded.tcp().sourcePort() == 50000
                        && decoded.tcp().destinationPort() == 80
                        && decoded.tcp().sequenceNumber() == 0x12345678L
                        && decoded.tcp().flags().syn()
                        && decoded.tcp().headerBytes() == 44
                        && decoded.tcp().options().mss() == 1200
                        && decoded.tcp().options().windowScale() == 2
                        && decoded.tcp().options().sackPermitted()
                        && decoded.tcp().options().timestampValue() == 12345L,
                "Raw decoder must recover IPv4, TCP, and SYN option fields from the pinned vector"
        );
    }

    private static RawIpv4TcpTestResult payloadRoundTrip() {
        byte[] payload =
                "GET / HTTP/1.1\r\nHost: example.test\r\n\r\n"
                        .getBytes(
                                StandardCharsets.US_ASCII
                        );

        TcpOptionSet options =
                new TcpOptionSet(
                        TcpOptionSet.ABSENT,
                        TcpOptionSet.ABSENT,
                        false,
                        List.of(),
                        9000L,
                        8000L
                );

        byte[] raw =
                RawIpv4TcpCodec.encode(
                        "10.0.0.10",
                        "10.0.0.20",
                        49152,
                        80,
                        1001L,
                        5001L,
                        TcpFlags.data(),
                        32768,
                        51,
                        0x1234,
                        true,
                        options,
                        payload
                );

        RawIpv4TcpPacket decoded =
                RawIpv4TcpCodec.decode(
                        raw
                );

        return result(
                "wifi-w195-payload-roundtrip",
                decoded.valid()
                        && decoded.tcp().flags().ack()
                        && decoded.tcp().flags().psh()
                        && decoded.tcp().options().timestampValue() == 9000L
                        && java.util.Arrays.equals(
                        payload,
                        decoded.tcp().payload()
                )
                        && decoded.ipv4().totalLength() == raw.length,
                "Raw IPv4/TCP packet with Timestamp option and application bytes must round-trip exactly"
        );
    }

    private static RawIpv4TcpTestResult ipv4CorruptionDetection() {
        byte[] raw =
                RawPacketHex.decode(
                        SYN_KAT_HEX
                );

        byte[] corrupted =
                RawPacketCorruption.flipBit(
                        raw,
                        8,
                        0
                );

        RawIpv4Packet decoded =
                RawIpv4Decoder.decode(
                        corrupted
                );

        return result(
                "wifi-w195-ipv4-corruption",
                !decoded.checksumValid(),
                "One-bit corruption in the IPv4 header must fail the IPv4 header checksum"
        );
    }

    private static RawIpv4TcpTestResult tcpCorruptionDetection() {
        byte[] raw =
                RawPacketHex.decode(
                        SYN_KAT_HEX
                );

        byte[] corrupted =
                RawPacketCorruption.flipBit(
                        raw,
                        raw.length - 1,
                        0
                );

        RawIpv4TcpPacket decoded =
                RawIpv4TcpCodec.decode(
                        corrupted
                );

        return result(
                "wifi-w195-tcp-corruption",
                decoded.ipv4().checksumValid()
                        && !decoded.tcp().checksumValid(),
                "One-bit corruption inside TCP/options must preserve IPv4 checksum but fail the TCP pseudo-header checksum"
        );
    }

    private static RawIpv4TcpTestResult invalidProtocolRejected() {
        byte[] raw =
                RawPacketHex.decode(
                        SYN_KAT_HEX
                );

        byte[] modified =
                raw.clone();

        modified[9] =
                17;

        modified[10] =
                0;

        modified[11] =
                0;

        int checksum =
                com.k1ngtle.vsia.signality.engineering.wifi.ip.InternetChecksum
                        .compute(
                                java.util.Arrays.copyOfRange(
                                        modified,
                                        0,
                                        20
                                )
                        );

        modified[10] =
                (
                        byte
                ) (
                checksum >>> 8
        );

        modified[11] =
                (
                        byte
                ) checksum;

        boolean rejected =
                false;

        try {
            RawIpv4TcpCodec.decode(
                    modified
            );
        } catch (IllegalArgumentException expected) {
            rejected =
                    true;
        }

        return result(
                "wifi-w195-protocol-reject",
                rejected,
                "Raw IPv4/TCP decoder must reject an IPv4 packet whose Protocol field is not 6"
        );
    }

    private static RawIpv4TcpTestResult fragmentedPacketRejected() {
        byte[] raw =
                RawPacketHex.decode(
                        SYN_KAT_HEX
                );

        byte[] modified =
                raw.clone();

        modified[6] =
                0x20;

        modified[7] =
                0x00;

        modified[10] =
                0;

        modified[11] =
                0;

        int checksum =
                com.k1ngtle.vsia.signality.engineering.wifi.ip.InternetChecksum
                        .compute(
                                java.util.Arrays.copyOfRange(
                                        modified,
                                        0,
                                        20
                                )
                        );

        modified[10] =
                (
                        byte
                ) (
                checksum >>> 8
        );

        modified[11] =
                (
                        byte
                ) checksum;

        boolean rejected =
                false;

        try {
            RawIpv4TcpCodec.decode(
                    modified
            );
        } catch (IllegalArgumentException expected) {
            rejected =
                    true;
        }

        return result(
                "wifi-w195-fragment-reject",
                rejected,
                "TCP lab decoder must reject fragmented IPv4 input until reassembly exists"
        );
    }

    private static RawIpv4TcpTestResult hexRoundTrip() {
        byte[] bytes =
                RawPacketHex.decode(
                        "45 00 00 28 DE AD 40 00"
                                + " 40 06 00 00 C0 00 02 01"
                );

        return result(
                "wifi-w195-hex-roundtrip",
                "45000028DEAD400040060000C0000201"
                        .equals(
                                RawPacketHex.encode(
                                        bytes
                                )
                        ),
                "Hex utility must provide deterministic byte-oriented lab vectors"
        );
    }

    private static RawIpv4TcpTestResult result(
            String id,
            boolean passed,
            String detail
    ) {
        return new RawIpv4TcpTestResult(
                id,
                passed,
                detail
        );
    }
}

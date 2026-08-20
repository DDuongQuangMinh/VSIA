package com.k1ngtle.vsia.signality.engineering.wifi.ip.raw;

import com.k1ngtle.vsia.signality.engineering.wifi.tcp.raw.RawIpv4Decoder;
import com.k1ngtle.vsia.signality.engineering.wifi.tcp.raw.RawIpv4Packet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class RawIcmpSemanticsTestSuite {
    private RawIcmpSemanticsTestSuite() {
    }

    private static final byte[] OPTIONS =
            new byte[] {
                    (byte) 0x94,
                    0x04,
                    0x00,
                    0x00,
                    0x07,
                    0x07,
                    0x04,
                    0x00,
                    0x00,
                    0x00,
                    0x00,
                    0x00
            };

    public static List<RawIcmpSemanticsTestResult> runAll() {
        List<RawIcmpSemanticsTestResult> out =
                new ArrayList<>();

        byte[] udpPayload =
                payload(64);

        byte[] udp =
                RawUdpCodec.encode(
                        "192.0.2.10",
                        "198.51.100.20",
                        50000,
                        40001,
                        udpPayload
                );

        byte[] ordinary =
                RawIpv4Encoder.encode(
                        "192.0.2.10",
                        "198.51.100.20",
                        0,
                        0x1301,
                        false,
                        false,
                        0,
                        64,
                        17,
                        udp
                );

        byte[] quote =
                RawIcmpQuote.fromRawIpv4(
                        ordinary
                );

        check(
                out,
                "wifi-w1130-quote-ihl5-length",
                quote.length == 28,
                "IHL=5 ICMP quote must contain 20-byte header + 8 bytes"
        );

        check(
                out,
                "wifi-w1130-quote-prefix",
                Arrays.equals(
                        quote,
                        Arrays.copyOf(
                                ordinary,
                                28
                        )
                ),
                "ICMP quote must preserve the original packet prefix"
        );

        byte[] optionPacket =
                RawIpv4Encoder.encodeWithOptions(
                        "192.0.2.10",
                        "198.51.100.20",
                        0,
                        0x1302,
                        false,
                        false,
                        0,
                        64,
                        17,
                        OPTIONS,
                        udp
                );

        byte[] optionQuote =
                RawIcmpQuote.fromRawIpv4(
                        optionPacket
                );

        RawIpv4Packet optionDecoded =
                RawIpv4Decoder.decode(
                        optionPacket
                );

        check(
                out,
                "wifi-w1130-quote-variable-ihl-length",
                optionQuote.length
                        == optionDecoded.headerBytes() + 8,
                "ICMP quote must use the real IPv4 header length"
        );

        check(
                out,
                "wifi-w1130-quote-options-preserved",
                Arrays.equals(
                        Arrays.copyOfRange(
                                optionQuote,
                                20,
                                optionDecoded.headerBytes()
                        ),
                        OPTIONS
                ),
                "ICMP quote stripped IPv4 options"
        );

        byte[] shortPayloadPacket =
                RawIpv4Encoder.encode(
                        "192.0.2.10",
                        "198.51.100.20",
                        0,
                        0x1303,
                        false,
                        false,
                        0,
                        64,
                        99,
                        new byte[] {
                                1, 2, 3
                        }
                );

        check(
                out,
                "wifi-w1130-quote-short-payload",
                RawIcmpQuote.fromRawIpv4(
                        shortPayloadPacket
                ).length == 23,
                "quote must stop at the real packet end"
        );

        check(
                out,
                "wifi-w1130-unicast-udp-allowed",
                RawIcmpErrorPolicy.shouldSend(
                        ordinary
                ),
                "ordinary unicast UDP should allow an ICMP error"
        );

        List<byte[]> fragments =
                RawIpv4Fragmenter.fragment(
                        RawIpv4Encoder.encode(
                                "192.0.2.10",
                                "198.51.100.20",
                                0,
                                0x1304,
                                false,
                                false,
                                0,
                                64,
                                17,
                                new byte[4000]
                        ),
                        1500
                );

        check(
                out,
                "wifi-w1130-fragment-zero-allowed",
                RawIcmpErrorPolicy.shouldSend(
                        fragments.get(0)
                ),
                "fragment zero may generate an ICMP error"
        );

        RawIcmpErrorPolicy.Decision nonInitial =
                RawIcmpErrorPolicy.evaluate(
                        fragments.get(1)
                );

        check(
                out,
                "wifi-w1130-noninitial-suppressed",
                !nonInitial.allowed()
                        && "NON_INITIAL_FRAGMENT".equals(
                        nonInitial.reason()
                ),
                "non-initial fragment must suppress ICMP errors"
        );

        byte[] echo =
                RawIcmpCodec.encodeEcho(
                        false,
                        0x1234,
                        7,
                        payload(16)
                );

        byte[] echoIp =
                RawIpv4Encoder.encode(
                        "192.0.2.10",
                        "198.51.100.20",
                        0,
                        0x1305,
                        false,
                        false,
                        0,
                        64,
                        1,
                        echo
                );

        check(
                out,
                "wifi-w1130-icmp-echo-allowed",
                RawIcmpErrorPolicy.shouldSend(
                        echoIp
                ),
                "ICMP informational message should not be treated as an ICMP error"
        );

        byte[] innerError =
                RawIcmpCodec.encodeError(
                        3,
                        0,
                        0,
                        RawIcmpQuote.fromRawIpv4(
                                ordinary
                        )
                );

        byte[] innerErrorIp =
                RawIpv4Encoder.encode(
                        "192.0.2.10",
                        "198.51.100.20",
                        0,
                        0x1306,
                        false,
                        false,
                        0,
                        64,
                        1,
                        innerError
                );

        RawIcmpErrorPolicy.Decision errorToError =
                RawIcmpErrorPolicy.evaluate(
                        innerErrorIp
                );

        check(
                out,
                "wifi-w1130-error-to-error-suppressed",
                !errorToError.allowed()
                        && "ICMP_ERROR_TO_ICMP_ERROR".equals(
                        errorToError.reason()
                ),
                "ICMP errors must not trigger another ICMP error"
        );

        check(
                out,
                "wifi-w1130-icmp-error-type3",
                RawIcmpErrorPolicy.isIcmpErrorType(3),
                "Type 3 must be an ICMP error type"
        );

        check(
                out,
                "wifi-w1130-icmp-error-type11",
                RawIcmpErrorPolicy.isIcmpErrorType(11),
                "Type 11 must be an ICMP error type"
        );

        check(
                out,
                "wifi-w1130-icmp-echo-not-error-type",
                !RawIcmpErrorPolicy.isIcmpErrorType(8),
                "Echo Request is informational, not an error"
        );

        byte[] multicastDestination =
                RawIpv4Encoder.encode(
                        "192.0.2.10",
                        "224.0.0.1",
                        0,
                        0x1307,
                        false,
                        false,
                        0,
                        64,
                        17,
                        new byte[16]
                );

        check(
                out,
                "wifi-w1130-multicast-dst-suppressed",
                !RawIcmpErrorPolicy.shouldSend(
                        multicastDestination
                ),
                "multicast destination must suppress ICMP errors"
        );

        byte[] multicastSource =
                RawIpv4Encoder.encode(
                        "224.0.0.1",
                        "198.51.100.20",
                        0,
                        0x1308,
                        false,
                        false,
                        0,
                        64,
                        17,
                        new byte[16]
                );

        check(
                out,
                "wifi-w1130-multicast-src-suppressed",
                !RawIcmpErrorPolicy.shouldSend(
                        multicastSource
                ),
                "multicast source must suppress ICMP errors"
        );

        byte[] broadcastDestination =
                RawIpv4Encoder.encode(
                        "192.0.2.10",
                        "255.255.255.255",
                        0,
                        0x1309,
                        false,
                        false,
                        0,
                        64,
                        17,
                        new byte[16]
                );

        check(
                out,
                "wifi-w1130-broadcast-dst-suppressed",
                !RawIcmpErrorPolicy.shouldSend(
                        broadcastDestination
                ),
                "limited broadcast destination must suppress ICMP errors"
        );

        byte[] unspecifiedSource =
                RawIpv4Encoder.encode(
                        "0.0.0.0",
                        "198.51.100.20",
                        0,
                        0x1310,
                        false,
                        false,
                        0,
                        64,
                        17,
                        new byte[16]
                );

        check(
                out,
                "wifi-w1130-unspecified-src-suppressed",
                !RawIcmpErrorPolicy.shouldSend(
                        unspecifiedSource
                ),
                "unspecified source must suppress ICMP errors"
        );

        byte[] corrupted =
                ordinary.clone();

        corrupted[8] =
                (byte) (
                        corrupted[8] - 1
                );

        RawIcmpErrorPolicy.Decision badChecksum =
                RawIcmpErrorPolicy.evaluate(
                        corrupted
                );

        check(
                out,
                "wifi-w1130-bad-checksum-suppressed",
                !badChecksum.allowed()
                        && "BAD_IPV4_CHECKSUM".equals(
                        badChecksum.reason()
                ),
                "bad IPv4 checksum must suppress generated ICMP"
        );

        byte[] icmp =
                RawIcmpCodec.encodeError(
                        11,
                        0,
                        0,
                        optionQuote
                );

        RawIcmpPacket decodedIcmp =
                RawIcmpCodec.decode(
                        icmp
                );

        check(
                out,
                "wifi-w1130-error-checksum",
                decodedIcmp.checksumValid(),
                "ICMP error checksum invalid"
        );

        check(
                out,
                "wifi-w1130-error-quote-roundtrip",
                Arrays.equals(
                        decodedIcmp.payload(),
                        optionQuote
                ),
                "ICMP error did not preserve the quote bytes"
        );

        byte[] code4 =
                RawIcmpCodec.encodeError(
                        3,
                        4,
                        1000,
                        quote
                );

        RawIcmpPacket decodedCode4 =
                RawIcmpCodec.decode(
                        code4
                );

        check(
                out,
                "wifi-w1130-code4-mtu-field",
                decodedCode4.type() == 3
                        && decodedCode4.code() == 4
                        && decodedCode4.nextHopMtu() == 1000,
                "Type 3 Code 4 next-hop MTU field regressed"
        );

        return List.copyOf(
                out
        );
    }

    private static byte[] payload(
            int length
    ) {
        byte[] out =
                new byte[length];

        for (int i = 0;
             i < out.length;
             i++) {
            out[i] =
                    (byte) (
                            i * 31 + 13
                    );
        }

        return out;
    }

    private static void check(
            List<RawIcmpSemanticsTestResult> out,
            String id,
            boolean passed,
            String detail
    ) {
        out.add(
                new RawIcmpSemanticsTestResult(
                        id,
                        passed,
                        passed ? "PASS" : detail
                )
        );
    }
}

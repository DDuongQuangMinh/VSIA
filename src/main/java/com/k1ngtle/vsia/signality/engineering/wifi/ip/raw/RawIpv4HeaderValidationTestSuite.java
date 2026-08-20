package com.k1ngtle.vsia.signality.engineering.wifi.ip.raw;

import com.k1ngtle.vsia.signality.engineering.wifi.ip.router.IcmpErrorModel;

import java.util.ArrayList;
import java.util.List;

public final class RawIpv4HeaderValidationTestSuite {
    private RawIpv4HeaderValidationTestSuite() {
    }

    public static List<RawIpv4HeaderValidationTestResult> runAll() {
        List<RawIpv4HeaderValidationTestResult> out =
                new ArrayList<>();

        byte[] valid =
                RawIpv4Encoder.encode(
                        "192.0.2.10",
                        "198.51.100.20",
                        0,
                        0x1400,
                        false,
                        false,
                        0,
                        64,
                        17,
                        new byte[64]
                );

        var validResult =
                RawIpv4HeaderValidator.validate(
                        valid
                );

        check(
                out,
                "wifi-w1140-valid-header",
                validResult.valid()
                        && validResult.headerBytes() == 20,
                "ordinary IPv4 header was rejected"
        );

        byte[] options =
                new byte[] {
                        (byte) 0x94,
                        0x04,
                        0x00,
                        0x00
                };

        byte[] validOptions =
                RawIpv4Encoder.encodeWithOptions(
                        "192.0.2.10",
                        "198.51.100.20",
                        0,
                        0x1401,
                        false,
                        false,
                        0,
                        64,
                        17,
                        options,
                        new byte[64]
                );

        check(
                out,
                "wifi-w1140-valid-options",
                RawIpv4HeaderValidator.validate(
                        validOptions
                ).valid(),
                "valid copied IPv4 option was rejected"
        );

        byte[] badIhl =
                RawIpv4HeaderFaultFactory.apply(
                        valid,
                        "bad_ihl"
                );

        var badIhlResult =
                RawIpv4HeaderValidator.validate(
                        badIhl
                );

        check(
                out,
                "wifi-w1140-bad-ihl",
                !badIhlResult.valid()
                        && badIhlResult.issue()
                        == RawIpv4HeaderValidator.Issue.INVALID_IHL
                        && badIhlResult.pointer() == 0
                        && badIhlResult.sendParameterProblem(),
                "invalid IHL did not produce pointer 0 Parameter Problem"
        );

        byte[] badLength =
                RawIpv4HeaderFaultFactory.apply(
                        valid,
                        "bad_total_length"
                );

        var badLengthResult =
                RawIpv4HeaderValidator.validate(
                        badLength
                );

        check(
                out,
                "wifi-w1140-bad-total-length",
                !badLengthResult.valid()
                        && badLengthResult.issue()
                        == RawIpv4HeaderValidator.Issue.BAD_TOTAL_LENGTH
                        && badLengthResult.pointer() == 2
                        && badLengthResult.icmpCode() == 2
                        && badLengthResult.sendParameterProblem(),
                "bad Total Length did not map to Type 12 Code 2"
        );

        byte[] truncated =
                java.util.Arrays.copyOf(
                        valid,
                        30
                );

        var truncatedResult =
                RawIpv4HeaderValidator.validate(
                        truncated
                );

        check(
                out,
                "wifi-w1140-truncated-packet",
                !truncatedResult.valid()
                        && truncatedResult.issue()
                        == RawIpv4HeaderValidator.Issue.TRUNCATED_PACKET
                        && !truncatedResult.sendParameterProblem(),
                "truncated receive should be dropped silently"
        );

        byte[] badChecksum =
                RawIpv4HeaderFaultFactory.apply(
                        valid,
                        "bad_checksum"
                );

        var checksumResult =
                RawIpv4HeaderValidator.validate(
                        badChecksum
                );

        check(
                out,
                "wifi-w1140-bad-checksum",
                !checksumResult.valid()
                        && checksumResult.issue()
                        == RawIpv4HeaderValidator.Issue.BAD_HEADER_CHECKSUM
                        && !checksumResult.sendParameterProblem(),
                "bad header checksum should be silent"
        );

        byte[] reserved =
                RawIpv4HeaderFaultFactory.apply(
                        valid,
                        "reserved_flag"
                );

        var reservedResult =
                RawIpv4HeaderValidator.validate(
                        reserved
                );

        check(
                out,
                "wifi-w1140-reserved-flag",
                !reservedResult.valid()
                        && reservedResult.issue()
                        == RawIpv4HeaderValidator.Issue.RESERVED_FLAG_SET
                        && reservedResult.pointer() == 6
                        && reservedResult.sendParameterProblem(),
                "reserved IPv4 flag was not rejected with pointer 6"
        );

        byte[] dfMf =
                RawIpv4HeaderFaultFactory.apply(
                        valid,
                        "df_mf_conflict"
                );

        var dfMfResult =
                RawIpv4HeaderValidator.validate(
                        dfMf
                );

        check(
                out,
                "wifi-w1140-df-mf-conflict",
                !dfMfResult.valid()
                        && dfMfResult.issue()
                        == RawIpv4HeaderValidator.Issue.DF_MF_CONFLICT
                        && dfMfResult.pointer() == 6,
                "DF+MF conflict was accepted"
        );

        byte[] malformedOption =
                RawIpv4HeaderFaultFactory.apply(
                        valid,
                        "malformed_option"
                );

        var malformedOptionResult =
                RawIpv4HeaderValidator.validate(
                        malformedOption
                );

        check(
                out,
                "wifi-w1140-malformed-option",
                !malformedOptionResult.valid()
                        && malformedOptionResult.issue()
                        == RawIpv4HeaderValidator.Issue.INVALID_OPTION_LENGTH
                        && malformedOptionResult.pointer() == 21
                        && malformedOptionResult.sendParameterProblem(),
                "malformed option length did not identify byte 21"
        );

        byte[] eolPacket =
                RawIpv4Encoder.encodeWithOptions(
                        "192.0.2.10",
                        "198.51.100.20",
                        0,
                        0x1402,
                        false,
                        false,
                        0,
                        64,
                        17,
                        new byte[] {
                                0, 0, 0, 0
                        },
                        new byte[8]
                );

        eolPacket[21] =
                1;

        recompute(
                eolPacket,
                24
        );

        var eolResult =
                RawIpv4HeaderValidator.validate(
                        eolPacket
                );

        check(
                out,
                "wifi-w1140-nonzero-after-eol",
                !eolResult.valid()
                        && eolResult.issue()
                        == RawIpv4HeaderValidator.Issue.NON_ZERO_AFTER_EOL
                        && eolResult.pointer() == 21,
                "non-zero option byte after EOL was accepted"
        );

        byte[] nonInitialCopyViolation =
                RawIpv4Encoder.encodeWithOptions(
                        "192.0.2.10",
                        "198.51.100.20",
                        0,
                        0x1403,
                        false,
                        true,
                        1,
                        64,
                        17,
                        new byte[] {
                                (byte) 0x94,
                                0x04,
                                0x00,
                                0x00
                        },
                        new byte[8]
                );

        nonInitialCopyViolation[20] =
                0x07;

        recompute(
                nonInitialCopyViolation,
                24
        );

        var copyViolation =
                RawIpv4HeaderValidator.validate(
                        nonInitialCopyViolation
                );

        check(
                out,
                "wifi-w1140-noninitial-copy-rule",
                !copyViolation.valid()
                        && copyViolation.issue()
                        == RawIpv4HeaderValidator.Issue.NON_COPIED_OPTION_ON_NON_INITIAL_FRAGMENT
                        && !copyViolation.sendParameterProblem(),
                "copy=0 option on non-initial fragment was not suppressed"
        );

        byte[] multicast =
                RawIpv4Encoder.encode(
                        "192.0.2.10",
                        "224.0.0.1",
                        0,
                        0x1404,
                        false,
                        false,
                        0,
                        64,
                        17,
                        new byte[8]
                );

        multicast =
                RawIpv4HeaderFaultFactory.apply(
                        multicast,
                        "reserved_flag"
                );

        check(
                out,
                "wifi-w1140-multicast-suppression",
                !RawIpv4HeaderValidator.validate(
                        multicast
                ).sendParameterProblem(),
                "multicast destination generated Parameter Problem"
        );

        byte[] unspecified =
                RawIpv4Encoder.encode(
                        "0.0.0.0",
                        "198.51.100.20",
                        0,
                        0x1405,
                        false,
                        false,
                        0,
                        64,
                        17,
                        new byte[8]
                );

        unspecified =
                RawIpv4HeaderFaultFactory.apply(
                        unspecified,
                        "reserved_flag"
                );

        check(
                out,
                "wifi-w1140-unspecified-source-suppression",
                !RawIpv4HeaderValidator.validate(
                        unspecified
                ).sendParameterProblem(),
                "0.0.0.0 source generated Parameter Problem"
        );

        check(
                out,
                "wifi-w1140-short-header-silent",
                !RawIpv4HeaderValidator.validate(
                        new byte[12]
                ).sendParameterProblem(),
                "short base header should be silent"
        );

        byte[] notIpv4 =
                valid.clone();

        notIpv4[0] =
                (byte) (
                        0x60
                                | (notIpv4[0] & 0x0F)
                );

        check(
                out,
                "wifi-w1140-not-ipv4-silent",
                RawIpv4HeaderValidator.validate(
                        notIpv4
                ).issue()
                        == RawIpv4HeaderValidator.Issue.NOT_IPV4
                        && !RawIpv4HeaderValidator.validate(
                        notIpv4
                ).sendParameterProblem(),
                "non-IPv4 version should be ignored by IPv4 ICMP logic"
        );

        byte[] quote =
                RawIcmpQuote.fromPossiblyMalformedIpv4(
                        malformedOption
                );

        check(
                out,
                "wifi-w1140-malformed-quote",
                quote.length == 32,
                "malformed IHL=6 packet should still provide 24+8 quote bytes"
        );

        byte[] parameter =
                IcmpErrorModel.encodeParameterProblem(
                        0,
                        6,
                        quote
                );

        check(
                out,
                "wifi-w1140-parameter-type",
                (parameter[0] & 0xFF) == 12
                        && (parameter[1] & 0xFF) == 0,
                "Parameter Problem type/code incorrect"
        );

        check(
                out,
                "wifi-w1140-parameter-pointer",
                (parameter[4] & 0xFF) == 6
                        && parameter[5] == 0
                        && parameter[6] == 0
                        && parameter[7] == 0,
                "Parameter Problem pointer not encoded in high-order octet"
        );

        check(
                out,
                "wifi-w1140-parameter-checksum",
                IcmpErrorModel.internetChecksum(
                        parameter
                ) == 0,
                "Parameter Problem checksum invalid"
        );

        byte[] badLengthIcmp =
                IcmpErrorModel.encodeParameterProblem(
                        2,
                        2,
                        RawIcmpQuote.fromPossiblyMalformedIpv4(
                                badLength
                        )
                );

        check(
                out,
                "wifi-w1140-code2-bad-length",
                (badLengthIcmp[0] & 0xFF) == 12
                        && (badLengthIcmp[1] & 0xFF) == 2
                        && (badLengthIcmp[4] & 0xFF) == 2,
                "bad-length Parameter Problem encoding incorrect"
        );

        check(
                out,
                "wifi-w1140-fault-reserved-deterministic",
                RawIpv4HeaderValidator.validate(
                        RawIpv4HeaderFaultFactory.apply(
                                valid,
                                "reserved_flag"
                        )
                ).issue()
                        == RawIpv4HeaderValidator.Issue.RESERVED_FLAG_SET,
                "reserved-flag fault factory is not deterministic"
        );

        check(
                out,
                "wifi-w1140-fault-checksum-deterministic",
                RawIpv4HeaderValidator.validate(
                        RawIpv4HeaderFaultFactory.apply(
                                valid,
                                "bad_checksum"
                        )
                ).issue()
                        == RawIpv4HeaderValidator.Issue.BAD_HEADER_CHECKSUM,
                "checksum fault factory is not deterministic"
        );

        check(
                out,
                "wifi-w1140-fault-option-deterministic",
                RawIpv4HeaderValidator.validate(
                        RawIpv4HeaderFaultFactory.apply(
                                valid,
                                "malformed_option"
                        )
                ).issue()
                        == RawIpv4HeaderValidator.Issue.INVALID_OPTION_LENGTH,
                "option fault factory is not deterministic"
        );

        byte[] missingOption =
                IcmpErrorModel.encodeParameterProblem(
                        1,
                        20,
                        quote
                );

        check(
                out,
                "wifi-w1140-code1-supported",
                (missingOption[0] & 0xFF) == 12
                        && (missingOption[1] & 0xFF) == 1
                        && (missingOption[4] & 0xFF) == 20
                        && IcmpErrorModel.internetChecksum(
                        missingOption
                ) == 0,
                "Parameter Problem Code 1 encoding is not supported"
        );

        return List.copyOf(
                out
        );
    }

    private static void recompute(
            byte[] raw,
            int headerBytes
    ) {
        raw[10] =
                0;

        raw[11] =
                0;

        int checksum =
                com.k1ngtle.vsia.signality.engineering.wifi.ip.InternetChecksum.compute(
                        java.util.Arrays.copyOfRange(
                                raw,
                                0,
                                headerBytes
                        )
                );

        raw[10] =
                (byte) (
                        checksum >>> 8
                );

        raw[11] =
                (byte) checksum;
    }

    private static void check(
            List<RawIpv4HeaderValidationTestResult> out,
            String id,
            boolean passed,
            String detail
    ) {
        out.add(
                new RawIpv4HeaderValidationTestResult(
                        id,
                        passed,
                        passed ? "PASS" : detail
                )
        );
    }
}

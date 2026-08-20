package com.k1ngtle.vsia.signality.engineering.wifi.ip.raw;

import com.k1ngtle.vsia.signality.engineering.wifi.tcp.raw.RawIpv4Decoder;
import com.k1ngtle.vsia.signality.engineering.wifi.tcp.raw.RawIpv4Packet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class RawIpv4OptionsTestSuite {
    private RawIpv4OptionsTestSuite() {
    }

    private static final byte[] ROUTER_ALERT =
            new byte[] {
                    (byte) 0x94,
                    0x04,
                    0x00,
                    0x00
            };

    private static final byte[] RECORD_ROUTE =
            new byte[] {
                    0x07,
                    0x07,
                    0x04,
                    0x00,
                    0x00,
                    0x00,
                    0x00
            };

    private static final byte[] FULL_OPTIONS =
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

    public static List<RawIpv4OptionsTestResult> runAll() {
        List<RawIpv4OptionsTestResult> out =
                new ArrayList<>();

        byte[] payload =
                new byte[4000];

        for (int i = 0;
             i < payload.length;
             i++) {
            payload[i] =
                    (byte) (
                            i * 23 + 9
                    );
        }

        byte[] raw =
                RawIpv4Encoder.encodeWithOptions(
                        "192.168.1.100",
                        "192.168.2.20",
                        0,
                        0x1200,
                        false,
                        false,
                        0,
                        64,
                        17,
                        FULL_OPTIONS,
                        payload
                );

        RawIpv4Packet decoded =
                RawIpv4Decoder.decode(
                        raw
                );

        check(
                out,
                "wifi-w1120-encoder-ihl",
                decoded.ihlWords() == 8
                        && decoded.headerBytes() == 32,
                "IHL must describe 32-byte header"
        );

        check(
                out,
                "wifi-w1120-encoder-options-roundtrip",
                Arrays.equals(
                        decoded.options(),
                        FULL_OPTIONS
                ),
                "options changed during encode/decode"
        );

        check(
                out,
                "wifi-w1120-header-checksum",
                decoded.checksumValid(),
                "options header checksum invalid"
        );

        RawIpv4Options.Parsed parsed =
                RawIpv4Options.parse(
                        FULL_OPTIONS
                );

        check(
                out,
                "wifi-w1120-option-parser-count",
                parsed.options().size() == 3,
                "expected Router Alert, Record Route and EOL"
        );

        check(
                out,
                "wifi-w1120-copy-bit",
                parsed.options().get(0).copyToFragments()
                        && !parsed.options().get(1).copyToFragments(),
                "IPv4 option copy bit decoded incorrectly"
        );

        check(
                out,
                "wifi-w1120-router-alert-fields",
                parsed.options().get(0).type() == 0x94
                        && parsed.options().get(0).optionClass() == 0
                        && parsed.options().get(0).number() == 20,
                "Router Alert option type fields incorrect"
        );

        check(
                out,
                "wifi-w1120-record-route-fields",
                parsed.options().get(1).type() == 7
                        && parsed.options().get(1).optionClass() == 0
                        && parsed.options().get(1).number() == 7,
                "Record Route option type fields incorrect"
        );

        byte[] copied =
                RawIpv4Options.copiedForNonInitialFragment(
                        FULL_OPTIONS
                );

        check(
                out,
                "wifi-w1120-copy-selection",
                Arrays.equals(
                        copied,
                        ROUTER_ALERT
                ),
                "non-initial option set must contain only Router Alert"
        );

        boolean malformedLengthRejected =
                false;

        try {
            RawIpv4Options.parse(
                    new byte[] {
                            0x07,
                            0x01,
                            0x00,
                            0x00
                    }
            );
        } catch (IllegalArgumentException exception) {
            malformedLengthRejected =
                    true;
        }

        check(
                out,
                "wifi-w1120-malformed-option-length",
                malformedLengthRejected,
                "option length < 2 was accepted"
        );

        boolean nonZeroAfterEolRejected =
                false;

        try {
            RawIpv4Options.parse(
                    new byte[] {
                            0x00,
                            0x01,
                            0x00,
                            0x00
                    }
            );
        } catch (IllegalArgumentException exception) {
            nonZeroAfterEolRejected =
                    true;
        }

        check(
                out,
                "wifi-w1120-eol-padding",
                nonZeroAfterEolRejected,
                "non-zero option data after EOL was accepted"
        );

        boolean nonCopyOnNonInitialRejected =
                false;

        try {
            RawIpv4Encoder.encodeWithOptions(
                    "10.0.0.1",
                    "10.0.0.2",
                    0,
                    5,
                    false,
                    true,
                    1,
                    64,
                    17,
                    FULL_OPTIONS,
                    new byte[8]
            );
        } catch (IllegalArgumentException exception) {
            nonCopyOnNonInitialRejected =
                    true;
        }

        check(
                out,
                "wifi-w1120-noncopy-noninitial-rejected",
                nonCopyOnNonInitialRejected,
                "non-copied option was allowed on non-initial fragment"
        );

        List<byte[]> fragments =
                RawIpv4Fragmenter.fragment(
                        raw,
                        1500
                );

        check(
                out,
                "wifi-w1120-fragment-count",
                fragments.size() == 3,
                "4000-byte options datagram should produce three fragments"
        );

        RawIpv4Packet f0 =
                RawIpv4Decoder.decode(
                        fragments.get(0)
                );

        RawIpv4Packet f1 =
                RawIpv4Decoder.decode(
                        fragments.get(1)
                );

        RawIpv4Packet f2 =
                RawIpv4Decoder.decode(
                        fragments.get(2)
                );

        check(
                out,
                "wifi-w1120-first-fragment-options",
                f0.ihlWords() == 8
                        && Arrays.equals(
                        f0.options(),
                        FULL_OPTIONS
                ),
                "initial fragment lost full option set"
        );

        check(
                out,
                "wifi-w1120-noninitial-copy-options",
                f1.ihlWords() == 6
                        && f2.ihlWords() == 6
                        && Arrays.equals(
                        f1.options(),
                        ROUTER_ALERT
                )
                        && Arrays.equals(
                        f2.options(),
                        ROUTER_ALERT
                ),
                "non-initial fragments did not keep only copied options"
        );

        check(
                out,
                "wifi-w1120-variable-header-offsets",
                f0.fragmentOffset() == 0
                        && f1.fragmentOffset() == 183
                        && f2.fragmentOffset() == 367,
                "fragment offsets do not account for variable header sizes"
        );

        check(
                out,
                "wifi-w1120-variable-header-lengths",
                f0.totalLength() == 1496
                        && f1.totalLength() == 1496
                        && f2.totalLength() == 1088,
                "fragment lengths incorrect for variable IHL"
        );

        check(
                out,
                "wifi-w1120-fragment-checksums",
                f0.checksumValid()
                        && f1.checksumValid()
                        && f2.checksumValid(),
                "one or more option-bearing fragment checksums invalid"
        );

        byte[] forwarded =
                RawIpv4TransitForwarder.forward(
                        fragments.get(0),
                        63
                );

        RawIpv4Packet forwardedDecoded =
                RawIpv4Decoder.decode(
                        forwarded
                );

        check(
                out,
                "wifi-w1120-transit-options-preserved",
                forwardedDecoded.ttl() == 63
                        && Arrays.equals(
                        forwardedDecoded.options(),
                        FULL_OPTIONS
                )
                        && forwardedDecoded.checksumValid(),
                "router transit changed options or failed checksum update"
        );

        List<byte[]> children =
                RawIpv4Refragmenter.refragment(
                        fragments.get(0),
                        1000,
                        62
                );

        check(
                out,
                "wifi-w1120-refragment-count",
                children.size() == 2,
                "first parent should split into two children"
        );

        RawIpv4Packet c0 =
                RawIpv4Decoder.decode(
                        children.get(0)
                );

        RawIpv4Packet c1 =
                RawIpv4Decoder.decode(
                        children.get(1)
                );

        check(
                out,
                "wifi-w1120-refragment-option-copy",
                Arrays.equals(
                        c0.options(),
                        FULL_OPTIONS
                )
                        && Arrays.equals(
                        c1.options(),
                        ROUTER_ALERT
                ),
                "re-fragmentation copied options incorrectly"
        );

        check(
                out,
                "wifi-w1120-refragment-offsets",
                c0.fragmentOffset() == 0
                        && c1.fragmentOffset() == 121,
                "re-fragment child offsets incorrect"
        );

        check(
                out,
                "wifi-w1120-refragment-mtu",
                c0.totalLength() <= 1000
                        && c1.totalLength() <= 1000,
                "re-fragment child exceeds egress MTU"
        );

        RawIpv4ReassemblyTable table =
                new RawIpv4ReassemblyTable();

        var r2 =
                table.accept(
                        fragments.get(2),
                        0L
                );

        var r0 =
                table.accept(
                        fragments.get(0),
                        1L
                );

        var complete =
                table.accept(
                        fragments.get(1),
                        2L
                );

        check(
                out,
                "wifi-w1120-reassembly-out-of-order",
                r2.status()
                        == RawIpv4ReassemblyTable.Status.WAITING
                        && r0.status()
                        == RawIpv4ReassemblyTable.Status.WAITING
                        && complete.status()
                        == RawIpv4ReassemblyTable.Status.COMPLETE,
                "option-bearing fragments did not reassemble out of order"
        );

        RawIpv4Packet reassembled =
                RawIpv4Decoder.decode(
                        complete.rawPacket()
                );

        check(
                out,
                "wifi-w1120-reassembly-options-restored",
                reassembled.ihlWords() == 8
                        && Arrays.equals(
                        reassembled.options(),
                        FULL_OPTIONS
                ),
                "reassembled datagram did not restore initial-fragment options"
        );

        check(
                out,
                "wifi-w1120-reassembly-payload",
                Arrays.equals(
                        reassembled.payload(),
                        payload
                ),
                "reassembled option-bearing payload changed"
        );

        byte[] wrongCopied =
                new byte[] {
                        (byte) 0x94,
                        0x04,
                        0x00,
                        0x01
                };

        byte[] badNonInitial =
                RawIpv4Encoder.encodeWithOptions(
                        f1.sourceAddress(),
                        f1.destinationAddress(),
                        f1.dscpEcn(),
                        f1.identification(),
                        false,
                        true,
                        f1.fragmentOffset(),
                        f1.ttl(),
                        f1.protocol(),
                        wrongCopied,
                        f1.payload()
                );

        RawIpv4ReassemblyTable mismatchTable =
                new RawIpv4ReassemblyTable();

        mismatchTable.accept(
                fragments.get(0),
                0L
        );

        var mismatch =
                mismatchTable.accept(
                        badNonInitial,
                        1L
                );

        check(
                out,
                "wifi-w1120-copy-mismatch-rejected",
                mismatch.status()
                        == RawIpv4ReassemblyTable.Status.REJECTED
                        && "OPTION_COPY_MISMATCH".equals(
                        mismatch.reason()
                ),
                "inconsistent copied option was accepted"
        );

        byte[] noOptions =
                RawIpv4Encoder.encode(
                        "10.1.1.1",
                        "10.1.1.2",
                        0,
                        77,
                        false,
                        false,
                        0,
                        64,
                        17,
                        new byte[4000]
                );

        List<byte[]> noOptionFragments =
                RawIpv4Fragmenter.fragment(
                        noOptions,
                        1500
                );

        check(
                out,
                "wifi-w1120-ihl5-regression",
                noOptionFragments.size() == 3
                        && RawIpv4Decoder.decode(
                        noOptionFragments.get(0)
                ).ihlWords() == 5,
                "ordinary IHL=5 fragmentation regressed"
        );

        boolean maxHeaderAccepted =
                true;

        try {
            byte[] forty =
                    new byte[40];

            RawIpv4Encoder.encodeWithOptions(
                    "10.2.2.1",
                    "10.2.2.2",
                    0,
                    88,
                    false,
                    false,
                    0,
                    64,
                    17,
                    forty,
                    new byte[1]
            );
        } catch (Exception exception) {
            maxHeaderAccepted =
                    false;
        }

        check(
                out,
                "wifi-w1120-ihl15-max-header",
                maxHeaderAccepted,
                "legal 60-byte IPv4 header was rejected"
        );

        boolean overMaxRejected =
                false;

        try {
            RawIpv4Options.parse(
                    new byte[44]
            );
        } catch (IllegalArgumentException exception) {
            overMaxRejected =
                    true;
        }

        check(
                out,
                "wifi-w1120-options-over-40-rejected",
                overMaxRejected,
                "options beyond IHL=15 were accepted"
        );

        return List.copyOf(out);
    }

    private static void check(
            List<RawIpv4OptionsTestResult> out,
            String id,
            boolean passed,
            String detail
    ) {
        out.add(
                new RawIpv4OptionsTestResult(
                        id,
                        passed,
                        passed ? "PASS" : detail
                )
        );
    }
}

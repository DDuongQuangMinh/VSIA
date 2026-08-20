package com.k1ngtle.vsia.signality.engineering.wifi.ip.raw;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public final class RawIpv4RefragmentationTestSuite {
    private RawIpv4RefragmentationTestSuite() {
    }

    public static List<RawIpv4RefragmentationTestResult> runAll() {
        List<RawIpv4RefragmentationTestResult> out =
                new ArrayList<>();

        byte[] datagramPayload =
                new byte[4008];

        for (int i = 0;
             i < datagramPayload.length;
             i++) {
            datagramPayload[i] =
                    (byte) (
                            i * 37
                                    + 11
                    );
        }

        byte[] original =
                RawIpv4Encoder.encode(
                        "192.168.1.100",
                        "192.168.2.20",
                        0,
                        0x600D,
                        false,
                        false,
                        0,
                        64,
                        17,
                        datagramPayload
                );

        List<byte[]> sourceFragments =
                RawIpv4Fragmenter.fragment(
                        original,
                        1500
                );

        check(
                out,
                "wifi-w1113-source-three-fragments",
                sourceFragments.size() == 3,
                "source MTU 1500 should produce three fragments"
        );

        List<byte[]> firstChildren =
                RawIpv4Refragmenter.refragment(
                        sourceFragments.get(0),
                        1000,
                        62
                );

        check(
                out,
                "wifi-w1113-first-two-children",
                firstChildren.size() == 2,
                "first parent should split into two"
        );

        RawIpv4TransitForwarder.FragmentInfo firstA =
                RawIpv4TransitForwarder.inspect(
                        firstChildren.get(0)
                );

        RawIpv4TransitForwarder.FragmentInfo firstB =
                RawIpv4TransitForwarder.inspect(
                        firstChildren.get(1)
                );

        check(
                out,
                "wifi-w1113-first-lengths",
                firstA.totalLength() == 996
                        && firstB.totalLength() == 524,
                "first child lengths should be 996 and 524"
        );

        check(
                out,
                "wifi-w1113-first-offsets",
                firstA.fragmentOffset() == 0
                        && firstB.fragmentOffset() == 122,
                "first child offsets should be 0 and 122"
        );

        check(
                out,
                "wifi-w1113-parent-mf-propagates",
                firstA.moreFragments()
                        && firstB.moreFragments(),
                "children of an MF=1 parent must both keep MF=1"
        );

        List<byte[]> middleChildren =
                RawIpv4Refragmenter.refragment(
                        sourceFragments.get(1),
                        1000,
                        62
                );

        RawIpv4TransitForwarder.FragmentInfo middleA =
                RawIpv4TransitForwarder.inspect(
                        middleChildren.get(0)
                );

        RawIpv4TransitForwarder.FragmentInfo middleB =
                RawIpv4TransitForwarder.inspect(
                        middleChildren.get(1)
                );

        check(
                out,
                "wifi-w1113-middle-offsets",
                middleA.fragmentOffset() == 185
                        && middleB.fragmentOffset() == 307,
                "middle offsets must remain relative to the original datagram"
        );

        check(
                out,
                "wifi-w1113-middle-mf",
                middleA.moreFragments()
                        && middleB.moreFragments(),
                "middle parent MF must propagate"
        );

        List<byte[]> finalChildren =
                RawIpv4Refragmenter.refragment(
                        sourceFragments.get(2),
                        1000,
                        62
                );

        RawIpv4TransitForwarder.FragmentInfo finalA =
                RawIpv4TransitForwarder.inspect(
                        finalChildren.get(0)
                );

        RawIpv4TransitForwarder.FragmentInfo finalB =
                RawIpv4TransitForwarder.inspect(
                        finalChildren.get(1)
                );

        check(
                out,
                "wifi-w1113-final-offsets",
                finalA.fragmentOffset() == 370
                        && finalB.fragmentOffset() == 492,
                "final offsets must be 370 and 492"
        );

        check(
                out,
                "wifi-w1113-final-mf",
                finalA.moreFragments()
                        && !finalB.moreFragments(),
                "only the last child of the original final fragment may clear MF"
        );

        check(
                out,
                "wifi-w1113-final-lengths",
                finalA.totalLength() == 996
                        && finalB.totalLength() == 92,
                "final child lengths should be 996 and 92"
        );

        List<byte[]> allChildren =
                new ArrayList<>();

        allChildren.addAll(
                firstChildren
        );

        allChildren.addAll(
                middleChildren
        );

        allChildren.addAll(
                finalChildren
        );

        boolean idPreserved =
                allChildren.stream()
                        .map(
                                RawIpv4TransitForwarder::inspect
                        )
                        .allMatch(
                                fragment ->
                                        fragment.identification()
                                                == 0x600D
                        );

        check(
                out,
                "wifi-w1113-identification-preserved",
                idPreserved,
                "all children must preserve IPv4 Identification"
        );

        boolean ttlCorrect =
                allChildren.stream()
                        .map(
                                RawIpv4TransitForwarder::inspect
                        )
                        .allMatch(
                                fragment ->
                                        fragment.ttl()
                                                == 62
                        );

        check(
                out,
                "wifi-w1113-ttl-applied",
                ttlCorrect,
                "every child must use the router outgoing TTL"
        );

        boolean checksumValid =
                true;

        for (byte[] child
                : allChildren) {
            try {
                RawIpv4TransitForwarder.inspect(
                        child
                );
            } catch (Exception exception) {
                checksumValid =
                        false;
            }
        }

        check(
                out,
                "wifi-w1113-checksums-valid",
                checksumValid,
                "one or more child IPv4 checksums are invalid"
        );

        boolean fitsMtu =
                allChildren.stream()
                        .map(
                                RawIpv4TransitForwarder::inspect
                        )
                        .allMatch(
                                fragment ->
                                        fragment.totalLength()
                                                <= 1000
                        );

        check(
                out,
                "wifi-w1113-egress-mtu-obeyed",
                fitsMtu,
                "a child exceeds egress MTU"
        );

        boolean parentsReconstruct =
                reconstructs(
                        sourceFragments.get(0),
                        firstChildren
                )
                        && reconstructs(
                        sourceFragments.get(1),
                        middleChildren
                )
                        && reconstructs(
                        sourceFragments.get(2),
                        finalChildren
                );

        check(
                out,
                "wifi-w1113-parent-payloads-preserved",
                parentsReconstruct,
                "child payloads do not reconstruct their parent fragments"
        );

        List<RawIpv4TransitForwarder.FragmentInfo> ordered =
                allChildren.stream()
                        .map(
                                RawIpv4TransitForwarder::inspect
                        )
                        .sorted(
                                Comparator.comparingInt(
                                        RawIpv4TransitForwarder.FragmentInfo::fragmentOffsetBytes
                                )
                        )
                        .toList();

        byte[] reconstructed =
                new byte[datagramPayload.length];

        boolean contiguous =
                true;

        int cursor =
                0;

        for (RawIpv4TransitForwarder.FragmentInfo fragment
                : ordered) {
            int start =
                    fragment.fragmentOffsetBytes();

            if (start != cursor) {
                contiguous =
                        false;
                break;
            }

            byte[] part =
                    fragment.payload();

            if (start + part.length
                    > reconstructed.length) {
                contiguous =
                        false;
                break;
            }

            System.arraycopy(
                    part,
                    0,
                    reconstructed,
                    start,
                    part.length
            );

            cursor +=
                    part.length;
        }

        contiguous &=
                cursor
                        == datagramPayload.length;

        check(
                out,
                "wifi-w1113-full-datagram-coverage",
                contiguous
                        && Arrays.equals(
                        reconstructed,
                        datagramPayload
                ),
                "re-fragmented set has a gap, overlap or payload change"
        );

        byte[] dfPacket =
                RawIpv4Encoder.encode(
                        "192.168.1.100",
                        "192.168.2.20",
                        0,
                        0x700D,
                        true,
                        false,
                        0,
                        64,
                        17,
                        new byte[1400]
                );

        boolean dfRejected =
                false;

        try {
            RawIpv4Refragmenter.refragment(
                    dfPacket,
                    1000,
                    63
            );
        } catch (RawIpv4Fragmenter.FragmentationNeededException exception) {
            dfRejected =
                    exception.nextHopMtu()
                            == 1000;
        }

        check(
                out,
                "wifi-w1113-df-fragmentation-needed",
                dfRejected,
                "DF oversized packet must report Fragmentation Needed"
        );

        byte[] fits =
                RawIpv4Encoder.encode(
                        "192.168.1.100",
                        "192.168.2.20",
                        0,
                        0x1234,
                        false,
                        false,
                        0,
                        64,
                        17,
                        new byte[900]
                );

        List<byte[]> fitResult =
                RawIpv4Refragmenter.refragment(
                        fits,
                        1000,
                        63
                );

        RawIpv4TransitForwarder.FragmentInfo fitInfo =
                RawIpv4TransitForwarder.inspect(
                        fitResult.get(0)
                );

        check(
                out,
                "wifi-w1113-fit-forward-only",
                fitResult.size() == 1
                        && fitInfo.ttl() == 63
                        && fitInfo.identification() == 0x1234
                        && !fitInfo.moreFragments()
                        && fitInfo.fragmentOffset() == 0,
                "packet that fits MTU should only receive normal transit forwarding"
        );

        byte[] invalidMf =
                RawIpv4Encoder.encode(
                        "192.168.1.100",
                        "192.168.2.20",
                        0,
                        0x5555,
                        false,
                        true,
                        0,
                        64,
                        17,
                        new byte[981]
                );

        boolean badAlignmentRejected =
                false;

        try {
            RawIpv4Refragmenter.refragment(
                    invalidMf,
                    700,
                    63
            );
        } catch (IllegalArgumentException exception) {
            badAlignmentRejected =
                    exception.getMessage()
                            .contains(
                                    "8-byte aligned"
                            );
        }

        check(
                out,
                "wifi-w1113-invalid-parent-alignment",
                badAlignmentRejected,
                "MF=1 parent with non-aligned payload must be rejected"
        );

        return List.copyOf(
                out
        );
    }

    private static boolean reconstructs(
            byte[] parentRaw,
            List<byte[]> children
    ) {
        RawIpv4TransitForwarder.FragmentInfo parent =
                RawIpv4TransitForwarder.inspect(
                        parentRaw
                );

        List<RawIpv4TransitForwarder.FragmentInfo> ordered =
                children.stream()
                        .map(
                                RawIpv4TransitForwarder::inspect
                        )
                        .sorted(
                                Comparator.comparingInt(
                                        RawIpv4TransitForwarder.FragmentInfo::fragmentOffsetBytes
                                )
                        )
                        .toList();

        int base =
                parent.fragmentOffsetBytes();

        byte[] reconstructed =
                new byte[parent.payload().length];

        int cursor =
                0;

        for (RawIpv4TransitForwarder.FragmentInfo child
                : ordered) {
            int localStart =
                    child.fragmentOffsetBytes()
                            - base;

            if (localStart != cursor
                    || localStart < 0
                    || localStart
                    + child.payload().length
                    > reconstructed.length) {
                return false;
            }

            byte[] part =
                    child.payload();

            System.arraycopy(
                    part,
                    0,
                    reconstructed,
                    localStart,
                    part.length
            );

            cursor +=
                    part.length;
        }

        return cursor
                == reconstructed.length
                && Arrays.equals(
                reconstructed,
                parent.payload()
        );
    }

    private static void check(
            List<RawIpv4RefragmentationTestResult> out,
            String id,
            boolean passed,
            String detail
    ) {
        out.add(
                new RawIpv4RefragmentationTestResult(
                        id,
                        passed,
                        passed
                                ? "PASS"
                                : detail
                )
        );
    }
}

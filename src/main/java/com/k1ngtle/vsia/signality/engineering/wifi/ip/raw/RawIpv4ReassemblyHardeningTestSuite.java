package com.k1ngtle.vsia.signality.engineering.wifi.ip.raw;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class RawIpv4ReassemblyHardeningTestSuite {
    private RawIpv4ReassemblyHardeningTestSuite() {
    }

    public static List<RawIpv4ReassemblyHardeningTestResult> runAll() {
        List<RawIpv4ReassemblyHardeningTestResult> out =
                new ArrayList<>();

        byte[] payload =
                new byte[4008];

        for (int i = 0;
             i < payload.length;
             i++) {
            payload[i] =
                    (byte) (
                            i * 19 + 5
                    );
        }

        byte[] original =
                RawIpv4Encoder.encode(
                        "192.168.1.100",
                        "192.168.2.20",
                        0,
                        0x7114,
                        false,
                        false,
                        0,
                        64,
                        17,
                        payload
                );

        List<byte[]> fragments =
                RawIpv4Fragmenter.fragment(
                        original,
                        1500
                );

        check(
                out,
                "wifi-w1114-source-fragments",
                fragments.size() == 3,
                "expected three source fragments"
        );

        RawIpv4ReassemblyTable table =
                new RawIpv4ReassemblyTable(
                        1_000_000L
                );

        var r0 =
                table.accept(
                        fragments.get(0),
                        0L
                );

        check(
                out,
                "wifi-w1114-first-waiting",
                r0.status()
                        == RawIpv4ReassemblyTable.Status.WAITING
                        && r0.fragments() == 1,
                "first fragment should wait"
        );

        var duplicate =
                table.accept(
                        fragments.get(0),
                        10L
                );

        check(
                out,
                "wifi-w1114-exact-duplicate",
                duplicate.status()
                        == RawIpv4ReassemblyTable.Status.DUPLICATE
                        && duplicate.fragments() == 1
                        && table.pendingAssemblies() == 1,
                "exact duplicate should be ignored, not rejected"
        );

        var r2 =
                table.accept(
                        fragments.get(2),
                        20L
                );

        check(
                out,
                "wifi-w1114-out-of-order-final",
                r2.status()
                        == RawIpv4ReassemblyTable.Status.WAITING,
                "final fragment may arrive before middle fragment"
        );

        var complete =
                table.accept(
                        fragments.get(1),
                        30L
                );

        check(
                out,
                "wifi-w1114-complete-out-of-order",
                complete.status()
                        == RawIpv4ReassemblyTable.Status.COMPLETE,
                "out-of-order assembly did not complete"
        );

        RawIpv4TransitForwarder.FragmentInfo completedInfo =
                RawIpv4TransitForwarder.inspect(
                        complete.rawPacket()
                );

        check(
                out,
                "wifi-w1114-payload-preserved",
                Arrays.equals(
                        completedInfo.payload(),
                        payload
                ),
                "reassembled payload changed"
        );

        check(
                out,
                "wifi-w1114-table-cleared-on-complete",
                table.pendingAssemblies() == 0,
                "completed assembly remained pending"
        );

        RawIpv4ReassemblyTable timeoutTable =
                new RawIpv4ReassemblyTable(
                        100L
                );

        var waiting =
                timeoutTable.accept(
                        fragments.get(0),
                        0L
                );

        RawIpv4ReassemblyKey timeoutKey =
                waiting.key();

        var tooEarly =
                timeoutTable.expireKey(
                        timeoutKey,
                        99L
                );

        check(
                out,
                "wifi-w1114-timeout-not-early",
                tooEarly == null
                        && timeoutTable.pendingAssemblies() == 1,
                "assembly expired too early"
        );

        var expired =
                timeoutTable.expireKey(
                        timeoutKey,
                        100L
                );

        check(
                out,
                "wifi-w1114-timeout-boundary",
                expired != null
                        && timeoutTable.pendingAssemblies() == 0,
                "assembly did not expire at timeout boundary"
        );

        check(
                out,
                "wifi-w1114-timeout-zero-fragment",
                expired != null
                        && expired.zeroFragmentSeen()
                        && expired.zeroFragment().length > 20,
                "timeout did not retain fragment zero for ICMP quote"
        );

        RawIpv4ReassemblyTable noZeroTable =
                new RawIpv4ReassemblyTable(
                        100L
                );

        var nonZero =
                noZeroTable.accept(
                        fragments.get(1),
                        0L
                );

        var noZeroExpired =
                noZeroTable.expireKey(
                        nonZero.key(),
                        100L
                );

        check(
                out,
                "wifi-w1114-timeout-without-zero",
                noZeroExpired != null
                        && !noZeroExpired.zeroFragmentSeen(),
                "non-zero-only assembly incorrectly claims fragment zero"
        );

        byte[] overlapPayload =
                new byte[800];

        byte[] overlap =
                RawIpv4Encoder.encode(
                        "192.168.1.100",
                        "192.168.2.20",
                        0,
                        0x7114,
                        false,
                        true,
                        100,
                        64,
                        17,
                        overlapPayload
                );

        RawIpv4ReassemblyTable overlapTable =
                new RawIpv4ReassemblyTable();

        overlapTable.accept(
                fragments.get(0),
                0L
        );

        var overlapResult =
                overlapTable.accept(
                        overlap,
                        1L
                );

        check(
                out,
                "wifi-w1114-overlap-rejected",
                overlapResult.status()
                        == RawIpv4ReassemblyTable.Status.REJECTED
                        && "OVERLAP".equals(
                        overlapResult.reason()
                ),
                "partial overlap must reject the assembly"
        );

        check(
                out,
                "wifi-w1114-overlap-drops-assembly",
                overlapTable.pendingAssemblies() == 0,
                "overlap-rejected assembly remained pending"
        );

        byte[] misaligned =
                RawIpv4Encoder.encode(
                        "10.0.0.1",
                        "10.0.0.2",
                        0,
                        0x2222,
                        false,
                        true,
                        0,
                        64,
                        17,
                        new byte[999]
                );

        var alignment =
                new RawIpv4ReassemblyTable()
                        .accept(
                                misaligned,
                                0L
                        );

        check(
                out,
                "wifi-w1114-nonfinal-alignment",
                alignment.status()
                        == RawIpv4ReassemblyTable.Status.REJECTED
                        && "NONFINAL_ALIGNMENT".equals(
                        alignment.reason()
                ),
                "misaligned non-final fragment was accepted"
        );

        RawIpv4ReassemblyTable limited =
                new RawIpv4ReassemblyTable(
                        1_000_000L,
                        1,
                        64
                );

        byte[] firstA =
                RawIpv4Encoder.encode(
                        "10.0.0.1",
                        "10.0.0.2",
                        0,
                        1,
                        false,
                        true,
                        0,
                        64,
                        17,
                        new byte[8]
                );

        byte[] firstB =
                RawIpv4Encoder.encode(
                        "10.0.0.3",
                        "10.0.0.4",
                        0,
                        2,
                        false,
                        true,
                        0,
                        64,
                        17,
                        new byte[8]
                );

        limited.accept(
                firstA,
                0L
        );

        var assemblyLimit =
                limited.accept(
                        firstB,
                        1L
                );

        check(
                out,
                "wifi-w1114-assembly-limit",
                assemblyLimit.status()
                        == RawIpv4ReassemblyTable.Status.REJECTED
                        && "ASSEMBLY_LIMIT".equals(
                        assemblyLimit.reason()
                ),
                "assembly resource limit not enforced"
        );

        RawIpv4ReassemblyTable fragmentLimited =
                new RawIpv4ReassemblyTable(
                        1_000_000L,
                        64,
                        1
                );

        fragmentLimited.accept(
                fragments.get(0),
                0L
        );

        var fragmentLimit =
                fragmentLimited.accept(
                        fragments.get(1),
                        1L
                );

        check(
                out,
                "wifi-w1114-fragment-limit",
                fragmentLimit.status()
                        == RawIpv4ReassemblyTable.Status.REJECTED
                        && "FRAGMENT_LIMIT".equals(
                        fragmentLimit.reason()
                ),
                "per-assembly fragment limit not enforced"
        );

        RawIpv4ReassemblyTable refreshTable =
                new RawIpv4ReassemblyTable(
                        100L
                );

        var refreshWaiting =
                refreshTable.accept(
                        fragments.get(0),
                        0L
                );

        refreshTable.accept(
                fragments.get(0),
                80L
        );

        var refreshedTooEarly =
                refreshTable.expireKey(
                        refreshWaiting.key(),
                        179L
                );

        var refreshedAtBoundary =
                refreshTable.expireKey(
                        refreshWaiting.key(),
                        180L
                );

        check(
                out,
                "wifi-w1114-duplicate-refreshes-timeout",
                refreshedTooEarly == null
                        && refreshedAtBoundary != null,
                "exact duplicate did not refresh timeout activity"
        );

        RawIpv4ReassemblyTable detailTable =
                new RawIpv4ReassemblyTable(
                        100L
                );

        var detailWaiting =
                detailTable.accept(
                        fragments.get(0),
                        0L
                );

        detailTable.accept(
                fragments.get(1),
                1L
        );

        var detailExpired =
                detailTable.expireKey(
                        detailWaiting.key(),
                        101L
                );

        check(
                out,
                "wifi-w1114-timeout-accounting",
                detailExpired != null
                        && detailExpired.fragments() == 2
                        && detailExpired.receivedPayloadBytes() == 2960,
                "expired assembly accounting incorrect"
        );

        RawIpv4ReassemblyTable unfragmented =
                new RawIpv4ReassemblyTable();

        byte[] small =
                RawIpv4Encoder.encode(
                        "10.1.1.1",
                        "10.1.1.2",
                        0,
                        9,
                        false,
                        false,
                        0,
                        64,
                        17,
                        new byte[32]
                );

        var smallResult =
                unfragmented.accept(
                        small,
                        0L
                );

        check(
                out,
                "wifi-w1114-unfragmented-fast-path",
                smallResult.status()
                        == RawIpv4ReassemblyTable.Status.COMPLETE
                        && unfragmented.pendingAssemblies() == 0,
                "unfragmented fast path regressed"
        );

        check(
                out,
                "wifi-w1114-default-timeout-30s",
                new RawIpv4ReassemblyTable()
                        .timeoutMicros()
                        == 30_000_000L,
                "default timeout is not 30 seconds"
        );

        return List.copyOf(out);
    }

    private static void check(
            List<RawIpv4ReassemblyHardeningTestResult> out,
            String id,
            boolean passed,
            String detail
    ) {
        out.add(
                new RawIpv4ReassemblyHardeningTestResult(
                        id,
                        passed,
                        passed ? "PASS" : detail
                )
        );
    }
}

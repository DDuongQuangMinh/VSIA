package com.k1ngtle.vsia.signality.engineering.wifi.ip.mtu;

import com.k1ngtle.vsia.signality.engineering.wifi.ip.router.IcmpErrorModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class WifiPmtuTestSuite {
    private WifiPmtuTestSuite() {
    }

    public static List<WifiPmtuTestResult> runAll() {
        List<WifiPmtuTestResult> out =
                new ArrayList<>();

        byte[] payload =
                new byte[
                        4000
                ];

        for (int i = 0;
             i < payload.length;
             i++) {
            payload[i] =
                    (byte) (
                            i
                                    & 0xFF
                    );
        }

        List<Ipv4Fragment> fragments =
                Ipv4Fragmenter.fragment(
                        0x4242,
                        20,
                        1500,
                        payload
                );

        out.add(
                result(
                        "wifi-w1109-fragment-count",
                        fragments.size() == 3,
                        "4000-byte payload over 1500 MTU should make 3 fragments"
                )
        );

        out.add(
                result(
                        "wifi-w1109-fragment-offsets",
                        fragments.get(0).offsetBytes() == 0
                                && fragments.get(1).offsetBytes() == 1480
                                && fragments.get(2).offsetBytes() == 2960,
                        "IPv4 fragment offsets must use 1480-byte aligned chunks"
                )
        );

        out.add(
                result(
                        "wifi-w1109-mf-flags",
                        fragments.get(0).moreFragments()
                                && fragments.get(1).moreFragments()
                                && !fragments.get(2).moreFragments(),
                        "Only final fragment may clear MF"
                )
        );

        out.add(
                result(
                        "wifi-w1109-fragment-mtu",
                        fragments.stream()
                                .allMatch(
                                        fragment ->
                                                fragment.totalLength()
                                                        <= 1500
                                ),
                        "Every fragment must fit the MTU"
                )
        );

        byte[] reassembled =
                Ipv4Reassembler.reassemble(
                        List.of(
                                fragments.get(2),
                                fragments.get(0),
                                fragments.get(1)
                        )
                );

        out.add(
                result(
                        "wifi-w1109-reassembly",
                        Arrays.equals(
                                payload,
                                reassembled
                        ),
                        "Out-of-order fragments must reassemble to original payload"
                )
        );

        boolean overlapRejected =
                false;

        try {
            Ipv4Reassembler.reassemble(
                    List.of(
                            fragments.get(0),
                            new Ipv4Fragment(
                                    0x4242,
                                    1472,
                                    true,
                                    20,
                                    new byte[16]
                            ),
                            fragments.get(2)
                    )
            );
        } catch (IllegalArgumentException expected) {
            overlapRejected =
                    true;
        }

        out.add(
                result(
                        "wifi-w1109-overlap-reject",
                        overlapRejected,
                        "Overlapping fragments must be rejected"
                )
        );

        byte[] icmpFragNeeded =
                IcmpErrorModel.encodeFragmentationNeeded(
                        1200,
                        new byte[28]
                );

        out.add(
                result(
                        "wifi-w1109-icmp-code4-mtu",
                        (icmpFragNeeded[0] & 0xFF) == 3
                                && (icmpFragNeeded[1] & 0xFF) == 4
                                && (((icmpFragNeeded[6] & 0xFF) << 8)
                                | (icmpFragNeeded[7] & 0xFF)) == 1200
                                && IcmpErrorModel.internetChecksum(
                                icmpFragNeeded
                        ) == 0,
                        "ICMP Type 3 Code 4 must carry next-hop MTU and a valid checksum"
                )
        );

        PathMtuCache cache =
                new PathMtuCache();

        cache.learn(
                "192.168.2.20",
                1200,
                10L
        );

        cache.learn(
                "192.168.2.20",
                1400,
                20L
        );

        out.add(
                result(
                        "wifi-w1109-pmtu-cache-min",
                        cache.mtuFor(
                                "192.168.2.20"
                        ) == 1200,
                        "PMTU cache must not increase from a larger advertisement"
                )
        );

        PathMtuDiscoverySession discovery =
                new PathMtuDiscoverySession(
                        77L,
                        "192.168.2.20",
                        1500
                );

        out.add(
                result(
                        "wifi-w1109-frag-needed",
                        discovery.onFragmentationNeeded(
                                1200
                        )
                                && discovery.currentProbeBytes()
                                == 1200,
                        "ICMP code 4 must reduce the next DF probe size"
                )
        );

        out.add(
                result(
                        "wifi-w1109-late-echo-reject",
                        !discovery.onEchoReply(
                                77L,
                                1500,
                                "192.168.2.20"
                        ),
                        "Late reply for the old larger probe must be rejected"
                )
        );

        out.add(
                result(
                        "wifi-w1109-pmtu-confirm",
                        discovery.onEchoReply(
                                77L,
                                1200,
                                "192.168.2.20"
                        )
                                && discovery.complete()
                                && discovery.learnedMtu()
                                == 1200,
                        "Echo at advertised MTU must confirm PMTU"
                )
        );

        return List.copyOf(
                out
        );
    }

    private static WifiPmtuTestResult result(
            String name,
            boolean passed,
            String detail
    ) {
        return new WifiPmtuTestResult(
                name,
                passed,
                detail
        );
    }
}

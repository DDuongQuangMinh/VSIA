package com.k1ngtle.vsia.signality.engineering.wifi.ip.ipv6;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class Ipv6EngineeringTestSuite {
    private Ipv6EngineeringTestSuite() {
    }

    public static List<Ipv6EngineeringTestResult> runAll() {
        List<Ipv6EngineeringTestResult> out = new ArrayList<>();

        Ipv6Address a =
                Ipv6Address.parse("2001:db8:1::100");

        Ipv6Address b =
                Ipv6Address.parse("2001:db8:2::200");

        check(out, "wifi-w1150-address-128bit",
                a.bytes().length == 16,
                "IPv6 address is not 128-bit");

        check(out, "wifi-w1150-address-roundtrip",
                Ipv6Address.parse(a.toString()).equals(a),
                "IPv6 address text round-trip failed");

        Ipv6Prefix p64 =
                Ipv6Prefix.parse("2001:db8:1::/64");

        check(out, "wifi-w1150-prefix-contains",
                p64.contains(a)
                        && !p64.contains(b),
                "IPv6 prefix containment failed");

        Ipv6Address linkLocal =
                Ipv6Address.linkLocalFromMac(
                        "02:11:22:33:44:55"
                );

        check(out, "wifi-w1150-linklocal-eui64",
                linkLocal.isLinkLocal(),
                "MAC-derived link-local address invalid");

        Ipv6Address solicited =
                Ipv6Address.solicitedNodeMulticast(a);

        check(out, "wifi-w1150-solicited-node",
                solicited.isMulticast()
                        && solicited.toString().startsWith("ff02:"),
                "solicited-node multicast invalid");

        byte[] payload = payload(64);

        byte[] raw =
                RawIpv6Codec.encode(
                        a,
                        b,
                        0x2E,
                        0xABCDE,
                        17,
                        64,
                        payload
                );

        RawIpv6Packet decoded =
                RawIpv6Codec.decode(raw);

        check(out, "wifi-w1150-base-header-40",
                raw.length == 40 + payload.length
                        && decoded.totalLength() == raw.length,
                "IPv6 base header is not 40 bytes");

        check(out, "wifi-w1150-traffic-class",
                decoded.trafficClass() == 0x2E,
                "Traffic Class changed");

        check(out, "wifi-w1150-flow-label",
                decoded.flowLabel() == 0xABCDE,
                "Flow Label changed");

        check(out, "wifi-w1150-next-header",
                decoded.nextHeader() == 17,
                "Next Header changed");

        check(out, "wifi-w1150-hop-limit",
                decoded.hopLimit() == 64,
                "Hop Limit changed");

        byte[] forwarded =
                RawIpv6Codec.decrementHopLimit(raw);

        check(out, "wifi-w1150-hop-limit-forward",
                RawIpv6Codec.decode(forwarded).hopLimit() == 63,
                "IPv6 router did not decrement Hop Limit");

        boolean hopExpired = false;
        try {
            RawIpv6Codec.decrementHopLimit(
                    RawIpv6Codec.encode(
                            a, b, 0, 0, 17, 1, payload(8)
                    )
            );
        } catch (RawIpv6Codec.HopLimitExceededException expected) {
            hopExpired = true;
        }

        check(out, "wifi-w1150-hop-limit-expiry",
                hopExpired,
                "Hop Limit=1 was forwarded");

        byte[] udp =
                RawUdp6Codec.encode(
                        a,
                        b,
                        50000,
                        40001,
                        payload(128)
                );

        RawUdp6Codec.Decoded udpDecoded =
                RawUdp6Codec.decode(
                        a,
                        b,
                        udp
                );

        check(out, "wifi-w1150-udp6-checksum",
                udpDecoded.checksumValid(),
                "UDP/IPv6 pseudo-header checksum invalid");

        check(out, "wifi-w1150-udp6-payload",
                Arrays.equals(
                        udpDecoded.payload(),
                        payload(128)
                ),
                "UDP/IPv6 payload changed");

        byte[] zeroChecksumUdp = udp.clone();
        zeroChecksumUdp[6] = 0;
        zeroChecksumUdp[7] = 0;

        boolean zeroChecksumRejected = false;
        try {
            RawUdp6Codec.decode(
                    a,
                    b,
                    zeroChecksumUdp
            );
        } catch (IllegalArgumentException expected) {
            zeroChecksumRejected = true;
        }

        check(out, "wifi-w1150-udp6-zero-checksum-rejected",
                zeroChecksumRejected,
                "IPv6 UDP zero checksum was accepted");

        byte[] tcp = new byte[20];
        tcp[12] = 0x50;
        int tcpChecksum =
                Ipv6Checksum.transportChecksum(
                        a,
                        b,
                        6,
                        tcp
                );
        tcp[16] = (byte) (tcpChecksum >>> 8);
        tcp[17] = (byte) tcpChecksum;

        check(out, "wifi-w1150-tcp6-pseudoheader",
                Ipv6Checksum.transportChecksum(
                        a,
                        b,
                        6,
                        tcp
                ) == 0,
                "TCP/IPv6 pseudo-header checksum failed");

        byte[] echo =
                Icmpv6Codec.encode(
                        a,
                        b,
                        Icmpv6Codec.ECHO_REQUEST,
                        0,
                        0x12340007,
                        payload(32)
                );

        Icmpv6Codec.Decoded echoDecoded =
                Icmpv6Codec.decode(
                        a,
                        b,
                        echo
                );

        check(out, "wifi-w1150-icmpv6-checksum",
                echoDecoded.checksumValid(),
                "ICMPv6 checksum invalid");

        check(out, "wifi-w1150-icmpv6-echo",
                echoDecoded.type()
                        == Icmpv6Codec.ECHO_REQUEST,
                "ICMPv6 Echo type incorrect");

        byte[] ptb =
                Icmpv6Codec.encodePacketTooBig(
                        b,
                        a,
                        1400,
                        Arrays.copyOf(
                                raw,
                                Math.min(
                                        raw.length,
                                        64
                                )
                        )
                );

        Icmpv6Codec.Decoded ptbDecoded =
                Icmpv6Codec.decode(
                        b,
                        a,
                        ptb
                );

        check(out, "wifi-w1150-packet-too-big",
                ptbDecoded.type()
                        == Icmpv6Codec.PACKET_TOO_BIG
                        && ptbDecoded.unsignedRest() == 1400,
                "ICMPv6 Packet Too Big MTU field incorrect");

        byte[] ns =
                Ipv6NeighborDiscoveryCodec.encodeNeighborSolicitation(
                        linkLocal,
                        Ipv6Address.solicitedNodeMulticast(a),
                        a,
                        "02:11:22:33:44:55"
                );

        check(out, "wifi-w1150-nd-neighbor-solicitation",
                Icmpv6Codec.decode(
                        linkLocal,
                        Ipv6Address.solicitedNodeMulticast(a),
                        ns
                ).type()
                        == Icmpv6Codec.NEIGHBOR_SOLICITATION,
                "Neighbor Solicitation encoding failed");

        byte[] na =
                Ipv6NeighborDiscoveryCodec.encodeNeighborAdvertisement(
                        a,
                        linkLocal,
                        a,
                        false,
                        true,
                        true,
                        "02:aa:bb:cc:dd:ee"
                );

        check(out, "wifi-w1150-nd-neighbor-advertisement",
                Icmpv6Codec.decode(
                        a,
                        linkLocal,
                        na
                ).type()
                        == Icmpv6Codec.NEIGHBOR_ADVERTISEMENT,
                "Neighbor Advertisement encoding failed");

        byte[] rs =
                Ipv6NeighborDiscoveryCodec.encodeRouterSolicitation(
                        linkLocal,
                        Ipv6Address.allRoutersLinkLocal(),
                        "02:11:22:33:44:55"
                );

        check(out, "wifi-w1150-router-solicitation",
                Icmpv6Codec.decode(
                        linkLocal,
                        Ipv6Address.allRoutersLinkLocal(),
                        rs
                ).type()
                        == Icmpv6Codec.ROUTER_SOLICITATION,
                "Router Solicitation encoding failed");

        Ipv6Prefix slaacPrefix =
                Ipv6Prefix.parse(
                        "2001:db8:100::/64"
                );

        byte[] ra =
                Ipv6NeighborDiscoveryCodec.encodeRouterAdvertisement(
                        Ipv6Address.parse("fe80::1"),
                        Ipv6Address.allNodesLinkLocal(),
                        64,
                        1800,
                        slaacPrefix,
                        3600,
                        1800,
                        1500,
                        "02:00:00:00:00:01"
                );

        check(out, "wifi-w1150-router-advertisement",
                Icmpv6Codec.decode(
                        Ipv6Address.parse("fe80::1"),
                        Ipv6Address.allNodesLinkLocal(),
                        ra
                ).type()
                        == Icmpv6Codec.ROUTER_ADVERTISEMENT,
                "Router Advertisement encoding failed");

        Ipv6Address slaac =
                SlaacEngine.formAddress(
                        slaacPrefix,
                        "02:11:22:33:44:55"
                );

        check(out, "wifi-w1150-slaac",
                slaacPrefix.contains(slaac),
                "SLAAC address is outside advertised prefix");

        SlaacEngine.TentativeAddress tentative =
                SlaacEngine.beginDad(
                        slaac,
                        0L,
                        1_000_000L
                );

        check(out, "wifi-w1150-dad-tentative",
                tentative.state()
                        == SlaacEngine.DadState.TENTATIVE
                        && tentative.solicitedNode().isMulticast(),
                "DAD did not enter tentative state");

        check(out, "wifi-w1150-dad-success",
                SlaacEngine.dadSuccess(
                        tentative
                ).state()
                        == SlaacEngine.DadState.PREFERRED,
                "DAD success state incorrect");

        check(out, "wifi-w1150-dad-duplicate",
                SlaacEngine.dadDuplicate(
                        tentative
                ).state()
                        == SlaacEngine.DadState.DUPLICATE,
                "DAD duplicate state incorrect");

        NeighborDiscoveryTable nd =
                new NeighborDiscoveryTable();

        nd.learn(
                a,
                "02:aa:bb:cc:dd:ee",
                0L,
                1_000L
        );

        check(out, "wifi-w1150-nd-reachable",
                nd.lookup(
                        a,
                        500L
                ).state()
                        == NeighborDiscoveryTable.State.REACHABLE,
                "ND reachable state incorrect");

        check(out, "wifi-w1150-nd-stale",
                nd.lookup(
                        a,
                        1_000L
                ).state()
                        == NeighborDiscoveryTable.State.STALE,
                "ND stale transition incorrect");

        Ipv6RoutingTable routing =
                new Ipv6RoutingTable();

        routing.add(
                new Ipv6Route(
                        Ipv6Prefix.parse("::/0"),
                        Ipv6Address.parse("fe80::1"),
                        "lan0",
                        100
                )
        );

        routing.add(
                new Ipv6Route(
                        Ipv6Prefix.parse("2001:db8:2::/64"),
                        Ipv6Address.parse("fe80::2"),
                        "transit0",
                        10
                )
        );

        check(out, "wifi-w1150-route-lpm",
                "transit0".equals(
                        routing.lookup(b)
                                .egressInterface()
                ),
                "IPv6 longest-prefix route lookup failed");

        routing.add(
                new Ipv6Route(
                        Ipv6Prefix.parse("2001:db8:2::/64"),
                        Ipv6Address.parse("fe80::3"),
                        "transit1",
                        20
                )
        );

        check(out, "wifi-w1150-route-metric",
                routing.lookup(b)
                        .nextHop()
                        .equals(
                                Ipv6Address.parse("fe80::2")
                        ),
                "IPv6 route metric tie-break failed");

        Ipv6PmtuCache pmtu =
                new Ipv6PmtuCache();

        check(out, "wifi-w1150-pmtu-default",
                pmtu.pmtuFor(
                        b,
                        1500,
                        0L
                ) == 1500,
                "IPv6 PMTU did not start at interface MTU");

        pmtu.learnPacketTooBig(
                b,
                1400,
                0L,
                1_000_000L
        );

        check(out, "wifi-w1150-pmtu-learn",
                pmtu.pmtuFor(
                        b,
                        1500,
                        100L
                ) == 1400,
                "IPv6 Packet Too Big did not reduce PMTU");

        pmtu.learnPacketTooBig(
                b,
                900,
                100L,
                1_000_000L
        );

        check(out, "wifi-w1150-pmtu-min1280",
                pmtu.pmtuFor(
                        b,
                        1500,
                        200L
                ) == 1280,
                "IPv6 PMTU fell below 1280");

        byte[] large =
                RawIpv6Codec.encode(
                        a,
                        b,
                        0,
                        0,
                        17,
                        64,
                        payload(4000)
                );

        List<byte[]> fragments =
                Ipv6SourceFragmenter.fragment(
                        large,
                        1500,
                        0x12345678L
                );

        check(out, "wifi-w1150-source-fragment-count",
                fragments.size() == 3,
                "IPv6 source fragmentation count incorrect");

        RawIpv6Packet frag0 =
                RawIpv6Codec.decode(
                        fragments.get(0)
                );

        Ipv6FragmentHeader fh0 =
                Ipv6FragmentHeader.decode(
                        Arrays.copyOfRange(
                                frag0.payload(),
                                0,
                                8
                        )
                );

        check(out, "wifi-w1150-fragment-header",
                frag0.nextHeader()
                        == Ipv6FragmentHeader.NEXT_HEADER_FRAGMENT
                        && fh0.identification()
                        == 0x12345678L
                        && fh0.fragmentOffsetUnits() == 0,
                "IPv6 Fragment extension header incorrect");

        check(out, "wifi-w1150-fragment-mtu",
                fragments.stream().allMatch(
                        fragment ->
                                fragment.length <= 1500
                ),
                "IPv6 source fragment exceeds MTU");

        Ipv6FragmentReassemblyTable reassembly =
                new Ipv6FragmentReassemblyTable();

        var rr2 =
                reassembly.accept(
                        fragments.get(2),
                        0L
                );

        var rr0 =
                reassembly.accept(
                        fragments.get(0),
                        1L
                );

        var rr1 =
                reassembly.accept(
                        fragments.get(1),
                        2L
                );

        check(out, "wifi-w1150-reassembly-out-of-order",
                rr2.status()
                        == Ipv6FragmentReassemblyTable.Status.WAITING
                        && rr0.status()
                        == Ipv6FragmentReassemblyTable.Status.WAITING
                        && rr1.status()
                        == Ipv6FragmentReassemblyTable.Status.COMPLETE,
                "IPv6 fragment reassembly failed out of order");

        check(out, "wifi-w1150-reassembly-payload",
                Arrays.equals(
                        RawIpv6Codec.decode(
                                rr1.rawPacket()
                        ).payload(),
                        payload(4000)
                ),
                "IPv6 reassembly changed payload");

        check(out, "wifi-w1150-no-router-fragmentation",
                routerMtuDecision(
                        large.length,
                        1500
                ) == RouterMtuDecision.PACKET_TOO_BIG,
                "IPv6 router incorrectly selected router-side fragmentation");

        check(out, "wifi-w1150-dualstack-prefers-aaaa",
                DualStackRouteSelector.select(
                        true,
                        true,
                        true,
                        true
                )
                        == DualStackRouteSelector.Family.IPV6,
                "dual stack did not prefer IPv6 when AAAA is available");

        check(out, "wifi-w1150-dualstack-a-fallback",
                DualStackRouteSelector.select(
                        true,
                        true,
                        true,
                        false
                )
                        == DualStackRouteSelector.Family.IPV4,
                "dual stack did not fall back to IPv4 A record");

        Ipv6DnsRecord aaaa =
                new Ipv6DnsRecord(
                        "server.vsia.test",
                        b,
                        300
                );

        check(out, "wifi-w1150-dns-aaaa",
                "AAAA".equals(
                        aaaa.type()
                )
                        && aaaa.address().equals(b),
                "IPv6 AAAA record model failed");

        byte[] hop1 =
                RawIpv6Codec.decrementHopLimit(
                        RawIpv6Codec.encode(
                                a,
                                b,
                                0,
                                0,
                                17,
                                64,
                                payload(32)
                        )
                );

        byte[] hop2 =
                RawIpv6Codec.decrementHopLimit(
                        hop1
                );

        check(out, "wifi-w1150-multirouter-hoplimit",
                RawIpv6Codec.decode(
                        hop2
                ).hopLimit() == 62,
                "two-router IPv6 Hop Limit forwarding incorrect");

        VsShipIpv6Validator.Validation ship =
                VsShipIpv6Validator.validate(
                        "02:11:22:33:44:55",
                        slaacPrefix,
                        100.0,
                        70.0,
                        -20.0,
                        5.0,
                        2.0,
                        8.0
                );

        check(out, "wifi-w1150-vs-ship-address-stability",
                ship.valid()
                        && slaacPrefix.contains(
                        ship.global()
                ),
                "VS ship coordinate validation changed IPv6 identity");

        Ipv6AnalyzerSnapshot snapshot =
                new Ipv6AnalyzerSnapshot(
                        linkLocal,
                        slaac,
                        slaacPrefix,
                        Ipv6Address.parse("fe80::1"),
                        64,
                        1500,
                        1,
                        2,
                        "REACHABLE",
                        "PREFERRED",
                        "ICMPv6/UDP6 READY"
                );

        check(out, "wifi-w1150-analyzer-ipv6-view",
                snapshot.render().contains("IPv6")
                        && snapshot.render().contains("ICMPv6/UDP6 READY"),
                "IPv6 analyzer snapshot missing required data");

        DualStackDnsTable dns =
                new DualStackDnsTable();

        dns.putA(
                "server.vsia.test",
                "192.0.2.20"
        );

        dns.putAaaa(
                new Ipv6DnsRecord(
                        "server.vsia.test",
                        b,
                        300
                )
        );

        check(out, "wifi-w1150-dns-dualstack-aaaa",
                dns.resolve(
                        "server.vsia.test",
                        true,
                        true
                ).family()
                        == DualStackRouteSelector.Family.IPV6,
                "dual-stack DNS did not select AAAA/IPv6");

        check(out, "wifi-w1150-dns-dualstack-a-fallback",
                dns.resolve(
                        "server.vsia.test",
                        true,
                        false
                ).family()
                        == DualStackRouteSelector.Family.IPV4,
                "dual-stack DNS did not fall back to A/IPv4");

        return List.copyOf(out);
    }

    public static RouterMtuDecision routerMtuDecision(
            int packetBytes,
            int egressMtu
    ) {
        return packetBytes <= egressMtu
                ? RouterMtuDecision.FORWARD
                : RouterMtuDecision.PACKET_TOO_BIG;
    }

    public enum RouterMtuDecision {
        FORWARD,
        PACKET_TOO_BIG
    }

    private static byte[] payload(int length) {
        byte[] out = new byte[length];
        for (int i = 0; i < length; i++) {
            out[i] = (byte) (i * 29 + 7);
        }
        return out;
    }

    private static void check(
            List<Ipv6EngineeringTestResult> out,
            String id,
            boolean passed,
            String detail
    ) {
        out.add(
                new Ipv6EngineeringTestResult(
                        id,
                        passed,
                        passed ? "PASS" : detail
                )
        );
    }
}

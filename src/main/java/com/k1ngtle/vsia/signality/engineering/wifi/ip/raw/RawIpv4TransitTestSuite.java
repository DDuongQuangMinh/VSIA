package com.k1ngtle.vsia.signality.engineering.wifi.ip.raw;

import com.k1ngtle.vsia.signality.engineering.wifi.ip.router.RouterEngine;
import com.k1ngtle.vsia.signality.engineering.wifi.ip.router.RouterForwardAction;
import com.k1ngtle.vsia.signality.engineering.wifi.ip.router.RouterInterface;
import com.k1ngtle.vsia.signality.engineering.wifi.ip.router.RouterPacket;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class RawIpv4TransitTestSuite {
    private RawIpv4TransitTestSuite() {
    }

    public static List<RawIpv4TransitTestResult> runAll() {
        List<RawIpv4TransitTestResult> out = new ArrayList<>();

        byte[] payload = new byte[4008];

        for (int i = 0; i < payload.length; i++) {
            payload[i] = (byte) (i * 31 + 7);
        }

        byte[] raw = RawIpv4Encoder.encode(
                "192.168.10.10",
                "192.168.20.20",
                0,
                0x4242,
                false,
                false,
                0,
                64,
                17,
                payload
        );

        List<byte[]> fragments = RawIpv4Fragmenter.fragment(
                raw,
                1500
        );

        check(out, "wifi-w1112-fragment-count",
                fragments.size() == 3,
                "expected three fragments");

        RawIpv4TransitForwarder.FragmentInfo f0 =
                RawIpv4TransitForwarder.inspect(fragments.get(0));
        RawIpv4TransitForwarder.FragmentInfo f1 =
                RawIpv4TransitForwarder.inspect(fragments.get(1));
        RawIpv4TransitForwarder.FragmentInfo f2 =
                RawIpv4TransitForwarder.inspect(fragments.get(2));

        check(out, "wifi-w1112-identification",
                f0.identification() == 0x4242
                        && f1.identification() == 0x4242
                        && f2.identification() == 0x4242,
                "identification changed");

        check(out, "wifi-w1112-offsets",
                f0.fragmentOffset() == 0
                        && f1.fragmentOffset() == 185
                        && f2.fragmentOffset() == 370,
                "fragment offsets incorrect");

        check(out, "wifi-w1112-mf-flags",
                f0.moreFragments()
                        && f1.moreFragments()
                        && !f2.moreFragments(),
                "MF flags incorrect");

        byte[] hop1 = RawIpv4TransitForwarder.forward(
                fragments.get(1),
                63
        );

        RawIpv4TransitForwarder.FragmentInfo hop1Info =
                RawIpv4TransitForwarder.inspect(hop1);

        check(out, "wifi-w1112-ttl-one-hop",
                hop1Info.ttl() == 63,
                "TTL was not decremented");

        check(out, "wifi-w1112-offset-preserved",
                hop1Info.fragmentOffset() == f1.fragmentOffset(),
                "offset changed");

        check(out, "wifi-w1112-mf-preserved",
                hop1Info.moreFragments() == f1.moreFragments(),
                "MF changed");

        check(out, "wifi-w1112-payload-preserved",
                Arrays.equals(hop1Info.payload(), f1.payload()),
                "payload changed");

        byte[] hop2 = RawIpv4TransitForwarder.forward(hop1, 62);
        byte[] hop3 = RawIpv4TransitForwarder.forward(hop2, 61);

        RawIpv4TransitForwarder.FragmentInfo hop3Info =
                RawIpv4TransitForwarder.inspect(hop3);

        check(out, "wifi-w1112-three-hop-ttl",
                hop3Info.ttl() == 61,
                "three-hop TTL incorrect");

        check(out, "wifi-w1112-three-hop-identity",
                hop3Info.identification() == 0x4242
                        && hop3Info.fragmentOffset() == 185
                        && hop3Info.moreFragments(),
                "identity changed after three hops");

        check(out, "wifi-w1112-three-hop-payload",
                Arrays.equals(hop3Info.payload(), f1.payload()),
                "payload changed after three hops");

        boolean reorderedOkay = true;

        for (byte[] fragment : List.of(
                fragments.get(2),
                fragments.get(0),
                fragments.get(1)
        )) {
            RawIpv4TransitForwarder.FragmentInfo info =
                    RawIpv4TransitForwarder.inspect(
                            RawIpv4TransitForwarder.forward(fragment, 63)
                    );

            reorderedOkay &=
                    info.ttl() == 63
                            && info.identification() == 0x4242;
        }

        check(out, "wifi-w1112-out-of-order-independent",
                reorderedOkay,
                "forwarding depended on fragment order");

        RouterEngine router = new RouterEngine();

        router.putInterface(
                new RouterInterface(
                        "lan0",
                        "192.168.10.1",
                        24,
                        "02:00:00:00:10:01",
                        true
                )
        );

        router.putInterface(
                new RouterInterface(
                        "lan1",
                        "192.168.20.1",
                        24,
                        "02:00:00:00:20:01",
                        true
                )
        );

        router.neighbors().learn(
                "lan1",
                "192.168.20.20",
                "AA:BB:CC:DD:EE:FF"
        );

        RouterPacket fragmentPacket = new RouterPacket(
                f1.sourceIp(),
                f1.destinationIp(),
                f1.ttl(),
                f1.protocol(),
                f1.identification(),
                f1.dontFragment(),
                f1.moreFragments(),
                f1.fragmentOffset(),
                f1.totalLength(),
                f1.payload()
        );

        var decision = router.evaluate(
                "lan0",
                fragmentPacket
        );

        check(out, "wifi-w1112-router-forward",
                decision.action() == RouterForwardAction.FORWARD,
                "router did not forward fragment");

        check(out, "wifi-w1112-router-ttl",
                decision.outgoingTtl() == 63,
                "router outgoing TTL incorrect");

        check(out, "wifi-w1112-noninitial-opaque",
                fragmentPacket.nonInitialFragment()
                        && fragmentPacket.fragmentOffsetBytes() == 1480,
                "non-initial fragment metadata incorrect");

        RouterPacket ttlOne = new RouterPacket(
                f1.sourceIp(),
                f1.destinationIp(),
                1,
                f1.protocol(),
                f1.identification(),
                f1.dontFragment(),
                f1.moreFragments(),
                f1.fragmentOffset(),
                f1.totalLength(),
                f1.payload()
        );

        check(out, "wifi-w1112-ttl-expiry",
                router.evaluate(
                        "lan0",
                        ttlOne
                ).action() == RouterForwardAction.ICMP_TIME_EXCEEDED,
                "TTL=1 fragment did not expire");

        return List.copyOf(out);
    }

    private static void check(
            List<RawIpv4TransitTestResult> out,
            String id,
            boolean passed,
            String detail
    ) {
        out.add(
                new RawIpv4TransitTestResult(
                        id,
                        passed,
                        passed ? "PASS" : detail
                )
        );
    }
}

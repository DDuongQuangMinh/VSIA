package com.k1ngtle.vsia.signality.engineering.firewall.transit;

import java.util.ArrayList;
import java.util.List;

public final class FirewallTransitTestSuite {
    private FirewallTransitTestSuite() {
    }

    public static List<FirewallTransitTestResult> runAll() {
        List<FirewallTransitTestResult> out = new ArrayList<>();

        check(out,"wifi-w1161-lan-ingress",
                FirewallTransitModel.ingressFor("A","A","B")
                        == FirewallTransitModel.Port.LAN,
                "LAN ingress mapping failed");

        check(out,"wifi-w1161-wan-ingress",
                FirewallTransitModel.ingressFor("B","A","B")
                        == FirewallTransitModel.Port.WAN,
                "WAN ingress mapping failed");

        check(out,"wifi-w1161-unknown-ingress",
                FirewallTransitModel.ingressFor("C","A","B")
                        == FirewallTransitModel.Port.NONE,
                "unknown ingress accepted");

        check(out,"wifi-w1161-gi11-egress",
                FirewallTransitModel.egressForInterface("GigabitEthernet1/1")
                        == FirewallTransitModel.Port.LAN,
                "Gi1/1 egress mapping failed");

        check(out,"wifi-w1161-gi12-egress",
                FirewallTransitModel.egressForInterface("GigabitEthernet1/2")
                        == FirewallTransitModel.Port.WAN,
                "Gi1/2 egress mapping failed");

        check(out,"wifi-w1161-nameif-inside",
                FirewallTransitModel.egressForInterface("inside")
                        == FirewallTransitModel.Port.LAN,
                "inside nameif mapping failed");

        check(out,"wifi-w1161-nameif-outside",
                FirewallTransitModel.egressForInterface("outside")
                        == FirewallTransitModel.Port.WAN,
                "outside nameif mapping failed");

        check(out,"wifi-w1161-inside-outside-forward",
                FirewallTransitModel.shouldForward(
                        FirewallTransitModel.Port.LAN,
                        FirewallTransitModel.Port.WAN,
                        true,true,true),
                "LAN to WAN not forwarded");

        check(out,"wifi-w1161-outside-inside-forward",
                FirewallTransitModel.shouldForward(
                        FirewallTransitModel.Port.WAN,
                        FirewallTransitModel.Port.LAN,
                        true,true,true),
                "WAN to LAN return not forwarded");

        check(out,"wifi-w1161-same-interface-reject",
                !FirewallTransitModel.shouldForward(
                        FirewallTransitModel.Port.LAN,
                        FirewallTransitModel.Port.LAN,
                        true,true,true),
                "same-interface packet forwarded");

        check(out,"wifi-w1161-ingress-down",
                !FirewallTransitModel.shouldForward(
                        FirewallTransitModel.Port.LAN,
                        FirewallTransitModel.Port.WAN,
                        false,true,true),
                "down ingress forwarded");

        check(out,"wifi-w1161-egress-down",
                !FirewallTransitModel.shouldForward(
                        FirewallTransitModel.Port.LAN,
                        FirewallTransitModel.Port.WAN,
                        true,false,true),
                "down egress forwarded");

        check(out,"wifi-w1161-not-booted",
                !FirewallTransitModel.shouldForward(
                        FirewallTransitModel.Port.LAN,
                        FirewallTransitModel.Port.WAN,
                        true,true,false),
                "unbooted firewall forwarded");

        check(out,"wifi-w1161-ttl64",
                FirewallTransitModel.forwardedTtl(64)==63,
                "TTL 64 did not become 63");

        check(out,"wifi-w1161-ttl2",
                FirewallTransitModel.forwardedTtl(2)==1,
                "TTL 2 did not become 1");

        check(out,"wifi-w1161-ttl1-expired",
                FirewallTransitModel.forwardedTtl(1)==-1,
                "TTL 1 did not expire");

        check(out,"wifi-w1161-topology-complete",
                FirewallTransitModel.validTopology("A","B"),
                "complete topology rejected");

        check(out,"wifi-w1161-topology-missing-lan",
                !FirewallTransitModel.validTopology(null,"B"),
                "missing LAN accepted");

        check(out,"wifi-w1161-topology-missing-wan",
                !FirewallTransitModel.validTopology("A",null),
                "missing WAN accepted");

        check(out,"wifi-w1161-topology-same-peer",
                !FirewallTransitModel.validTopology("A","A"),
                "same peer accepted for both sides");

        return List.copyOf(out);
    }

    private static void check(
            List<FirewallTransitTestResult> out,
            String id,
            boolean passed,
            String detail
    ) {
        out.add(new FirewallTransitTestResult(
                id,
                passed,
                passed ? "PASS" : detail
        ));
    }
}

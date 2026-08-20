package com.k1ngtle.vsia.signality.engineering.wifi.ip.router;

import java.util.ArrayList;
import java.util.List;

public final class RouterMultiHopTestSuite {
    private static final int ICMP_PROTOCOL = 1;

    private RouterMultiHopTestSuite() {
    }

    public static List<RouterMultiHopTestResult> runAll() {
        List<RouterMultiHopTestResult> out =
                new ArrayList<>();

        RouterEngine a =
                routerA();

        RouterEngine b =
                routerB();

        RouterPacket ttl3 =
                packet(
                        "192.168.1.100",
                        "192.168.2.20",
                        3
                );

        RouterForwardDecision aForward =
                a.evaluate(
                        "lan0",
                        ttl3
                );

        out.add(
                result(
                        "wifi-w1107-a-forward",
                        aForward.action()
                                == RouterForwardAction.FORWARD
                                && "transit0".equals(
                                aForward.egressInterface()
                        )
                                && "10.0.0.2".equals(
                                aForward.nextHopIp()
                        )
                                && aForward.outgoingTtl()
                                == 2,
                        aForward.detail()
                )
        );

        RouterPacket atB =
                packet(
                        ttl3.sourceIp(),
                        ttl3.destinationIp(),
                        aForward.outgoingTtl()
                );

        RouterForwardDecision bForward =
                b.evaluate(
                        "transit0",
                        atB
                );

        out.add(
                result(
                        "wifi-w1107-b-forward",
                        bForward.action()
                                == RouterForwardAction.FORWARD
                                && "lan1".equals(
                                bForward.egressInterface()
                        )
                                && "192.168.2.20".equals(
                                bForward.nextHopIp()
                        )
                                && bForward.outgoingTtl()
                                == 1,
                        bForward.detail()
                )
        );

        RouterForwardDecision ttl1 =
                a.evaluate(
                        "lan0",
                        packet(
                                "192.168.1.100",
                                "192.168.2.20",
                                1
                        )
                );

        out.add(
                result(
                        "wifi-w1107-ttl1-hop1-expired",
                        ttl1.action()
                                == RouterForwardAction.ICMP_TIME_EXCEEDED,
                        ttl1.detail()
                )
        );

        RouterForwardDecision firstForTtl2 =
                a.evaluate(
                        "lan0",
                        packet(
                                "192.168.1.100",
                                "192.168.2.20",
                                2
                        )
                );

        RouterForwardDecision secondForTtl2 =
                b.evaluate(
                        "transit0",
                        packet(
                                "192.168.1.100",
                                "192.168.2.20",
                                firstForTtl2.outgoingTtl()
                        )
                );

        out.add(
                result(
                        "wifi-w1107-ttl2-hop2-expired",
                        firstForTtl2.action()
                                == RouterForwardAction.FORWARD
                                && secondForTtl2.action()
                                == RouterForwardAction.ICMP_TIME_EXCEEDED,
                        secondForTtl2.detail()
                )
        );

        RouterForwardDecision reverseB =
                b.evaluate(
                        "lan1",
                        packet(
                                "192.168.2.20",
                                "192.168.1.100",
                                64
                        )
                );

        RouterForwardDecision reverseA =
                a.evaluate(
                        "transit0",
                        packet(
                                "192.168.2.20",
                                "192.168.1.100",
                                reverseB.outgoingTtl()
                        )
                );

        out.add(
                result(
                        "wifi-w1107-reverse-path",
                        reverseB.action()
                                == RouterForwardAction.FORWARD
                                && reverseA.action()
                                == RouterForwardAction.FORWARD
                                && reverseB.outgoingTtl()
                                == 63
                                && reverseA.outgoingTtl()
                                == 62,
                        reverseB.detail()
                                + " | "
                                + reverseA.detail()
                )
        );

        RouterEngine arpA =
                routerAWithoutTransitNeighbor();

        RouterForwardDecision arpRequired =
                arpA.evaluate(
                        "lan0",
                        ttl3
                );

        out.add(
                result(
                        "wifi-w1107-hop1-arp-required",
                        arpRequired.action()
                                == RouterForwardAction.ARP_REQUIRED
                                && "transit0".equals(
                                arpRequired.egressInterface()
                        )
                                && "10.0.0.2".equals(
                                arpRequired.nextHopIp()
                        ),
                        arpRequired.detail()
                )
        );

        RouterEngine arpB =
                routerBWithoutServerNeighbor();

        RouterForwardDecision arpBRequired =
                arpB.evaluate(
                        "transit0",
                        packet(
                                "192.168.1.100",
                                "192.168.2.20",
                                2
                        )
                );

        out.add(
                result(
                        "wifi-w1107-hop2-arp-required",
                        arpBRequired.action()
                                == RouterForwardAction.ARP_REQUIRED
                                && "lan1".equals(
                                arpBRequired.egressInterface()
                        )
                                && "192.168.2.20".equals(
                                arpBRequired.nextHopIp()
                        ),
                        arpBRequired.detail()
                )
        );

        RouterEngine lpm =
                routerA();

        lpm.addRoute(
                new RouterRoute(
                        "192.168.2.20",
                        32,
                        "10.0.0.2",
                        "transit0",
                        200,
                        "STATIC_HOST"
                )
        );

        RouterForwardDecision lpmDecision =
                lpm.evaluate(
                        "lan0",
                        ttl3
                );

        out.add(
                result(
                        "wifi-w1107-longest-prefix",
                        lpmDecision.prefixLength()
                                == 32
                                && lpmDecision.metric()
                                == 200
                                && "10.0.0.2".equals(
                                lpmDecision.nextHopIp()
                        ),
                        lpmDecision.detail()
                )
        );

        RouterEngine metric =
                routerA();

        metric.addRoute(
                new RouterRoute(
                        "192.168.2.0",
                        24,
                        "10.0.0.2",
                        "transit0",
                        50,
                        "STATIC_HIGH"
                )
        );

        RouterForwardDecision metricDecision =
                metric.evaluate(
                        "lan0",
                        ttl3
                );

        out.add(
                result(
                        "wifi-w1107-metric-tiebreak",
                        metricDecision.prefixLength()
                                == 24
                                && metricDecision.metric()
                                == 10,
                        metricDecision.detail()
                )
        );

        RouterEngine noRoute =
                new RouterEngine();

        noRoute.putInterface(
                iface(
                        "lan0",
                        "192.168.1.1",
                        24,
                        "A00000000001"
                )
        );

        RouterForwardDecision noRouteDecision =
                noRoute.evaluate(
                        "lan0",
                        ttl3
                );

        out.add(
                result(
                        "wifi-w1107-no-route",
                        noRouteDecision.action()
                                == RouterForwardAction.ICMP_DESTINATION_UNREACHABLE,
                        noRouteDecision.detail()
                )
        );

        return List.copyOf(
                out
        );
    }

    private static RouterEngine routerA() {
        RouterEngine router =
                routerAWithoutTransitNeighbor();

        router.neighbors()
                .learn(
                        "transit0",
                        "10.0.0.2",
                        "B00000000002"
                );

        router.neighbors()
                .learn(
                        "lan0",
                        "192.168.1.100",
                        "C00000000100"
                );

        return router;
    }

    private static RouterEngine routerAWithoutTransitNeighbor() {
        RouterEngine router =
                new RouterEngine();

        router.putInterface(
                iface(
                        "lan0",
                        "192.168.1.1",
                        24,
                        "A00000000001"
                )
        );

        router.putInterface(
                iface(
                        "transit0",
                        "10.0.0.1",
                        30,
                        "A00000000002"
                )
        );

        router.addRoute(
                new RouterRoute(
                        "192.168.2.0",
                        24,
                        "10.0.0.2",
                        "transit0",
                        10,
                        "STATIC"
                )
        );

        return router;
    }

    private static RouterEngine routerB() {
        RouterEngine router =
                routerBWithoutServerNeighbor();

        router.neighbors()
                .learn(
                        "lan1",
                        "192.168.2.20",
                        "C00000000020"
                );

        router.neighbors()
                .learn(
                        "transit0",
                        "10.0.0.1",
                        "A00000000002"
                );

        return router;
    }

    private static RouterEngine routerBWithoutServerNeighbor() {
        RouterEngine router =
                new RouterEngine();

        router.putInterface(
                iface(
                        "transit0",
                        "10.0.0.2",
                        30,
                        "B00000000002"
                )
        );

        router.putInterface(
                iface(
                        "lan1",
                        "192.168.2.1",
                        24,
                        "B00000000003"
                )
        );

        router.addRoute(
                new RouterRoute(
                        "192.168.1.0",
                        24,
                        "10.0.0.1",
                        "transit0",
                        10,
                        "STATIC"
                )
        );

        return router;
    }

    private static RouterPacket packet(
            String sourceIp,
            String destinationIp,
            int ttl
    ) {
        return new RouterPacket(
                sourceIp,
                destinationIp,
                ttl,
                ICMP_PROTOCOL,
                new byte[0]
        );
    }

    private static RouterInterface iface(
            String name,
            String ipv4,
            int prefixLength,
            String macAddress
    ) {
        return new RouterInterface(
                name,
                ipv4,
                prefixLength,
                macAddress,
                true
        );
    }

    private static RouterMultiHopTestResult result(
            String name,
            boolean passed,
            String detail
    ) {
        return new RouterMultiHopTestResult(
                name,
                passed,
                detail == null
                        ? ""
                        : detail
        );
    }
}

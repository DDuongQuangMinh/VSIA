package com.k1ngtle.vsia.signality.engineering.wifi.ip.router;

import java.util.List;

public final class RouterLiveTestSuite {
    private RouterLiveTestSuite() {
    }

    public static List<RouterLiveTestResult> runAll() {
        return List.of(
                l2RewritePreservesIpv4(),
                reversePath(),
                logicalInterfaceSelection(),
                gatewayNextHopSeparation(),
                timeExceededFields(),
                unreachableFields()
        );
    }

    private static RouterEngine router() {
        RouterEngine router =
                new RouterEngine();

        router.putInterface(
                new RouterInterface(
                        "lan0",
                        "192.168.1.1",
                        24,
                        "routermac",
                        true
                )
        );

        router.putInterface(
                new RouterInterface(
                        "lan1",
                        "192.168.2.1",
                        24,
                        "routermac",
                        true
                )
        );

        return router;
    }

    private static RouterLiveTestResult l2RewritePreservesIpv4() {
        RouterEngine router =
                router();

        router.neighbors().learn(
                "lan1",
                "192.168.2.20",
                "servermac"
        );

        RouterForwardDecision decision =
                router.evaluate(
                        "lan0",
                        new RouterPacket(
                                "192.168.1.101",
                                "192.168.2.20",
                                64,
                                6,
                                new byte[0]
                        )
                );

        RouterEgressPlan plan =
                RouterEgressPlan.from(
                        decision,
                        "routermac"
                );

        return result(
                "wifi-w1106b-l2-ip-separation",
                plan.sourceMac().equals("routermac")
                        && plan.destinationMac().equals("servermac")
                        && plan.sourceIp().equals("192.168.1.101")
                        && plan.destinationIp().equals("192.168.2.20")
                        && plan.ttl() == 63,
                "Router must rewrite L2 addresses while preserving IPv4 source/destination and decrementing TTL"
        );
    }

    private static RouterLiveTestResult reversePath() {
        RouterEngine router =
                router();

        router.neighbors().learn(
                "lan0",
                "192.168.1.101",
                "clientmac"
        );

        RouterForwardDecision decision =
                router.evaluate(
                        "lan1",
                        new RouterPacket(
                                "192.168.2.20",
                                "192.168.1.101",
                                64,
                                6,
                                new byte[0]
                        )
                );

        return result(
                "wifi-w1106b-reverse-path",
                decision.action() == RouterForwardAction.FORWARD
                        && decision.egressInterface().equals("lan0")
                        && decision.nextHopMac().equals("clientmac"),
                "Return traffic must route from lan1 back to lan0"
        );
    }

    private static RouterLiveTestResult logicalInterfaceSelection() {
        RouterEngine router =
                router();

        return result(
                "wifi-w1106b-interface-selection",
                router.interfaceForSourceNetwork("192.168.1.101")
                        .name().equals("lan0")
                        && router.interfaceForSourceNetwork("192.168.2.20")
                        .name().equals("lan1"),
                "Source network must identify the logical ingress interface"
        );
    }

    private static RouterLiveTestResult gatewayNextHopSeparation() {
        RouterEngine router =
                router();

        router.addRoute(
                new RouterRoute(
                        "203.0.113.0",
                        24,
                        "192.168.2.254",
                        "lan1",
                        10,
                        "STATIC"
                )
        );

        router.neighbors().learn(
                "lan1",
                "192.168.2.254",
                "upstream"
        );

        RouterForwardDecision decision =
                router.evaluate(
                        "lan0",
                        new RouterPacket(
                                "192.168.1.101",
                                "203.0.113.7",
                                64,
                                6,
                                new byte[0]
                        )
                );

        return result(
                "wifi-w1106b-next-hop-separation",
                decision.destinationIp().equals("203.0.113.7")
                        && decision.nextHopIp().equals("192.168.2.254")
                        && decision.nextHopMac().equals("upstream"),
                "Final IPv4 destination must remain separate from routed next hop"
        );
    }

    private static RouterLiveTestResult timeExceededFields() {
        RouterForwardDecision decision =
                router().evaluate(
                        "lan0",
                        new RouterPacket(
                                "192.168.1.101",
                                "192.168.2.20",
                                1,
                                6,
                                new byte[0]
                        )
                );

        return result(
                "wifi-w1106b-time-exceeded",
                decision.action()
                        == RouterForwardAction.ICMP_TIME_EXCEEDED
                        && decision.icmpType() == 11
                        && decision.icmpCode() == 0,
                "TTL expiration must request ICMP Type 11 Code 0"
        );
    }

    private static RouterLiveTestResult unreachableFields() {
        RouterForwardDecision decision =
                router().evaluate(
                        "lan0",
                        new RouterPacket(
                                "192.168.1.101",
                                "10.10.10.10",
                                64,
                                6,
                                new byte[0]
                        )
                );

        return result(
                "wifi-w1106b-unreachable",
                decision.action()
                        == RouterForwardAction.ICMP_DESTINATION_UNREACHABLE
                        && decision.icmpType() == 3
                        && decision.icmpCode() == 0,
                "No route must request ICMP Type 3 Code 0"
        );
    }

    private static RouterLiveTestResult result(
            String id,
            boolean passed,
            String detail
    ) {
        return new RouterLiveTestResult(
                id,
                passed,
                detail
        );
    }
}

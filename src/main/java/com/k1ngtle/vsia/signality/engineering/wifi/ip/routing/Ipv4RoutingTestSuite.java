package com.k1ngtle.vsia.signality.engineering.wifi.ip.routing;

import java.util.List;

public final class Ipv4RoutingTestSuite {
    private Ipv4RoutingTestSuite() {
    }

    public static List<Ipv4RoutingTestResult> runAll() {
        return List.of(
                mask24(),
                onLinkRoute(),
                gatewayRoute(),
                noGateway(),
                longestPrefix(),
                metricTieBreak(),
                hostRoute32(),
                defaultRoute0(),
                ttlDecrement(),
                ttlExceeded()
        );
    }

    private static Ipv4RoutingTestResult mask24() {
        return result(
                "wifi-w1105-mask-24",
                Ipv4Prefix.prefixLengthFromMask("255.255.255.0") == 24
                        && Ipv4Prefix.network("192.168.1.101", 24)
                        .equals("192.168.1.0"),
                "/24 mask and network calculation must be exact"
        );
    }

    private static Ipv4RoutingTestResult onLinkRoute() {
        Ipv4RouteDecision d =
                Ipv4RoutingTable.hostTable(
                        "192.168.1.101",
                        "255.255.255.0",
                        "192.168.1.1"
                ).resolve("192.168.1.50");

        return result(
                "wifi-w1105-on-link",
                d.kind() == Ipv4RouteKind.ON_LINK
                        && d.nextHopIp().equals("192.168.1.50")
                        && d.prefixLength() == 24,
                "Same-subnet destination must ARP the destination itself"
        );
    }

    private static Ipv4RoutingTestResult gatewayRoute() {
        Ipv4RouteDecision d =
                Ipv4RoutingTable.hostTable(
                        "192.168.1.101",
                        "255.255.255.0",
                        "192.168.1.1"
                ).resolve("192.168.2.50");

        return result(
                "wifi-w1105-default-gateway",
                d.kind() == Ipv4RouteKind.GATEWAY
                        && d.nextHopIp().equals("192.168.1.1")
                        && d.destinationIp().equals("192.168.2.50"),
                "Off-link destination must keep final IPv4 destination but use the default gateway as L2 next hop"
        );
    }

    private static Ipv4RoutingTestResult noGateway() {
        Ipv4RouteDecision d =
                Ipv4RoutingTable.hostTable(
                        "192.168.1.101",
                        "255.255.255.0",
                        ""
                ).resolve("10.0.0.1");

        return result(
                "wifi-w1105-no-route",
                d.kind() == Ipv4RouteKind.UNREACHABLE,
                "Off-link destination without a default route must be unreachable"
        );
    }

    private static Ipv4RoutingTestResult longestPrefix() {
        Ipv4RoutingTable table =
                new Ipv4RoutingTable(
                        List.of(
                                new Ipv4RouteEntry(
                                        "0.0.0.0",
                                        0,
                                        "192.168.1.1",
                                        100,
                                        "DEFAULT"
                                ),
                                new Ipv4RouteEntry(
                                        "10.20.0.0",
                                        16,
                                        "192.168.1.2",
                                        50,
                                        "STATIC"
                                ),
                                new Ipv4RouteEntry(
                                        "10.20.30.0",
                                        24,
                                        "192.168.1.3",
                                        200,
                                        "STATIC"
                                )
                        )
                );

        Ipv4RouteDecision d =
                table.resolve("10.20.30.99");

        return result(
                "wifi-w1105-longest-prefix",
                d.nextHopIp().equals("192.168.1.3")
                        && d.prefixLength() == 24,
                "Longest prefix match must beat a lower metric on a shorter prefix"
        );
    }

    private static Ipv4RoutingTestResult metricTieBreak() {
        Ipv4RoutingTable table =
                new Ipv4RoutingTable(
                        List.of(
                                new Ipv4RouteEntry(
                                        "10.0.0.0",
                                        8,
                                        "192.168.1.9",
                                        50,
                                        "STATIC"
                                ),
                                new Ipv4RouteEntry(
                                        "10.0.0.0",
                                        8,
                                        "192.168.1.8",
                                        10,
                                        "STATIC"
                                )
                        )
                );

        return result(
                "wifi-w1105-metric",
                table.resolve("10.4.5.6")
                        .nextHopIp()
                        .equals("192.168.1.8"),
                "Equal prefix lengths must prefer the lower route metric"
        );
    }

    private static Ipv4RoutingTestResult hostRoute32() {
        Ipv4RoutingTable table =
                new Ipv4RoutingTable(
                        List.of(
                                new Ipv4RouteEntry(
                                        "203.0.113.7",
                                        32,
                                        "192.168.1.9",
                                        1,
                                        "HOST"
                                ),
                                new Ipv4RouteEntry(
                                        "0.0.0.0",
                                        0,
                                        "192.168.1.1",
                                        100,
                                        "DEFAULT"
                                )
                        )
                );

        return result(
                "wifi-w1105-host-route",
                table.resolve("203.0.113.7")
                        .nextHopIp()
                        .equals("192.168.1.9"),
                "/32 host route must override the default route"
        );
    }

    private static Ipv4RoutingTestResult defaultRoute0() {
        Ipv4RouteDecision d =
                new Ipv4RoutingTable(
                        List.of(
                                new Ipv4RouteEntry(
                                        "0.0.0.0",
                                        0,
                                        "192.168.1.1",
                                        100,
                                        "DEFAULT"
                                )
                        )
                ).resolve("198.51.100.40");

        return result(
                "wifi-w1105-default-prefix",
                d.prefixLength() == 0
                        && d.kind() == Ipv4RouteKind.GATEWAY,
                "/0 must match arbitrary usable IPv4 destinations"
        );
    }

    private static Ipv4RoutingTestResult ttlDecrement() {
        Ipv4ForwardingResult result =
                Ipv4ForwardingEngine.evaluate(
                        Ipv4RoutingTable.hostTable(
                                "192.168.1.1",
                                "255.255.255.0",
                                "192.168.1.254"
                        ),
                        "192.168.1.50",
                        64
                );

        return result(
                "wifi-w1105-ttl-decrement",
                result.forward()
                        && !result.timeExceeded()
                        && result.outgoingTtl() == 63,
                "A forwarding hop must decrement TTL by exactly one"
        );
    }

    private static Ipv4RoutingTestResult ttlExceeded() {
        Ipv4ForwardingResult forwarding =
                Ipv4ForwardingEngine.evaluate(
                        Ipv4RoutingTable.hostTable(
                                "192.168.1.1",
                                "255.255.255.0",
                                "192.168.1.254"
                        ),
                        "192.168.1.50",
                        1
                );

        return result(
                "wifi-w1105-ttl-expired",
                !forwarding.forward()
                        && forwarding.timeExceeded()
                        && forwarding.outgoingTtl() == 0,
                "TTL 1 must expire at the router and require ICMP Time Exceeded"
        );
    }

    private static Ipv4RoutingTestResult result(
            String id,
            boolean passed,
            String detail
    ) {
        return new Ipv4RoutingTestResult(
                id,
                passed,
                detail
        );
    }
}

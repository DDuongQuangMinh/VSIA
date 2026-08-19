package com.k1ngtle.vsia.signality.engineering.wifi.ip.routing;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class Ipv4RoutingTable {
    private final List<Ipv4RouteEntry> routes;

    public Ipv4RoutingTable(List<Ipv4RouteEntry> routes) {
        this.routes = routes == null ? List.of() : List.copyOf(routes);
    }

    public static Ipv4RoutingTable hostTable(
            String localIp,
            String subnetMask,
            String defaultGateway
    ) {
        if (!Ipv4Prefix.isUsableUnicast(localIp)) {
            return new Ipv4RoutingTable(List.of());
        }

        int prefix = Ipv4Prefix.prefixLengthFromMask(subnetMask);
        List<Ipv4RouteEntry> routes = new ArrayList<>();

        routes.add(
                new Ipv4RouteEntry(
                        Ipv4Prefix.network(localIp, prefix),
                        prefix,
                        "",
                        0,
                        "CONNECTED"
                )
        );

        if (Ipv4Prefix.isUsableUnicast(defaultGateway)) {
            routes.add(
                    new Ipv4RouteEntry(
                            "0.0.0.0",
                            0,
                            defaultGateway,
                            100,
                            "DEFAULT"
                    )
            );
        }

        return new Ipv4RoutingTable(routes);
    }

    public List<Ipv4RouteEntry> routes() {
        return routes;
    }

    public Ipv4RouteDecision resolve(String destinationIp) {
        if (!Ipv4Prefix.isUsableUnicast(destinationIp)) {
            return unreachable(
                    destinationIp,
                    "Destination is not usable IPv4 unicast"
            );
        }

        Ipv4RouteEntry best =
                routes.stream()
                        .filter(route -> route.matches(destinationIp))
                        .min(
                                Comparator
                                        .comparingInt(
                                                Ipv4RouteEntry::prefixLength
                                        )
                                        .reversed()
                                        .thenComparingInt(
                                                Ipv4RouteEntry::metric
                                        )
                        )
                        .orElse(null);

        if (best == null) {
            return unreachable(
                    destinationIp,
                    "No matching IPv4 route"
            );
        }

        if (best.onLink()) {
            return new Ipv4RouteDecision(
                    Ipv4RouteKind.ON_LINK,
                    destinationIp,
                    destinationIp,
                    best.network(),
                    best.prefixLength(),
                    best.metric(),
                    best.source(),
                    "ON-LINK "
                            + destinationIp
                            + " via "
                            + best.network()
                            + "/"
                            + best.prefixLength()
            );
        }

        return new Ipv4RouteDecision(
                Ipv4RouteKind.GATEWAY,
                destinationIp,
                best.nextHop(),
                best.network(),
                best.prefixLength(),
                best.metric(),
                best.source(),
                "GATEWAY "
                        + destinationIp
                        + " via "
                        + best.nextHop()
                        + " matched "
                        + best.network()
                        + "/"
                        + best.prefixLength()
        );
    }

    private static Ipv4RouteDecision unreachable(
            String destinationIp,
            String detail
    ) {
        return new Ipv4RouteDecision(
                Ipv4RouteKind.UNREACHABLE,
                destinationIp == null ? "" : destinationIp,
                "",
                "",
                -1,
                Integer.MAX_VALUE,
                "",
                detail
        );
    }
}

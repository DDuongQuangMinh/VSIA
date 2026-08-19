package com.k1ngtle.vsia.signality.engineering.wifi.ip.router;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class RouterEngine {
    private final Map<String, RouterInterface> interfaces =
            new LinkedHashMap<>();

    private final List<RouterRoute> staticRoutes =
            new ArrayList<>();

    private final RouterNeighborTable neighbors =
            new RouterNeighborTable();

    public void putInterface(
            RouterInterface iface
    ) {
        interfaces.put(
                iface.name(),
                iface
        );
    }

    public void removeInterface(
            String name
    ) {
        interfaces.remove(name);
    }

    public void clearInterfaces() {
        interfaces.clear();
    }

    public RouterInterface interfaceByName(
            String name
    ) {
        return interfaces.get(name);
    }

    public Collection<RouterInterface> interfaces() {
        return List.copyOf(
                interfaces.values()
        );
    }

    public RouterNeighborTable neighbors() {
        return neighbors;
    }

    public void addRoute(
            RouterRoute route
    ) {
        staticRoutes.add(route);
    }

    public void clearStaticRoutes() {
        staticRoutes.clear();
    }

    public List<RouterRoute> staticRoutes() {
        return List.copyOf(
                staticRoutes
        );
    }

    public void clearConfiguration() {
        interfaces.clear();
        staticRoutes.clear();
        neighbors.clear();
    }

    public RouterInterface interfaceForLocalIp(
            String ip
    ) {
        return interfaces.values()
                .stream()
                .filter(
                        RouterInterface::enabled
                )
                .filter(
                        iface ->
                                iface.ipv4Address()
                                        .equals(ip)
                )
                .findFirst()
                .orElse(null);
    }

    public RouterInterface interfaceForSourceNetwork(
            String ip
    ) {
        return interfaces.values()
                .stream()
                .filter(
                        RouterInterface::enabled
                )
                .filter(
                        iface ->
                                iface.contains(ip)
                )
                .max(
                        Comparator.comparingInt(
                                RouterInterface::prefixLength
                        )
                )
                .orElse(null);
    }

    public List<RouterRoute> routes() {
        List<RouterRoute> out =
                new ArrayList<>();

        for (RouterInterface iface
                : interfaces.values()) {
            if (iface.enabled()) {
                out.add(
                        new RouterRoute(
                                iface.network(),
                                iface.prefixLength(),
                                "",
                                iface.name(),
                                0,
                                "CONNECTED"
                        )
                );
            }
        }

        out.addAll(
                staticRoutes
        );

        return List.copyOf(out);
    }

    public RouterForwardDecision evaluate(
            String ingress,
            RouterPacket packet
    ) {
        if (packet == null) {
            return drop(
                    "null packet"
            );
        }

        RouterInterface local =
                interfaceForLocalIp(
                        packet.destinationIp()
                );

        if (local != null) {
            return decision(
                    RouterForwardAction.LOCAL_DELIVERY,
                    ingress,
                    local.name(),
                    packet,
                    "",
                    "",
                    packet.ttl(),
                    -1,
                    -1,
                    0,
                    0,
                    "LOCAL " + local.name()
            );
        }

        if (packet.ttl() <= 1) {
            return decision(
                    RouterForwardAction.ICMP_TIME_EXCEEDED,
                    ingress,
                    "",
                    packet,
                    "",
                    "",
                    0,
                    -1,
                    -1,
                    11,
                    0,
                    "TTL expired"
            );
        }

        RouterRoute best =
                routes()
                        .stream()
                        .filter(
                                route ->
                                        route.matches(
                                                packet.destinationIp()
                                        )
                        )
                        .min(
                                Comparator
                                        .comparingInt(
                                                RouterRoute::prefixLength
                                        )
                                        .reversed()
                                        .thenComparingInt(
                                                RouterRoute::metric
                                        )
                        )
                        .orElse(null);

        if (best == null) {
            return decision(
                    RouterForwardAction.ICMP_DESTINATION_UNREACHABLE,
                    ingress,
                    "",
                    packet,
                    "",
                    "",
                    packet.ttl(),
                    -1,
                    -1,
                    3,
                    0,
                    "No route"
            );
        }

        RouterInterface out =
                interfaces.get(
                        best.egressInterface()
                );

        if (out == null
                || !out.enabled()) {
            return decision(
                    RouterForwardAction.ICMP_DESTINATION_UNREACHABLE,
                    ingress,
                    best.egressInterface(),
                    packet,
                    "",
                    "",
                    packet.ttl(),
                    best.prefixLength(),
                    best.metric(),
                    3,
                    0,
                    "Egress unavailable"
            );
        }

        String nextHop =
                best.connected()
                        ? packet.destinationIp()
                        : best.nextHop();

        String mac =
                neighbors.lookup(
                        out.name(),
                        nextHop
                );

        if (mac.isBlank()) {
            return decision(
                    RouterForwardAction.ARP_REQUIRED,
                    ingress,
                    out.name(),
                    packet,
                    nextHop,
                    "",
                    packet.ttl() - 1,
                    best.prefixLength(),
                    best.metric(),
                    0,
                    0,
                    "ARP required for "
                            + nextHop
            );
        }

        return decision(
                RouterForwardAction.FORWARD,
                ingress,
                out.name(),
                packet,
                nextHop,
                mac,
                packet.ttl() - 1,
                best.prefixLength(),
                best.metric(),
                0,
                0,
                "FORWARD "
                        + packet.destinationIp()
                        + " via "
                        + nextHop
                        + " on "
                        + out.name()
                        + " TTL "
                        + packet.ttl()
                        + "->"
                        + (packet.ttl() - 1)
        );
    }

    private static RouterForwardDecision decision(
            RouterForwardAction action,
            String ingress,
            String egress,
            RouterPacket packet,
            String nextHop,
            String mac,
            int ttlOut,
            int prefix,
            int metric,
            int icmpType,
            int icmpCode,
            String detail
    ) {
        return new RouterForwardDecision(
                action,
                ingress,
                egress,
                packet.sourceIp(),
                packet.destinationIp(),
                nextHop,
                mac,
                packet.ttl(),
                ttlOut,
                "",
                prefix,
                metric,
                icmpType,
                icmpCode,
                detail
        );
    }

    private static RouterForwardDecision drop(
            String detail
    ) {
        return new RouterForwardDecision(
                RouterForwardAction.DROP,
                "",
                "",
                "",
                "",
                "",
                "",
                0,
                0,
                "",
                -1,
                Integer.MAX_VALUE,
                0,
                0,
                detail
        );
    }
}

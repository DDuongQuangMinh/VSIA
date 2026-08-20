package com.k1ngtle.vsia.signality.engineering.wifi.ip.ipv6;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class Ipv6RoutingTable {
    private final List<Ipv6Route> routes =
            new ArrayList<>();

    public void add(Ipv6Route route) {
        routes.add(route);
    }

    public Ipv6Route lookup(Ipv6Address destination) {
        return routes.stream()
                .filter(route -> route.prefix().contains(destination))
                .sorted(
                        Comparator
                                .comparingInt((Ipv6Route route) -> route.prefix().length())
                                .reversed()
                                .thenComparingInt(Ipv6Route::metric)
                )
                .findFirst()
                .orElse(null);
    }

    public List<Ipv6Route> routes() {
        return List.copyOf(routes);
    }
}

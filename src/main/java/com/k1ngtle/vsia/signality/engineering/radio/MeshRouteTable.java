package com.k1ngtle.vsia.signality.engineering.radio;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class MeshRouteTable {
    private final Map<UUID, RadioRoute> routes =
            new HashMap<>();

    public RadioRoute routeTo(
            UUID destination
    ) {
        RadioRoute route =
                routes.get(
                        destination
                );

        if (route == null) {
            return null;
        }

        if (route.expired(
                System.nanoTime()
        )) {
            routes.remove(
                    destination
            );

            return null;
        }

        return route;
    }

    public void learn(
            UUID destination,
            UUID nextHop,
            int hopCount,
            int destinationSequenceNumber,
            long lifetimeNanos
    ) {
        RadioRoute current =
                routes.get(
                        destination
                );

        boolean replace =
                current == null
                        || destinationSequenceNumber
                        > current.destinationSequenceNumber()
                        || (
                        destinationSequenceNumber
                                == current.destinationSequenceNumber()
                                && hopCount
                                < current.hopCount()
                );

        if (replace) {
            routes.put(
                    destination,
                    new RadioRoute(
                            destination,
                            nextHop,
                            hopCount,
                            destinationSequenceNumber,
                            System.nanoTime()
                                    + Math.max(
                                    1L,
                                    lifetimeNanos
                            )
                    )
            );
        }
    }

    public void invalidate(
            UUID destination
    ) {
        routes.remove(
                destination
        );
    }
}

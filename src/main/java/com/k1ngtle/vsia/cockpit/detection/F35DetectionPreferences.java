package com.k1ngtle.vsia.cockpit.detection;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class F35DetectionPreferences {
    private static final Map<UUID, F35DetectionFilter> FILTERS =
            new ConcurrentHashMap<>();

    private F35DetectionPreferences() {
    }

    public static F35DetectionFilter get(
            UUID playerId
    ) {
        return FILTERS.getOrDefault(
                playerId,
                F35DetectionFilter.ALL
        );
    }

    public static void set(
            UUID playerId,
            F35DetectionFilter filter
    ) {
        FILTERS.put(
                playerId,
                filter == null
                        ? F35DetectionFilter.ALL
                        : filter
        );
    }

    public static void clear(
            UUID playerId
    ) {
        FILTERS.remove(
                playerId
        );
    }
}

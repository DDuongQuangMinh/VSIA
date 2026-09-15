package com.k1ngtle.vsia.signality.radar.iff;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class IffNetworkKeyRegistry {
    public static final String DEFAULT_TEST_KEY = "VSIA-IFF-TEST-KEY";

    private static final Map<String, String> KEYS =
            new ConcurrentHashMap<>();

    private IffNetworkKeyRegistry() {
    }

    public static void setKey(String networkId, String key) {
        String network = normalize(networkId);
        if (key == null || key.isBlank()) {
            KEYS.remove(network);
            return;
        }
        KEYS.put(network, key);
    }

    public static boolean hasCustomKey(String networkId) {
        return KEYS.containsKey(normalize(networkId));
    }

    public static void resetKey(String networkId) {
        KEYS.remove(normalize(networkId));
    }

    public static String keyFor(String networkId) {
        return KEYS.getOrDefault(
                normalize(networkId),
                DEFAULT_TEST_KEY
        );
    }

    public static void clear() {
        KEYS.clear();
    }

    private static String normalize(String value) {
        return value == null || value.isBlank()
                ? "default"
                : value.trim().toLowerCase();
    }
}

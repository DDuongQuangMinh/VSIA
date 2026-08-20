package com.k1ngtle.vsia.signality.engineering.firewall.w117;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class W117NeighborCache {
    public record Entry(
            String ipv4,
            String mac,
            long learnedMillis,
            long expiresAtMillis
    ) {
        public boolean expired(long nowMillis) {
            return nowMillis >= expiresAtMillis;
        }
    }

    private final Map<String, Entry> entries =
            new LinkedHashMap<>();

    private final long timeoutMillis;

    public W117NeighborCache() {
        this(60_000L);
    }

    public W117NeighborCache(long timeoutMillis) {
        if (timeoutMillis <= 0L) {
            throw new IllegalArgumentException("timeoutMillis");
        }

        this.timeoutMillis = timeoutMillis;
    }

    public void learn(
            String ipv4,
            String mac,
            long nowMillis
    ) {
        if (!W117Ipv4.valid(ipv4)) {
            throw new IllegalArgumentException("ipv4");
        }

        if (mac == null || mac.isBlank()) {
            throw new IllegalArgumentException("mac");
        }

        entries.put(
                ipv4,
                new Entry(
                        ipv4,
                        mac,
                        nowMillis,
                        nowMillis + timeoutMillis
                )
        );
    }

    public Optional<Entry> lookup(
            String ipv4,
            long nowMillis
    ) {
        expire(nowMillis);
        return Optional.ofNullable(entries.get(ipv4));
    }

    public int expire(long nowMillis) {
        int before = entries.size();

        entries.entrySet().removeIf(
                entry -> entry.getValue().expired(nowMillis)
        );

        return before - entries.size();
    }

    public int size(long nowMillis) {
        expire(nowMillis);
        return entries.size();
    }

    public void clear() {
        entries.clear();
    }

    public Map<String, Entry> snapshot(long nowMillis) {
        expire(nowMillis);
        return Map.copyOf(entries);
    }
}

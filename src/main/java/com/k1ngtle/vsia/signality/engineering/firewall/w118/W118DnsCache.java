package com.k1ngtle.vsia.signality.engineering.firewall.w118;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class W118DnsCache {
    public record Entry(
            String name,
            String answer,
            int rcode,
            long expiresAtMillis
    ) {
        public boolean expired(long nowMillis) {
            return nowMillis >= expiresAtMillis;
        }
    }

    private final Map<String, Entry> entries =
            new LinkedHashMap<>();

    public void put(
            String name,
            String answer,
            int rcode,
            long ttlSeconds,
            long nowMillis
    ) {
        String key =
                W118DnsMessage.normalize(name);

        entries.put(
                key,
                new Entry(
                        key,
                        answer == null ? "" : answer,
                        rcode,
                        nowMillis
                                + Math.max(
                                0L,
                                ttlSeconds
                        ) * 1000L
                )
        );
    }

    public Optional<Entry> lookup(
            String name,
            long nowMillis
    ) {
        expire(nowMillis);

        return Optional.ofNullable(
                entries.get(
                        W118DnsMessage.normalize(name)
                )
        );
    }

    public int expire(long nowMillis) {
        int before =
                entries.size();

        entries.entrySet().removeIf(
                e -> e.getValue().expired(nowMillis)
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
}

package com.k1ngtle.vsia.signality.engineering.wifi.bridge.w119;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class W119BridgeTable {
    public record Entry(
            String macAddress,
            W119BridgePort port,
            long lastSeenMillis
    ) {
    }

    private static final long DEFAULT_LIFETIME_MILLIS =
            300_000L;

    private final Map<String, Entry> entries =
            new LinkedHashMap<>();

    private final long lifetimeMillis;

    public W119BridgeTable() {
        this(DEFAULT_LIFETIME_MILLIS);
    }

    public W119BridgeTable(long lifetimeMillis) {
        this.lifetimeMillis =
                Math.max(1_000L, lifetimeMillis);
    }

    public void learn(
            String macAddress,
            W119BridgePort port,
            long nowMillis
    ) {
        if (W119Mac.isGroup(macAddress)) {
            return;
        }

        String key =
                W119Mac.normalize(macAddress);

        entries.put(
                key,
                new Entry(
                        key,
                        port,
                        nowMillis
                )
        );
    }

    public Optional<Entry> lookup(
            String macAddress,
            long nowMillis
    ) {
        expire(nowMillis);

        return Optional.ofNullable(
                entries.get(
                        W119Mac.normalize(macAddress)
                )
        );
    }

    public boolean remove(
            String macAddress
    ) {
        if (macAddress == null
                || macAddress.isBlank()) {
            return false;
        }

        return entries.remove(
                W119Mac.normalize(
                        macAddress
                )
        ) != null;
    }

    public int expire(long nowMillis) {
        int before =
                entries.size();

        entries.entrySet().removeIf(
                entry -> nowMillis
                        - entry.getValue().lastSeenMillis()
                        >= lifetimeMillis
        );

        return before - entries.size();
    }

    public int size(long nowMillis) {
        expire(nowMillis);
        return entries.size();
    }

    public Map<String, Entry> snapshot(
            long nowMillis
    ) {
        expire(nowMillis);
        return Map.copyOf(entries);
    }

    public void clear() {
        entries.clear();
    }
}

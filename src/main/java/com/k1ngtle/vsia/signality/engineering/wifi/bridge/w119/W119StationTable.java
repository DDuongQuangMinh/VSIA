package com.k1ngtle.vsia.signality.engineering.wifi.bridge.w119;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class W119StationTable {
    private static final long DEFAULT_LIFETIME_MILLIS =
            300_000L;

    private final Map<String, W119StationEntry> entries =
            new LinkedHashMap<>();

    private final long lifetimeMillis;
    private int nextAssociationId = 1;

    public W119StationTable() {
        this(DEFAULT_LIFETIME_MILLIS);
    }

    public W119StationTable(long lifetimeMillis) {
        this.lifetimeMillis =
                Math.max(1_000L, lifetimeMillis);
    }

    public W119StationEntry learn(
            String macAddress,
            long nowMillis
    ) {
        String key =
                normalize(macAddress);

        W119StationEntry previous =
                entries.get(key);

        int associationId;

        if (previous != null) {
            associationId =
                    previous.associationId();
        } else {
            associationId =
                    nextAssociationId++;

            if (nextAssociationId > 2007) {
                nextAssociationId = 1;
            }
        }

        W119StationEntry updated =
                new W119StationEntry(
                        key,
                        associationId,
                        nowMillis
                );

        entries.put(key, updated);
        return updated;
    }

    public Optional<W119StationEntry> lookup(
            String macAddress,
            long nowMillis
    ) {
        expire(nowMillis);

        return Optional.ofNullable(
                entries.get(
                        normalize(macAddress)
                )
        );
    }

    public boolean contains(
            String macAddress,
            long nowMillis
    ) {
        return lookup(
                macAddress,
                nowMillis
        ).isPresent();
    }

    public int expire(long nowMillis) {
        int before =
                entries.size();

        entries.entrySet().removeIf(
                entry -> entry.getValue()
                        .expired(
                                nowMillis,
                                lifetimeMillis
                        )
        );

        return before - entries.size();
    }

    public int size(long nowMillis) {
        expire(nowMillis);
        return entries.size();
    }

    public Map<String, W119StationEntry> snapshot(
            long nowMillis
    ) {
        expire(nowMillis);
        return Map.copyOf(entries);
    }

    public void clear() {
        entries.clear();
        nextAssociationId = 1;
    }

    private static String normalize(String macAddress) {
        return W119Mac.normalize(macAddress);
    }
}

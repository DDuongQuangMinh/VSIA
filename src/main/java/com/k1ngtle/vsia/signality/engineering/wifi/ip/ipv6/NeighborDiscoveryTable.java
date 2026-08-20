package com.k1ngtle.vsia.signality.engineering.wifi.ip.ipv6;

import java.util.LinkedHashMap;
import java.util.Map;

public final class NeighborDiscoveryTable {
    private final Map<Ipv6Address, NeighborEntry> entries =
            new LinkedHashMap<>();

    public void learn(
            Ipv6Address address,
            String mac,
            long nowMicros,
            long reachableMicros
    ) {
        if (address == null) throw new IllegalArgumentException("address");
        if (mac == null || mac.isBlank()) throw new IllegalArgumentException("mac");

        entries.put(
                address,
                new NeighborEntry(
                        address,
                        mac,
                        State.REACHABLE,
                        nowMicros + Math.max(1L, reachableMicros)
                )
        );
    }

    public NeighborEntry lookup(
            Ipv6Address address,
            long nowMicros
    ) {
        NeighborEntry entry = entries.get(address);
        if (entry == null) return null;

        if (entry.state() == State.REACHABLE
                && nowMicros >= entry.reachableUntilMicros()) {
            entry = new NeighborEntry(
                    entry.address(),
                    entry.mac(),
                    State.STALE,
                    entry.reachableUntilMicros()
            );
            entries.put(address, entry);
        }

        return entry;
    }

    public void remove(Ipv6Address address) {
        entries.remove(address);
    }

    public int size() {
        return entries.size();
    }

    public record NeighborEntry(
            Ipv6Address address,
            String mac,
            State state,
            long reachableUntilMicros
    ) {
    }

    public enum State {
        INCOMPLETE,
        REACHABLE,
        STALE,
        DELAY,
        PROBE
    }
}

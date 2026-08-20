package com.k1ngtle.vsia.signality.engineering.firewall;

import java.util.LinkedHashMap;
import java.util.Map;

public final class FragmentAssociationTable {
    private final Map<Key, Association> entries = new LinkedHashMap<>();
    private final long timeoutMillis;

    public FragmentAssociationTable() {
        this(30_000L);
    }

    public FragmentAssociationTable(long timeoutMillis) {
        if (timeoutMillis <= 0) throw new IllegalArgumentException("timeoutMillis");
        this.timeoutMillis = timeoutMillis;
    }

    public void remember(
            FirewallPacketView firstFragment,
            ConntrackState state,
            Nat44Mapping natMapping,
            long nowMillis
    ) {
        if (!firstFragment.fragmented() || firstFragment.fragmentOffset() != 0) {
            return;
        }

        entries.put(
                key(firstFragment),
                new Association(
                        state,
                        natMapping,
                        nowMillis + timeoutMillis
                )
        );
    }

    public Association lookup(
            FirewallPacketView fragment,
            long nowMillis
    ) {
        expire(nowMillis);
        return entries.get(key(fragment));
    }

    public int expire(long nowMillis) {
        int before = entries.size();
        entries.entrySet().removeIf(entry ->
                nowMillis >= entry.getValue().expiresAtMillis()
        );
        return before - entries.size();
    }

    private static Key key(FirewallPacketView packet) {
        return new Key(
                packet.family(),
                packet.sourceIp(),
                packet.destinationIp(),
                packet.protocol(),
                packet.fragmentIdentification()
        );
    }

    public record Key(
            IpFamily family,
            String sourceIp,
            String destinationIp,
            String protocol,
            int identification
    ) {
    }

    public record Association(
            ConntrackState state,
            Nat44Mapping natMapping,
            long expiresAtMillis
    ) {
    }
}

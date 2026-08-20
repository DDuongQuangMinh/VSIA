package com.k1ngtle.vsia.signality.engineering.firewall;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class ConntrackTable {
    private final List<ConntrackEntry> entries = new ArrayList<>();
    private final int maxEntries;

    public ConntrackTable() {
        this(4096);
    }

    public ConntrackTable(int maxEntries) {
        if (maxEntries < 1) throw new IllegalArgumentException("maxEntries");
        this.maxEntries = maxEntries;
    }

    public Lookup classify(
            FirewallPacketView packet,
            long nowMillis
    ) {
        expire(nowMillis);

        if (packet.icmpError() && packet.relatedFlow() != null) {
            for (ConntrackEntry entry : entries) {
                if (entry.matches(packet.relatedFlow())) {
                    return new Lookup(
                            ConntrackState.RELATED,
                            entry,
                            false
                    );
                }
            }
            return new Lookup(ConntrackState.INVALID, null, false);
        }

        FirewallFlowKey key = packet.flowKey();

        for (ConntrackEntry entry : entries) {
            if (entry.matches(key)) {
                boolean reply = entry.replyDirection(key);
                entry.observe(packet, reply, nowMillis);
                return new Lookup(entry.state(), entry, reply);
            }
        }

        if (packet.nonInitialFragment()) {
            return new Lookup(ConntrackState.INVALID, null, false);
        }

        return new Lookup(ConntrackState.NEW, null, false);
    }

    public Lookup classifyInboundNat(
            FirewallPacketView packet,
            Nat44Mapping mapping,
            long nowMillis
    ) {
        if (packet == null) {
            throw new IllegalArgumentException("packet");
        }

        if (mapping == null) {
            throw new IllegalArgumentException("mapping");
        }

        FirewallPacketView translated =
                new FirewallPacketView(
                        packet.family(),
                        packet.protocol(),
                        packet.sourceIp(),
                        packet.sourcePort(),
                        mapping.insideLocalIp(),
                        mapping.insideLocalPort(),
                        packet.ingressInterface(),
                        packet.egressInterface(),
                        packet.tcpSyn(),
                        packet.tcpAck(),
                        packet.tcpFin(),
                        packet.tcpRst(),
                        packet.icmpError(),
                        packet.relatedFlow(),
                        packet.fragmentIdentification(),
                        packet.fragmentOffset(),
                        packet.moreFragments()
                );

        Lookup lookup =
                classify(
                        translated,
                        nowMillis
                );

        if (lookup.entry() == null) {
            return new Lookup(
                    ConntrackState.INVALID,
                    null,
                    true
            );
        }

        return new Lookup(
                lookup.state(),
                lookup.entry(),
                true
        );
    }

    public ConntrackEntry create(
            FirewallPacketView packet,
            long nowMillis
    ) {
        expire(nowMillis);

        if (entries.size() >= maxEntries) {
            throw new IllegalStateException("conntrack table full");
        }

        ConntrackEntry entry =
                new ConntrackEntry(
                        packet.flowKey(),
                        nowMillis,
                        30_000L
                );

        entry.observe(packet, false, nowMillis);
        entries.add(entry);
        return entry;
    }

    public int expire(long nowMillis) {
        int removed = 0;
        Iterator<ConntrackEntry> iterator = entries.iterator();

        while (iterator.hasNext()) {
            if (iterator.next().expired(nowMillis)) {
                iterator.remove();
                removed++;
            }
        }

        return removed;
    }

    public int size() {
        return entries.size();
    }

    public List<ConntrackEntry> entries() {
        return List.copyOf(entries);
    }

    public record Lookup(
            ConntrackState state,
            ConntrackEntry entry,
            boolean replyDirection
    ) {
    }
}

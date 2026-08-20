package com.k1ngtle.vsia.signality.engineering.firewall;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class Nat44Table {
    private final List<Nat44Mapping> mappings = new ArrayList<>();
    private final int portStart;
    private final int portEnd;
    private int nextPort;

    public Nat44Table() {
        this(40000, 60000);
    }

    public Nat44Table(int portStart, int portEnd) {
        if (portStart < 1024 || portEnd > 65535 || portStart > portEnd) {
            throw new IllegalArgumentException("PAT port range");
        }

        this.portStart = portStart;
        this.portEnd = portEnd;
        this.nextPort = portStart;
    }

    public Nat44Mapping allocatePat(
            FirewallPacketView packet,
            String publicIp,
            long nowMillis
    ) {
        if (packet.family() != IpFamily.IPV4) {
            throw new IllegalArgumentException("NAT44 only supports IPv4");
        }

        expire(nowMillis);

        for (Nat44Mapping mapping : mappings) {
            if (mapping.outboundMatches(packet)) {
                return mapping;
            }
        }

        int port = allocatePort(
                publicIp,
                packet.protocol()
        );

        long timeout =
                packet.protocol().equals("TCP")
                        ? 300_000L
                        : 60_000L;

        Nat44Mapping mapping =
                new Nat44Mapping(
                        packet.protocol(),
                        packet.sourceIp(),
                        packet.sourcePort(),
                        publicIp,
                        port,
                        packet.destinationIp(),
                        packet.destinationPort(),
                        nowMillis,
                        nowMillis + timeout
                );

        mappings.add(mapping);
        return mapping;
    }

    public Nat44Mapping findInbound(
            FirewallPacketView packet,
            long nowMillis
    ) {
        expire(nowMillis);

        for (Nat44Mapping mapping : mappings) {
            if (mapping.inboundMatches(packet)) {
                return mapping;
            }
        }

        return null;
    }

    public int expire(long nowMillis) {
        int removed = 0;
        Iterator<Nat44Mapping> iterator = mappings.iterator();

        while (iterator.hasNext()) {
            if (iterator.next().expired(nowMillis)) {
                iterator.remove();
                removed++;
            }
        }

        return removed;
    }

    private int allocatePort(
            String publicIp,
            String protocol
    ) {
        int range = portEnd - portStart + 1;

        for (int attempt = 0; attempt < range; attempt++) {
            int candidate = nextPort++;
            if (nextPort > portEnd) nextPort = portStart;

            boolean used = mappings.stream().anyMatch(mapping ->
                    mapping.insideGlobalPort() == candidate
                            && mapping.insideGlobalIp().equals(publicIp)
                            && mapping.protocol().equalsIgnoreCase(protocol)
            );

            if (!used) return candidate;
        }

        throw new IllegalStateException("PAT port pool exhausted");
    }

    public int size() {
        return mappings.size();
    }

    public List<Nat44Mapping> mappings() {
        return List.copyOf(mappings);
    }
}

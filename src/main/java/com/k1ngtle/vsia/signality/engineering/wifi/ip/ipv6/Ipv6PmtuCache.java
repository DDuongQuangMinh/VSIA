package com.k1ngtle.vsia.signality.engineering.wifi.ip.ipv6;

import java.util.LinkedHashMap;
import java.util.Map;

public final class Ipv6PmtuCache {
    public static final int MIN_IPV6_MTU = 1280;

    private final Map<Ipv6Address, Entry> entries =
            new LinkedHashMap<>();

    public int pmtuFor(
            Ipv6Address destination,
            int interfaceMtu,
            long nowMicros
    ) {
        if (interfaceMtu < MIN_IPV6_MTU) {
            throw new IllegalArgumentException("IPv6 interface MTU must be >= 1280");
        }

        Entry entry = entries.get(destination);
        if (entry == null || nowMicros >= entry.expiresAtMicros()) {
            if (entry != null) entries.remove(destination);
            return interfaceMtu;
        }

        return Math.min(interfaceMtu, entry.mtu());
    }

    public void learnPacketTooBig(
            Ipv6Address destination,
            int advertisedMtu,
            long nowMicros,
            long lifetimeMicros
    ) {
        if (advertisedMtu < MIN_IPV6_MTU) {
            advertisedMtu = MIN_IPV6_MTU;
        }

        if (advertisedMtu > 65535) {
            advertisedMtu = 65535;
        }

        Entry previous = entries.get(destination);
        int effective =
                previous == null
                        ? advertisedMtu
                        : Math.min(previous.mtu(), advertisedMtu);

        entries.put(
                destination,
                new Entry(
                        effective,
                        nowMicros + Math.max(1L, lifetimeMicros)
                )
        );
    }

    public int size() {
        return entries.size();
    }

    public record Entry(
            int mtu,
            long expiresAtMicros
    ) {
    }
}

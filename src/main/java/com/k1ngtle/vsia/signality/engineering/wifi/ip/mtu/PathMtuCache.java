package com.k1ngtle.vsia.signality.engineering.wifi.ip.mtu;

import java.util.LinkedHashMap;
import java.util.Map;

public final class PathMtuCache {
    public static final int DEFAULT_IPV4_MTU =
            1500;

    public static final int MIN_IPV4_REASSEMBLY_MTU =
            576;

    private final Map<String, Entry> entries =
            new LinkedHashMap<>();

    public void learn(
            String destinationIp,
            int mtu,
            long nowMicros
    ) {
        if (destinationIp == null
                || destinationIp.isBlank()) {
            return;
        }

        int normalized =
                Math.max(
                        MIN_IPV4_REASSEMBLY_MTU,
                        Math.min(
                                65535,
                                mtu
                        )
                );

        Entry current =
                entries.get(
                        destinationIp
                );

        if (current == null
                || normalized
                < current.mtu()) {
            entries.put(
                    destinationIp,
                    new Entry(
                            normalized,
                            nowMicros
                    )
            );
        }
    }

    public int mtuFor(
            String destinationIp
    ) {
        Entry entry =
                entries.get(
                        destinationIp
                );

        return entry == null
                ? DEFAULT_IPV4_MTU
                : entry.mtu();
    }

    public Entry entry(
            String destinationIp
    ) {
        return entries.get(
                destinationIp
        );
    }

    public void clear() {
        entries.clear();
    }

    public record Entry(
            int mtu,
            long learnedMicros
    ) {
    }
}

package com.k1ngtle.vsia.signality.engineering.wifi.ip.ipv6;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class DualStackDnsTable {
    private final Map<String, String> aRecords =
            new LinkedHashMap<>();

    private final Map<String, Ipv6DnsRecord> aaaaRecords =
            new LinkedHashMap<>();

    public void putA(
            String name,
            String ipv4
    ) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("name");
        if (ipv4 == null || ipv4.isBlank()) throw new IllegalArgumentException("ipv4");
        aRecords.put(normalize(name), ipv4);
    }

    public void putAaaa(
            Ipv6DnsRecord record
    ) {
        if (record == null) throw new IllegalArgumentException("record");
        aaaaRecords.put(normalize(record.name()), record);
    }

    public Resolution resolve(
            String name,
            boolean ipv4Available,
            boolean ipv6Available
    ) {
        String key = normalize(name);

        String a = aRecords.get(key);
        Ipv6DnsRecord aaaa = aaaaRecords.get(key);

        DualStackRouteSelector.Family family =
                DualStackRouteSelector.select(
                        ipv4Available,
                        ipv6Available,
                        a != null,
                        aaaa != null
                );

        return new Resolution(
                family,
                a,
                aaaa == null ? null : aaaa.address()
        );
    }

    private static String normalize(String name) {
        return name.trim()
                .toLowerCase(Locale.ROOT);
    }

    public record Resolution(
            DualStackRouteSelector.Family family,
            String ipv4,
            Ipv6Address ipv6
    ) {
    }
}

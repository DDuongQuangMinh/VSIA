package com.k1ngtle.vsia.signality.engineering.firewall.w118;

import com.k1ngtle.vsia.signality.engineering.firewall.w117.W117Ipv4;
import com.k1ngtle.vsia.signality.internet.OSINetworkPacket;

import java.util.LinkedHashMap;
import java.util.Map;

public final class W118DnsServer {
    public record Record(
            String name,
            String ipv4,
            long ttlSeconds
    ) {
    }

    private final Map<String, Record> records =
            new LinkedHashMap<>();

    private final W118DnsCache cache =
            new W118DnsCache();

    private long queries = 0L;
    private long cacheHits = 0L;
    private long nxDomain = 0L;

    public W118DnsServer() {
        addA(
                "vsia.test",
                "203.0.113.20",
                120L
        );

        addA(
                "gateway.lan",
                "192.168.10.1",
                120L
        );

        addA(
                "wan-host.test",
                "203.0.113.20",
                120L
        );
    }

    public void addA(
            String name,
            String ipv4,
            long ttlSeconds
    ) {
        if (!W117Ipv4.valid(ipv4)) {
            throw new IllegalArgumentException("ipv4");
        }

        String key =
                W118DnsMessage.normalize(name);

        records.put(
                key,
                new Record(
                        key,
                        ipv4,
                        Math.max(
                                1L,
                                ttlSeconds
                        )
                )
        );
    }

    public OSINetworkPacket handle(
            OSINetworkPacket query,
            String serverIp,
            long nowMillis
    ) {
        if (!W118DnsMessage.isDns(query)
                || W118DnsMessage.isResponse(query)) {
            return null;
        }

        queries++;

        String name =
                W118DnsMessage.queryName(query);

        var cached =
                cache.lookup(
                        name,
                        nowMillis
                );

        if (cached.isPresent()) {
            cacheHits++;

            W118DnsCache.Entry entry =
                    cached.get();

            return W118DnsMessage.response(
                    query,
                    serverIp,
                    entry.answer(),
                    30L,
                    entry.rcode()
            );
        }

        Record record =
                records.get(
                        W118DnsMessage.normalize(name)
                );

        if (record == null) {
            nxDomain++;

            cache.put(
                    name,
                    "",
                    3,
                    30L,
                    nowMillis
            );

            return W118DnsMessage.response(
                    query,
                    serverIp,
                    "",
                    30L,
                    3
            );
        }

        cache.put(
                name,
                record.ipv4(),
                0,
                record.ttlSeconds(),
                nowMillis
        );

        return W118DnsMessage.response(
                query,
                serverIp,
                record.ipv4(),
                record.ttlSeconds(),
                0
        );
    }

    public void clearCache() {
        cache.clear();
    }

    public String status(long nowMillis) {
        return "DNS records="
                + records.size()
                + " cache="
                + cache.size(nowMillis)
                + " queries="
                + queries
                + " cacheHits="
                + cacheHits
                + " nxdomain="
                + nxDomain;
    }
}

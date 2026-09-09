package com.k1ngtle.vsia.signality.internet.provider;

public record InternetDnsAnswer(
        String queryName,
        String type,
        String value,
        int ttlSeconds,
        String zone,
        boolean authoritative,
        String trace
) {
}

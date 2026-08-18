package com.k1ngtle.vsia.signality.engineering.wifi.dns;

public record DnsQuestion(
        String name,
        DnsType type,
        int dnsClass
) {
    public static final int CLASS_IN = 1;
}

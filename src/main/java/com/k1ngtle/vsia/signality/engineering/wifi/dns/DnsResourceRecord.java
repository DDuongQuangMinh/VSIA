package com.k1ngtle.vsia.signality.engineering.wifi.dns;

public record DnsResourceRecord(
        String name,
        DnsType type,
        int dnsClass,
        long ttl,
        byte[] rdata,
        String text
) {
    public DnsResourceRecord {
        rdata = rdata == null ? new byte[0] : rdata.clone();
        text = text == null ? "" : text;
    }

    @Override
    public byte[] rdata() {
        return rdata.clone();
    }
}

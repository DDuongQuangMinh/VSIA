package com.k1ngtle.vsia.signality.engineering.wifi.ip.ipv6;

public record RawIpv6Packet(
        int trafficClass,
        int flowLabel,
        int payloadLength,
        int nextHeader,
        int hopLimit,
        Ipv6Address source,
        Ipv6Address destination,
        byte[] payload
) {
    public RawIpv6Packet {
        if (trafficClass < 0 || trafficClass > 255) throw new IllegalArgumentException("trafficClass");
        if (flowLabel < 0 || flowLabel > 0xFFFFF) throw new IllegalArgumentException("flowLabel");
        if (payloadLength < 0 || payloadLength > 65535) throw new IllegalArgumentException("payloadLength");
        if (nextHeader < 0 || nextHeader > 255) throw new IllegalArgumentException("nextHeader");
        if (hopLimit < 0 || hopLimit > 255) throw new IllegalArgumentException("hopLimit");
        if (source == null || destination == null) throw new IllegalArgumentException("address");
        payload = payload == null ? new byte[0] : payload.clone();

        if (payload.length != payloadLength) {
            throw new IllegalArgumentException("IPv6 payload length mismatch");
        }
    }

    @Override
    public byte[] payload() {
        return payload.clone();
    }

    public int totalLength() {
        return 40 + payloadLength;
    }
}

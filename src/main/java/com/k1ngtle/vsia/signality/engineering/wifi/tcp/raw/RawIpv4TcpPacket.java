package com.k1ngtle.vsia.signality.engineering.wifi.tcp.raw;

public record RawIpv4TcpPacket(
        RawIpv4Packet ipv4,
        RawTcpPacket tcp,
        byte[] rawBytes
) {
    public RawIpv4TcpPacket {
        rawBytes =
                rawBytes == null
                        ? new byte[0]
                        : rawBytes.clone();
    }

    @Override
    public byte[] rawBytes() {
        return rawBytes.clone();
    }

    public boolean valid() {
        return ipv4.checksumValid()
                && tcp.checksumValid()
                && ipv4.protocol() == 6;
    }
}

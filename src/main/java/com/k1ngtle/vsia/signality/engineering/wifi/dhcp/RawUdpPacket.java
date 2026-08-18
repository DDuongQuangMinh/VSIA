package com.k1ngtle.vsia.signality.engineering.wifi.dhcp;

public record RawUdpPacket(
        int sourcePort,
        int destinationPort,
        int length,
        int checksum,
        byte[] payload,
        boolean checksumValid
) {
    public RawUdpPacket {
        payload =
                payload == null
                        ? new byte[0]
                        : payload.clone();
    }

    @Override
    public byte[] payload() {
        return payload.clone();
    }
}

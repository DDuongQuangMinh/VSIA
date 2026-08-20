package com.k1ngtle.vsia.signality.engineering.wifi.ip.raw;

public record RawIcmpPacket(
        int type,
        int code,
        int checksum,
        int restOfHeader,
        byte[] payload,
        boolean checksumValid
) {
    public RawIcmpPacket {
        payload =
                payload == null
                        ? new byte[0]
                        : payload.clone();
    }

    @Override
    public byte[] payload() {
        return payload.clone();
    }

    public int nextHopMtu() {
        if (type == 3
                && code == 4) {
            return restOfHeader
                    & 0xFFFF;
        }

        return 0;
    }
}

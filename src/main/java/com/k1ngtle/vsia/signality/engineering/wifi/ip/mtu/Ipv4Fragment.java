package com.k1ngtle.vsia.signality.engineering.wifi.ip.mtu;

public record Ipv4Fragment(
        int identification,
        int offsetBytes,
        boolean moreFragments,
        int headerBytes,
        byte[] payload
) {
    public Ipv4Fragment {
        if (identification < 0
                || identification > 0xFFFF) {
            throw new IllegalArgumentException(
                    "IPv4 identification must be 0..65535"
            );
        }

        if (offsetBytes < 0
                || offsetBytes % 8 != 0) {
            throw new IllegalArgumentException(
                    "IPv4 fragment offset must be a non-negative multiple of 8 bytes"
            );
        }

        if (headerBytes < 20
                || headerBytes % 4 != 0) {
            throw new IllegalArgumentException(
                    "IPv4 header size must be >=20 and 32-bit aligned"
            );
        }

        payload =
                payload == null
                        ? new byte[0]
                        : payload.clone();
    }

    @Override
    public byte[] payload() {
        return payload.clone();
    }

    public int fragmentOffsetUnits() {
        return offsetBytes / 8;
    }

    public int totalLength() {
        return headerBytes
                + payload.length;
    }
}

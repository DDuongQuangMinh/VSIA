package com.k1ngtle.vsia.signality.engineering.wifi.tcp.raw;

import java.util.Arrays;

public record RawIpv4Packet(
        int version,
        int ihlWords,
        int dscpEcn,
        int totalLength,
        int identification,
        boolean dontFragment,
        boolean moreFragments,
        int fragmentOffset,
        int ttl,
        int protocol,
        int headerChecksum,
        String sourceAddress,
        String destinationAddress,
        byte[] options,
        byte[] payload,
        boolean checksumValid
) {
    public RawIpv4Packet {
        options =
                options == null
                        ? new byte[0]
                        : options.clone();

        payload =
                payload == null
                        ? new byte[0]
                        : payload.clone();
    }

    @Override
    public byte[] options() {
        return options.clone();
    }

    @Override
    public byte[] payload() {
        return payload.clone();
    }

    public int headerBytes() {
        return ihlWords
                * 4;
    }
}

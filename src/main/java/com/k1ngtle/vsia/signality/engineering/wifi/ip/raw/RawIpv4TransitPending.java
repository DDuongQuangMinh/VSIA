package com.k1ngtle.vsia.signality.engineering.wifi.ip.raw;

import net.minecraft.nbt.CompoundTag;

public record RawIpv4TransitPending(
        byte[] rawIpv4,
        String sourceMac,
        CompoundTag metadata,
        int fragmentIndex,
        int fragmentCount,
        String egressInterface,
        String nextHopIp,
        int outgoingTtl
) {
    public RawIpv4TransitPending {
        rawIpv4 = rawIpv4 == null ? new byte[0] : rawIpv4.clone();
        sourceMac = sourceMac == null ? "" : sourceMac;
        metadata = metadata == null ? new CompoundTag() : metadata.copy();
        egressInterface = egressInterface == null ? "" : egressInterface;
        nextHopIp = nextHopIp == null ? "" : nextHopIp;
    }

    @Override
    public byte[] rawIpv4() {
        return rawIpv4.clone();
    }

    @Override
    public CompoundTag metadata() {
        return metadata.copy();
    }
}

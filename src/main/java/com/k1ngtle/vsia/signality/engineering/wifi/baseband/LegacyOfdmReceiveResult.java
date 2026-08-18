package com.k1ngtle.vsia.signality.engineering.wifi.baseband;

public record LegacyOfdmReceiveResult(
        byte[] psdu,
        LegacyPacketDetection detection,
        int synchronizedPacketStart,
        double estimatedCfoHz,
        LegacySignalField signal,
        LegacyChannelEstimate channelEstimate
) {
    public LegacyOfdmReceiveResult {
        psdu =
                psdu.clone();
    }

    @Override
    public byte[] psdu() {
        return psdu.clone();
    }
}

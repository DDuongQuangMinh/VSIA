package com.k1ngtle.vsia.signality.engineering.cellular.userplane;

// CELLULAR_USER_PLANE_V1
public record CellularUserPlaneStatsSnapshot(
        long txPackets,
        long rxPackets,
        long droppedPackets,
        long txIpBytes,
        long rxIpBytes,
        long txRadioBytes,
        long rxRadioBytes,
        int lastSequence,
        String lastTarget,
        String lastResult,
        long lastRoundTripMicros
) {
    public double packetLossPercent() {
        if (txPackets <= 0L) {
            return 0.0;
        }

        long lost = Math.max(0L, txPackets - rxPackets);
        return 100.0 * lost / txPackets;
    }
}

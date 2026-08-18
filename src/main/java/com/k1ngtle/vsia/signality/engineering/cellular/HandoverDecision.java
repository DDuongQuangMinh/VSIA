package com.k1ngtle.vsia.signality.engineering.cellular;
public record HandoverDecision(
        boolean shouldHandover,
        CellRecord target,
        double marginDb
) {
    public static HandoverDecision none() {
        return new HandoverDecision(false, null, 0.0);
    }
}

package com.k1ngtle.vsia.signality.engineering.cellular;
import java.util.UUID;

public final class UeContext {
    private final UUID ueId;
    private final int rnti;
    private int cqi = 1;
    private int requestedResourceBlocks = 1;
    private long deliveredBits;
    private long scheduledTransmissions;

    public UeContext(UUID ueId, int rnti) {
        this.ueId = ueId;
        this.rnti = rnti;
    }

    public UUID ueId() { return ueId; }
    public int rnti() { return rnti; }
    public int cqi() { return cqi; }

    public void setCqi(int value) {
        cqi = Math.max(1, Math.min(15, value));
    }

    public int requestedResourceBlocks() {
        return requestedResourceBlocks;
    }

    public void setRequestedResourceBlocks(int value) {
        requestedResourceBlocks = Math.max(1, value);
    }

    public double averageDeliveredBits() {
        if (scheduledTransmissions == 0L) {
            return 1.0;
        }
        return Math.max(1.0, deliveredBits / (double) scheduledTransmissions);
    }

    public void recordDelivery(long bits) {
        deliveredBits += Math.max(0L, bits);
        scheduledTransmissions++;
    }
}

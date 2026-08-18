package com.k1ngtle.vsia.signality.engineering.cellular.mac;

public final class HarqProcess {
    private final int processId;
    private int transmissionCount;
    private boolean awaitingAck;
    private long transportBlockId = -1L;

    public HarqProcess(int processId) {
        if (processId < 0) {
            throw new IllegalArgumentException("processId");
        }
        this.processId = processId;
    }

    public int processId() {
        return processId;
    }

    public int transmissionCount() {
        return transmissionCount;
    }

    public boolean awaitingAck() {
        return awaitingAck;
    }

    public long transportBlockId() {
        return transportBlockId;
    }

    public void begin(long transportBlockId) {
        this.transportBlockId = transportBlockId;
        this.transmissionCount = 1;
        this.awaitingAck = true;
    }

    public void retransmit() {
        if (!awaitingAck) {
            throw new IllegalStateException("HARQ process is idle");
        }
        transmissionCount++;
    }

    public void acknowledge() {
        awaitingAck = false;
        transportBlockId = -1L;
        transmissionCount = 0;
    }

    public void negativeAcknowledge() {
        if (!awaitingAck) {
            return;
        }
    }
}

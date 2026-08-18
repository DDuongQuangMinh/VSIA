package com.k1ngtle.vsia.signality.engineering.wifi.trace;

public record WifiPacketTraceEvent(
        long sequence,
        long timestampMicros,
        WifiPacketDirection direction,
        String frameType,
        int subtype,
        String sourceMac,
        String destinationMac,
        int mcsIndex,
        String phyGeneration,
        int frameBytes,
        long airtimeMicros,
        double rssiDbm,
        double snrDb,
        double sinrDb,
        boolean retry,
        String detailedPhyPath,
        WifiPacketOutcome outcome,
        String detail
) {
    public WifiPacketTraceEvent {
        frameType = safe(frameType);
        sourceMac = safe(sourceMac);
        destinationMac = safe(destinationMac);
        phyGeneration = safe(phyGeneration);
        detailedPhyPath = safe(detailedPhyPath);
        detail = safe(detail);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}

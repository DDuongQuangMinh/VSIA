package com.k1ngtle.vsia.signality.engineering.reality;

public record NetworkRealityAssessment(
        double propagationDelayMicros,
        long desiredAirtimeMicros,
        double microTemporalInterferenceFactor,
        double correctedInterferencePowerWatts,
        double correctedSinrDb,
        double captureMarginDb,
        boolean receiverCapturedDesiredFrame
) {
}

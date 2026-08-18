package com.k1ngtle.vsia.signality.engineering.conformance;

public record ProtocolTraceEntry(
        long sequence,
        long tick,
        TraceDirection direction,
        String layer,
        String event,
        String detail
) {
}

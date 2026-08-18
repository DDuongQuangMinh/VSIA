package com.k1ngtle.vsia.signality.engineering.conformance;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

public final class ProtocolTraceRecorder {
    private final int capacity;
    private final ArrayDeque<ProtocolTraceEntry> entries =
            new ArrayDeque<>();

    private long nextSequence;

    public ProtocolTraceRecorder(
            int capacity
    ) {
        if (capacity < 1) {
            throw new IllegalArgumentException(
                    "capacity"
            );
        }

        this.capacity =
                capacity;
    }

    public synchronized void record(
            long tick,
            TraceDirection direction,
            String layer,
            String event,
            String detail
    ) {
        while (entries.size()
                >= capacity) {
            entries.removeFirst();
        }

        entries.addLast(
                new ProtocolTraceEntry(
                        nextSequence++,
                        tick,
                        direction,
                        layer == null
                                ? ""
                                : layer,
                        event == null
                                ? ""
                                : event,
                        detail == null
                                ? ""
                                : detail
                )
        );
    }

    public synchronized List<ProtocolTraceEntry> snapshot() {
        return List.copyOf(
                new ArrayList<>(
                        entries
                )
        );
    }

    public synchronized void clear() {
        entries.clear();
    }
}

package com.k1ngtle.vsia.signality.engineering.wifi.trace;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public final class WifiPacketTraceBuffer {
    public static final int DEFAULT_CAPACITY = 128;

    private final int capacity;
    private final Deque<WifiPacketTraceEvent> events =
            new ArrayDeque<>();

    private long sequence;

    public WifiPacketTraceBuffer() {
        this(DEFAULT_CAPACITY);
    }

    public WifiPacketTraceBuffer(int capacity) {
        if (capacity < 8) {
            throw new IllegalArgumentException(
                    "capacity must be >= 8"
            );
        }

        this.capacity = capacity;
    }

    public synchronized WifiPacketTraceEvent append(
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
        WifiPacketTraceEvent event =
                new WifiPacketTraceEvent(
                        sequence++,
                        timestampMicros,
                        direction,
                        frameType,
                        subtype,
                        sourceMac,
                        destinationMac,
                        mcsIndex,
                        phyGeneration,
                        frameBytes,
                        airtimeMicros,
                        rssiDbm,
                        snrDb,
                        sinrDb,
                        retry,
                        detailedPhyPath,
                        outcome,
                        detail
                );

        events.addLast(event);

        while (events.size() > capacity) {
            events.removeFirst();
        }

        return event;
    }

    public synchronized List<WifiPacketTraceEvent> snapshot() {
        return List.copyOf(
                new ArrayList<>(events)
        );
    }

    public synchronized void clear() {
        events.clear();
    }

    public synchronized int size() {
        return events.size();
    }

    public int capacity() {
        return capacity;
    }
}

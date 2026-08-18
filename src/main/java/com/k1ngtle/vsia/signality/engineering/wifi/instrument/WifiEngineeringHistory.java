package com.k1ngtle.vsia.signality.engineering.wifi.instrument;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public final class WifiEngineeringHistory {
    private final int capacity;

    private final Deque<WifiEngineeringSample> samples =
            new ArrayDeque<>();

    private long sequence;

    public WifiEngineeringHistory(
            int capacity
    ) {
        if (capacity < 2) {
            throw new IllegalArgumentException(
                    "capacity must be >= 2"
            );
        }

        this.capacity =
                capacity;
    }

    public void add(
            WifiEngineeringSnapshot snapshot
    ) {
        samples.addLast(
                new WifiEngineeringSample(
                        sequence++,
                        snapshot.snrDb(),
                        snapshot.correctedSinrDb(),
                        snapshot.bitErrorRate(),
                        snapshot.frameErrorRate(),
                        snapshot.mediumEnergyDbm(),
                        snapshot.estimatedPhyRateBps(),
                        snapshot.liveDecoderIterations(),
                        snapshot.liveDelivered()
                )
        );

        while (samples.size() > capacity) {
            samples.removeFirst();
        }
    }

    public void clear() {
        samples.clear();
    }

    public int size() {
        return samples.size();
    }

    public int capacity() {
        return capacity;
    }

    public List<WifiEngineeringSample> samples() {
        return List.copyOf(
                new ArrayList<>(
                        samples
                )
        );
    }
}

package com.k1ngtle.vsia.signality.engineering.reality;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class RfMicroTimingRegistry {
    private static final Map<UUID, RfMicroTiming> TIMINGS =
            new LinkedHashMap<>();

    private static final long RETENTION_US =
            5_000_000L;

    private RfMicroTimingRegistry() {
    }

    public static synchronized void register(
            RfMicroTiming timing
    ) {
        TIMINGS.put(
                timing.transmissionId(),
                timing
        );

        cleanup(
                timing.startMicros()
        );
    }

    public static synchronized RfMicroTiming get(
            UUID transmissionId
    ) {
        return TIMINGS.get(
                transmissionId
        );
    }

    public static synchronized Collection<RfMicroTiming> inDimension(
            String dimensionId,
            long referenceMicros
    ) {
        cleanup(
                referenceMicros
        );

        List<RfMicroTiming> result =
                new ArrayList<>();

        for (RfMicroTiming timing
                : TIMINGS.values()) {
            if (timing.dimensionId()
                    .equals(
                            dimensionId
                    )) {
                result.add(
                        timing
                );
            }
        }

        return List.copyOf(
                result
        );
    }

    public static synchronized void clear() {
        TIMINGS.clear();
    }

    private static void cleanup(
            long referenceMicros
    ) {
        Iterator<RfMicroTiming> iterator =
                TIMINGS.values()
                        .iterator();

        while (iterator.hasNext()) {
            RfMicroTiming timing =
                    iterator.next();

            if (timing.endMicros()
                    < referenceMicros
                    - RETENTION_US) {
                iterator.remove();
            }
        }
    }
}

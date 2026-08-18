package com.k1ngtle.vsia.signality.engineering.wifi;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

public final class WifiMacTimingScheduler {
    public static final long SERVER_TICK_MICROS =
            50_000L;

    private static final Set<WifiMacController> TRACKED =
            Collections.newSetFromMap(
                    new IdentityHashMap<>()
            );

    private static long currentMicros;

    private WifiMacTimingScheduler() {
    }

    public static synchronized long now() {
        return currentMicros;
    }

    public static synchronized long nowMicros() {
        return currentMicros;
    }

    public static synchronized void track(
            WifiMacController controller
    ) {
        TRACKED.add(
                controller
        );
    }

    public static synchronized void untrack(
            WifiMacController controller
    ) {
        TRACKED.remove(
                controller
        );
    }

    public static void tick(
            long serverTick
    ) {
        long endMicros =
                Math.multiplyExact(
                        serverTick,
                        SERVER_TICK_MICROS
                );

        WifiMacController[] snapshot;

        synchronized (WifiMacTimingScheduler.class) {
            currentMicros =
                    endMicros;

            snapshot =
                    TRACKED.toArray(
                            WifiMacController[]::new
                    );
        }

        for (WifiMacController controller : snapshot) {
            boolean stillPending =
                    controller.onTimingTick(
                            endMicros
                    );

            if (!stillPending) {
                untrack(
                        controller
                );
            }
        }
    }

    public static long quantizedResponseDeadlineMicros(
            long nowMicros,
            int protocolTimeoutMicros
    ) {
        long minimum =
                SERVER_TICK_MICROS
                        * 3L;

        return nowMicros
                + Math.max(
                minimum,
                protocolTimeoutMicros
        );
    }

    public static synchronized void clear() {
        TRACKED.clear();
        currentMicros =
                0L;
    }
}

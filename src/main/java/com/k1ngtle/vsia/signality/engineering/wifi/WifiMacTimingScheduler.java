package com.k1ngtle.vsia.signality.engineering.wifi;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

public final class WifiMacTimingScheduler {
    private static final Set<WifiMacController> TRACKED =
            Collections.newSetFromMap(
                    new IdentityHashMap<>()
            );

    private static long currentTick;

    private WifiMacTimingScheduler() {
    }

    public static synchronized long now() {
        return currentTick;
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
            long tick
    ) {
        WifiMacController[] snapshot;

        synchronized (WifiMacTimingScheduler.class) {
            currentTick =
                    tick;

            snapshot =
                    TRACKED.toArray(
                            WifiMacController[]::new
                    );
        }

        for (WifiMacController controller : snapshot) {
            boolean stillPending =
                    controller.onTimingTick(
                            tick
                    );

            if (!stillPending) {
                untrack(
                        controller
                );
            }
        }
    }

    public static synchronized void clear() {
        TRACKED.clear();
        currentTick =
                0L;
    }
}

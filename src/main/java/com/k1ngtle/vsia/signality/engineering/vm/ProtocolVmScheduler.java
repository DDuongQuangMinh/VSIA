package com.k1ngtle.vsia.signality.engineering.vm;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class ProtocolVmScheduler {
    private static final Map<UUID, ProtocolVmController> CONTROLLERS =
            new LinkedHashMap<>();

    private ProtocolVmScheduler() {
    }

    public static synchronized void register(
            UUID id,
            ProtocolVmController controller
    ) {
        CONTROLLERS.put(
                id,
                controller
        );
    }

    public static synchronized void unregister(
            UUID id
    ) {
        CONTROLLERS.remove(
                id
        );
    }

    public static void tickAll() {
        ProtocolVmController[] snapshot;

        synchronized (ProtocolVmScheduler.class) {
            snapshot =
                    CONTROLLERS.values()
                            .toArray(
                                    ProtocolVmController[]::new
                            );
        }

        for (ProtocolVmController controller : snapshot) {
            controller.tick();
        }
    }

    public static synchronized void clear() {
        CONTROLLERS.clear();
    }
}

package com.k1ngtle.vsia.signality.engineering.vm;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class ProtocolVmScheduler {
    private record Entry(
            Object owner,
            ProtocolVmController controller
    ) {
    }

    private static final Map<UUID, Entry> CONTROLLERS =
            new LinkedHashMap<>();

    private ProtocolVmScheduler() {
    }

    public static synchronized void register(
            UUID id,
            ProtocolVmController controller
    ) {
        register(
                id,
                controller,
                controller
        );
    }

    public static synchronized void register(
            UUID id,
            Object owner,
            ProtocolVmController controller
    ) {
        if (id == null
                || owner == null
                || controller == null) {
            return;
        }

        CONTROLLERS.put(
                id,
                new Entry(
                        owner,
                        controller
                )
        );
    }

    public static synchronized void unregister(
            UUID id
    ) {
        if (id != null) {
            CONTROLLERS.remove(id);
        }
    }

    public static synchronized void unregister(
            UUID id,
            Object expectedOwner
    ) {
        if (id == null
                || expectedOwner == null) {
            return;
        }

        Entry current =
                CONTROLLERS.get(id);

        if (current != null
                && current.owner() == expectedOwner) {
            CONTROLLERS.remove(id);
        }
    }

    public static synchronized boolean isRegisteredTo(
            UUID id,
            Object expectedOwner
    ) {
        if (id == null
                || expectedOwner == null) {
            return false;
        }

        Entry current =
                CONTROLLERS.get(id);

        return current != null
                && current.owner() == expectedOwner;
    }

    public static void tickAll() {
        ProtocolVmController[] snapshot;

        synchronized (ProtocolVmScheduler.class) {
            snapshot =
                    CONTROLLERS.values()
                            .stream()
                            .map(Entry::controller)
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

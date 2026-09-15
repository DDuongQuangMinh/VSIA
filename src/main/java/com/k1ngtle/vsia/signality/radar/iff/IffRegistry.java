package com.k1ngtle.vsia.signality.radar.iff;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.jetbrains.annotations.Nullable;

public final class IffRegistry {
    private static final Map<UUID, IIffTransponder> TRANSPONDERS =
            new ConcurrentHashMap<>();

    private IffRegistry() {
    }

    public static void register(IIffTransponder transponder) {
        if (transponder != null) {
            TRANSPONDERS.put(transponder.iffTargetId(), transponder);
        }
    }

    public static void unregister(UUID targetId) {
        if (targetId != null) {
            TRANSPONDERS.remove(targetId);
        }
    }

    @Nullable
    public static IIffTransponder find(UUID targetId) {
        return targetId == null ? null : TRANSPONDERS.get(targetId);
    }

    public static void clear() {
        TRANSPONDERS.clear();
    }
}

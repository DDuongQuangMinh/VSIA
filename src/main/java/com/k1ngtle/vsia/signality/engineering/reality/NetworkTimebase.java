package com.k1ngtle.vsia.signality.engineering.reality;

import net.minecraft.server.level.ServerLevel;

public final class NetworkTimebase {
    public static final long MICROS_PER_SERVER_TICK =
            50_000L;

    private NetworkTimebase() {
    }

    public static long tickStartMicros(
            long gameTick
    ) {
        return Math.multiplyExact(
                gameTick,
                MICROS_PER_SERVER_TICK
        );
    }

    public static long nowMicros(
            ServerLevel level
    ) {
        return tickStartMicros(
                level.getGameTime()
        );
    }

    public static long tickForMicros(
            long micros
    ) {
        if (micros <= 0L) {
            return 0L;
        }

        return micros
                / MICROS_PER_SERVER_TICK;
    }
}

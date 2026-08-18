package com.k1ngtle.vsia.signality.engineering.wifi.tcp.live;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class TcpLiveScheduler {
    private static final Map<UUID, Runnable> TICKERS =
            new ConcurrentHashMap<>();

    private TcpLiveScheduler() {
    }

    public static void register(
            UUID id,
            Runnable ticker
    ) {
        if (id == null
                || ticker == null) {
            return;
        }

        TICKERS.put(
                id,
                ticker
        );
    }

    public static void unregister(
            UUID id
    ) {
        if (id != null) {
            TICKERS.remove(
                    id
            );
        }
    }

    public static void tickAll() {
        for (Runnable ticker
                : TICKERS.values()) {
            ticker.run();
        }
    }

    public static void clear() {
        TICKERS.clear();
    }
}

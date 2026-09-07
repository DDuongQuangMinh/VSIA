package com.k1ngtle.vsia.signality.engineering.wifi.tcp.live;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class TcpLiveScheduler {
    private record Entry(
            Object owner,
            Runnable ticker
    ) {
    }

    private static final Map<UUID, Entry> TICKERS =
            new ConcurrentHashMap<>();

    private TcpLiveScheduler() {
    }

    public static void register(
            UUID id,
            Runnable ticker
    ) {
        register(
                id,
                ticker,
                ticker
        );
    }

    public static void register(
            UUID id,
            Object owner,
            Runnable ticker
    ) {
        if (id == null
                || owner == null
                || ticker == null) {
            return;
        }

        TICKERS.put(
                id,
                new Entry(
                        owner,
                        ticker
                )
        );
    }

    public static void unregister(
            UUID id
    ) {
        if (id != null) {
            TICKERS.remove(id);
        }
    }

    public static void unregister(
            UUID id,
            Object expectedOwner
    ) {
        if (id == null
                || expectedOwner == null) {
            return;
        }

        Entry current =
                TICKERS.get(id);

        if (current != null
                && current.owner() == expectedOwner) {
            TICKERS.remove(
                    id,
                    current
            );
        }
    }

    public static boolean isRegisteredTo(
            UUID id,
            Object expectedOwner
    ) {
        if (id == null
                || expectedOwner == null) {
            return false;
        }

        Entry current =
                TICKERS.get(id);

        return current != null
                && current.owner() == expectedOwner;
    }

    public static void tickAll() {
        Entry[] snapshot =
                TICKERS.values()
                        .toArray(
                                Entry[]::new
                        );

        for (Entry entry : snapshot) {
            entry.ticker().run();
        }
    }

    public static void clear() {
        TICKERS.clear();
    }
}

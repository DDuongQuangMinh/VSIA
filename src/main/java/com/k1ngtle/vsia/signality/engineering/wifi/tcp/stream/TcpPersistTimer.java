package com.k1ngtle.vsia.signality.engineering.wifi.tcp.stream;

public final class TcpPersistTimer {
    private static final long INITIAL_MICROS =
            1_000_000L;

    private static final long MAX_MICROS =
            60_000_000L;

    private boolean armed;

    private long deadlineMicros;

    private long intervalMicros =
            INITIAL_MICROS;

    public void observeWindow(
            int advertisedWindow,
            long nowMicros
    ) {
        if (advertisedWindow > 0) {
            armed =
                    false;

            intervalMicros =
                    INITIAL_MICROS;

            return;
        }

        if (!armed) {
            armed =
                    true;

            deadlineMicros =
                    nowMicros
                            + intervalMicros;
        }
    }

    public boolean shouldProbe(
            long nowMicros
    ) {
        return armed
                && nowMicros
                >= deadlineMicros;
    }

    public void onProbeSent(
            long nowMicros
    ) {
        if (!armed) {
            return;
        }

        intervalMicros =
                Math.min(
                        MAX_MICROS,
                        intervalMicros
                                * 2L
                );

        deadlineMicros =
                nowMicros
                        + intervalMicros;
    }

    public boolean armed() {
        return armed;
    }

    public long intervalMicros() {
        return intervalMicros;
    }
}

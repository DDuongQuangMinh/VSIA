package com.k1ngtle.vsia.signality.engineering.wifi;

public final class WifiNavState {
    private long navUntilMicros;

    public void observe(
            long nowMicros,
            int durationMicroseconds
    ) {
        if (durationMicroseconds <= 0) {
            return;
        }

        navUntilMicros =
                Math.max(
                        navUntilMicros,
                        nowMicros
                                + durationMicroseconds
                );
    }

    public boolean active(
            long nowMicros
    ) {
        return nowMicros
                < navUntilMicros;
    }

    public long remainingMicros(
            long nowMicros
    ) {
        return Math.max(
                0L,
                navUntilMicros
                        - nowMicros
        );
    }

    public void clear() {
        navUntilMicros =
                0L;
    }
}

package com.k1ngtle.vsia.signality.engineering.wifi.integration.w127;

public final class W127ResiliencePolicy {
    public static final long DNS_BASE_TIMEOUT_TICKS = 60L;
    public static final long DNS_MAX_TIMEOUT_TICKS = 240L;
    public static final long LIVE_STAGE_TIMEOUT_TICKS = 360L;
    public static final long TRANSFER_INCOMPLETE_OBSERVE_TICKS = 100L;
    public static final long CLEAN_STABILITY_TICKS = 80L;
    public static final int MAX_DELAYED_PACKETS = 256;

    private W127ResiliencePolicy() {
    }

    public static long dnsTimeoutTicks(int retryIndex) {
        int shift = Math.max(0, Math.min(2, retryIndex));
        long value = DNS_BASE_TIMEOUT_TICKS << shift;
        return Math.min(DNS_MAX_TIMEOUT_TICKS, value);
    }

    public static long boundedDelay(long requestedTicks) {
        return Math.max(1L, Math.min(200L, requestedTicks));
    }
}

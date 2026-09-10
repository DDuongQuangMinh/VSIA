package com.k1ngtle.vsia.signality.engineering.wifi.integration.w127;

public record W127FaultSnapshot(
        int activeRules,
        int delayedQueued,
        long matched,
        long dropped,
        long duplicated,
        long delayed,
        long delayedDelivered,
        long mutated,
        String lastEvent
) {
    public String compact() {
        return "rules="
                + activeRules
                + " queued="
                + delayedQueued
                + " match="
                + matched
                + " drop="
                + dropped
                + " dup="
                + duplicated
                + " delay="
                + delayed
                + "/"
                + delayedDelivered
                + " mutate="
                + mutated
                + " last="
                + lastEvent;
    }
}

package com.k1ngtle.vsia.signality.engineering.cellular;

public record CellularQosClass(
        int fiveQi,
        String name,
        int priority,
        int packetDelayBudgetMillis,
        double packetErrorRate,
        double schedulerWeight
) {
    public CellularQosClass {
        fiveQi = Math.max(1, fiveQi);
        name = name == null ? "" : name;
        priority = Math.max(1, priority);
        packetDelayBudgetMillis = Math.max(1, packetDelayBudgetMillis);
        packetErrorRate = Math.max(0.0, Math.min(1.0, packetErrorRate));
        schedulerWeight = Math.max(0.01, schedulerWeight);
    }
}

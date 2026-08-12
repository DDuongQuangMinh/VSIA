package com.k1ngtle.vsia.weapon.data;

public record ReloadDefinition(int tacticalTicks, int emptyTicks, boolean detachableMagazine) {
    public ReloadDefinition {
        if (tacticalTicks < 0 || emptyTicks < 0) throw new IllegalArgumentException("reload times must be non-negative");
    }
}

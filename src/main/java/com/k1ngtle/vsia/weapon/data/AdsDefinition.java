package com.k1ngtle.vsia.weapon.data;

public record AdsDefinition(int aimTicks, float fovMultiplier, float movementMultiplier) {
    public AdsDefinition {
        if (aimTicks < 0) throw new IllegalArgumentException("aimTicks must be non-negative");
        if (fovMultiplier <= 0 || movementMultiplier < 0) throw new IllegalArgumentException("ADS multipliers are invalid");
    }
}

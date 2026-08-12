package com.k1ngtle.vsia.weapon.data;

public record BallisticsDefinition(
        float damage,
        float range,
        float minimumDamageMultiplier,
        float spreadDegrees,
        float headshotMultiplier,
        int entityPenetration,
        float penetrationDamageMultiplier) {
    public BallisticsDefinition {
        if (damage < 0 || range <= 0 || minimumDamageMultiplier < 0 || minimumDamageMultiplier > 1
                || spreadDegrees < 0 || headshotMultiplier < 1 || entityPenetration < 0
                || penetrationDamageMultiplier < 0 || penetrationDamageMultiplier > 1) {
            throw new IllegalArgumentException("Invalid ballistics values");
        }
    }
}

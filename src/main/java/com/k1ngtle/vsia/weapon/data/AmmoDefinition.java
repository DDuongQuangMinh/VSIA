package com.k1ngtle.vsia.weapon.data;

import net.minecraft.resources.ResourceLocation;

public record AmmoDefinition(ResourceLocation ammoId, int magazineCapacity, boolean chambered) {
    public AmmoDefinition {
        if (magazineCapacity < 0) throw new IllegalArgumentException("magazineCapacity must be non-negative");
    }
}

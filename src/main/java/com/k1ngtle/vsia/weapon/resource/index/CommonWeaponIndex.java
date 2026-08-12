package com.k1ngtle.vsia.weapon.resource.index;

import com.k1ngtle.vsia.weapon.data.WeaponDefinition;
import net.minecraft.resources.ResourceLocation;

public record CommonWeaponIndex(ResourceLocation id, WeaponDefinition definition) {
    public CommonWeaponIndex {
        if (!id.equals(definition.id())) throw new IllegalArgumentException("index and definition IDs differ");
    }
}

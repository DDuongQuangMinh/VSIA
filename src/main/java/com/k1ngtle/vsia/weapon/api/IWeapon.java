package com.k1ngtle.vsia.weapon.api;

import com.k1ngtle.vsia.weapon.data.WeaponDefinition;
import com.k1ngtle.vsia.weapon.state.WeaponRuntimeState;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public interface IWeapon {
    Optional<ResourceLocation> getWeaponId(ItemStack stack);

    void setWeaponId(ItemStack stack, ResourceLocation id);

    default Optional<WeaponDefinition> getDefinition(ItemStack stack) {
        return getWeaponId(stack).flatMap(VSIAWeaponAPI::getWeapon);
    }

    default WeaponRuntimeState getRuntimeState(ItemStack stack) {
        return WeaponRuntimeState.get(stack);
    }
}

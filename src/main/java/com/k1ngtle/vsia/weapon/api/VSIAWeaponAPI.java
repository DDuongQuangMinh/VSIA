package com.k1ngtle.vsia.weapon.api;

import com.k1ngtle.vsia.weapon.data.WeaponDefinition;
import com.k1ngtle.vsia.weapon.resource.CommonWeaponManager;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public final class VSIAWeaponAPI {
    private VSIAWeaponAPI() {}

    public static Optional<WeaponDefinition> getWeapon(ResourceLocation id) {
        return CommonWeaponManager.getInstance().get(id);
    }

    public static Optional<WeaponDefinition> getWeapon(ItemStack stack) {
        if (stack.getItem() instanceof IWeapon weapon) {
            return weapon.getWeaponId(stack).flatMap(VSIAWeaponAPI::getWeapon);
        }
        return Optional.empty();
    }

    public static boolean isWeapon(ItemStack stack) {
        return stack.getItem() instanceof IWeapon weapon && weapon.getWeaponId(stack).isPresent();
    }
}

package com.k1ngtle.vsia.weapon.client.render;

import com.k1ngtle.vsia.weapon.api.IWeapon;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public final class WeaponAssetResolver {
    private static final ResourceLocation FALLBACK = new ResourceLocation("vsia", "m4a1");
    private WeaponAssetResolver() {}

    public static ResourceLocation weaponId(ItemStack stack) {
        if (stack.getItem() instanceof IWeapon weapon) return weapon.getWeaponId(stack).orElse(FALLBACK);
        return FALLBACK;
    }

    public static ResourceLocation model(ItemStack stack) {
        ResourceLocation id = weaponId(stack);
        return new ResourceLocation(id.getNamespace(), "geo/weapons/" + id.getPath() + ".geo.json");
    }

    public static ResourceLocation texture(ItemStack stack) {
        ResourceLocation id = weaponId(stack);
        ResourceLocation candidate = new ResourceLocation(id.getNamespace(),
                "textures/weapons/" + id.getPath() + ".png");
        return net.minecraft.client.Minecraft.getInstance().getResourceManager().getResource(candidate).isPresent()
                ? candidate : new ResourceLocation("minecraft", "textures/item/iron_hoe.png");
    }

    public static ResourceLocation animation(ItemStack stack) {
        ResourceLocation id = weaponId(stack);
        return new ResourceLocation(id.getNamespace(), "animations/weapons/" + id.getPath() + ".animation.json");
    }
}

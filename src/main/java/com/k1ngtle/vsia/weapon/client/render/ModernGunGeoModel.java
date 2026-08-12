package com.k1ngtle.vsia.weapon.client.render;

import com.k1ngtle.vsia.weapon.item.ModernGunItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class ModernGunGeoModel extends GeoModel<ModernGunItem> {
    @Override public ResourceLocation getModelResource(ModernGunItem item) {
        return WeaponAssetResolver.model(WeaponRenderContext.stack());
    }
    @Override public ResourceLocation getTextureResource(ModernGunItem item) {
        return WeaponAssetResolver.texture(WeaponRenderContext.stack());
    }
    @Override public ResourceLocation getAnimationResource(ModernGunItem item) {
        return WeaponAssetResolver.animation(WeaponRenderContext.stack());
    }
}

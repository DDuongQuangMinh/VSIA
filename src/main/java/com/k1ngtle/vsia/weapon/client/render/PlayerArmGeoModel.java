package com.k1ngtle.vsia.weapon.client.render;

import com.k1ngtle.vsia.weapon.item.ModernGunItem;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

final class PlayerArmGeoModel extends GeoModel<ModernGunItem> {
    private static final ResourceLocation MODEL = new ResourceLocation("vsia", "geo/weapons/ak74m_arms.geo.json");
    private static final ResourceLocation ANIMATION = new ResourceLocation("vsia", "animations/weapons/ak74m.animation.json");

    @Override public ResourceLocation getModelResource(ModernGunItem item) { return MODEL; }
    @Override public ResourceLocation getAnimationResource(ModernGunItem item) { return ANIMATION; }

    @Override
    public ResourceLocation getTextureResource(ModernGunItem item) {
        if (Minecraft.getInstance().player != null) return Minecraft.getInstance().player.getSkinTextureLocation();
        return new ResourceLocation("minecraft", "textures/entity/steve.png");
    }
}

package com.k1ngtle.vsia.weapon.client;

import com.k1ngtle.vsia.weapon.GunItem;
import com.k1ngtle.vsia.weapon.registry.WeaponItems;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/**
 * Resolves model/texture/animation files by the same naming convention
 * you already use elsewhere: assets/vsia/geo/item/<gunName>.geo.json,
 * textures/item/<gunName>.png, animations/item/<gunName>.animation.json.
 * One model class covers every gun - no need for a new class per weapon.
 */
public class GunGeoModel extends GeoModel<GunItem> {

    @Override
    public ResourceLocation getModelResource(GunItem gun) {
        return new ResourceLocation(WeaponItems.MODID, "geo/item/" + gun.getGunName() + ".geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(GunItem gun) {
        return new ResourceLocation(WeaponItems.MODID, "textures/item/" + gun.getGunName() + ".png");
    }

    @Override
    public ResourceLocation getAnimationResource(GunItem gun) {
        return new ResourceLocation(WeaponItems.MODID, "animations/item/" + gun.getGunName() + ".animation.json");
    }
}
package com.k1ngtle.vsia.client.model;

import com.k1ngtle.vsia.Vsia;
import com.k1ngtle.vsia.item.HelmetGPNVG18Item;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class HelmetGPNVG18Model extends GeoModel<HelmetGPNVG18Item> {

    @Override
    public ResourceLocation getModelResource(HelmetGPNVG18Item object) {
        return new ResourceLocation(Vsia.MOD_ID, "geo/armor/helmet_gpnvg18.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(HelmetGPNVG18Item object) {
        return new ResourceLocation(Vsia.MOD_ID, "textures/armor/helmet_gpnvg18.png");
    }

    @Override
    public ResourceLocation getAnimationResource(HelmetGPNVG18Item animatable) {
        return new ResourceLocation(Vsia.MOD_ID, "animations/armor/helmet_gpnvg18.animation.json");
    }
}
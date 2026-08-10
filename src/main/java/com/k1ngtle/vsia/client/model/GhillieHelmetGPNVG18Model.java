package com.k1ngtle.vsia.client.model;

import com.k1ngtle.vsia.Vsia;
import com.k1ngtle.vsia.item.GhillieHelmetGPNVG18Item;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class GhillieHelmetGPNVG18Model extends GeoModel<GhillieHelmetGPNVG18Item> {

    @Override
    public ResourceLocation getModelResource(GhillieHelmetGPNVG18Item object) {
        return new ResourceLocation(Vsia.MOD_ID, "geo/armor/helmet_gpnvg18_ghillie.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(GhillieHelmetGPNVG18Item object) {
        return new ResourceLocation(Vsia.MOD_ID, "textures/armor/helmet_gpnvg18_ghillie.png");
    }

    @Override
    public ResourceLocation getAnimationResource(GhillieHelmetGPNVG18Item animatable) {
        // Shared animation file
        return new ResourceLocation(Vsia.MOD_ID, "animations/armor/helmet_gpnvg18.animation.json");
    }
}
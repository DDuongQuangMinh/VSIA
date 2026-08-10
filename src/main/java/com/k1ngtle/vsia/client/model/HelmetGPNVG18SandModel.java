package com.k1ngtle.vsia.client.model;

import com.k1ngtle.vsia.Vsia;
import com.k1ngtle.vsia.item.HelmetGPNVG18SandItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class HelmetGPNVG18SandModel extends GeoModel<HelmetGPNVG18SandItem> {

    @Override
    public ResourceLocation getModelResource(HelmetGPNVG18SandItem object) {
        return new ResourceLocation(Vsia.MOD_ID, "geo/armor/helmet_gpnvg18_sand.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(HelmetGPNVG18SandItem object) {
        return new ResourceLocation(Vsia.MOD_ID, "textures/armor/helmet_gpnvg18_sand.png");
    }

    @Override
    public ResourceLocation getAnimationResource(HelmetGPNVG18SandItem animatable) {
        // Shared animation file for GPNVG-18 helmets
        return new ResourceLocation(Vsia.MOD_ID, "animations/armor/helmet_gpnvg18.animation.json");
    }
}
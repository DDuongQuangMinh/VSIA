package com.k1ngtle.vsia.client.model;

import com.k1ngtle.vsia.Vsia;
import com.k1ngtle.vsia.item.HelmetGPNVG18SnowItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class HelmetGPNVG18SnowModel extends GeoModel<HelmetGPNVG18SnowItem> {

    @Override
    public ResourceLocation getModelResource(HelmetGPNVG18SnowItem object) {
        return new ResourceLocation(Vsia.MOD_ID, "geo/armor/helmet_gpnvg18_snow.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(HelmetGPNVG18SnowItem object) {
        return new ResourceLocation(Vsia.MOD_ID, "textures/armor/helmet_gpnvg18_snow.png");
    }

    @Override
    public ResourceLocation getAnimationResource(HelmetGPNVG18SnowItem animatable) {
        // Shared animation file for GPNVG-18 helmets
        return new ResourceLocation(Vsia.MOD_ID, "animations/armor/helmet_gpnvg18.animation.json");
    }
}
package com.k1ngtle.vsia.client.model;

import com.k1ngtle.vsia.Vsia;
import com.k1ngtle.vsia.item.SnowHelmetPVS31Item;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class SnowHelmetPVS31Model extends GeoModel<SnowHelmetPVS31Item> {

    @Override
    public ResourceLocation getModelResource(SnowHelmetPVS31Item object) {
        return new ResourceLocation(Vsia.MOD_ID, "geo/armor/helmet_pvs31_snow.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(SnowHelmetPVS31Item object) {
        return new ResourceLocation(Vsia.MOD_ID, "textures/armor/helmet_pvs31_snow.png");
    }

    @Override
    public ResourceLocation getAnimationResource(SnowHelmetPVS31Item animatable) {
        // Shared animation file for all PVS-31 helmets
        return new ResourceLocation(Vsia.MOD_ID, "animations/armor/helmet_pvs31.animation.json");
    }
}
package com.k1ngtle.vsia.client.model;

import com.k1ngtle.vsia.Vsia;
import com.k1ngtle.vsia.item.HelmetPVS31Item;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class HelmetPVS31Model extends GeoModel<HelmetPVS31Item> {

    @Override
    public ResourceLocation getModelResource(HelmetPVS31Item object) {
        return new ResourceLocation(Vsia.MOD_ID, "geo/armor/helmet_pvs31.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(HelmetPVS31Item object) {
        return new ResourceLocation(Vsia.MOD_ID, "textures/armor/helmet_pvs31.png");
    }

    @Override
    public ResourceLocation getAnimationResource(HelmetPVS31Item animatable) {
        return new ResourceLocation(Vsia.MOD_ID, "animations/armor/helmet_pvs31.animation.json");
    }
}
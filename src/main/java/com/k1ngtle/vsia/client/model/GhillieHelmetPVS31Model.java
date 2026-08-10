package com.k1ngtle.vsia.client.model;

import com.k1ngtle.vsia.Vsia;
import com.k1ngtle.vsia.item.GhillieHelmetPVS31Item;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class GhillieHelmetPVS31Model extends GeoModel<GhillieHelmetPVS31Item> {

    @Override
    public ResourceLocation getModelResource(GhillieHelmetPVS31Item object) {
        return new ResourceLocation(Vsia.MOD_ID, "geo/armor/helmet_pvs31_ghillie.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(GhillieHelmetPVS31Item object) {
        return new ResourceLocation(Vsia.MOD_ID, "textures/armor/helmet_pvs31_ghillie.png");
    }

    @Override
    public ResourceLocation getAnimationResource(GhillieHelmetPVS31Item animatable) {
        return new ResourceLocation(Vsia.MOD_ID, "animations/armor/helmet_pvs31.animation.json");
    }
}
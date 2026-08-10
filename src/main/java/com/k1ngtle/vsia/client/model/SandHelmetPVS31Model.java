package com.k1ngtle.vsia.client.model;

import com.k1ngtle.vsia.Vsia;
import com.k1ngtle.vsia.item.SandHelmetPVS31Item;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class SandHelmetPVS31Model extends GeoModel<SandHelmetPVS31Item> {

    @Override
    public ResourceLocation getModelResource(SandHelmetPVS31Item object) {
        return new ResourceLocation(Vsia.MOD_ID, "geo/armor/helmet_pvs31_sand.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(SandHelmetPVS31Item object) {
        return new ResourceLocation(Vsia.MOD_ID, "textures/armor/helmet_pvs31_sand.png");
    }

    @Override
    public ResourceLocation getAnimationResource(SandHelmetPVS31Item animatable) {
        // Points to the exact same shared animation file as the standard PVS-31
        return new ResourceLocation(Vsia.MOD_ID, "animations/armor/helmet_pvs31.animation.json");
    }
}
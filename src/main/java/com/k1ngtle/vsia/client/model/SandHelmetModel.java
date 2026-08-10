package com.k1ngtle.vsia.client.model;

import com.k1ngtle.vsia.Vsia;
import com.k1ngtle.vsia.item.SandHelmetItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class SandHelmetModel extends GeoModel<SandHelmetItem> {

    @Override
    public ResourceLocation getModelResource(SandHelmetItem object) {
        return new ResourceLocation(Vsia.MOD_ID, "geo/armor/helmet_sand.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(SandHelmetItem object) {
        // Points to the helmet_sand.png image
        return new ResourceLocation(Vsia.MOD_ID, "textures/armor/helmet_sand.png");
    }

    @Override
    public ResourceLocation getAnimationResource(SandHelmetItem animatable) {
        return null;
    }
}
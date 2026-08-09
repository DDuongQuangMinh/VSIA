package com.k1ngtle.vsia.client.model;

import com.k1ngtle.vsia.Vsia;
import com.k1ngtle.vsia.item.HelmetItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class HelmetModel extends GeoModel<HelmetItem> {

    @Override
    public ResourceLocation getModelResource(HelmetItem object) {
        return new ResourceLocation(Vsia.MOD_ID, "geo/armor/base_helmet.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(HelmetItem object) {
        return new ResourceLocation(Vsia.MOD_ID, "textures/armor/helmet.png");
    }

    @Override
    public ResourceLocation getAnimationResource(HelmetItem animatable) {
        // No animations needed
        return null;
    }
}
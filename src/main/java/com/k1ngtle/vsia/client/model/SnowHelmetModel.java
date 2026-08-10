package com.k1ngtle.vsia.client.model;

import com.k1ngtle.vsia.Vsia;
import com.k1ngtle.vsia.item.SnowHelmetItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class SnowHelmetModel extends GeoModel<SnowHelmetItem> {

    @Override
    public ResourceLocation getModelResource(SnowHelmetItem object) {
        return new ResourceLocation(Vsia.MOD_ID, "geo/armor/helmet_snow.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(SnowHelmetItem object) {
        // Points to the helmet_snow.png image
        return new ResourceLocation(Vsia.MOD_ID, "textures/armor/helmet_snow.png");
    }

    @Override
    public ResourceLocation getAnimationResource(SnowHelmetItem animatable) {
        return null;
    }
}
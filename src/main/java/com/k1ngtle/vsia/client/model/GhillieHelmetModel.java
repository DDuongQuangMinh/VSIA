package com.k1ngtle.vsia.client.model;

import com.k1ngtle.vsia.Vsia;
import com.k1ngtle.vsia.item.GhillieHelmetItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class GhillieHelmetModel extends GeoModel<GhillieHelmetItem> {

    @Override
    public ResourceLocation getModelResource(GhillieHelmetItem object) {
        return new ResourceLocation(Vsia.MOD_ID, "geo/armor/helmet_ghillie.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(GhillieHelmetItem object) {
        // Points to the helmet_ghillie.png image
        return new ResourceLocation(Vsia.MOD_ID, "textures/armor/helmet_ghillie.png");
    }

    @Override
    public ResourceLocation getAnimationResource(GhillieHelmetItem animatable) {
        return null;
    }
}
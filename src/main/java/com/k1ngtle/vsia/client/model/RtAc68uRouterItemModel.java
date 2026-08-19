package com.k1ngtle.vsia.client.model;

import com.k1ngtle.vsia.Vsia;
import com.k1ngtle.vsia.signality.internet.router.RtAc68uRouterItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class RtAc68uRouterItemModel
        extends GeoModel<RtAc68uRouterItem> {

    @Override
    public ResourceLocation getModelResource(
            RtAc68uRouterItem object
    ) {
        return new ResourceLocation(
                Vsia.MOD_ID,
                "geo/block/rt_ac68u_router.geo.json"
        );
    }

    @Override
    public ResourceLocation getTextureResource(
            RtAc68uRouterItem object
    ) {
        return new ResourceLocation(
                Vsia.MOD_ID,
                "textures/block/rt_ac68u_router.png"
        );
    }

    @Override
    public ResourceLocation getAnimationResource(
            RtAc68uRouterItem object
    ) {
        return new ResourceLocation(
                Vsia.MOD_ID,
                "animations/block/rt_ac68u_router.animation.json"
        );
    }
}
package com.k1ngtle.vsia.client.model;

import com.k1ngtle.vsia.Vsia;
import com.k1ngtle.vsia.signality.internet.router.RtAc68uRouterBlockEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class RtAc68uRouterModel
        extends GeoModel<RtAc68uRouterBlockEntity> {

    @Override
    public ResourceLocation getModelResource(
            RtAc68uRouterBlockEntity object
    ) {
        return new ResourceLocation(
                Vsia.MOD_ID,
                "geo/block/rt_ac68u_router.geo.json"
        );
    }

    @Override
    public ResourceLocation getTextureResource(
            RtAc68uRouterBlockEntity object
    ) {
        return new ResourceLocation(
                Vsia.MOD_ID,
                "textures/block/rt_ac68u_router.png"
        );
    }

    @Override
    public ResourceLocation getAnimationResource(
            RtAc68uRouterBlockEntity object
    ) {
        return new ResourceLocation(
                Vsia.MOD_ID,
                "animations/block/rt_ac68u_router.animation.json"
        );
    }
}

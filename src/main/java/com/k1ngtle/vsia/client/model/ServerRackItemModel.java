package com.k1ngtle.vsia.client.model;

import com.k1ngtle.vsia.Vsia;
import com.k1ngtle.vsia.signality.internet.server.ServerRackItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class ServerRackItemModel
        extends GeoModel<ServerRackItem> {

    @Override
    public ResourceLocation getModelResource(
            ServerRackItem animatable
    ) {
        return new ResourceLocation(
                Vsia.MOD_ID,
                "geo/block/server_rack.geo.json"
        );
    }

    @Override
    public ResourceLocation getTextureResource(
            ServerRackItem animatable
    ) {
        return new ResourceLocation(
                Vsia.MOD_ID,
                "textures/block/server_rack.png"
        );
    }

    @Override
    public ResourceLocation getAnimationResource(
            ServerRackItem animatable
    ) {
        return new ResourceLocation(
                Vsia.MOD_ID,
                "animations/block/server_rack.animation.json"
        );
    }
}
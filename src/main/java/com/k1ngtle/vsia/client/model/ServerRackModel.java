package com.k1ngtle.vsia.client.model;

import com.k1ngtle.vsia.Vsia;
import com.k1ngtle.vsia.signality.internet.server.ServerRackBlockEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class ServerRackModel extends GeoModel<ServerRackBlockEntity> {
    @Override
    public ResourceLocation getModelResource(ServerRackBlockEntity animatable) {
        return new ResourceLocation(Vsia.MOD_ID, "geo/block/server_rack.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(ServerRackBlockEntity animatable) {
        return new ResourceLocation(Vsia.MOD_ID, "textures/block/server_rack.png");
    }

    @Override
    public ResourceLocation getAnimationResource(ServerRackBlockEntity animatable) {
        return new ResourceLocation(Vsia.MOD_ID, "animations/block/server_rack.animation.json");
    }
}

package com.k1ngtle.vsia.client.model;

import com.k1ngtle.vsia.Vsia;
import com.k1ngtle.vsia.signality.internet.server.NetworkSwitchBlockEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class NetworkSwitchModel extends GeoModel<NetworkSwitchBlockEntity> {

    @Override
    public ResourceLocation getModelResource(NetworkSwitchBlockEntity object) {
        return new ResourceLocation(Vsia.MOD_ID, "geo/block/network_switch.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(NetworkSwitchBlockEntity object) {
        return new ResourceLocation(Vsia.MOD_ID, "textures/block/network_switch.png");
    }

    @Override
    public ResourceLocation getAnimationResource(NetworkSwitchBlockEntity object) {
        return new ResourceLocation(Vsia.MOD_ID, "animations/block/network_switch.animation.json");
    }
}
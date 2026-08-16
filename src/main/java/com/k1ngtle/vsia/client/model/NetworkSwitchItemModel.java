package com.k1ngtle.vsia.client.model;

import com.k1ngtle.vsia.Vsia;
import com.k1ngtle.vsia.signality.internet.server.NetworkSwitchItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class NetworkSwitchItemModel extends GeoModel<NetworkSwitchItem> {

    @Override
    public ResourceLocation getModelResource(NetworkSwitchItem animatable) {
        return new ResourceLocation(Vsia.MOD_ID, "geo/block/network_switch.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(NetworkSwitchItem animatable) {
        return new ResourceLocation(Vsia.MOD_ID, "textures/block/network_switch.png");
    }

    @Override
    public ResourceLocation getAnimationResource(NetworkSwitchItem animatable) {
        return new ResourceLocation(Vsia.MOD_ID, "animations/block/network_switch.animation.json");
    }
}
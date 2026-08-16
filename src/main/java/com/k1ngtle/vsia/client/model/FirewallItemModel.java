package com.k1ngtle.vsia.client.model;

import com.k1ngtle.vsia.Vsia;
import com.k1ngtle.vsia.signality.internet.server.FirewallItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class FirewallItemModel extends GeoModel<FirewallItem> {

    @Override
    public ResourceLocation getModelResource(FirewallItem animatable) {
        return new ResourceLocation(Vsia.MOD_ID, "geo/block/firewall.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(FirewallItem animatable) {
        return new ResourceLocation(Vsia.MOD_ID, "textures/block/firewall.png");
    }

    @Override
    public ResourceLocation getAnimationResource(FirewallItem animatable) {
        return new ResourceLocation(Vsia.MOD_ID, "animations/block/firewall.animation.json");
    }
}
package com.k1ngtle.vsia.client.model;

import com.k1ngtle.vsia.Vsia;
import com.k1ngtle.vsia.signality.internet.server.FirewallBlockEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class FirewallModel extends GeoModel<FirewallBlockEntity> {

    @Override
    public ResourceLocation getModelResource(FirewallBlockEntity object) {
        return new ResourceLocation(Vsia.MOD_ID, "geo/block/firewall.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(FirewallBlockEntity object) {
        return new ResourceLocation(Vsia.MOD_ID, "textures/block/firewall.png");
    }

    @Override
    public ResourceLocation getAnimationResource(FirewallBlockEntity object) {
        return new ResourceLocation(Vsia.MOD_ID, "animations/block/firewall.animation.json");
    }
}
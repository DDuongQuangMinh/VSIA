package com.k1ngtle.vsia.client.model;

import com.k1ngtle.vsia.Vsia;
import com.k1ngtle.vsia.signality.internet.server.StorageServerItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class StorageServerItemModel extends GeoModel<StorageServerItem> {

    @Override
    public ResourceLocation getModelResource(StorageServerItem animatable) {
        return new ResourceLocation(Vsia.MOD_ID, "geo/block/storage_server.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(StorageServerItem animatable) {
        return new ResourceLocation(Vsia.MOD_ID, "textures/block/storage_server.png");
    }

    @Override
    public ResourceLocation getAnimationResource(StorageServerItem animatable) {
        return new ResourceLocation(Vsia.MOD_ID, "animations/block/storage_server.animation.json");
    }
}
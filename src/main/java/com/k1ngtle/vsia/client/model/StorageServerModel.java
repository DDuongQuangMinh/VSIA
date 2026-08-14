package com.k1ngtle.vsia.client.model;

import com.k1ngtle.vsia.Vsia;
import com.k1ngtle.vsia.signality.internet.server.StorageServerBlockEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class StorageServerModel extends GeoModel<StorageServerBlockEntity> {

    @Override
    public ResourceLocation getModelResource(StorageServerBlockEntity object) {
        return new ResourceLocation(Vsia.MOD_ID, "geo/block/storage_server.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(StorageServerBlockEntity object) {
        return new ResourceLocation(Vsia.MOD_ID, "textures/block/storage_server.png");
    }

    @Override
    public ResourceLocation getAnimationResource(StorageServerBlockEntity object) {
        return new ResourceLocation(Vsia.MOD_ID, "animations/block/storage_server.animation.json");
    }
}
package com.k1ngtle.vsia.client.renderer;

import com.k1ngtle.vsia.client.model.StorageServerModel;
import com.k1ngtle.vsia.signality.internet.server.StorageServerBlockEntity;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public final class StorageServerRenderer extends GeoBlockRenderer<StorageServerBlockEntity> {
    public StorageServerRenderer() {
        super(new StorageServerModel());
    }
}
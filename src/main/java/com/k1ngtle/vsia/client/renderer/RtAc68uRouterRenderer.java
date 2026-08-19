package com.k1ngtle.vsia.client.renderer;

import com.k1ngtle.vsia.client.model.RtAc68uRouterModel;
import com.k1ngtle.vsia.signality.internet.router.RtAc68uRouterBlockEntity;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public final class RtAc68uRouterRenderer
        extends GeoBlockRenderer<RtAc68uRouterBlockEntity> {

    public RtAc68uRouterRenderer() {
        super(
                new RtAc68uRouterModel()
        );
    }
}

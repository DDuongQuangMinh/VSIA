package com.k1ngtle.vsia.client.renderer;

import com.k1ngtle.vsia.client.model.RtAc68uRouterItemModel;
import com.k1ngtle.vsia.signality.internet.router.RtAc68uRouterItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public final class RtAc68uRouterItemRenderer
        extends GeoItemRenderer<RtAc68uRouterItem> {

    public RtAc68uRouterItemRenderer() {
        super(
                new RtAc68uRouterItemModel()
        );
    }
}
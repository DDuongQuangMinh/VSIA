package com.k1ngtle.vsia.client.renderer;

import com.k1ngtle.vsia.client.model.GhillieHelmetModel;
import com.k1ngtle.vsia.item.GhillieHelmetItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class GhillieHelmetItemRenderer extends GeoItemRenderer<GhillieHelmetItem> {
    public GhillieHelmetItemRenderer() {
        super(new GhillieHelmetModel());
    }
}
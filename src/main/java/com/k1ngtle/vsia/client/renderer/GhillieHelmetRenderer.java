package com.k1ngtle.vsia.client.renderer;

import com.k1ngtle.vsia.client.model.GhillieHelmetModel;
import com.k1ngtle.vsia.item.GhillieHelmetItem;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

public class GhillieHelmetRenderer extends GeoArmorRenderer<GhillieHelmetItem> {
    public GhillieHelmetRenderer() {
        super(new GhillieHelmetModel());
    }
}
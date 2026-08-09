package com.k1ngtle.vsia.client.renderer;

import com.k1ngtle.vsia.client.model.HelmetModel;
import com.k1ngtle.vsia.item.HelmetItem;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

public class HelmetRenderer extends GeoArmorRenderer<HelmetItem> {
    public HelmetRenderer() {
        super(new HelmetModel());
    }
}
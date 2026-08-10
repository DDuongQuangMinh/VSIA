package com.k1ngtle.vsia.client.renderer;

import com.k1ngtle.vsia.client.model.SnowHelmetModel;
import com.k1ngtle.vsia.item.SnowHelmetItem;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

public class SnowHelmetRenderer extends GeoArmorRenderer<SnowHelmetItem> {
    public SnowHelmetRenderer() {
        super(new SnowHelmetModel());
    }
}
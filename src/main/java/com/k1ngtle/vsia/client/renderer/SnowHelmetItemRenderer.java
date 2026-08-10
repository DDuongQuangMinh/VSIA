package com.k1ngtle.vsia.client.renderer;

import com.k1ngtle.vsia.client.model.SnowHelmetModel;
import com.k1ngtle.vsia.item.SnowHelmetItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class SnowHelmetItemRenderer extends GeoItemRenderer<SnowHelmetItem> {
    public SnowHelmetItemRenderer() {
        super(new SnowHelmetModel());
    }
}
package com.k1ngtle.vsia.client.renderer;

import com.k1ngtle.vsia.client.model.HelmetModel;
import com.k1ngtle.vsia.item.HelmetItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class HelmetItemRenderer extends GeoItemRenderer<HelmetItem> {
    public HelmetItemRenderer() {
        super(new HelmetModel());
    }
}
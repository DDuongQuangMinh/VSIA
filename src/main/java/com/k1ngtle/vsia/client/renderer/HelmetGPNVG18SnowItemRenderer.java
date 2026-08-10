package com.k1ngtle.vsia.client.renderer;

import com.k1ngtle.vsia.client.model.HelmetGPNVG18SnowModel;
import com.k1ngtle.vsia.item.HelmetGPNVG18SnowItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class HelmetGPNVG18SnowItemRenderer extends GeoItemRenderer<HelmetGPNVG18SnowItem> {
    public HelmetGPNVG18SnowItemRenderer() {
        super(new HelmetGPNVG18SnowModel());
    }
}
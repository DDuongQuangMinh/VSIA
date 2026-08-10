package com.k1ngtle.vsia.client.renderer;

import com.k1ngtle.vsia.client.model.HelmetGPNVG18SnowModel;
import com.k1ngtle.vsia.item.HelmetGPNVG18SnowItem;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

public class HelmetGPNVG18SnowRenderer extends GeoArmorRenderer<HelmetGPNVG18SnowItem> {
    public HelmetGPNVG18SnowRenderer() {
        super(new HelmetGPNVG18SnowModel());
    }
}
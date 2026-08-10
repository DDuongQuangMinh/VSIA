package com.k1ngtle.vsia.client.renderer;

import com.k1ngtle.vsia.client.model.HelmetGPNVG18SandModel;
import com.k1ngtle.vsia.item.HelmetGPNVG18SandItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class HelmetGPNVG18SandItemRenderer extends GeoItemRenderer<HelmetGPNVG18SandItem> {
    public HelmetGPNVG18SandItemRenderer() {
        super(new HelmetGPNVG18SandModel());
    }
}
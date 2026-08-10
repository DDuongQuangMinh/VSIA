package com.k1ngtle.vsia.client.renderer;

import com.k1ngtle.vsia.client.model.HelmetGPNVG18SandModel;
import com.k1ngtle.vsia.item.HelmetGPNVG18SandItem;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

public class HelmetGPNVG18SandRenderer extends GeoArmorRenderer<HelmetGPNVG18SandItem> {
    public HelmetGPNVG18SandRenderer() {
        super(new HelmetGPNVG18SandModel());
    }
}
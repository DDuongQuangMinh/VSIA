package com.k1ngtle.vsia.client.renderer;

import com.k1ngtle.vsia.client.model.HelmetGPNVG18Model;
import com.k1ngtle.vsia.item.HelmetGPNVG18Item;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

public class HelmetGPNVG18Renderer extends GeoArmorRenderer<HelmetGPNVG18Item> {
    public HelmetGPNVG18Renderer() {
        super(new HelmetGPNVG18Model());
    }
}
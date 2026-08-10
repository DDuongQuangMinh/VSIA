package com.k1ngtle.vsia.client.renderer;

import com.k1ngtle.vsia.client.model.HelmetGPNVG18Model;
import com.k1ngtle.vsia.item.HelmetGPNVG18Item;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class HelmetGPNVG18ItemRenderer extends GeoItemRenderer<HelmetGPNVG18Item> {
    public HelmetGPNVG18ItemRenderer() {
        super(new HelmetGPNVG18Model());
    }
}
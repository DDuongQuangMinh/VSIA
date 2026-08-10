package com.k1ngtle.vsia.client.renderer;

import com.k1ngtle.vsia.client.model.GhillieHelmetGPNVG18Model;
import com.k1ngtle.vsia.item.GhillieHelmetGPNVG18Item;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class GhillieHelmetGPNVG18ItemRenderer extends GeoItemRenderer<GhillieHelmetGPNVG18Item> {
    public GhillieHelmetGPNVG18ItemRenderer() {
        super(new GhillieHelmetGPNVG18Model());
    }
}
package com.k1ngtle.vsia.client.renderer;

import com.k1ngtle.vsia.client.model.GhillieHelmetGPNVG18Model;
import com.k1ngtle.vsia.item.GhillieHelmetGPNVG18Item;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

public class GhillieHelmetGPNVG18Renderer extends GeoArmorRenderer<GhillieHelmetGPNVG18Item> {
    public GhillieHelmetGPNVG18Renderer() {
        super(new GhillieHelmetGPNVG18Model());
    }
}
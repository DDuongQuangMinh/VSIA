package com.k1ngtle.vsia.client.renderer;

import com.k1ngtle.vsia.client.model.GhillieHelmetPVS31Model;
import com.k1ngtle.vsia.item.GhillieHelmetPVS31Item;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

public class GhillieHelmetPVS31Renderer extends GeoArmorRenderer<GhillieHelmetPVS31Item> {
    public GhillieHelmetPVS31Renderer() {
        super(new GhillieHelmetPVS31Model());
    }
}
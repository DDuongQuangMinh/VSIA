package com.k1ngtle.vsia.client.renderer;

import com.k1ngtle.vsia.client.model.HelmetPVS31Model;
import com.k1ngtle.vsia.item.HelmetPVS31Item;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

public class HelmetPVS31Renderer extends GeoArmorRenderer<HelmetPVS31Item> {
    public HelmetPVS31Renderer() {
        super(new HelmetPVS31Model());
    }
}
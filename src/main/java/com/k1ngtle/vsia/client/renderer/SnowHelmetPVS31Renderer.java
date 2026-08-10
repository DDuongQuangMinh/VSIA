package com.k1ngtle.vsia.client.renderer;

import com.k1ngtle.vsia.client.model.SnowHelmetPVS31Model;
import com.k1ngtle.vsia.item.SnowHelmetPVS31Item;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

public class SnowHelmetPVS31Renderer extends GeoArmorRenderer<SnowHelmetPVS31Item> {
    public SnowHelmetPVS31Renderer() {
        super(new SnowHelmetPVS31Model());
    }
}
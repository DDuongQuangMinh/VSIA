package com.k1ngtle.vsia.client.renderer;

import com.k1ngtle.vsia.client.model.SandHelmetPVS31Model;
import com.k1ngtle.vsia.item.SandHelmetPVS31Item;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

public class SandHelmetPVS31Renderer extends GeoArmorRenderer<SandHelmetPVS31Item> {
    public SandHelmetPVS31Renderer() {
        super(new SandHelmetPVS31Model());
    }
}
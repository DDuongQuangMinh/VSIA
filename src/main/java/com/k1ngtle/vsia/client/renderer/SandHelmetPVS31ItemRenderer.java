package com.k1ngtle.vsia.client.renderer;

import com.k1ngtle.vsia.client.model.SandHelmetPVS31Model;
import com.k1ngtle.vsia.item.SandHelmetPVS31Item;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class SandHelmetPVS31ItemRenderer extends GeoItemRenderer<SandHelmetPVS31Item> {
    public SandHelmetPVS31ItemRenderer() {
        super(new SandHelmetPVS31Model());
    }
}
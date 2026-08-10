package com.k1ngtle.vsia.client.renderer;

import com.k1ngtle.vsia.client.model.HelmetPVS31Model;
import com.k1ngtle.vsia.item.HelmetPVS31Item;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class HelmetPVS31ItemRenderer extends GeoItemRenderer<HelmetPVS31Item> {
    public HelmetPVS31ItemRenderer() {
        super(new HelmetPVS31Model());
    }
}
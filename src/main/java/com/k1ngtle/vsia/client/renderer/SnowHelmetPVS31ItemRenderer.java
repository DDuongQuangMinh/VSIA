package com.k1ngtle.vsia.client.renderer;

import com.k1ngtle.vsia.client.model.SnowHelmetPVS31Model;
import com.k1ngtle.vsia.item.SnowHelmetPVS31Item;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class SnowHelmetPVS31ItemRenderer extends GeoItemRenderer<SnowHelmetPVS31Item> {
    public SnowHelmetPVS31ItemRenderer() {
        super(new SnowHelmetPVS31Model());
    }
}
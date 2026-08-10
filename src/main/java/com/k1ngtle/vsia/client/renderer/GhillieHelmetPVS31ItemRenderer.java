package com.k1ngtle.vsia.client.renderer;

import com.k1ngtle.vsia.client.model.GhillieHelmetPVS31Model;
import com.k1ngtle.vsia.item.GhillieHelmetPVS31Item;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class GhillieHelmetPVS31ItemRenderer extends GeoItemRenderer<GhillieHelmetPVS31Item> {
    public GhillieHelmetPVS31ItemRenderer() {
        super(new GhillieHelmetPVS31Model());
    }
}
package com.k1ngtle.vsia.client.renderer;

import com.k1ngtle.vsia.client.model.SandHelmetModel;
import com.k1ngtle.vsia.item.SandHelmetItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class SandHelmetItemRenderer extends GeoItemRenderer<SandHelmetItem> {
    public SandHelmetItemRenderer() {
        super(new SandHelmetModel());
    }
}
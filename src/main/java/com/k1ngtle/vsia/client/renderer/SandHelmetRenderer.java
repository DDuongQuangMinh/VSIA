package com.k1ngtle.vsia.client.renderer;

import com.k1ngtle.vsia.client.model.SandHelmetModel;
import com.k1ngtle.vsia.item.SandHelmetItem;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

public class SandHelmetRenderer extends GeoArmorRenderer<SandHelmetItem> {
    public SandHelmetRenderer() {
        super(new SandHelmetModel());
    }
}
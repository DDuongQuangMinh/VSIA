package com.k1ngtle.vsia.client.renderer;

import com.k1ngtle.vsia.client.model.F35HelmetModel;
import com.k1ngtle.vsia.item.F35HelmetItem;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

public final class F35HelmetRenderer extends GeoArmorRenderer<F35HelmetItem> {
    public F35HelmetRenderer(){super(new F35HelmetModel());}
}

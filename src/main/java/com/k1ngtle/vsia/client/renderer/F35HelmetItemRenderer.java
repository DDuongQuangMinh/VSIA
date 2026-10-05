package com.k1ngtle.vsia.client.renderer;

import com.k1ngtle.vsia.client.model.F35HelmetModel;
import com.k1ngtle.vsia.item.F35HelmetItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public final class F35HelmetItemRenderer extends GeoItemRenderer<F35HelmetItem> {
    public F35HelmetItemRenderer(){super(new F35HelmetModel());}
}

package com.k1ngtle.vsia.client.renderer;

import com.k1ngtle.vsia.client.model.FirewallModel;
import com.k1ngtle.vsia.signality.internet.server.FirewallBlockEntity;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public final class FirewallRenderer extends GeoBlockRenderer<FirewallBlockEntity> {
    public FirewallRenderer() {
        super(new FirewallModel());
    }
}
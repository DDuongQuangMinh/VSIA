package com.k1ngtle.vsia.client.renderer;

import com.k1ngtle.vsia.client.model.NetworkSwitchModel;
import com.k1ngtle.vsia.signality.internet.server.NetworkSwitchBlockEntity;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public final class NetworkSwitchRenderer extends GeoBlockRenderer<NetworkSwitchBlockEntity> {
    public NetworkSwitchRenderer() {
        super(new NetworkSwitchModel());
    }
}
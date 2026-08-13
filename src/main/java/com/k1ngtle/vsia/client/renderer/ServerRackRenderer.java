package com.k1ngtle.vsia.client.renderer;

import com.k1ngtle.vsia.client.model.ServerRackModel;
import com.k1ngtle.vsia.signality.internet.server.ServerRackBlockEntity;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public final class ServerRackRenderer extends GeoBlockRenderer<ServerRackBlockEntity> {
    public ServerRackRenderer() {
        super(new ServerRackModel());
    }
}

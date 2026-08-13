package com.k1ngtle.vsia.client.renderer;

import com.k1ngtle.vsia.client.model.ServerRackItemModel;
import com.k1ngtle.vsia.signality.internet.server.ServerRackItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public final class ServerRackItemRenderer
        extends GeoItemRenderer<ServerRackItem> {

    public ServerRackItemRenderer() {
        super(new ServerRackItemModel());
    }
}
package com.k1ngtle.vsia.client.web;

import com.k1ngtle.vsia.client.screen.W128WebIdeScreen;
import com.k1ngtle.vsia.network.web.W128IdeSnapshotPacket;
import net.minecraft.client.Minecraft;

public final class W128IdeClient {
    private W128IdeClient() {
    }

    public static void handleSnapshot(W128IdeSnapshotPacket packet) {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.screen instanceof W128WebIdeScreen screen
                && screen.host().equals(packet.host())) {
            screen.acceptSnapshot(packet);
            return;
        }

        minecraft.setScreen(new W128WebIdeScreen(packet));
    }
}

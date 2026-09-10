package com.k1ngtle.vsia.client.web;

import com.k1ngtle.vsia.client.screen.W128WebIdeScreen;
import com.k1ngtle.vsia.network.web.W129IdeEventPacket;
import net.minecraft.client.Minecraft;

public final class W129IdeClient {
    private W129IdeClient() {
    }

    public static void handle(W129IdeEventPacket packet) {
        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.screen
                instanceof W128WebIdeScreen screen) {
            screen.acceptW129Event(packet);
        }
    }
}

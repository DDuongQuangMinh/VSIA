package com.k1ngtle.vsia.signality.internet.satellite.client;

import com.k1ngtle.vsia.signality.internet.satellite.gui.SatelliteGuiSnapshot;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

public final class ClientSatelliteHooks {
    private ClientSatelliteHooks() {
    }

    public static void open(
            BlockPos pos
    ) {
        Minecraft.getInstance()
                .setScreen(
                        new SatelliteTerminalScreen(
                                pos
                        )
                );
    }

    public static void acceptSnapshot(
            SatelliteGuiSnapshot snapshot
    ) {
        if (Minecraft.getInstance()
                .screen
                instanceof SatelliteTerminalScreen screen) {
            screen.acceptSnapshot(
                    snapshot
            );
        }
    }
}

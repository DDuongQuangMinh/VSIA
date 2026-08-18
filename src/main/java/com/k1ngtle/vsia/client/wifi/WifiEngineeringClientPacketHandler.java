package com.k1ngtle.vsia.client.wifi;

import com.k1ngtle.vsia.client.screen.WifiEngineeringScreen;
import com.k1ngtle.vsia.signality.engineering.wifi.instrument.WifiEngineeringSnapshot;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

public final class WifiEngineeringClientPacketHandler {
    private WifiEngineeringClientPacketHandler() {
    }

    public static void handleSnapshot(
            BlockPos pos,
            WifiEngineeringSnapshot snapshot,
            boolean openScreen
    ) {
        Minecraft minecraft =
                Minecraft.getInstance();

        if (openScreen) {
            minecraft.setScreen(
                    new WifiEngineeringScreen(
                            pos,
                            snapshot
                    )
            );

            return;
        }

        if (minecraft.screen
                instanceof WifiEngineeringScreen screen
                && screen.targetPos()
                .equals(
                        pos
                )) {
            screen.acceptSnapshot(
                    snapshot
            );
        }
    }
}

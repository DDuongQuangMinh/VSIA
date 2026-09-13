package com.k1ngtle.vsia.signality.internet.radio.client;

import com.k1ngtle.vsia.signality.internet.radio.gui.RadioGuiSnapshot;
import com.k1ngtle.vsia.signality.internet.radio.gui.RadioGuiTarget;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;

public final class ClientRadioHooks {
    private ClientRadioHooks() {
    }

    public static void openBlock(
            BlockPos pos
    ) {
        Minecraft.getInstance()
                .setScreen(
                        new RadioControlScreen(
                                RadioGuiTarget.block(
                                        pos
                                )
                        )
                );
    }

    public static void openHeld(
            InteractionHand hand
    ) {
        Minecraft.getInstance()
                .setScreen(
                        new RadioControlScreen(
                                RadioGuiTarget.hand(
                                        hand
                                )
                        )
                );
    }

    public static void acceptSnapshot(
            RadioGuiSnapshot snapshot
    ) {
        if (Minecraft.getInstance()
                .screen
                instanceof RadioControlScreen screen) {
            screen.acceptSnapshot(
                    snapshot
            );
        }
    }
}

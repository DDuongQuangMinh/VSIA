package com.k1ngtle.vsia.cockpit.client;

import com.k1ngtle.vsia.Vsia;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = Vsia.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE,
        value = Dist.CLIENT
)
public final class F35DisplayClientInputEvents {
    private F35DisplayClientInputEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(
            TickEvent.ClientTickEvent event
    ) {
        if (event.phase
                != TickEvent.Phase.END) {
            return;
        }

        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.player == null) {
            return;
        }

        while (F35DisplayKeyMappings
                .CONFIGURE_DISPLAY
                .consumeClick()) {
            if (minecraft.screen
                    instanceof F35DisplayConfigScreen) {
                minecraft.setScreen(
                        null
                );
            } else if (minecraft.screen
                    == null) {
                minecraft.setScreen(
                        new F35DisplayConfigScreen()
                );
            }
        }
    }
}

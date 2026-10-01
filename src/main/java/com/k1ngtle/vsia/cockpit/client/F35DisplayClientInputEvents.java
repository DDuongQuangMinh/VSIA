package com.k1ngtle.vsia.cockpit.client;

import com.k1ngtle.vsia.Vsia;
import com.k1ngtle.vsia.cockpit.network.C2SF35DetectionFilterPacket;
import com.k1ngtle.vsia.network.VsiaNetwork;
import java.util.UUID;
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
    private static UUID syncedPlayerId;

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

        if (minecraft.player == null
                || minecraft.getConnection()
                == null) {
            syncedPlayerId =
                    null;
            return;
        }

        UUID playerId =
                minecraft.player
                        .getUUID();

        if (!playerId.equals(
                syncedPlayerId
        )) {
            F35DisplayClientConfig.ensureLoaded();

            VsiaNetwork.sendToServer(
                    new C2SF35DetectionFilterPacket(
                            F35DisplayClientConfig.detectMobs(),
                            F35DisplayClientConfig.detectPlayers(),
                            F35DisplayClientConfig.detectShips(),
                            F35DisplayClientConfig.showMissiles()
                    )
            );

            syncedPlayerId =
                    playerId;
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

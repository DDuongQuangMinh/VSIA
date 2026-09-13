package com.k1ngtle.vsia.signality.internet.satellite;

import com.k1ngtle.vsia.Vsia;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = Vsia.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class SatelliteRuntimeEvents {
    private SatelliteRuntimeEvents() {
    }

    @SubscribeEvent
    public static void onServerTick(
            TickEvent.ServerTickEvent event
    ) {
        if (event.phase
                != TickEvent.Phase.END) {
            return;
        }

        for (ServerLevel level
                : event.getServer()
                .getAllLevels()) {
            SatelliteNetworkManager.tick(
                    level
            );

            VsAwareSatelliteLinkService.tick(
                    level
            );
        }
    }

    @SubscribeEvent
    public static void onServerStopped(
            ServerStoppedEvent event
    ) {
        SatelliteNetworkManager.clear();
        VsAwareSatelliteLinkService.clear();
    }
}

package com.k1ngtle.vsia.signality.radar.network;

import com.k1ngtle.vsia.Vsia;
import com.k1ngtle.vsia.signality.api.events.ContactDetectedEvent;
import com.k1ngtle.vsia.signality.radar.iff.IffCommand;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = Vsia.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class RadarNetworkForgeEvents {
    private RadarNetworkForgeEvents() {
    }

    @SubscribeEvent(
            priority = EventPriority.LOWEST
    )
    public static void onContactDetected(
            ContactDetectedEvent event
    ) {
        RadarNetworkService.receive(
                event.emitter(),
                event.contact()
        );
    }

    @SubscribeEvent(
            priority = EventPriority.LOWEST
    )
    public static void onServerTick(
            TickEvent.ServerTickEvent event
    ) {
        if (event.phase
                != TickEvent.Phase.END) {
            return;
        }

        for (var level :
                event.getServer()
                        .getAllLevels()) {
            RadarNetworkService.tick(
                    level
            );
        }
    }

    @SubscribeEvent
    public static void onRegisterCommands(
            RegisterCommandsEvent event
    ) {
        RadarNetworkCommand.register(
                event.getDispatcher()
        );

        IffCommand.register(
                event.getDispatcher()
        );
    }

    @SubscribeEvent
    public static void onServerStopping(
            ServerStoppingEvent event
    ) {
        RadarNetworkService.clear();
    }
}

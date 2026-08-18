package com.k1ngtle.vsia.signality.engineering.reality;

import com.k1ngtle.vsia.Vsia;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = Vsia.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class RealityLifecycle {
    private RealityLifecycle() {
    }

    @SubscribeEvent
    public static void onServerStopped(
            ServerStoppedEvent event
    ) {
        RfMicroTimingRegistry.clear();
    }
}

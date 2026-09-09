package com.k1ngtle.vsia.signality.engineering.wifi.integration.w126;

import com.k1ngtle.vsia.Vsia;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = Vsia.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class W126TestManager {
    private static W126IntegrationSession active;

    private W126TestManager() {
    }

    public static synchronized boolean start(
            ServerLevel level,
            BlockPos sta1,
            BlockPos sta2,
            BlockPos ap1,
            BlockPos ap2,
            BlockPos networkSwitch,
            BlockPos server
    ) {
        if (active != null && !active.finished()) return false;

        active = new W126IntegrationSession(
                level,
                sta1,
                sta2,
                ap1,
                ap2,
                networkSwitch,
                server
        );

        return true;
    }

    public static synchronized W126Snapshot snapshot() {
        return active == null ? null : active.snapshot();
    }

    public static synchronized void reset() {
        active = null;
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        W126IntegrationSession current;

        synchronized (W126TestManager.class) {
            current = active;
        }

        if (current != null && !current.finished()) current.tick();
    }
}

package com.k1ngtle.vsia.signality.engineering.wifi.integration.w1221;

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
public final class W1221RoamingTestManager {
    private static W1221RoamingSession active;

    private W1221RoamingTestManager() {
    }

    public static synchronized boolean start(
            ServerLevel level,
            BlockPos station,
            BlockPos ap1,
            BlockPos ap2,
            BlockPos networkSwitch,
            BlockPos server
    ) {
        if (active != null
                && !active.finished()) {
            return false;
        }

        active =
                new W1221RoamingSession(
                        level,
                        station,
                        ap1,
                        ap2,
                        networkSwitch,
                        server
                );

        return true;
    }

    public static synchronized W1221RoamingSnapshot snapshot() {
        return active == null
                ? null
                : active.snapshot();
    }

    public static synchronized void reset() {
        active =
                null;
    }

    @SubscribeEvent
    public static void onServerTick(
            TickEvent.ServerTickEvent event
    ) {
        if (event.phase
                != TickEvent.Phase.END) {
            return;
        }

        W1221RoamingSession current;

        synchronized (W1221RoamingTestManager.class) {
            current =
                    active;
        }

        if (current != null
                && !current.finished()) {
            current.tick();
        }
    }
}

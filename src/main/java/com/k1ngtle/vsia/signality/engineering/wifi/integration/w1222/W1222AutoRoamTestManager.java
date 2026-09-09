package com.k1ngtle.vsia.signality.engineering.wifi.integration.w1222;

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
public final class W1222AutoRoamTestManager {
    private static W1222AutoRoamSession active;

    private W1222AutoRoamTestManager() {
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
                new W1222AutoRoamSession(
                        level,
                        station,
                        ap1,
                        ap2,
                        networkSwitch,
                        server
                );

        return true;
    }

    public static synchronized W1222AutoRoamSnapshot snapshot() {
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

        W1222AutoRoamSession current;

        synchronized (W1222AutoRoamTestManager.class) {
            current =
                    active;
        }

        if (current != null
                && !current.finished()) {
            current.tick();
        }
    }
}

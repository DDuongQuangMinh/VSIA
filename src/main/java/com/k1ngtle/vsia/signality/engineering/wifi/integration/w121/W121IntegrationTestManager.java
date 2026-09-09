package com.k1ngtle.vsia.signality.engineering.wifi.integration.w121;

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
public final class W121IntegrationTestManager {
    private static W121IntegrationSession active;

    private W121IntegrationTestManager() {
    }

    public static synchronized boolean start(
            ServerLevel level,
            BlockPos station,
            BlockPos accessPoint,
            BlockPos networkSwitch,
            BlockPos server
    ) {
        if (active != null
                && !active.finished()) {
            return false;
        }

        active =
                new W121IntegrationSession(
                        level,
                        station,
                        accessPoint,
                        networkSwitch,
                        server
                );

        return true;
    }

    public static synchronized W121IntegrationSnapshot snapshot() {
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

        W121IntegrationSession current;

        synchronized (W121IntegrationTestManager.class) {
            current =
                    active;
        }

        if (current != null
                && !current.finished()) {
            current.tick();
        }
    }
}

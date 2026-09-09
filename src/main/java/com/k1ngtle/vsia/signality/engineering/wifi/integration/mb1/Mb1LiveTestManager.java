package com.k1ngtle.vsia.signality.engineering.wifi.integration.mb1;

import com.k1ngtle.vsia.Vsia;
import com.k1ngtle.vsia.signality.engineering.wifi.smartconnect.WifiBand;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = Vsia.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class Mb1LiveTestManager {
    private static Mb1LiveSession active;

    private Mb1LiveTestManager() {
    }

    public static synchronized boolean start(
            ServerLevel level,
            BlockPos station,
            BlockPos ap24,
            BlockPos ap5,
            WifiBand expectedBand
    ) {
        if (active != null
                && !active.finished()) {
            return false;
        }

        active =
                new Mb1LiveSession(
                        level,
                        station,
                        ap24,
                        ap5,
                        expectedBand
                );

        return true;
    }

    public static synchronized Mb1LiveSnapshot snapshot() {
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

        Mb1LiveSession current;

        synchronized (Mb1LiveTestManager.class) {
            current =
                    active;
        }

        if (current != null
                && !current.finished()) {
            current.tick();
        }
    }
}

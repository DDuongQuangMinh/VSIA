package com.k1ngtle.vsia.signality.engineering.wifi.integration.w123;

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
public final class W123ContentionTestManager {
    private static W123ContentionSession active;

    private W123ContentionTestManager() {
    }

    public static synchronized boolean start(
            ServerLevel level,
            BlockPos station,
            BlockPos accessPoint
    ) {
        if (active != null
                && !active.finished()) {
            return false;
        }

        active =
                new W123ContentionSession(
                        level,
                        station,
                        accessPoint
                );

        return true;
    }

    public static synchronized W123ContentionSnapshot snapshot() {
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

        W123ContentionSession current;

        synchronized (W123ContentionTestManager.class) {
            current =
                    active;
        }

        if (current != null
                && !current.finished()) {
            current.tick();
        }
    }
}

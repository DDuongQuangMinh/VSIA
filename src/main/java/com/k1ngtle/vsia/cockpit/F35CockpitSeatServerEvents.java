package com.k1ngtle.vsia.cockpit;

import com.k1ngtle.vsia.Vsia;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = Vsia.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class F35CockpitSeatServerEvents {
    private F35CockpitSeatServerEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTick(
            TickEvent.PlayerTickEvent event
    ) {
        if (event.phase
                != TickEvent.Phase.END) {
            return;
        }

        if (event.player
                instanceof ServerPlayer player) {
            F35SeatController.tickPassenger(
                    player
            );
        }
    }
}

package com.k1ngtle.vsia.signality.internet.radio.portable;

import com.k1ngtle.vsia.Vsia;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = Vsia.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class PortableRadioEvents {
    private PortableRadioEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTick(
            TickEvent.PlayerTickEvent event
    ) {
        if (event.phase
                != TickEvent.Phase.END
                || event.player.level()
                .isClientSide()) {
            return;
        }

        if (event.player
                instanceof ServerPlayer player) {
            PortableRadioService.tick(
                    player
            );
        }
    }

    @SubscribeEvent
    public static void onServerStopped(
            ServerStoppedEvent event
    ) {
        PortableRadioService.clear();
    }

    @SubscribeEvent
    public static void onLogout(
            PlayerEvent.PlayerLoggedOutEvent event
    ) {
        if (event.getEntity()
                instanceof ServerPlayer player) {
            PortableRadioService.remove(
                    player
            );
        }
    }
}

package com.k1ngtle.vsia.phone.network.realism;

import com.k1ngtle.vsia.Vsia;
import com.k1ngtle.vsia.phone.messages.PhoneMessageService;
import com.k1ngtle.vsia.phone.subscriber.PhoneSubscriberService;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;

@Mod.EventBusSubscriber(
        modid = Vsia.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class PhoneWirelessRuntimeEvents {
    private PhoneWirelessRuntimeEvents() {
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            PhoneWirelessServerService.tick(player);

            if (server.overworld().getGameTime() % 40L == 0L) {
                PhoneSubscriberService.refresh(player);
                PhoneMessageService.refresh(player);
            }
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PhoneSubscriberService.refresh(player);
            PhoneMessageService.refresh(player);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        PhoneWirelessServerService.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        PhoneWirelessServerService.clear();
    }
}

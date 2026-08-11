package com.k1ngtle.vsia.weapon;

import com.k1ngtle.vsia.weapon.registry.WeaponItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = WeaponItems.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class GunServerEvents {

    private GunServerEvents() {}

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            GunFireLogic.tick(player);
        }
    }
}
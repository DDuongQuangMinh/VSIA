package com.k1ngtle.vsia.weapon;

import com.k1ngtle.vsia.weapon.network.WeaponNetwork;
import com.k1ngtle.vsia.weapon.resource.WeaponDefinitionReloadListener;
import com.k1ngtle.vsia.weapon.server.ServerWeaponEvents;
import com.k1ngtle.vsia.weapon.server.ServerWeaponOperations;
import com.k1ngtle.vsia.weapon.server.hitscan.ServerHitscanShotExecutor;
import com.k1ngtle.vsia.weapon.server.hitscan.HitscanEvents;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;

public final class VSIAWeaponBootstrap {
    private VSIAWeaponBootstrap() {}

    public static void initialize(String modId) {
        WeaponNetwork.initialize(modId);
        ServerWeaponOperations.setShotExecutor(new ServerHitscanShotExecutor());
        HitscanEvents.setListener(result -> {
            net.minecraft.server.MinecraftServer server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
            if (server == null) return;
            net.minecraft.world.entity.Entity entity = null;
            for (net.minecraft.server.level.ServerLevel level : server.getAllLevels()) {
                entity = level.getEntity(result.shooterEntityId());
                if (entity != null) break;
            }
            if (entity instanceof net.minecraft.server.level.ServerPlayer player) {
                WeaponNetwork.broadcastShot(player, result.start(), result.end());
            }
        });
        MinecraftForge.EVENT_BUS.addListener(VSIAWeaponBootstrap::addReloadListeners);
        MinecraftForge.EVENT_BUS.addListener(ServerWeaponEvents::onPlayerTick);
        MinecraftForge.EVENT_BUS.addListener(ServerWeaponEvents::onPlayerLogout);
        MinecraftForge.EVENT_BUS.addListener(ServerWeaponEvents::onItemToss);
        MinecraftForge.EVENT_BUS.addListener(ServerWeaponEvents::onDeath);
    }

    private static void addReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new WeaponDefinitionReloadListener());
    }
}

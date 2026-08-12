package com.k1ngtle.vsia.weapon.server;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;

public final class ServerWeaponEvents {
    private ServerWeaponEvents() {}

    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide
                || !(event.player instanceof ServerPlayer player)) return;
        ServerWeaponOperations.tickReload(player, InteractionHand.MAIN_HAND);
        ServerWeaponOperations.tickReload(player, InteractionHand.OFF_HAND);
        ServerTriggerController.tick(player);
        for (net.minecraft.world.item.ItemStack stack : player.getInventory().items) {
            if (stack != player.getMainHandItem() && stack != player.getOffhandItem()) {
                ServerWeaponOperations.cancelReloadStack(player, stack, InteractionHand.MAIN_HAND);
            }
        }
    }

    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ServerTriggerController.remove(player);
            ServerAimController.remove(player);
        }
    }

    public static void onItemToss(ItemTossEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player) {
            ServerWeaponOperations.cancelReloadStack(player, event.getEntity().getItem(), InteractionHand.MAIN_HAND);
        }
    }

    public static void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        for (net.minecraft.world.item.ItemStack stack : player.getInventory().items) {
            ServerWeaponOperations.cancelReloadStack(player, stack, InteractionHand.MAIN_HAND);
        }
        ServerAimController.remove(player);
        ServerTriggerController.remove(player);
    }
}

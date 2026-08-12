package com.k1ngtle.vsia.weapon.server;

import com.k1ngtle.vsia.weapon.data.WeaponDefinition;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

@FunctionalInterface
public interface ShotExecutor {
    void execute(ServerPlayer player, ItemStack weapon, WeaponDefinition definition);
}

package com.k1ngtle.vsia.weapon.server;

import com.k1ngtle.vsia.weapon.api.VSIAWeaponAPI;
import com.k1ngtle.vsia.weapon.network.WeaponNetwork;
import com.k1ngtle.vsia.weapon.state.WeaponEventType;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;

public final class ServerAimController {
    private static final Set<UUID> AIMING = ConcurrentHashMap.newKeySet();
    private ServerAimController() {}
    public static void setAiming(ServerPlayer player, InteractionHand hand, boolean aiming) {
        if (aiming && VSIAWeaponAPI.getWeapon(player.getItemInHand(hand)).isEmpty()) return;
        boolean changed = aiming ? AIMING.add(player.getUUID()) : AIMING.remove(player.getUUID());
        if (changed) WeaponNetwork.broadcastEvent(player, hand,
                aiming ? WeaponEventType.ADS_STARTED : WeaponEventType.ADS_STOPPED, "");
    }
    public static boolean isAiming(ServerPlayer player) { return AIMING.contains(player.getUUID()); }
    public static void remove(ServerPlayer player) { AIMING.remove(player.getUUID()); }
}

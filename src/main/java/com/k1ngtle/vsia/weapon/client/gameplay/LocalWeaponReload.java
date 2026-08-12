package com.k1ngtle.vsia.weapon.client.gameplay;

import com.k1ngtle.vsia.weapon.network.WeaponNetwork;
import com.k1ngtle.vsia.weapon.network.c2s.ReloadPacket;
import net.minecraft.world.InteractionHand;

public final class LocalWeaponReload {
    public void request(InteractionHand hand) {
        WeaponNetwork.sendToServer(new ReloadPacket(hand));
    }

    public void cancel(InteractionHand hand) {
        WeaponNetwork.sendToServer(new com.k1ngtle.vsia.weapon.network.c2s.CancelReloadPacket(hand));
    }
}

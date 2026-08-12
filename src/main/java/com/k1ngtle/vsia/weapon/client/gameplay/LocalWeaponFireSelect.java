package com.k1ngtle.vsia.weapon.client.gameplay;

import com.k1ngtle.vsia.weapon.network.WeaponNetwork;
import com.k1ngtle.vsia.weapon.network.c2s.CycleFireModePacket;
import net.minecraft.world.InteractionHand;

public final class LocalWeaponFireSelect {
    public void request(InteractionHand hand) {
        WeaponNetwork.sendToServer(new CycleFireModePacket(hand));
    }
}

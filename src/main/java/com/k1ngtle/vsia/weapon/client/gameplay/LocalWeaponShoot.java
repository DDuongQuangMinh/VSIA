package com.k1ngtle.vsia.weapon.client.gameplay;

import com.k1ngtle.vsia.weapon.network.WeaponNetwork;
import com.k1ngtle.vsia.weapon.network.c2s.TriggerPacket;
import net.minecraft.world.InteractionHand;

public final class LocalWeaponShoot {
    public void request(InteractionHand hand) {
        if (!ClientWeaponContext.getInstance().actions().isLocked()) {
            press(hand);
            release(hand);
        }
    }

    public void press(InteractionHand hand) {
        if (!ClientWeaponContext.getInstance().actions().isLocked())
            WeaponNetwork.sendToServer(new TriggerPacket(hand, true));
    }

    public void release(InteractionHand hand) {
        WeaponNetwork.sendToServer(new TriggerPacket(hand, false));
    }
}

package com.k1ngtle.vsia.weapon.client.gameplay;

import com.k1ngtle.vsia.weapon.network.WeaponNetwork;
import com.k1ngtle.vsia.weapon.network.c2s.TriggerPacket;
import com.k1ngtle.vsia.weapon.state.WeaponRuntimeState;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;

public final class LocalWeaponShoot {
    public void request(InteractionHand hand) {
        if (!ClientWeaponContext.getInstance().actions().isLocked()) {
            press(hand);
            release(hand);
        }
    }

    public void press(InteractionHand hand) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;
        if (WeaponRuntimeState.get(minecraft.player.getItemInHand(hand)).getAvailableRounds() <= 0) {
            WeaponNetwork.sendToServer(new TriggerPacket(hand, false));
            return;
        }
        if (!ClientWeaponContext.getInstance().actions().isLocked())
            WeaponNetwork.sendToServer(new TriggerPacket(hand, true));
    }

    public void release(InteractionHand hand) {
        WeaponNetwork.sendToServer(new TriggerPacket(hand, false));
    }
}

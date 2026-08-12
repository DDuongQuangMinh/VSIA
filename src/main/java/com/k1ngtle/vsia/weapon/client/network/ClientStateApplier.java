package com.k1ngtle.vsia.weapon.client.network;

import com.k1ngtle.vsia.weapon.network.s2c.WeaponStatePacket;
import com.k1ngtle.vsia.weapon.state.WeaponRuntimeState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public final class ClientStateApplier {
    private ClientStateApplier() {}

    public static void apply(WeaponStatePacket packet) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) WeaponRuntimeState.get(player.getItemInHand(packet.hand())).loadTag(packet.state());
    }
}

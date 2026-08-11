package com.k1ngtle.vsia.weapon.network;

import com.k1ngtle.vsia.weapon.GunFireLogic;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Client tells the server "I fired". Server is authoritative: it
 * re-validates ammo/cooldown/fire mode itself rather than trusting
 * anything from the client, so this packet carries no payload at all.
 */
public class C2SFirePacket {

    public static void encode(C2SFirePacket msg, FriendlyByteBuf buf) {}

    public static C2SFirePacket decode(FriendlyByteBuf buf) {
        return new C2SFirePacket();
    }

    public static void handle(C2SFirePacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player != null) {
                GunFireLogic.handleFireRequest(player);
            }
        });
        ctx.setPacketHandled(true);
    }
}
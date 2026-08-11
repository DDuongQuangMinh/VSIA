package com.k1ngtle.vsia.weapon.network;

import com.k1ngtle.vsia.weapon.GunFireLogic;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class C2SReloadPacket {

    public static void encode(C2SReloadPacket msg, FriendlyByteBuf buf) {}

    public static C2SReloadPacket decode(FriendlyByteBuf buf) {
        return new C2SReloadPacket();
    }

    public static void handle(C2SReloadPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player != null) {
                GunFireLogic.handleReloadRequest(player);
            }
        });
        ctx.setPacketHandled(true);
    }
}
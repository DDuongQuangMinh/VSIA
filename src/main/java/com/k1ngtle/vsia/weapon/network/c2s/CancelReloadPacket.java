package com.k1ngtle.vsia.weapon.network.c2s;

import com.k1ngtle.vsia.weapon.server.ServerWeaponOperations;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.network.NetworkEvent;

public record CancelReloadPacket(InteractionHand hand) {
    public static void encode(CancelReloadPacket packet, FriendlyByteBuf buffer) { buffer.writeEnum(packet.hand); }
    public static CancelReloadPacket decode(FriendlyByteBuf buffer) { return new CancelReloadPacket(buffer.readEnum(InteractionHand.class)); }
    public static void handle(CancelReloadPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender != null) ServerWeaponOperations.cancelReload(sender, packet.hand);
        });
        context.setPacketHandled(true);
    }
}

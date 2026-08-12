package com.k1ngtle.vsia.weapon.network.c2s;

import com.k1ngtle.vsia.weapon.server.ServerAimController;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.network.NetworkEvent;

public record AimPacket(InteractionHand hand, boolean aiming) {
    public static void encode(AimPacket packet, FriendlyByteBuf buffer) { buffer.writeEnum(packet.hand); buffer.writeBoolean(packet.aiming); }
    public static AimPacket decode(FriendlyByteBuf buffer) { return new AimPacket(buffer.readEnum(InteractionHand.class), buffer.readBoolean()); }
    public static void handle(AimPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender != null) ServerAimController.setAiming(sender, packet.hand, packet.aiming);
        });
        context.setPacketHandled(true);
    }
}

package com.k1ngtle.vsia.weapon.network.c2s;

import com.k1ngtle.vsia.weapon.server.ServerWeaponOperations;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.network.NetworkEvent;

public record ReloadPacket(InteractionHand hand) {
    public static void encode(ReloadPacket packet, FriendlyByteBuf buffer) { buffer.writeEnum(packet.hand); }
    public static ReloadPacket decode(FriendlyByteBuf buffer) { return new ReloadPacket(buffer.readEnum(InteractionHand.class)); }
    public static void handle(ReloadPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender != null) ServerWeaponOperations.startReload(sender, packet.hand);
        });
        context.setPacketHandled(true);
    }
}

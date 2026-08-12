package com.k1ngtle.vsia.weapon.network.c2s;

import com.k1ngtle.vsia.weapon.server.ServerWeaponOperations;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.network.NetworkEvent;

public record CycleFireModePacket(InteractionHand hand) {
    public static void encode(CycleFireModePacket packet, FriendlyByteBuf buffer) { buffer.writeEnum(packet.hand); }
    public static CycleFireModePacket decode(FriendlyByteBuf buffer) { return new CycleFireModePacket(buffer.readEnum(InteractionHand.class)); }
    public static void handle(CycleFireModePacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender != null) ServerWeaponOperations.cycleFireMode(sender, packet.hand);
        });
        context.setPacketHandled(true);
    }
}

package com.k1ngtle.vsia.weapon.network.c2s;

import com.k1ngtle.vsia.weapon.server.ServerTriggerController;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.network.NetworkEvent;

public record TriggerPacket(InteractionHand hand, boolean pressed) {
    public static void encode(TriggerPacket packet, FriendlyByteBuf buffer) {
        buffer.writeEnum(packet.hand);
        buffer.writeBoolean(packet.pressed);
    }
    public static TriggerPacket decode(FriendlyByteBuf buffer) {
        return new TriggerPacket(buffer.readEnum(InteractionHand.class), buffer.readBoolean());
    }
    public static void handle(TriggerPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender != null) ServerTriggerController.setPressed(sender, packet.hand, packet.pressed);
        });
        context.setPacketHandled(true);
    }
}

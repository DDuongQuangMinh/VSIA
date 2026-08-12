package com.k1ngtle.vsia.weapon.network.s2c;

import com.k1ngtle.vsia.weapon.client.network.ClientStateApplier;
import java.util.function.Supplier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.network.NetworkEvent;

public record WeaponStatePacket(InteractionHand hand, CompoundTag state) {
    public static void encode(WeaponStatePacket packet, FriendlyByteBuf buffer) {
        buffer.writeEnum(packet.hand);
        buffer.writeNbt(packet.state);
    }

    public static WeaponStatePacket decode(FriendlyByteBuf buffer) {
        InteractionHand hand = buffer.readEnum(InteractionHand.class);
        CompoundTag state = buffer.readNbt();
        return new WeaponStatePacket(hand, state == null ? new CompoundTag() : state);
    }

    public static void handle(WeaponStatePacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> ClientStateApplier.apply(packet));
        context.setPacketHandled(true);
    }
}

package com.k1ngtle.vsia.weapon.network.s2c;

import com.k1ngtle.vsia.weapon.client.network.ClientWeaponEventBus;
import com.k1ngtle.vsia.weapon.state.WeaponEventType;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

public record WeaponEventPacket(int sourceEntityId, InteractionHand hand, WeaponEventType type,
                                Vec3 start, Vec3 end, String detail) {
    public static WeaponEventPacket simple(int source, InteractionHand hand, WeaponEventType type, String detail) {
        return new WeaponEventPacket(source, hand, type, Vec3.ZERO, Vec3.ZERO, detail);
    }
    public static void encode(WeaponEventPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.sourceEntityId); buffer.writeEnum(packet.hand); buffer.writeEnum(packet.type);
        buffer.writeDouble(packet.start.x); buffer.writeDouble(packet.start.y); buffer.writeDouble(packet.start.z);
        buffer.writeDouble(packet.end.x); buffer.writeDouble(packet.end.y); buffer.writeDouble(packet.end.z);
        buffer.writeUtf(packet.detail, 128);
    }
    public static WeaponEventPacket decode(FriendlyByteBuf buffer) {
        int source = buffer.readVarInt(); InteractionHand hand = buffer.readEnum(InteractionHand.class);
        WeaponEventType type = buffer.readEnum(WeaponEventType.class);
        Vec3 start = new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
        Vec3 end = new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
        return new WeaponEventPacket(source, hand, type, start, end, buffer.readUtf(128));
    }
    public static void handle(WeaponEventPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> ClientWeaponEventBus.publish(packet));
        context.setPacketHandled(true);
    }
}

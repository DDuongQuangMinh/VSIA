package com.k1ngtle.vsia.weapon.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Broadcast to the shooter and everyone tracking them (see
 * PacketDistributor.TRACKING_ENTITY_AND_SELF in GunFireLogic).
 * The shooter's client uses this to trigger the recoil camera kick;
 * every receiving client uses it to play the muzzle flash/sound/fire
 * animation on that entity, including in third person.
 */
public class S2CGunFirePacket {

    private final int shooterEntityId;

    public S2CGunFirePacket(int shooterEntityId) {
        this.shooterEntityId = shooterEntityId;
    }

    public static void encode(S2CGunFirePacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.shooterEntityId);
    }

    public static S2CGunFirePacket decode(FriendlyByteBuf buf) {
        return new S2CGunFirePacket(buf.readVarInt());
    }

    public static void handle(S2CGunFirePacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            if (ctx.getDirection().getReceptionSide() == LogicalSide.CLIENT) {
                // Deferred to a client-only class so this file has no
                // client-only imports and stays safe on the dedicated server.
                com.k1ngtle.vsia.weapon.client.ClientGunFeedback.onGunFired(msg.shooterEntityId);
            }
        });
        ctx.setPacketHandled(true);
    }
}
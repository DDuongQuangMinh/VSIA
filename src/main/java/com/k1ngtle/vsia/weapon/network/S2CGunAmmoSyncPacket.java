package com.k1ngtle.vsia.weapon.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Sent to the owning client to keep the ammo HUD and reload state in sync. */
public class S2CGunAmmoSyncPacket {

    private final int ammo;
    private final boolean reloading;

    public S2CGunAmmoSyncPacket(int ammo, boolean reloading) {
        this.ammo = ammo;
        this.reloading = reloading;
    }

    public static void encode(S2CGunAmmoSyncPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.ammo);
        buf.writeBoolean(msg.reloading);
    }

    public static S2CGunAmmoSyncPacket decode(FriendlyByteBuf buf) {
        return new S2CGunAmmoSyncPacket(buf.readVarInt(), buf.readBoolean());
    }

    public static void handle(S2CGunAmmoSyncPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            if (ctx.getDirection().getReceptionSide() == LogicalSide.CLIENT) {
                com.k1ngtle.vsia.weapon.client.ClientGunFeedback.onAmmoSync(msg.ammo, msg.reloading);
            }
        });
        ctx.setPacketHandled(true);
    }
}
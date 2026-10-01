package com.k1ngtle.vsia.cockpit.network;

import com.k1ngtle.vsia.cockpit.F35CockpitSeatBlockEntity;
import com.k1ngtle.vsia.cockpit.F35SeatController;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public record C2SF35DisplayActionPacket(UUID cockpitId, int action) {
    public C2SF35DisplayActionPacket(FriendlyByteBuf b) { this(b.readUUID(), b.readVarInt()); }
    public void toBytes(FriendlyByteBuf b) { b.writeUUID(cockpitId); b.writeVarInt(action); }
    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx=supplier.get(); ctx.enqueueWork(() -> {
            ServerPlayer player=ctx.getSender(); if(player==null)return;
            F35CockpitSeatBlockEntity cockpit=F35SeatController.cockpitFor(player);
            if(cockpit==null || !cockpitId.equals(cockpit.cockpitId()))return;
            cockpit.displaySettings().apply(action); cockpit.sync();
        }); ctx.setPacketHandled(true);
    }
}

package com.k1ngtle.vsia.cockpit.network;

import com.k1ngtle.vsia.cockpit.F35CockpitSeatBlockEntity;
import com.k1ngtle.vsia.cockpit.F35SeatController;
import java.time.Instant;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public record C2SF35IffRequestPacket(UUID cockpitId) {
    public C2SF35IffRequestPacket(FriendlyByteBuf buffer) { this(buffer.readUUID()); }
    public void toBytes(FriendlyByteBuf buffer) { buffer.writeUUID(cockpitId); }
    public void handle(Supplier<NetworkEvent.Context> supplier) { NetworkEvent.Context ctx = supplier.get(); ctx.enqueueWork(() -> {
        ServerPlayer p = ctx.getSender(); if (p == null) return; F35CockpitSeatBlockEntity c = F35SeatController.cockpitFor(p);
        if (c != null && cockpitId.equals(c.cockpitId())) F35IffPackets.sendSnapshot(p, c, Instant.now().getEpochSecond()); }); ctx.setPacketHandled(true); }
}

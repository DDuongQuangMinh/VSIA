package com.k1ngtle.vsia.cockpit.network;

import com.k1ngtle.vsia.cockpit.iff.F35IffClientState;
import java.util.function.Supplier;
import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

public record S2CF35IffSnapshotPacket(UUID cockpitId, String master, String mode1, String mode2, String mode3a, int enabledMask,
                                     int activeSlot, String keyId, long expiresAt, int telemetryMask, String status) {
    public S2CF35IffSnapshotPacket(FriendlyByteBuf b) { this(b.readUUID(), b.readUtf(8), b.readUtf(2), b.readUtf(4), b.readUtf(4), b.readVarInt(), b.readVarInt(), b.readUtf(16), b.readLong(), b.readVarInt(), b.readUtf(24)); }
    public void toBytes(FriendlyByteBuf b) { b.writeUUID(cockpitId); b.writeUtf(master, 8); b.writeUtf(mode1, 2); b.writeUtf(mode2, 4); b.writeUtf(mode3a, 4); b.writeVarInt(enabledMask); b.writeVarInt(activeSlot); b.writeUtf(keyId, 16); b.writeLong(expiresAt); b.writeVarInt(telemetryMask); b.writeUtf(status, 24); }
    public void handle(Supplier<NetworkEvent.Context> supplier) { NetworkEvent.Context ctx = supplier.get(); ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> F35IffClientState.accept(cockpitId, new F35IffClientState.Snapshot(master, mode1, mode2, mode3a, enabledMask, activeSlot, keyId, expiresAt, telemetryMask, status)))); ctx.setPacketHandled(true); }
}

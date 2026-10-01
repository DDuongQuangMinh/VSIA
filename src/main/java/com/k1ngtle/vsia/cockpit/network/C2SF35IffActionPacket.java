package com.k1ngtle.vsia.cockpit.network;

import com.k1ngtle.vsia.cockpit.F35CockpitSeatBlockEntity;
import com.k1ngtle.vsia.cockpit.F35SeatController;
import com.k1ngtle.vsia.cockpit.iff.F35IffConfig;
import java.time.Instant;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public record C2SF35IffActionPacket(UUID cockpitId, int action, String value) {
    public C2SF35IffActionPacket(FriendlyByteBuf b) { this(b.readUUID(), b.readVarInt(), b.readUtf(80)); }
    public void toBytes(FriendlyByteBuf b) { b.writeUUID(cockpitId); b.writeVarInt(action); b.writeUtf(value, 80); }
    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender(); if (player == null) return;
            F35CockpitSeatBlockEntity cockpit = F35SeatController.cockpitFor(player);
            if (cockpit == null || !cockpitId.equals(cockpit.cockpitId())) return;
            F35IffConfig iff = cockpit.iff();
            switch (action) {
                case 0 -> iff.cycleMaster();
                case 1, 2, 3, 4, 5 -> iff.toggleMode(action);
                case 10, 11, 12, 13 -> iff.toggleTelemetry(action - 10);
                case 20, 21 -> iff.selectSlot(action - 20);
                case 30 -> iff.generateKey(iff.activeSlot(), cleanId(value), 6 * 3600L);
                case 31 -> iff.zeroize();
                case 40 -> { String[] p = value.split(":", 3); if (p.length == 3) iff.setCodes(p[0], p[1], p[2]); }
                case 41 -> iff.setCodes(next(iff.mode1(), 100, false), iff.mode2(), iff.mode3a());
                case 42 -> iff.setCodes(iff.mode1(), next(iff.mode2(), 10000, false), iff.mode3a());
                case 43 -> iff.setCodes(iff.mode1(), iff.mode2(), next(iff.mode3a(), 4096, true));
                default -> { }
            }
            cockpit.sync();
            F35IffPackets.sendSnapshot(player, cockpit, Instant.now().getEpochSecond());
        });
        ctx.setPacketHandled(true);
    }
    private static String cleanId(String s) { String v = s == null ? "MISSION" : s.replaceAll("[^A-Za-z0-9_-]", ""); return v.isBlank() ? "MISSION" : v.substring(0, Math.min(16, v.length())); }
    private static String next(String value, int modulus, boolean octal) { int radix = octal ? 8 : 10; int v; try { v = Integer.parseInt(value, radix); } catch (Exception e) { v = 0; } v = (v + 1) % modulus; String out = Integer.toString(v, radix); return "0".repeat(Math.max(0, value.length() - out.length())) + out; }
}

package com.k1ngtle.vsia.network.wifi;

import com.k1ngtle.vsia.client.wifi.WifiEngineeringClientPacketHandler;
import com.k1ngtle.vsia.signality.engineering.wifi.trace.WifiPacketTraceCodec;
import com.k1ngtle.vsia.signality.engineering.wifi.trace.WifiPacketTraceEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.function.Supplier;

public final class WifiPacketTraceSnapshotPacket {
    private final BlockPos pos;
    private final List<WifiPacketTraceEvent> events;

    public WifiPacketTraceSnapshotPacket(
            BlockPos pos,
            List<WifiPacketTraceEvent> events
    ) {
        this.pos = pos.immutable();
        this.events = List.copyOf(events);
    }

    public WifiPacketTraceSnapshotPacket(
            FriendlyByteBuf buf
    ) {
        this.pos =
                buf.readBlockPos();

        this.events =
                WifiPacketTraceCodec.read(buf);
    }

    public void toBytes(
            FriendlyByteBuf buf
    ) {
        buf.writeBlockPos(pos);
        WifiPacketTraceCodec.write(
                buf,
                events
        );
    }

    public void handle(
            Supplier<NetworkEvent.Context> supplier
    ) {
        NetworkEvent.Context context =
                supplier.get();

        context.enqueueWork(
                () ->
                        DistExecutor.unsafeRunWhenOn(
                                Dist.CLIENT,
                                () ->
                                        () ->
                                                WifiEngineeringClientPacketHandler
                                                        .handlePacketTrace(
                                                                pos,
                                                                events
                                                        )
                        )
        );

        context.setPacketHandled(true);
    }
}

package com.k1ngtle.vsia.network.wifi;

import com.k1ngtle.vsia.client.wifi.WifiEngineeringClientPacketHandler;
import com.k1ngtle.vsia.signality.engineering.wifi.ip.WifiIpEngineeringSnapshot;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public final class WifiIpEngineeringSnapshotPacket {
    private final BlockPos pos;
    private final WifiIpEngineeringSnapshot snapshot;

    public WifiIpEngineeringSnapshotPacket(
            BlockPos pos,
            WifiIpEngineeringSnapshot snapshot
    ) {
        this.pos =
                pos.immutable();

        this.snapshot =
                snapshot;
    }

    public WifiIpEngineeringSnapshotPacket(
            FriendlyByteBuf buf
    ) {
        this.pos =
                buf.readBlockPos();

        this.snapshot =
                new WifiIpEngineeringSnapshot(
                        buf.readUtf(
                                64
                        ),
                        buf.readUtf(
                                64
                        ),
                        buf.readUtf(
                                64
                        ),
                        buf.readUtf(
                                64
                        ),
                        buf.readVarInt(),
                        buf.readVarInt(),
                        buf.readVarInt(),
                        buf.readVarLong(),
                        buf.readVarLong(),
                        buf.readVarInt(),
                        buf.readDouble(),
                        buf.readDouble(),
                        buf.readDouble(),
                        buf.readDouble(),
                        buf.readUtf(
                                64
                        ),
                        buf.readUtf(
                                1024
                        )
                );
    }

    public void toBytes(
            FriendlyByteBuf buf
    ) {
        buf.writeBlockPos(
                pos
        );

        buf.writeUtf(
                snapshot.localIp(),
                64
        );

        buf.writeUtf(
                snapshot.localMac(),
                64
        );

        buf.writeUtf(
                snapshot.peerIp(),
                64
        );

        buf.writeUtf(
                snapshot.peerMac(),
                64
        );

        buf.writeVarInt(
                snapshot.neighborCount()
        );

        buf.writeVarInt(
                snapshot.txPackets()
        );

        buf.writeVarInt(
                snapshot.rxPackets()
        );

        buf.writeVarLong(
                snapshot.txBytes()
        );

        buf.writeVarLong(
                snapshot.rxBytes()
        );

        buf.writeVarInt(
                snapshot.lostPackets()
        );

        buf.writeDouble(
                snapshot.lastRttMs()
        );

        buf.writeDouble(
                snapshot.averageRttMs()
        );

        buf.writeDouble(
                snapshot.jitterMs()
        );

        buf.writeDouble(
                snapshot.goodputKbps()
        );

        buf.writeUtf(
                snapshot.lastProtocol(),
                64
        );

        buf.writeUtf(
                snapshot.status(),
                1024
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
                                                        .handleIpSnapshot(
                                                                pos,
                                                                snapshot
                                                        )
                        )
        );

        context.setPacketHandled(
                true
        );
    }
}

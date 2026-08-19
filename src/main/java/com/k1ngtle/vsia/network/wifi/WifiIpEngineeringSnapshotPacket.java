package com.k1ngtle.vsia.network.wifi;

import com.k1ngtle.vsia.client.wifi.WifiEngineeringClientPacketHandler;
import com.k1ngtle.vsia.signality.engineering.wifi.ip.WifiIpEngineeringSnapshot;
import com.k1ngtle.vsia.signality.engineering.wifi.ip.router.RouterEngineeringSnapshot;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public final class WifiIpEngineeringSnapshotPacket {
    private final BlockPos pos;
    private final WifiIpEngineeringSnapshot snapshot;

    public WifiIpEngineeringSnapshotPacket(
            BlockPos pos,
            WifiIpEngineeringSnapshot snapshot
    ) {
        this.pos = pos.immutable();
        this.snapshot = snapshot;
    }

    public WifiIpEngineeringSnapshotPacket(
            FriendlyByteBuf buf
    ) {
        pos = buf.readBlockPos();

        snapshot = new WifiIpEngineeringSnapshot(
                buf.readUtf(64),
                buf.readUtf(64),
                buf.readUtf(64),
                buf.readUtf(64),
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
                buf.readUtf(64),
                buf.readUtf(64),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarLong(),
                buf.readVarLong(),
                buf.readVarLong(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readVarInt(),
                buf.readUtf(1024),
                buf.readUtf(1024),
                readRouterSnapshot(buf)
        );
    }

    public void toBytes(
            FriendlyByteBuf buf
    ) {
        buf.writeBlockPos(pos);
        buf.writeUtf(snapshot.localIp(),64);
        buf.writeUtf(snapshot.localMac(),64);
        buf.writeUtf(snapshot.peerIp(),64);
        buf.writeUtf(snapshot.peerMac(),64);
        buf.writeVarInt(snapshot.neighborCount());
        buf.writeVarInt(snapshot.txPackets());
        buf.writeVarInt(snapshot.rxPackets());
        buf.writeVarLong(snapshot.txBytes());
        buf.writeVarLong(snapshot.rxBytes());
        buf.writeVarInt(snapshot.lostPackets());
        buf.writeDouble(snapshot.lastRttMs());
        buf.writeDouble(snapshot.averageRttMs());
        buf.writeDouble(snapshot.jitterMs());
        buf.writeDouble(snapshot.goodputKbps());
        buf.writeUtf(snapshot.lastProtocol(),64);
        buf.writeUtf(snapshot.tcpState(),64);
        buf.writeVarInt(snapshot.tcpLocalPort());
        buf.writeVarInt(snapshot.tcpRemotePort());
        buf.writeVarLong(snapshot.tcpCongestionWindowBytes());
        buf.writeVarLong(snapshot.tcpSlowStartThresholdBytes());
        buf.writeVarLong(snapshot.tcpBytesInFlight());
        buf.writeDouble(snapshot.tcpSrttMs());
        buf.writeDouble(snapshot.tcpRtoMs());
        buf.writeVarInt(snapshot.tcpRetransmissions());
        buf.writeUtf(snapshot.tcpStatus(),1024);
        buf.writeUtf(snapshot.status(),1024);
        writeRouterSnapshot(buf,snapshot.router());
    }

    private static void writeRouterSnapshot(
            FriendlyByteBuf buf,
            RouterEngineeringSnapshot router
    ) {
        RouterEngineeringSnapshot value =
                router == null
                        ? RouterEngineeringSnapshot.empty()
                        : router;

        buf.writeBoolean(value.enabled());
        writeStrings(buf,value.interfaces());
        writeStrings(buf,value.routes());
        buf.writeVarInt(value.neighborCount());
        writeStrings(buf,value.diagnostics());
    }

    private static RouterEngineeringSnapshot readRouterSnapshot(
            FriendlyByteBuf buf
    ) {
        return new RouterEngineeringSnapshot(
                buf.readBoolean(),
                readStrings(buf),
                readStrings(buf),
                buf.readVarInt(),
                readStrings(buf)
        );
    }

    private static void writeStrings(
            FriendlyByteBuf buf,
            List<String> values
    ) {
        List<String> safe =
                values == null
                        ? List.of()
                        : values;

        buf.writeVarInt(safe.size());

        for (String value : safe) {
            buf.writeUtf(
                    value == null ? "" : value,
                    512
            );
        }
    }

    private static List<String> readStrings(
            FriendlyByteBuf buf
    ) {
        int size =
                Math.max(
                        0,
                        Math.min(
                                64,
                                buf.readVarInt()
                        )
                );

        List<String> out =
                new ArrayList<>(size);

        for (int i = 0; i < size; i++) {
            out.add(buf.readUtf(512));
        }

        return List.copyOf(out);
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

        context.setPacketHandled(true);
    }
}

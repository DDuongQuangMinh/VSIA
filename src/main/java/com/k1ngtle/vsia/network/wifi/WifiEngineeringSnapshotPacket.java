package com.k1ngtle.vsia.network.wifi;

import com.k1ngtle.vsia.client.wifi.WifiEngineeringClientPacketHandler;
import com.k1ngtle.vsia.signality.engineering.wifi.instrument.WifiEngineeringSnapshot;
import com.k1ngtle.vsia.signality.engineering.wifi.instrument.WifiEngineeringSnapshotCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public final class WifiEngineeringSnapshotPacket {
    private final BlockPos pos;
    private final WifiEngineeringSnapshot snapshot;
    private final boolean openScreen;

    public WifiEngineeringSnapshotPacket(
            BlockPos pos,
            WifiEngineeringSnapshot snapshot,
            boolean openScreen
    ) {
        this.pos =
                pos.immutable();

        this.snapshot =
                snapshot;

        this.openScreen =
                openScreen;
    }

    public WifiEngineeringSnapshotPacket(
            FriendlyByteBuf buf
    ) {
        this.pos =
                buf.readBlockPos();

        this.openScreen =
                buf.readBoolean();

        this.snapshot =
                WifiEngineeringSnapshotCodec.read(
                        buf
                );
    }

    public void toBytes(
            FriendlyByteBuf buf
    ) {
        buf.writeBlockPos(
                pos
        );

        buf.writeBoolean(
                openScreen
        );

        WifiEngineeringSnapshotCodec.write(
                buf,
                snapshot
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
                                                        .handleSnapshot(
                                                                pos,
                                                                snapshot,
                                                                openScreen
                                                        )
                        )
        );

        context.setPacketHandled(
                true
        );
    }

    public BlockPos pos() {
        return pos;
    }

    public WifiEngineeringSnapshot snapshot() {
        return snapshot;
    }

    public boolean openScreen() {
        return openScreen;
    }
}

package com.k1ngtle.vsia.network.wifi;

import com.k1ngtle.vsia.client.wifi.WifiEngineeringClientPacketHandler;
import com.k1ngtle.vsia.signality.engineering.wifi.instrument.WifiEngineeringSnapshot;
import com.k1ngtle.vsia.signality.engineering.wifi.instrument.WifiEngineeringSnapshotCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public final class WifiMultiEngineeringOpenPacket {
    public static final int DEVICE_COUNT = 4;

    private final List<BlockPos> positions;
    private final List<WifiEngineeringSnapshot> snapshots;

    public WifiMultiEngineeringOpenPacket(
            List<BlockPos> positions,
            List<WifiEngineeringSnapshot> snapshots
    ) {
        if (positions == null
                || snapshots == null
                || positions.size() != DEVICE_COUNT
                || snapshots.size() != DEVICE_COUNT) {
            throw new IllegalArgumentException(
                    "W1.23 multi analyzer requires exactly four positions and four snapshots"
            );
        }

        this.positions = positions.stream()
                .map(BlockPos::immutable)
                .toList();
        this.snapshots = List.copyOf(snapshots);
    }

    public WifiMultiEngineeringOpenPacket(FriendlyByteBuf buf) {
        List<BlockPos> decodedPositions =
                new ArrayList<>(DEVICE_COUNT);
        List<WifiEngineeringSnapshot> decodedSnapshots =
                new ArrayList<>(DEVICE_COUNT);

        for (int index = 0; index < DEVICE_COUNT; index++) {
            decodedPositions.add(
                    buf.readBlockPos()
            );
            decodedSnapshots.add(
                    WifiEngineeringSnapshotCodec.read(buf)
            );
        }

        this.positions = List.copyOf(decodedPositions);
        this.snapshots = List.copyOf(decodedSnapshots);
    }

    public void toBytes(FriendlyByteBuf buf) {
        for (int index = 0; index < DEVICE_COUNT; index++) {
            buf.writeBlockPos(positions.get(index));
            WifiEngineeringSnapshotCodec.write(
                    buf,
                    snapshots.get(index)
            );
        }
    }

    public void handle(
            Supplier<NetworkEvent.Context> supplier
    ) {
        NetworkEvent.Context context = supplier.get();

        context.enqueueWork(
                () -> DistExecutor.unsafeRunWhenOn(
                        Dist.CLIENT,
                        () -> () -> WifiEngineeringClientPacketHandler
                                .handleMultiOpen(
                                        positions,
                                        snapshots
                                )
                )
        );

        context.setPacketHandled(true);
    }
}

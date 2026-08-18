package com.k1ngtle.vsia.network.wifi;

import com.k1ngtle.vsia.client.wifi.WifiEngineeringClientPacketHandler;
import com.k1ngtle.vsia.signality.engineering.wifi.WifiMode;
import com.k1ngtle.vsia.signality.engineering.wifi.WifiSecurityState;
import com.k1ngtle.vsia.signality.engineering.wifi.WifiStationState;
import com.k1ngtle.vsia.signality.engineering.wifi.workflow.WifiEngineeringWorkflowSnapshot;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public final class WifiEngineeringWorkflowSnapshotPacket {
    private static final int MAX_LIST_ENTRIES =
            64;

    private final BlockPos pos;
    private final WifiEngineeringWorkflowSnapshot snapshot;

    public WifiEngineeringWorkflowSnapshotPacket(
            BlockPos pos,
            WifiEngineeringWorkflowSnapshot snapshot
    ) {
        this.pos =
                pos.immutable();

        this.snapshot =
                snapshot;
    }

    public WifiEngineeringWorkflowSnapshotPacket(
            FriendlyByteBuf buf
    ) {
        this.pos =
                buf.readBlockPos();

        String macAddress =
                buf.readUtf(
                        64
                );

        WifiMode mode =
                buf.readEnum(
                        WifiMode.class
                );

        WifiStationState stationState =
                buf.readEnum(
                        WifiStationState.class
                );

        WifiSecurityState securityState =
                buf.readEnum(
                        WifiSecurityState.class
                );

        List<String> discovered =
                readStrings(
                        buf
                );

        List<String> associated =
                readStrings(
                        buf
                );

        int pending =
                buf.readVarInt();

        String status =
                buf.readUtf(
                        1024
                );

        this.snapshot =
                new WifiEngineeringWorkflowSnapshot(
                        macAddress,
                        mode,
                        stationState,
                        securityState,
                        discovered,
                        associated,
                        pending,
                        status
                );
    }

    public void toBytes(
            FriendlyByteBuf buf
    ) {
        buf.writeBlockPos(pos);
        buf.writeUtf(
                snapshot.macAddress(),
                64
        );
        buf.writeEnum(snapshot.mode());
        buf.writeEnum(snapshot.stationState());
        buf.writeEnum(snapshot.securityState());

        writeStrings(
                buf,
                snapshot.discoveredSsids()
        );

        writeStrings(
                buf,
                snapshot.associatedStations()
        );

        buf.writeVarInt(
                snapshot.pendingDataTransmissions()
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
                                                        .handleWorkflowSnapshot(
                                                                pos,
                                                                snapshot
                                                        )
                        )
        );

        context.setPacketHandled(true);
    }

    private static void writeStrings(
            FriendlyByteBuf buf,
            List<String> values
    ) {
        int count =
                Math.min(
                        MAX_LIST_ENTRIES,
                        values == null
                                ? 0
                                : values.size()
                );

        buf.writeVarInt(count);

        for (int i = 0; i < count; i++) {
            buf.writeUtf(
                    values.get(i),
                    256
            );
        }
    }

    private static List<String> readStrings(
            FriendlyByteBuf buf
    ) {
        int count =
                Math.min(
                        MAX_LIST_ENTRIES,
                        Math.max(
                                0,
                                buf.readVarInt()
                        )
                );

        List<String> out =
                new ArrayList<>(count);

        for (int i = 0; i < count; i++) {
            out.add(
                    buf.readUtf(
                            256
                    )
            );
        }

        return List.copyOf(out);
    }
}

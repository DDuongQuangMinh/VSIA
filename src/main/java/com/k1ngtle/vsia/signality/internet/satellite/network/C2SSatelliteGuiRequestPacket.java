package com.k1ngtle.vsia.signality.internet.satellite.network;

import com.k1ngtle.vsia.signality.internet.field.FieldDeviceNetwork;
import com.k1ngtle.vsia.signality.internet.satellite.gui.SatelliteGuiServer;
import com.k1ngtle.vsia.signality.internet.satellite.gui.SatelliteGuiSnapshot;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record C2SSatelliteGuiRequestPacket(
        BlockPos pos
) {
    public C2SSatelliteGuiRequestPacket(
            FriendlyByteBuf buffer
    ) {
        this(
                buffer.readBlockPos()
        );
    }

    public void toBytes(
            FriendlyByteBuf buffer
    ) {
        buffer.writeBlockPos(
                pos
        );
    }

    public void handle(
            Supplier<NetworkEvent.Context> supplier
    ) {
        NetworkEvent.Context context =
                supplier.get();

        context.enqueueWork(() -> {
            ServerPlayer player =
                    context.getSender();

            if (player == null) {
                return;
            }

            SatelliteGuiSnapshot snapshot =
                    SatelliteGuiServer
                            .snapshot(
                                    player,
                                    pos
                            );

            FieldDeviceNetwork
                    .sendToPlayer(
                            player,
                            new S2CSatelliteGuiSnapshotPacket(
                                    snapshot
                            )
                    );
        });

        context.setPacketHandled(
                true
        );
    }
}

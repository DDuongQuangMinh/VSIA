package com.k1ngtle.vsia.signality.internet.satellite.network;

import com.k1ngtle.vsia.signality.internet.satellite.client.ClientSatelliteHooks;
import com.k1ngtle.vsia.signality.internet.satellite.gui.SatelliteGuiSnapshot;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record S2CSatelliteGuiSnapshotPacket(
        SatelliteGuiSnapshot snapshot
) {
    public S2CSatelliteGuiSnapshotPacket(
            FriendlyByteBuf buffer
    ) {
        this(
                SatelliteGuiSnapshot.decode(
                        buffer
                )
        );
    }

    public void toBytes(
            FriendlyByteBuf buffer
    ) {
        snapshot.encode(
                buffer
        );
    }

    public void handle(
            Supplier<NetworkEvent.Context> supplier
    ) {
        NetworkEvent.Context context =
                supplier.get();

        context.enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(
                        Dist.CLIENT,
                        () -> () ->
                                ClientSatelliteHooks
                                        .acceptSnapshot(
                                                snapshot
                                        )
                )
        );

        context.setPacketHandled(
                true
        );
    }
}

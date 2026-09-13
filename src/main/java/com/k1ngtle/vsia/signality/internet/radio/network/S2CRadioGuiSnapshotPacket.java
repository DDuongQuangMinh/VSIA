package com.k1ngtle.vsia.signality.internet.radio.network;

import com.k1ngtle.vsia.signality.internet.radio.client.ClientRadioHooks;
import com.k1ngtle.vsia.signality.internet.radio.gui.RadioGuiSnapshot;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record S2CRadioGuiSnapshotPacket(
        RadioGuiSnapshot snapshot
) {
    public S2CRadioGuiSnapshotPacket(
            FriendlyByteBuf buffer
    ) {
        this(
                RadioGuiSnapshot.decode(
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
                                ClientRadioHooks
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

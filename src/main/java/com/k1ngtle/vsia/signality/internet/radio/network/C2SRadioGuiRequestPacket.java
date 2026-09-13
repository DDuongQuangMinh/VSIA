package com.k1ngtle.vsia.signality.internet.radio.network;

import com.k1ngtle.vsia.signality.internet.field.FieldDeviceNetwork;
import com.k1ngtle.vsia.signality.internet.radio.gui.RadioGuiServer;
import com.k1ngtle.vsia.signality.internet.radio.gui.RadioGuiSnapshot;
import com.k1ngtle.vsia.signality.internet.radio.gui.RadioGuiTarget;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record C2SRadioGuiRequestPacket(
        RadioGuiTarget target
) {
    public C2SRadioGuiRequestPacket(
            FriendlyByteBuf buffer
    ) {
        this(
                RadioGuiTarget.decode(
                        buffer
                )
        );
    }

    public void toBytes(
            FriendlyByteBuf buffer
    ) {
        target.encode(
                buffer
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

            RadioGuiSnapshot snapshot =
                    RadioGuiServer.snapshot(
                            player,
                            target
                    );

            FieldDeviceNetwork.sendToPlayer(
                    player,
                    new S2CRadioGuiSnapshotPacket(
                            snapshot
                    )
            );
        });

        context.setPacketHandled(
                true
        );
    }
}

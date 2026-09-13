package com.k1ngtle.vsia.signality.internet.radio.voice.network;

import com.k1ngtle.vsia.signality.internet.radio.portable.PortableRadioEndpoint;
import com.k1ngtle.vsia.signality.internet.radio.portable.PortableRadioService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record C2SRadioVoicePttPacket(
        InteractionHand hand,
        int sessionId,
        boolean pressed
) {
    public C2SRadioVoicePttPacket(
            FriendlyByteBuf buffer
    ) {
        this(
                buffer.readEnum(
                        InteractionHand.class
                ),
                buffer.readVarInt(),
                buffer.readBoolean()
        );
    }

    public void toBytes(
            FriendlyByteBuf buffer
    ) {
        buffer.writeEnum(
                hand
        );

        buffer.writeVarInt(
                sessionId
        );

        buffer.writeBoolean(
                pressed
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

            PortableRadioEndpoint endpoint =
                    PortableRadioService.endpoint(
                            player,
                            hand
                    );

            if (endpoint == null) {
                return;
            }

            if (pressed) {
                endpoint.beginLivePtt(
                        sessionId
                );
            } else {
                endpoint.endLivePtt(
                        sessionId
                );
            }
        });

        context.setPacketHandled(
                true
        );
    }
}

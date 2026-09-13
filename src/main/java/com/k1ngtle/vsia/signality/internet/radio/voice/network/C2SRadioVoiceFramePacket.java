package com.k1ngtle.vsia.signality.internet.radio.voice.network;

import com.k1ngtle.vsia.signality.internet.radio.portable.PortableRadioEndpoint;
import com.k1ngtle.vsia.signality.internet.radio.portable.PortableRadioService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record C2SRadioVoiceFramePacket(
        InteractionHand hand,
        int sessionId,
        int sequenceNumber,
        byte[] encodedAudio
) {
    public static final int MAX_AUDIO_BYTES =
            512;

    public C2SRadioVoiceFramePacket(
            FriendlyByteBuf buffer
    ) {
        this(
                buffer.readEnum(
                        InteractionHand.class
                ),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readByteArray(
                        MAX_AUDIO_BYTES
                )
        );
    }

    public C2SRadioVoiceFramePacket {
        encodedAudio =
                encodedAudio == null
                        ? new byte[0]
                        : encodedAudio.clone();

        if (encodedAudio.length
                > MAX_AUDIO_BYTES) {
            throw new IllegalArgumentException(
                    "Radio voice frame is too large"
            );
        }
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

        buffer.writeVarInt(
                sequenceNumber
        );

        buffer.writeByteArray(
                encodedAudio
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

            endpoint.transmitLiveVoiceFrame(
                    sessionId,
                    sequenceNumber,
                    encodedAudio
            );
        });

        context.setPacketHandled(
                true
        );
    }
}

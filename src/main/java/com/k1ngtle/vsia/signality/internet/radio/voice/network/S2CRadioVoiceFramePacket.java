package com.k1ngtle.vsia.signality.internet.radio.voice.network;

import com.k1ngtle.vsia.signality.internet.radio.voice.client.RadioVoiceClient;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public record S2CRadioVoiceFramePacket(
        UUID sourceRadioId,
        int sequenceNumber,
        byte[] encodedAudio,
        boolean endOfTransmission,
        double snrDb,
        double intelligibility,
        String emission
) {
    private static final int MAX_AUDIO_BYTES =
            512;

    public S2CRadioVoiceFramePacket(
            FriendlyByteBuf buffer
    ) {
        this(
                buffer.readUUID(),
                buffer.readVarInt(),
                buffer.readByteArray(
                        MAX_AUDIO_BYTES
                ),
                buffer.readBoolean(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readUtf(
                        32
                )
        );
    }

    public S2CRadioVoiceFramePacket {
        encodedAudio =
                encodedAudio == null
                        ? new byte[0]
                        : encodedAudio.clone();

        emission =
                emission == null
                        ? ""
                        : emission;
    }

    public void toBytes(
            FriendlyByteBuf buffer
    ) {
        buffer.writeUUID(
                sourceRadioId
        );

        buffer.writeVarInt(
                sequenceNumber
        );

        buffer.writeByteArray(
                encodedAudio
        );

        buffer.writeBoolean(
                endOfTransmission
        );

        buffer.writeDouble(
                snrDb
        );

        buffer.writeDouble(
                intelligibility
        );

        buffer.writeUtf(
                emission,
                32
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
                                RadioVoiceClient
                                        .get()
                                        .receiveFrame(
                                                sourceRadioId,
                                                sequenceNumber,
                                                encodedAudio,
                                                endOfTransmission,
                                                snrDb,
                                                intelligibility,
                                                emission
                                        )
                )
        );

        context.setPacketHandled(
                true
        );
    }
}

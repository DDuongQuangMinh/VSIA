package com.k1ngtle.vsia.signality.internet.radio.gui;

import net.minecraft.network.FriendlyByteBuf;

public record RadioGuiSnapshot(
        RadioGuiTarget target,
        boolean valid,
        String sourceLabel,
        String profileId,
        String mode,
        String band,
        double frequencyHz,
        double bandwidthHz,
        String emission,
        double squelchDb,
        boolean meshEnabled,
        boolean fhssEnabled,
        boolean pttPressed,
        boolean measured,
        double receivedPowerDbm,
        double snrDb,
        double intelligibility,
        double packetSuccessProbability,
        boolean squelchOpen,
        String lastVoice,
        String status
) {
    public RadioGuiSnapshot {
        sourceLabel = safe(sourceLabel);
        profileId = safe(profileId);
        mode = safe(mode);
        band = safe(band);
        emission = safe(emission);
        lastVoice = safe(lastVoice);
        status = safe(status);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    public void encode(FriendlyByteBuf buffer) {
        target.encode(buffer);
        buffer.writeBoolean(valid);
        buffer.writeUtf(sourceLabel, 128);
        buffer.writeUtf(profileId, 256);
        buffer.writeUtf(mode, 64);
        buffer.writeUtf(band, 32);
        buffer.writeDouble(frequencyHz);
        buffer.writeDouble(bandwidthHz);
        buffer.writeUtf(emission, 32);
        buffer.writeDouble(squelchDb);
        buffer.writeBoolean(meshEnabled);
        buffer.writeBoolean(fhssEnabled);
        buffer.writeBoolean(pttPressed);
        buffer.writeBoolean(measured);
        buffer.writeDouble(receivedPowerDbm);
        buffer.writeDouble(snrDb);
        buffer.writeDouble(intelligibility);
        buffer.writeDouble(packetSuccessProbability);
        buffer.writeBoolean(squelchOpen);
        buffer.writeUtf(lastVoice, 1024);
        buffer.writeUtf(status, 512);
    }

    public static RadioGuiSnapshot decode(FriendlyByteBuf buffer) {
        return new RadioGuiSnapshot(
                RadioGuiTarget.decode(buffer),
                buffer.readBoolean(),
                buffer.readUtf(128),
                buffer.readUtf(256),
                buffer.readUtf(64),
                buffer.readUtf(32),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readUtf(32),
                buffer.readDouble(),
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readBoolean(),
                buffer.readUtf(1024),
                buffer.readUtf(512)
        );
    }

    public static RadioGuiSnapshot invalid(
            RadioGuiTarget target,
            String status
    ) {
        return new RadioGuiSnapshot(
                target,
                false,
                "",
                "",
                "",
                "",
                0.0,
                0.0,
                "",
                0.0,
                false,
                false,
                false,
                false,
                Double.NEGATIVE_INFINITY,
                Double.NEGATIVE_INFINITY,
                0.0,
                0.0,
                false,
                "",
                status
        );
    }
}

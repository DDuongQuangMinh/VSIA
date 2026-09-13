package com.k1ngtle.vsia.signality.internet.radio.voice.client;

import com.k1ngtle.vsia.signality.internet.field.FieldDeviceNetwork;
import com.k1ngtle.vsia.signality.internet.radio.device.TemporaryRadioItem;
import com.k1ngtle.vsia.signality.internet.radio.voice.network.C2SRadioVoiceFramePacket;
import com.k1ngtle.vsia.signality.internet.radio.voice.network.C2SRadioVoicePttPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public final class RadioVoiceClient {
    private static final RadioVoiceClient INSTANCE =
            new RadioVoiceClient();

    private final RadioMicrophoneCapture microphone =
            new RadioMicrophoneCapture();

    private final RadioAudioPlayback playback =
            new RadioAudioPlayback();

    private final AtomicInteger nextSession =
            new AtomicInteger(
                    1
            );

    private boolean transmitting;
    private InteractionHand transmitHand =
            InteractionHand.MAIN_HAND;

    private int sessionId;
    private int sequence;

    private RadioVoiceClient() {
    }

    public static RadioVoiceClient get() {
        return INSTANCE;
    }

    public void tick() {
        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.player == null
                || minecraft.level == null) {
            stopTransmit(
                    false
            );

            return;
        }

        InteractionHand radioHand =
                activeRadioHand();

        boolean pttDown =
                RadioVoiceKeyMappings.PTT
                        .isDown();

        if (pttDown
                && !transmitting
                && radioHand != null) {
            startTransmit(
                    radioHand
            );

            return;
        }

        if (transmitting
                && (
                !pttDown
                        || radioHand
                        != transmitHand
        )) {
            stopTransmit(
                    true
            );
        }
    }

    private void startTransmit(
            InteractionHand hand
    ) {
        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.player == null) {
            return;
        }

        int newSession =
                nextSession.getAndIncrement();

        sequence = 0;

        boolean microphoneStarted =
                microphone.start(
                        frame -> {
                            int frameSequence =
                                    sequence++;

                            Minecraft.getInstance()
                                    .execute(() -> {
                                        if (!transmitting
                                                || sessionId
                                                != newSession) {
                                            return;
                                        }

                                        FieldDeviceNetwork
                                                .sendToServer(
                                                        new C2SRadioVoiceFramePacket(
                                                                transmitHand,
                                                                newSession,
                                                                frameSequence,
                                                                frame
                                                        )
                                                );
                                    });
                        }
                );

        if (!microphoneStarted) {
            minecraft.player
                    .displayClientMessage(
                            Component.literal(
                                    "[Radio] Microphone unavailable: "
                                            + microphone.lastError()
                            ),
                            true
                    );

            return;
        }

        transmitting =
                true;

        transmitHand =
                hand;

        sessionId =
                newSession;

        FieldDeviceNetwork.sendToServer(
                new C2SRadioVoicePttPacket(
                        hand,
                        newSession,
                        true
                )
        );

        minecraft.player
                .displayClientMessage(
                        Component.literal(
                                "[Radio] PTT transmit"
                        ),
                        true
                );
    }

    private void stopTransmit(
            boolean notifyServer
    ) {
        if (!transmitting
                && !microphone.running()) {
            return;
        }

        int endingSession =
                sessionId;

        InteractionHand endingHand =
                transmitHand;

        transmitting =
                false;

        microphone.stop();

        if (notifyServer) {
            FieldDeviceNetwork.sendToServer(
                    new C2SRadioVoicePttPacket(
                            endingHand,
                            endingSession,
                            false
                    )
            );
        }

        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.player != null
                && notifyServer) {
            minecraft.player
                    .displayClientMessage(
                            Component.literal(
                                    "[Radio] PTT released"
                            ),
                            true
                    );
        }
    }

    public void receiveFrame(
            UUID sourceRadioId,
            int sequenceNumber,
            byte[] encodedAudio,
            boolean endOfTransmission,
            double snrDb,
            double intelligibility,
            String emission
    ) {
        playback.enqueue(
                encodedAudio,
                endOfTransmission,
                intelligibility,
                emission
        );
    }

    public boolean transmitting() {
        return transmitting;
    }

    public String pttKeyName() {
        return RadioVoiceKeyMappings.PTT
                .getTranslatedKeyMessage()
                .getString();
    }

    private InteractionHand activeRadioHand() {
        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.player == null) {
            return null;
        }

        ItemStack main =
                minecraft.player
                        .getMainHandItem();

        if (main.getItem()
                instanceof TemporaryRadioItem) {
            return InteractionHand.MAIN_HAND;
        }

        ItemStack off =
                minecraft.player
                        .getOffhandItem();

        if (off.getItem()
                instanceof TemporaryRadioItem) {
            return InteractionHand.OFF_HAND;
        }

        return null;
    }
}

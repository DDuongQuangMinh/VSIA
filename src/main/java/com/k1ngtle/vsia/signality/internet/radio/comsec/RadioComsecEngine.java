package com.k1ngtle.vsia.signality.internet.radio.comsec;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.UUID;

public final class RadioComsecEngine {
    public static final String ALGORITHM =
            "AES-256-GCM";

    private static final SecureRandom RANDOM =
            new SecureRandom();

    private RadioComsecEngine() {
    }

    public static CompoundTag protectVoice(
            ServerLevel level,
            UUID sourceRadioId,
            CompoundTag clearMessage,
            int keySlot,
            double frequencyHz
    ) {
        if (level == null
                || sourceRadioId == null
                || clearMessage == null) {
            throw new IllegalArgumentException(
                    "level/sourceRadioId/message"
            );
        }

        RadioComsecKeyStoreSavedData.KeyMaterial material =
                RadioComsecKeyStoreSavedData
                        .get(level)
                        .material(
                                keySlot
                        );

        CompoundTag protectedMessage =
                clearMessage.copy();

        int sequence =
                sequenceOf(
                        clearMessage
                );

        byte[] nonce =
                new byte[12];

        RANDOM.nextBytes(
                nonce
        );

        byte[] clear =
                clearMessage.getByteArray(
                        "voice_data"
                );

        byte[] aad =
                aad(
                        sourceRadioId,
                        material.slot(),
                        material.epoch(),
                        sequence,
                        frequencyHz,
                        clearMessage
                );

        byte[] ciphertext =
                crypt(
                        Cipher.ENCRYPT_MODE,
                        material.key(),
                        nonce,
                        aad,
                        clear
                );

        protectedMessage.putBoolean(
                "comsec_secure",
                true
        );

        protectedMessage.putString(
                "comsec_algorithm",
                ALGORITHM
        );

        protectedMessage.putInt(
                "comsec_key_slot",
                material.slot()
        );

        protectedMessage.putInt(
                "comsec_key_epoch",
                material.epoch()
        );

        protectedMessage.putUUID(
                "comsec_source",
                sourceRadioId
        );

        protectedMessage.putDouble(
                "comsec_frequency_hz",
                frequencyHz
        );

        protectedMessage.putByteArray(
                "comsec_nonce",
                nonce
        );

        protectedMessage.putByteArray(
                "voice_data",
                ciphertext
        );

        return protectedMessage;
    }

    public static IncomingVoiceResult prepareIncomingVoice(
            ServerLevel level,
            CompoundTag incoming,
            boolean receiverSecure,
            int receiverKeySlot
    ) {
        if (incoming == null) {
            return IncomingVoiceResult.reject(
                    "COMSEC: invalid voice frame"
            );
        }

        boolean senderSecure =
                incoming.getBoolean(
                        "comsec_secure"
                );

        if (!senderSecure) {
            if (receiverSecure) {
                return IncomingVoiceResult.reject(
                        "COMSEC: CLEAR traffic rejected in secure mode"
                );
            }

            return IncomingVoiceResult.accept(
                    incoming.copy(),
                    "CLEAR"
            );
        }

        if (!receiverSecure) {
            return IncomingVoiceResult.reject(
                    "COMSEC: encrypted traffic received while radio is in CLEAR"
            );
        }

        int senderSlot =
                incoming.getInt(
                        "comsec_key_slot"
                );

        if (senderSlot != RadioComsecKeyStoreSavedData
                .normalizeSlot(
                        receiverKeySlot
                )) {
            return IncomingVoiceResult.reject(
                    "COMSEC: key slot mismatch"
            );
        }

        if (!ALGORITHM.equals(
                incoming.getString(
                        "comsec_algorithm"
                )
        )) {
            return IncomingVoiceResult.reject(
                    "COMSEC: unsupported algorithm"
            );
        }

        RadioComsecKeyStoreSavedData.KeyMaterial material =
                RadioComsecKeyStoreSavedData
                        .get(level)
                        .material(
                                receiverKeySlot
                        );

        int senderEpoch =
                incoming.getInt(
                        "comsec_key_epoch"
                );

        if (senderEpoch != material.epoch()) {
            return IncomingVoiceResult.reject(
                    "COMSEC: key epoch mismatch"
            );
        }

        if (!incoming.hasUUID(
                "comsec_source"
        )) {
            return IncomingVoiceResult.reject(
                    "COMSEC: missing source identity"
            );
        }

        UUID source =
                incoming.getUUID(
                        "comsec_source"
                );

        double frequencyHz =
                incoming.getDouble(
                        "comsec_frequency_hz"
                );

        byte[] nonce =
                incoming.getByteArray(
                        "comsec_nonce"
                );

        if (nonce.length != 12) {
            return IncomingVoiceResult.reject(
                    "COMSEC: invalid nonce"
            );
        }

        int sequence =
                sequenceOf(
                        incoming
                );

        byte[] aad =
                aad(
                        source,
                        material.slot(),
                        material.epoch(),
                        sequence,
                        frequencyHz,
                        incoming
                );

        byte[] clear;

        try {
            clear =
                    crypt(
                            Cipher.DECRYPT_MODE,
                            material.key(),
                            nonce,
                            aad,
                            incoming.getByteArray(
                                    "voice_data"
                            )
                    );
        } catch (RuntimeException exception) {
            return IncomingVoiceResult.reject(
                    "COMSEC: authentication/decryption failed"
            );
        }

        CompoundTag clearMessage =
                incoming.copy();

        clearMessage.putByteArray(
                "voice_data",
                clear
        );

        return IncomingVoiceResult.accept(
                clearMessage,
                material.keyId()
        );
    }

    public static boolean selfTest(
            ServerLevel level,
            int keySlot
    ) {
        CompoundTag message =
                new CompoundTag();

        message.putString(
                "radio_message_type",
                "VOICE"
        );

        message.putString(
                "voice_stream_codec",
                "G711_MULAW_8K"
        );

        message.putInt(
                "voice_stream_seq",
                7
        );

        message.putBoolean(
                "end_of_transmission",
                false
        );

        byte[] sample =
                "VSIA-COMSEC-SELFTEST"
                        .getBytes(
                                StandardCharsets.UTF_8
                        );

        message.putByteArray(
                "voice_data",
                sample
        );

        UUID source =
                UUID.randomUUID();

        CompoundTag protectedMessage =
                protectVoice(
                        level,
                        source,
                        message,
                        keySlot,
                        150_000_000.0
                );

        IncomingVoiceResult result =
                prepareIncomingVoice(
                        level,
                        protectedMessage,
                        true,
                        keySlot
                );

        if (!result.accepted()) {
            return false;
        }

        return java.util.Arrays.equals(
                sample,
                result.message()
                        .getByteArray(
                                "voice_data"
                        )
        );
    }

    public static boolean mismatchSelfTest(
            ServerLevel level,
            int transmitSlot,
            int receiveSlot
    ) {
        CompoundTag message =
                new CompoundTag();

        message.putString(
                "radio_message_type",
                "VOICE"
        );

        message.putString(
                "voice_stream_codec",
                "G711_MULAW_8K"
        );

        message.putInt(
                "voice_stream_seq",
                11
        );

        message.putBoolean(
                "end_of_transmission",
                false
        );

        message.putByteArray(
                "voice_data",
                "VSIA-COMSEC-NEGATIVE-TEST"
                        .getBytes(
                                StandardCharsets.UTF_8
                        )
        );

        CompoundTag protectedMessage =
                protectVoice(
                        level,
                        UUID.randomUUID(),
                        message,
                        transmitSlot,
                        150_000_000.0
                );

        IncomingVoiceResult result =
                prepareIncomingVoice(
                        level,
                        protectedMessage,
                        true,
                        receiveSlot
                );

        return !result.accepted();
    }

    private static int sequenceOf(
            CompoundTag message
    ) {
        if (message.contains(
                "voice_stream_seq"
        )) {
            return message.getInt(
                    "voice_stream_seq"
            );
        }

        return message.getInt(
                "voice_sequence"
        );
    }

    private static byte[] aad(
            UUID source,
            int slot,
            int epoch,
            int sequence,
            double frequencyHz,
            CompoundTag message
    ) {
        byte[] type =
                message.getString(
                        "radio_message_type"
                )
                        .getBytes(
                                StandardCharsets.UTF_8
                        );

        byte[] codec =
                message.getString(
                        "voice_stream_codec"
                )
                        .getBytes(
                                StandardCharsets.UTF_8
                        );

        ByteBuffer buffer =
                ByteBuffer.allocate(
                        16
                                + 4
                                + 4
                                + 4
                                + 8
                                + 1
                                + 4
                                + type.length
                                + 4
                                + codec.length
                );

        buffer.putLong(
                source.getMostSignificantBits()
        );

        buffer.putLong(
                source.getLeastSignificantBits()
        );

        buffer.putInt(
                slot
        );

        buffer.putInt(
                epoch
        );

        buffer.putInt(
                sequence
        );

        buffer.putLong(
                Double.doubleToLongBits(
                        frequencyHz
                )
        );

        buffer.put(
                (byte) (
                        message.getBoolean(
                                "end_of_transmission"
                        )
                                ? 1
                                : 0
                )
        );

        buffer.putInt(
                type.length
        );

        buffer.put(
                type
        );

        buffer.putInt(
                codec.length
        );

        buffer.put(
                codec
        );

        return buffer.array();
    }

    private static byte[] crypt(
            int mode,
            byte[] key,
            byte[] nonce,
            byte[] aad,
            byte[] input
    ) {
        try {
            Cipher cipher =
                    Cipher.getInstance(
                            "AES/GCM/NoPadding"
                    );

            cipher.init(
                    mode,
                    new SecretKeySpec(
                            key,
                            "AES"
                    ),
                    new GCMParameterSpec(
                            128,
                            nonce
                    )
            );

            cipher.updateAAD(
                    aad
            );

            return cipher.doFinal(
                    input == null
                            ? new byte[0]
                            : input
            );
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "COMSEC AES-GCM operation failed",
                    exception
            );
        }
    }

    public record IncomingVoiceResult(
            boolean accepted,
            CompoundTag message,
            String status
    ) {
        public static IncomingVoiceResult accept(
                CompoundTag message,
                String status
        ) {
            return new IncomingVoiceResult(
                    true,
                    message,
                    status == null
                            ? ""
                            : status
            );
        }

        public static IncomingVoiceResult reject(
                String status
        ) {
            return new IncomingVoiceResult(
                    false,
                    new CompoundTag(),
                    status == null
                            ? "COMSEC rejected"
                            : status
            );
        }
    }
}

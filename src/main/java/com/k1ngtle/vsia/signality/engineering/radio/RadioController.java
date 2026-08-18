package com.k1ngtle.vsia.signality.engineering.radio;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

public final class RadioController {

    @FunctionalInterface
    public interface Sender {
        void send(
                CompoundTag message,
                double frequencyHz
        );
    }

    private static final UUID BROADCAST_ID =
            new UUID(
                    0L,
                    0L
            );

    private final PttController ptt =
            new PttController();

    private final MeshRoutingController mesh =
            new MeshRoutingController();

    private final Random random =
            new Random();

    private final Set<String> seenPackets =
            new HashSet<>();

    private RadioMode mode =
            RadioMode.LEGACY_DIRECT;

    private RadioChannel channel;

    private RepeaterConfig repeaterConfig;

    private FrequencyHopPlan hopPlan;

    private boolean meshEnabled;

    private double squelchSnrThresholdDb =
            2.0;

    private byte[] securityKey =
            new byte[0];

    private int voiceSequence;
    private int packetSequence;

    private RadioLinkQuality lastLinkQuality =
            new RadioLinkQuality(
                    Double.NEGATIVE_INFINITY,
                    Double.NEGATIVE_INFINITY,
                    0.0,
                    1.0,
                    0.0,
                    false
            );

    private byte[] lastReceivedVoice =
            new byte[0];

    public RadioMode mode() {
        return mode;
    }

    public RadioChannel channel() {
        return channel;
    }

    public boolean meshEnabled() {
        return meshEnabled;
    }

    public boolean pttPressed() {
        return ptt.transmitting();
    }

    public RadioLinkQuality lastLinkQuality() {
        return lastLinkQuality;
    }

    public byte[] lastReceivedVoice() {
        return lastReceivedVoice.clone();
    }

    public void useLegacyDirectMode() {
        mode =
                RadioMode.LEGACY_DIRECT;

        channel =
                null;

        repeaterConfig =
                null;

        hopPlan =
                null;

        meshEnabled =
                false;

        ptt.release();
    }

    public void configureTransceiver(
            RadioChannel channel
    ) {
        if (channel == null) {
            throw new IllegalArgumentException(
                    "channel"
            );
        }

        mode =
                RadioMode.TRANSCEIVER;

        this.channel =
                channel;

        repeaterConfig =
                null;

        ptt.release();
    }

    public void configureRepeater(
            RadioChannel channel,
            RepeaterConfig repeaterConfig
    ) {
        if (channel == null
                || repeaterConfig == null) {
            throw new IllegalArgumentException(
                    "channel/repeaterConfig"
            );
        }

        mode =
                RadioMode.REPEATER;

        this.channel =
                channel;

        this.repeaterConfig =
                repeaterConfig;

        ptt.release();
    }

    public void setSquelchSnrThresholdDb(
            double thresholdDb
    ) {
        squelchSnrThresholdDb =
                thresholdDb;
    }

    public void setSecurityKey(
            byte[] key
    ) {
        securityKey =
                key == null
                        ? new byte[0]
                        : key.clone();
    }

    public void enableMesh(
            boolean enabled
    ) {
        meshEnabled =
                enabled;
    }

    public void enableFrequencyHopping(
            double[] frequenciesHz,
            long seed
    ) {
        hopPlan =
                new FrequencyHopPlan(
                        frequenciesHz,
                        seed
                );
    }

    public void disableFrequencyHopping() {
        hopPlan =
                null;
    }

    public double[] receiveFrequenciesHz() {
        if (channel == null) {
            return new double[0];
        }

        if (hopPlan != null) {
            return new double[]{
                    hopPlan.currentFrequencyHz()
            };
        }

        if (mode == RadioMode.REPEATER
                && repeaterConfig != null) {
            return new double[]{
                    repeaterConfig.inputFrequencyHz()
            };
        }

        return new double[]{
                channel.frequencyHz()
        };
    }

    public double receiveBandwidthHz() {
        return channel == null
                ? 0.0
                : channel.bandwidthHz();
    }

    public double currentTransmitFrequencyHz() {
        if (hopPlan != null) {
            return hopPlan
                    .currentFrequencyHz();
        }

        if (mode == RadioMode.REPEATER
                && repeaterConfig != null) {
            return repeaterConfig
                    .outputFrequencyHz();
        }

        if (channel != null) {
            return channel
                    .frequencyHz();
        }

        return 0.0;
    }

    public double advanceHop() {
        if (hopPlan == null) {
            return currentTransmitFrequencyHz();
        }

        return hopPlan.advance();
    }

    public boolean pressPtt() {
        if (mode != RadioMode.TRANSCEIVER) {
            return false;
        }

        return ptt.press();
    }

    public void releasePtt() {
        ptt.release();
    }

    public boolean sendVoice(
            UUID ownId,
            byte[] encodedAudio,
            boolean endOfTransmission,
            Sender sender
    ) {
        if (mode != RadioMode.TRANSCEIVER
                || !ptt.transmitting()
                || channel == null) {
            return false;
        }

        RadioVoiceFrame frame =
                new RadioVoiceFrame(
                        voiceSequence++,
                        encodedAudio,
                        endOfTransmission
                );

        CompoundTag message =
                baseMessage(
                        RadioMessageType.VOICE,
                        ownId
                );

        message.putInt(
                "voice_sequence",
                frame.sequenceNumber()
        );

        message.putByteArray(
                "voice_data",
                frame.encodedAudio()
        );

        message.putBoolean(
                "end_of_transmission",
                frame.endOfTransmission()
        );

        sender.send(
                message,
                txFrequency()
        );

        if (endOfTransmission) {
            ptt.release();
        }

        advanceAfterTransmit();

        return true;
    }

    public boolean sendPacket(
            UUID ownId,
            UUID destinationId,
            CompoundTag payload,
            Sender sender
    ) {
        if (mode != RadioMode.TRANSCEIVER
                || channel == null) {
            return false;
        }

        UUID destination =
                destinationId == null
                        ? BROADCAST_ID
                        : destinationId;

        byte[] clear =
                serialize(
                        payload
                );

        byte[] protectedPayload =
                RadioSecurityEngine.protect(
                        securityKey,
                        clear
                );

        PacketRadioFrame frame =
                new PacketRadioFrame(
                        ownId,
                        destination,
                        packetSequence++,
                        16,
                        protectedPayload
                );

        sendPacketFrame(
                frame,
                RadioMessageType.PACKET,
                ownId,
                sender
        );

        return true;
    }

    public void discoverRoute(
            UUID ownId,
            UUID destination,
            Sender sender
    ) {
        if (!meshEnabled
                || mode
                != RadioMode.TRANSCEIVER) {
            return;
        }

        CompoundTag request =
                mesh.createRouteRequest(
                        ownId,
                        destination
                );

        attachCommon(
                request,
                ownId
        );

        sender.send(
                request,
                txFrequency()
        );

        advanceAfterTransmit();
    }

    public CompoundTag receive(
            UUID ownId,
            CompoundTag message,
            double actualFrequencyHz,
            double receivedPowerDbm,
            double snrDb,
            Sender sender
    ) {
        if (!message.contains(
                "radio_message_type"
        )) {
            return null;
        }

        if (!acceptFrequency(
                actualFrequencyHz
        )) {
            return null;
        }

        if (!acceptAccessCode(
                message
        )) {
            return null;
        }

        if (message.contains(
                "mesh_next_hop"
        )
                && !message.getUUID(
                "mesh_next_hop"
        ).equals(
                ownId
        )) {
            return null;
        }

        lastLinkQuality =
                RadioSquelchEngine.evaluate(
                        receivedPowerDbm,
                        snrDb,
                        squelchSnrThresholdDb
                );

        if (!lastLinkQuality
                .squelchOpen()) {
            return null;
        }

        RadioMessageType type;

        try {
            type =
                    RadioMessageType.valueOf(
                            message.getString(
                                    "radio_message_type"
                            )
                    );
        } catch (Exception ignored) {
            return null;
        }

        if (mode == RadioMode.REPEATER) {
            repeatIfApplicable(
                    ownId,
                    message,
                    actualFrequencyHz,
                    sender
            );

            return null;
        }

        switch (type) {
            case VOICE -> {
                lastReceivedVoice =
                        RadioVoiceModel.degrade(
                                message.getByteArray(
                                        "voice_data"
                                ),
                                channel.emission(),
                                lastLinkQuality,
                                random
                        );

                return null;
            }

            case PACKET,
                 REPEATER_FORWARD -> {
                return receivePacket(
                        ownId,
                        message,
                        sender
                );
            }

            case ROUTE_REQUEST -> {
                handleRouteRequest(
                        ownId,
                        message,
                        sender
                );
            }

            case ROUTE_REPLY -> {
                handleRouteReply(
                        ownId,
                        message,
                        sender
                );
            }

            case ROUTE_ERROR,
                 BEACON -> {
            }
        }

        return null;
    }

    private CompoundTag receivePacket(
            UUID ownId,
            CompoundTag message,
            Sender sender
    ) {
        PacketRadioFrame frame;

        try {
            frame =
                    PacketRadioFrame.decode(
                            message.getByteArray(
                                    "packet_frame"
                            )
                    );
        } catch (Exception ignored) {
            return null;
        }

        String packetId =
                frame.sourceId()
                        + ":"
                        + frame.sequenceNumber();

        if (!seenPackets.add(
                packetId
        )) {
            return null;
        }

        boolean forThisNode =
                frame.broadcast()
                        || frame.destinationId()
                        .equals(
                                ownId
                        );

        if (forThisNode) {
            try {
                byte[] clear =
                        RadioSecurityEngine.unprotect(
                                securityKey,
                                frame.payload()
                        );

                return deserialize(
                        clear
                );
            } catch (Exception ignored) {
                return null;
            }
        }

        if (meshEnabled
                && frame.ttl() > 1) {
            PacketRadioFrame forwarded =
                    frame.decrementTtl();

            sendPacketFrame(
                    forwarded,
                    RadioMessageType.PACKET,
                    ownId,
                    sender
            );
        }

        return null;
    }

    private void repeatIfApplicable(
            UUID ownId,
            CompoundTag message,
            double actualFrequencyHz,
            Sender sender
    ) {
        if (repeaterConfig == null) {
            return;
        }

        double halfBandwidth =
                channel == null
                        ? 12_500.0
                        : channel.bandwidthHz()
                        / 2.0;

        if (Math.abs(
                actualFrequencyHz
                        - repeaterConfig.inputFrequencyHz()
        ) > halfBandwidth) {
            return;
        }

        if (message.getBoolean(
                "repeated"
        )) {
            return;
        }

        CompoundTag forwarded =
                message.copy();

        forwarded.putBoolean(
                "repeated",
                true
        );

        forwarded.putUUID(
                "repeater_id",
                ownId
        );

        sender.send(
                forwarded,
                repeaterConfig
                        .outputFrequencyHz()
        );
    }

    private void handleRouteRequest(
            UUID ownId,
            CompoundTag message,
            Sender sender
    ) {
        if (!meshEnabled) {
            return;
        }

        UUID origin =
                message.getUUID(
                        "origin"
                );

        UUID destination =
                message.getUUID(
                        "destination"
                );

        int requestId =
                message.getInt(
                        "request_id"
                );

        int hopCount =
                message.getInt(
                        "hop_count"
                );

        if (!mesh.markAndCheckNewRequest(
                origin,
                requestId
        )) {
            return;
        }

        UUID previousHop =
                message.getUUID(
                        "sender_id"
                );

        mesh.learnReverseRoute(
                origin,
                previousHop,
                hopCount + 1,
                message.getInt(
                        "origin_sequence"
                )
        );

        if (destination.equals(
                ownId
        )) {
            CompoundTag reply =
                    new CompoundTag();

            reply.putString(
                    "radio_message_type",
                    RadioMessageType.ROUTE_REPLY.name()
            );

            reply.putUUID(
                    "origin",
                    origin
            );

            reply.putUUID(
                    "destination",
                    ownId
            );

            reply.putInt(
                    "destination_sequence",
                    mesh.ownSequenceNumber()
            );

            reply.putInt(
                    "hop_count",
                    0
            );

            attachCommon(
                    reply,
                    ownId
            );

            RadioRoute reverse =
                    mesh.routes()
                            .routeTo(
                                    origin
                            );

            if (reverse == null) {
                return;
            }

            reply.putUUID(
                    "mesh_next_hop",
                    reverse.nextHop()
            );

            sender.send(
                    reply,
                    txFrequency()
            );

            advanceAfterTransmit();

            return;
        }

        CompoundTag forwarded =
                message.copy();

        forwarded.putInt(
                "hop_count",
                hopCount + 1
        );

        forwarded.putUUID(
                "sender_id",
                ownId
        );

        sender.send(
                forwarded,
                txFrequency()
        );

        advanceAfterTransmit();
    }

    private void handleRouteReply(
            UUID ownId,
            CompoundTag message,
            Sender sender
    ) {
        if (!meshEnabled) {
            return;
        }

        UUID origin =
                message.getUUID(
                        "origin"
                );

        UUID destination =
                message.getUUID(
                        "destination"
                );

        UUID previousHop =
                message.getUUID(
                        "sender_id"
                );

        int hopCount =
                message.getInt(
                        "hop_count"
                );

        mesh.learnForwardRoute(
                destination,
                previousHop,
                hopCount + 1,
                message.getInt(
                        "destination_sequence"
                )
        );

        if (origin.equals(
                ownId
        )) {
            return;
        }

        RadioRoute reverse =
                mesh.routes()
                        .routeTo(
                                origin
                        );

        if (reverse == null) {
            return;
        }

        CompoundTag forwarded =
                message.copy();

        forwarded.putInt(
                "hop_count",
                hopCount + 1
        );

        forwarded.putUUID(
                "sender_id",
                ownId
        );

        forwarded.putUUID(
                "mesh_next_hop",
                reverse.nextHop()
        );

        sender.send(
                forwarded,
                txFrequency()
        );

        advanceAfterTransmit();
    }

    private void sendPacketFrame(
            PacketRadioFrame frame,
            RadioMessageType type,
            UUID senderId,
            Sender sender
    ) {
        CompoundTag message =
                baseMessage(
                        type,
                        senderId
                );

        message.putByteArray(
                "packet_frame",
                frame.encode()
        );

        sender.send(
                message,
                txFrequency()
        );

        advanceAfterTransmit();
    }

    private CompoundTag baseMessage(
            RadioMessageType type,
            UUID senderId
    ) {
        CompoundTag message =
                new CompoundTag();

        message.putString(
                "radio_message_type",
                type.name()
        );

        attachCommon(
                message,
                senderId
        );

        return message;
    }

    private void attachCommon(
            CompoundTag message,
            UUID senderId
    ) {
        message.putUUID(
                "sender_id",
                senderId
        );

        if (channel != null) {
            message.putString(
                    "radio_channel_id",
                    channel.id()
            );

            message.putString(
                    "radio_emission",
                    channel.emission()
                            .name()
            );

            message.putString(
                    "access_code",
                    channel.accessCode()
            );
        }

        if (hopPlan != null) {
            message.putInt(
                    "hop_index",
                    hopPlan.hopIndex()
            );
        }
    }

    private boolean acceptFrequency(
            double frequencyHz
    ) {
        if (channel == null) {
            return false;
        }

        if (hopPlan != null) {
            return Math.abs(
                    frequencyHz
                            - hopPlan.currentFrequencyHz()
            ) <= channel.bandwidthHz()
                    / 2.0;
        }

        double target =
                mode == RadioMode.REPEATER
                        && repeaterConfig != null
                        ? repeaterConfig.inputFrequencyHz()
                        : channel.frequencyHz();

        return Math.abs(
                frequencyHz - target
        ) <= channel.bandwidthHz()
                / 2.0;
    }

    private boolean acceptAccessCode(
            CompoundTag message
    ) {
        String required =
                mode == RadioMode.REPEATER
                        && repeaterConfig != null
                        ? repeaterConfig.accessCode()
                        : channel == null
                        ? ""
                        : channel.accessCode();

        if (required == null
                || required.isBlank()) {
            return true;
        }

        return required.equals(
                message.getString(
                        "access_code"
                )
        );
    }

    private double txFrequency() {
        double frequency =
                currentTransmitFrequencyHz();

        if (frequency <= 0.0
                && channel != null) {
            return channel.frequencyHz();
        }

        return frequency;
    }

    private void advanceAfterTransmit() {
        if (hopPlan != null) {
            hopPlan.advance();
        }
    }

    private static byte[] serialize(
            CompoundTag tag
    ) {
        try {
            ByteArrayOutputStream bytes =
                    new ByteArrayOutputStream();

            NbtIo.write(
                    tag,
                    new DataOutputStream(
                            bytes
                    )
            );

            return bytes.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Unable to serialize packet-radio payload",
                    exception
            );
        }
    }

    private static CompoundTag deserialize(
            byte[] bytes
    ) {
        try {
            CompoundTag tag =
                    NbtIo.read(
                            new DataInputStream(
                                    new ByteArrayInputStream(
                                            bytes
                                    )
                            )
                    );

            return tag == null
                    ? new CompoundTag()
                    : tag;
        } catch (Exception exception) {
            throw new IllegalArgumentException(
                    "Unable to deserialize packet-radio payload",
                    exception
            );
        }
    }

    public CompoundTag save() {
        CompoundTag tag =
                new CompoundTag();

        tag.putString(
                "Mode",
                mode.name()
        );

        tag.putDouble(
                "SquelchSnrThresholdDb",
                squelchSnrThresholdDb
        );

        tag.putBoolean(
                "MeshEnabled",
                meshEnabled
        );

        if (channel != null) {
            CompoundTag channelTag =
                    new CompoundTag();

            channelTag.putString(
                    "Id",
                    channel.id()
            );

            channelTag.putDouble(
                    "FrequencyHz",
                    channel.frequencyHz()
            );

            channelTag.putDouble(
                    "BandwidthHz",
                    channel.bandwidthHz()
            );

            channelTag.putString(
                    "Emission",
                    channel.emission()
                            .name()
            );

            channelTag.putString(
                    "AccessCode",
                    channel.accessCode()
            );

            tag.put(
                    "Channel",
                    channelTag
            );
        }

        if (repeaterConfig != null) {
            CompoundTag repeater =
                    new CompoundTag();

            repeater.putDouble(
                    "InputFrequencyHz",
                    repeaterConfig.inputFrequencyHz()
            );

            repeater.putDouble(
                    "OutputFrequencyHz",
                    repeaterConfig.outputFrequencyHz()
            );

            repeater.putString(
                    "AccessCode",
                    repeaterConfig.accessCode()
            );

            tag.put(
                    "Repeater",
                    repeater
            );
        }

        return tag;
    }

    public void load(
            CompoundTag tag
    ) {
        try {
            mode =
                    RadioMode.valueOf(
                            tag.getString(
                                    "Mode"
                            )
                    );
        } catch (Exception ignored) {
            mode =
                    RadioMode.LEGACY_DIRECT;
        }

        squelchSnrThresholdDb =
                tag.contains(
                        "SquelchSnrThresholdDb"
                )
                        ? tag.getDouble(
                        "SquelchSnrThresholdDb"
                )
                        : 2.0;

        meshEnabled =
                tag.getBoolean(
                        "MeshEnabled"
                );

        if (tag.contains(
                "Channel"
        )) {
            CompoundTag channelTag =
                    tag.getCompound(
                            "Channel"
                    );

            RadioEmission emission;

            try {
                emission =
                        RadioEmission.valueOf(
                                channelTag.getString(
                                        "Emission"
                                )
                        );
            } catch (Exception ignored) {
                emission =
                        RadioEmission.DIGITAL;
            }

            channel =
                    new RadioChannel(
                            channelTag.getString(
                                    "Id"
                            ),
                            channelTag.getDouble(
                                    "FrequencyHz"
                            ),
                            channelTag.getDouble(
                                    "BandwidthHz"
                            ),
                            emission,
                            channelTag.getString(
                                    "AccessCode"
                            )
                    );
        }

        if (tag.contains(
                "Repeater"
        )) {
            CompoundTag repeater =
                    tag.getCompound(
                            "Repeater"
                    );

            repeaterConfig =
                    new RepeaterConfig(
                            repeater.getDouble(
                                    "InputFrequencyHz"
                            ),
                            repeater.getDouble(
                                    "OutputFrequencyHz"
                            ),
                            repeater.getString(
                                    "AccessCode"
                            )
                    );
        }

        securityKey =
                new byte[0];

        hopPlan =
                null;

        ptt.release();

        lastReceivedVoice =
                new byte[0];
    }
}

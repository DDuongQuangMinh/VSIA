package com.k1ngtle.vsia.signality.internet.radio.portable;

import com.k1ngtle.vsia.signality.api.signal.ISignalReceiver;
import com.k1ngtle.vsia.signality.api.signal.ISignalTransmitter;
import com.k1ngtle.vsia.signality.api.signal.SignalBand;
import com.k1ngtle.vsia.signality.api.signal.SignalPacket;
import com.k1ngtle.vsia.signality.core.signal.SignalBus;
import com.k1ngtle.vsia.signality.engineering.channel.ActiveRfTransmission;
import com.k1ngtle.vsia.signality.engineering.channel.RfAntennaState;
import com.k1ngtle.vsia.signality.engineering.channel.RfChannelSettings;
import com.k1ngtle.vsia.signality.engineering.channel.RfDiscreteEventScheduler;
import com.k1ngtle.vsia.signality.engineering.channel.RfKinematicTracker;
import com.k1ngtle.vsia.signality.engineering.channel.ScheduledRfTransmission;
import com.k1ngtle.vsia.signality.engineering.radio.RadioController;
import com.k1ngtle.vsia.signality.engineering.radio.RadioLinkQuality;
import com.k1ngtle.vsia.signality.engineering.reality.GeneralRfAirtimeModel;
import com.k1ngtle.vsia.signality.engineering.reality.NetworkTimebase;
import com.k1ngtle.vsia.signality.engineering.reality.RfMicroTiming;
import com.k1ngtle.vsia.signality.engineering.reality.RfMicroTimingRegistry;
import com.k1ngtle.vsia.signality.internet.network.NetworkProfile;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public final class PortableRadioEndpoint
        implements ISignalReceiver, ISignalTransmitter {

    private final ServerPlayer player;
    private final InteractionHand hand;

    private UUID id;
    private RadioController controller;
    private long configuredRevision = Long.MIN_VALUE;

    public PortableRadioEndpoint(
            ServerPlayer player,
            InteractionHand hand
    ) {
        this.player = player;
        this.hand = hand;
        refresh();
    }

    public void refresh() {
        ItemStack stack = stack();

        if (stack.isEmpty()) {
            return;
        }

        id = PortableRadioState.id(stack);

        long revision =
                PortableRadioState.revision(
                        stack
                );

        if (controller == null
                || configuredRevision
                != revision) {
            controller =
                    PortableRadioState.controller(
                            stack
                    );

            configuredRevision =
                    revision;
        }
    }

    public boolean valid() {
        ItemStack stack = stack();

        return !stack.isEmpty()
                && stack.getItem()
                instanceof com.k1ngtle.vsia.signality.internet.radio.device.TemporaryRadioItem;
    }

    public InteractionHand hand() {
        return hand;
    }

    public ItemStack stack() {
        return player.getItemInHand(hand);
    }

    @Override
    public UUID id() {
        return id;
    }

    @Override
    public ServerLevel level() {
        return player.serverLevel();
    }

    @Override
    public Vec3 positionWorld() {
        return player.position()
                .add(
                        0.0,
                        player.getBbHeight() * 0.75,
                        0.0
                );
    }

    @Override
    public SignalBand band() {
        return SignalBand.forFrequency(
                PortableRadioState.frequencyHz(
                        stack()
                )
        );
    }

    @Override
    public double antennaGain() {
        return PortableRadioState
                .profile(stack())
                .antennaGain();
    }

    @Override
    public double sensitivityWatts() {
        return PortableRadioState
                .profile(stack())
                .sensitivityWatts();
    }

    @Override
    public double[] tunedFrequenciesHz() {
        refresh();
        return controller.receiveFrequenciesHz();
    }

    @Override
    public double tuningBandwidthHz() {
        return PortableRadioState.bandwidthHz(
                stack()
        );
    }

    @Override
    public double maximumReceptionRangeBlocks() {
        return PortableRadioState
                .profile(stack())
                .maximumRangeBlocks();
    }

    @Override
    public void onReceive(
            SignalPacket signal,
            double receivedPowerWatts
    ) {
        ItemStack stack = stack();

        if (!valid()) {
            return;
        }

        CompoundTag envelope =
                decode(
                        signal.payload()
                );

        if (envelope == null
                || !envelope.contains(
                "radio_message"
        )) {
            return;
        }

        String expectedMedium =
                PortableRadioState
                        .profile(stack)
                        .compatibilityGroup();

        if (envelope.contains(
                "signality_medium"
        )
                && !expectedMedium.equals(
                envelope.getString(
                        "signality_medium"
                )
        )) {
            return;
        }

        refresh();

        double powerDbm =
                wattsToDbm(
                        receivedPowerWatts
                );

        double bandwidthHz =
                Math.max(
                        1.0,
                        PortableRadioState
                                .bandwidthHz(stack)
                );

        double noiseDbm =
                -174.0
                        + 10.0
                        * Math.log10(
                        bandwidthHz
                )
                        + 6.0;

        double snrDb =
                powerDbm - noiseDbm;

        CompoundTag radioMessage =
                envelope.getCompound(
                        "radio_message"
                );

        controller.receive(
                id,
                radioMessage,
                signal.frequencyHz(),
                powerDbm,
                snrDb,
                this::transmitRadioMessage
        );

        RadioLinkQuality quality =
                controller.lastLinkQuality();

        PortableRadioState.storeLink(
                stack,
                quality.receivedPowerDbm(),
                quality.snrDb(),
                quality.intelligibility(),
                quality.packetSuccessProbability(),
                quality.squelchOpen()
        );

        if ("VOICE".equalsIgnoreCase(
                radioMessage.getString(
                        "radio_message_type"
                )
        )
                && quality.squelchOpen()) {

            String voice =
                    new String(
                            controller.lastReceivedVoice(),
                            StandardCharsets.UTF_8
                    );

            if (!voice.isBlank()) {
                PortableRadioState.storeVoice(
                        stack,
                        voice
                );

                player.displayClientMessage(
                        Component.literal(
                                "[Portable Radio] "
                                        + voice
                        ),
                        true
                );
            }
        }
    }

    public boolean transmitVoice(
            String text
    ) {
        if (!valid()) {
            return false;
        }

        refresh();

        if (!controller.pressPtt()) {
            return false;
        }

        boolean sent =
                controller.sendVoice(
                        id,
                        (
                                text == null
                                        ? ""
                                        : text
                        ).getBytes(
                                StandardCharsets.UTF_8
                        ),
                        true,
                        this::transmitRadioMessage
                );

        controller.releasePtt();

        if (sent) {
            PortableRadioState.status(
                    stack(),
                    "PTT transmission queued"
            );
        }

        return sent;
    }

    public boolean transmitPacketTest() {
        if (!valid()) {
            return false;
        }

        refresh();

        CompoundTag payload =
                new CompoundTag();

        payload.putString(
                "text",
                "Portable radio packet test"
        );

        boolean sent =
                controller.sendPacket(
                        id,
                        null,
                        payload,
                        this::transmitRadioMessage
                );

        if (sent) {
            PortableRadioState.status(
                    stack(),
                    "Broadcast packet queued"
            );
        }

        return sent;
    }

    private void transmitRadioMessage(
            CompoundTag radioMessage,
            double frequencyHz
    ) {
        ItemStack stack = stack();

        NetworkProfile profile =
                PortableRadioState.profile(
                        stack
                );

        CompoundTag envelope =
                new CompoundTag();

        envelope.putString(
                "signality_network_profile",
                profile.id().toString()
        );

        envelope.putString(
                "signality_medium",
                profile.compatibilityGroup()
        );

        envelope.putString(
                "signality_protocol",
                profile.protocol()
        );

        envelope.putString(
                "signality_security",
                profile.security()
        );

        envelope.put(
                "radio_message",
                radioMessage
        );

        UUID transmissionId =
                UUID.randomUUID();

        envelope.putUUID(
                "rf_tx_id",
                transmissionId
        );

        byte[] payload =
                encode(
                        envelope
                );

        if (payload.length == 0) {
            return;
        }

        ServerLevel level =
                player.serverLevel();

        Vec3 position =
                positionWorld();

        SignalPacket packet =
                new SignalPacket(
                        id,
                        position,
                        frequencyHz,
                        profile.transmitPowerWatts(),
                        profile.antennaGain(),
                        payload,
                        System.nanoTime(),
                        64,
                        null
                );

        long payloadBits =
                Math.max(
                        1L,
                        (long) payload.length
                                * 8L
                );

        long airtimeMicros =
                GeneralRfAirtimeModel
                        .estimateMicros(
                                payloadBits,
                                PortableRadioState
                                        .bandwidthHz(stack)
                        );

        long microStart =
                NetworkTimebase
                        .nowMicros(level);

        long microEnd =
                microStart
                        + airtimeMicros
                        - 1L;

        RfMicroTimingRegistry.register(
                new RfMicroTiming(
                        transmissionId,
                        level.dimension()
                                .location()
                                .toString(),
                        frequencyHz,
                        PortableRadioState
                                .bandwidthHz(stack),
                        microStart,
                        microEnd
                )
        );

        long currentTick =
                level.getGameTime();

        long airtimeTicks =
                Math.max(
                        1L,
                        (long) Math.ceil(
                                airtimeMicros
                                        / (double) NetworkTimebase
                                        .MICROS_PER_SERVER_TICK
                        )
                );

        long startTick =
                currentTick
                        + Math.max(
                        1L,
                        RfChannelSettings
                                .MIN_EVENT_LATENCY_TICKS
                );

        ActiveRfTransmission metadata =
                new ActiveRfTransmission(
                        transmissionId,
                        id,
                        level.dimension()
                                .location()
                                .toString(),
                        position,
                        frequencyHz,
                        PortableRadioState
                                .bandwidthHz(stack),
                        profile.transmitPowerWatts(),
                        profile.antennaGain(),
                        RfAntennaState.isotropic(),
                        RfKinematicTracker
                                .updateAndGetVelocityMetersPerSecond(
                                        id,
                                        position,
                                        currentTick
                                ),
                        startTick,
                        startTick
                                + airtimeTicks
                                - 1L,
                        payloadBits
                );

        RfDiscreteEventScheduler.schedule(
                new ScheduledRfTransmission(
                        metadata,
                        packet,
                        level
                )
        );
    }

    public void unregister() {
        SignalBus.unregisterReceiver(
                id,
                this
        );

        SignalBus.unregisterTransmitter(
                id,
                this
        );

        RfKinematicTracker.remove(
                id
        );
    }

    private static byte[] encode(
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
        } catch (Exception ignored) {
            return new byte[0];
        }
    }

    private static CompoundTag decode(
            byte[] bytes
    ) {
        try {
            return NbtIo.read(
                    new DataInputStream(
                            new ByteArrayInputStream(
                                    bytes
                            )
                    )
            );
        } catch (Exception ignored) {
            return null;
        }
    }

    private static double wattsToDbm(
            double watts
    ) {
        if (!(watts > 0.0)) {
            return Double.NEGATIVE_INFINITY;
        }

        return 10.0
                * Math.log10(
                watts * 1000.0
        );
    }
}

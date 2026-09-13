package com.k1ngtle.vsia.signality.internet.radio.gui;

import com.k1ngtle.vsia.signality.engineering.radio.RadioLinkQuality;
import com.k1ngtle.vsia.signality.internet.radio.RadioBandPreset;
import com.k1ngtle.vsia.signality.internet.radio.device.TemporaryRadioBlockEntity;
import com.k1ngtle.vsia.signality.internet.radio.device.TemporaryRadioItem;
import com.k1ngtle.vsia.signality.internet.radio.portable.PortableRadioEndpoint;
import com.k1ngtle.vsia.signality.internet.radio.portable.PortableRadioService;
import com.k1ngtle.vsia.signality.internet.radio.portable.PortableRadioState;
import com.k1ngtle.vsia.signality.integration.vs.VsNetworkPosition;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.Locale;

public final class RadioGuiServer {
    private static final double MAX_BLOCK_GUI_DISTANCE_SQR =
            64.0D * 64.0D;

    private RadioGuiServer() {
    }

    public static RadioGuiSnapshot snapshot(
            ServerPlayer player,
            RadioGuiTarget target
    ) {
        if (player == null
                || target == null) {
            return RadioGuiSnapshot.invalid(
                    target == null
                            ? RadioGuiTarget.block(
                            BlockPos.ZERO
                    )
                            : target,
                    "Invalid radio target"
            );
        }

        if (target.isBlock()) {
            TemporaryRadioBlockEntity radio =
                    blockRadio(
                            player,
                            target.blockPos()
                    );

            if (radio == null) {
                return RadioGuiSnapshot.invalid(
                        target,
                        "Radio block is missing, unloaded, or too far away"
                );
            }

            RadioLinkQuality quality =
                    radio.lastRadioLinkQuality();

            boolean measured =
                    quality != null
                            && Double.isFinite(
                            quality.receivedPowerDbm()
                    )
                            && Double.isFinite(
                            quality.snrDb()
                    );

            return new RadioGuiSnapshot(
                    target,
                    true,
                    "Placed Radio "
                            + target.blockPos()
                            .toShortString(),
                    radio.networkProfileId()
                            .toString(),
                    radio.radioMode()
                            .name(),
                    radio.presetForProfile()
                            .name(),
                    radio.activeFrequencyHz(),
                    radio.currentPhyProfile()
                            .bandwidthHz(),
                    radio.guiEmission()
                            .name(),
                    radio.guiSquelchDb(),
                    radio.guiMeshEnabled(),
                    radio.guiFhssEnabled(),
                    false,
                    0,
                    "COMSEC available on portable radio",
                    false,
                    measured,
                    measured
                            ? quality.receivedPowerDbm()
                            : Double.NEGATIVE_INFINITY,
                    measured
                            ? quality.snrDb()
                            : Double.NEGATIVE_INFINITY,
                    quality == null
                            ? 0.0
                            : quality.intelligibility(),
                    quality == null
                            ? 0.0
                            : quality.packetSuccessProbability(),
                    quality != null
                            && quality.squelchOpen(),
                    radio.lastVoiceText(),
                    radio.guiStatus()
            );
        }

        ItemStack stack =
                player.getItemInHand(
                        target.hand()
                );

        if (!(stack.getItem()
                instanceof TemporaryRadioItem)) {
            return RadioGuiSnapshot.invalid(
                    target,
                    "The selected hand is not holding a VS:IA radio"
            );
        }

        boolean measured =
                Double.isFinite(
                        PortableRadioState
                                .lastPowerDbm(stack)
                )
                        && Double.isFinite(
                        PortableRadioState
                                .lastSnrDb(stack)
                );

        return new RadioGuiSnapshot(
                target,
                true,
                target.kind()
                        == RadioGuiTarget.Kind.MAIN_HAND
                        ? "Portable Radio - Main Hand"
                        : "Portable Radio - Off Hand",
                PortableRadioState
                        .profile(stack)
                        .id()
                        .toString(),
                "PORTABLE_TRANSCEIVER",
                PortableRadioState
                        .band(stack)
                        .name(),
                PortableRadioState
                        .frequencyHz(stack),
                PortableRadioState
                        .bandwidthHz(stack),
                PortableRadioState
                        .emission(stack)
                        .name(),
                PortableRadioState
                        .squelchDb(stack),
                PortableRadioState
                        .mesh(stack),
                PortableRadioState
                        .fhss(stack),
                PortableRadioState
                        .comsecEnabled(stack),
                PortableRadioState
                        .comsecSlot(stack),
                PortableRadioState
                        .cryptoStatus(stack),
                false,
                measured,
                measured
                        ? PortableRadioState
                        .lastPowerDbm(stack)
                        : Double.NEGATIVE_INFINITY,
                measured
                        ? PortableRadioState
                        .lastSnrDb(stack)
                        : Double.NEGATIVE_INFINITY,
                PortableRadioState
                        .lastIntelligibility(stack),
                PortableRadioState
                        .lastPacketProbability(stack),
                PortableRadioState
                        .lastSquelchOpen(stack),
                PortableRadioState
                        .lastVoice(stack),
                PortableRadioState
                        .status(stack)
        );
    }

    public static RadioGuiSnapshot apply(
            ServerPlayer player,
            RadioGuiTarget target,
            RadioGuiAction action
    ) {
        if (player == null
                || target == null
                || action == null) {
            return snapshot(
                    player,
                    target
            );
        }

        if (target.isBlock()) {
            TemporaryRadioBlockEntity radio =
                    blockRadio(
                            player,
                            target.blockPos()
                    );

            if (radio == null) {
                return RadioGuiSnapshot.invalid(
                        target,
                        "Radio block is missing, unloaded, or too far away"
                );
            }

            applyBlockAction(
                    radio,
                    action
            );

            return snapshot(
                    player,
                    target
            );
        }

        ItemStack stack =
                player.getItemInHand(
                        target.hand()
                );

        if (!(stack.getItem()
                instanceof TemporaryRadioItem)) {
            return RadioGuiSnapshot.invalid(
                    target,
                    "The selected hand is not holding a VS:IA radio"
            );
        }

        applyHeldAction(
                player,
                target,
                stack,
                action
        );

        return snapshot(
                player,
                target
        );
    }

    private static void applyBlockAction(
            TemporaryRadioBlockEntity radio,
            RadioGuiAction action
    ) {
        switch (action) {
            case REFRESH -> {
            }

            case BAND_HF ->
                    radio.applyPreset(
                            RadioBandPreset.HF
                    );

            case BAND_VHF ->
                    radio.applyPreset(
                            RadioBandPreset.VHF
                    );

            case BAND_UHF ->
                    radio.applyPreset(
                            RadioBandPreset.UHF
                    );

            case TUNE_DOWN ->
                    radio.tuneBy(
                            -radio.tuneStepHz()
                    );

            case TUNE_UP ->
                    radio.tuneBy(
                            radio.tuneStepHz()
                    );

            case EMISSION_CYCLE ->
                    radio.cycleEmission();

            case SQUELCH_DOWN ->
                    radio.adjustSquelch(
                            -1.0
                    );

            case SQUELCH_UP ->
                    radio.adjustSquelch(
                            1.0
                    );

            case MESH_TOGGLE ->
                    radio.toggleMesh();

            case FHSS_TOGGLE ->
                    radio.toggleFhss();

            case COMSEC_TOGGLE,
                 COMSEC_SLOT_NEXT -> {
            }

            case PTT_TEST ->
                    radio.pttTest();

            case PACKET_TEST ->
                    radio.packetTest();
        }
    }

    private static void applyHeldAction(
            ServerPlayer player,
            RadioGuiTarget target,
            ItemStack stack,
            RadioGuiAction action
    ) {
        switch (action) {
            case REFRESH -> {
            }

            case BAND_HF ->
                    PortableRadioState.applyBand(
                            stack,
                            RadioBandPreset.HF
                    );

            case BAND_VHF ->
                    PortableRadioState.applyBand(
                            stack,
                            RadioBandPreset.VHF
                    );

            case BAND_UHF ->
                    PortableRadioState.applyBand(
                            stack,
                            RadioBandPreset.UHF
                    );

            case TUNE_DOWN ->
                    PortableRadioState.tune(
                            stack,
                            -PortableRadioState
                                    .tuneStepHz(stack)
                    );

            case TUNE_UP ->
                    PortableRadioState.tune(
                            stack,
                            PortableRadioState
                                    .tuneStepHz(stack)
                    );

            case EMISSION_CYCLE ->
                    PortableRadioState
                            .cycleEmission(
                                    stack
                            );

            case SQUELCH_DOWN ->
                    PortableRadioState
                            .adjustSquelch(
                                    stack,
                                    -1.0
                            );

            case SQUELCH_UP ->
                    PortableRadioState
                            .adjustSquelch(
                                    stack,
                                    1.0
                            );

            case MESH_TOGGLE ->
                    PortableRadioState
                            .toggleMesh(
                                    stack
                            );

            case FHSS_TOGGLE ->
                    PortableRadioState
                            .toggleFhss(
                                    stack
                            );

            case COMSEC_TOGGLE ->
                    PortableRadioState
                            .toggleComsec(
                                    stack
                            );

            case COMSEC_SLOT_NEXT ->
                    PortableRadioState
                            .nextComsecSlot(
                                    stack
                            );

            case PTT_TEST -> {
                PortableRadioEndpoint endpoint =
                        PortableRadioService
                                .endpoint(
                                        player,
                                        target.hand()
                                );

                if (endpoint == null
                        || !endpoint.transmitVoice(
                        "Radio check one two"
                )) {
                    PortableRadioState.status(
                            stack,
                            "Portable PTT test failed"
                    );
                }
            }

            case PACKET_TEST -> {
                PortableRadioEndpoint endpoint =
                        PortableRadioService
                                .endpoint(
                                        player,
                                        target.hand()
                                );

                if (endpoint == null
                        || !endpoint
                        .transmitPacketTest()) {
                    PortableRadioState.status(
                            stack,
                            "Portable packet test failed"
                    );
                }
            }
        }

        PortableRadioService.tick(
                player
        );
    }

    private static TemporaryRadioBlockEntity blockRadio(
            ServerPlayer player,
            BlockPos pos
    ) {
        if (player == null
                || pos == null
                || !player.serverLevel()
                .hasChunkAt(pos)
                || player.position()
                .distanceToSqr(
                        VsNetworkPosition.blockCenterWorld(
                                player.serverLevel(),
                                pos
                        )
                ) > MAX_BLOCK_GUI_DISTANCE_SQR) {
            return null;
        }

        if (player.serverLevel()
                .getBlockEntity(pos)
                instanceof TemporaryRadioBlockEntity radio) {
            return radio;
        }

        return null;
    }

    public static String compactFrequency(
            double frequencyHz
    ) {
        return String.format(
                Locale.ROOT,
                "%.4f MHz",
                frequencyHz
                        / 1_000_000.0
        );
    }
}

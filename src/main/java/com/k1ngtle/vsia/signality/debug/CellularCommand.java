package com.k1ngtle.vsia.signality.debug;

import com.k1ngtle.vsia.signality.Signality;
import com.k1ngtle.vsia.signality.engineering.cellular.CellularBand;
import com.k1ngtle.vsia.signality.engineering.cellular.CellularBandCatalog;
import com.k1ngtle.vsia.signality.engineering.cellular.CellularGeneration;
import com.k1ngtle.vsia.signality.engineering.cellular.CellularGenerationCatalog;
import com.k1ngtle.vsia.signality.engineering.cellular.CellularGenerationProfile;
import com.k1ngtle.vsia.signality.engineering.cellular.CellularMeasurement;
import com.k1ngtle.vsia.signality.engineering.cellular.CellularMeasurementEngine;
import com.k1ngtle.vsia.signality.engineering.cellular.CellularProtocolConceptCatalog;
import com.k1ngtle.vsia.signality.engineering.cellular.CellularSelfTest;
import com.k1ngtle.vsia.signality.engineering.cellular.CellularSimProfile;
import com.k1ngtle.vsia.signality.engineering.cellular.core.PduSession;
import com.k1ngtle.vsia.signality.internet.NetworkDeviceBlockEntity;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.Locale;

public final class CellularCommand {
    private static final String CELLULAR_RECOVERY_RACH_RRC_NAS_V1 =
            "CELLULAR_RECOVERY_RACH_RRC_NAS_V1";

    private CellularCommand() {
    }

    public static void register(
            CommandDispatcher<CommandSourceStack> dispatcher
    ) {
        dispatcher.register(
                Commands.literal("vsiacellular")
                        .requires(source -> source.hasPermission(2))
                        .executes(context -> help(context.getSource()))
                        .then(
                                Commands.literal("help")
                                        .executes(context -> help(context.getSource()))
                        )
                        .then(
                                Commands.literal("selftest")
                                        .executes(context -> selftest(context.getSource()))
                        )
                        .then(
                                Commands.literal("status")
                                        .executes(context -> status(context.getSource()))
                        )
                        .then(
                                Commands.literal("auto")
                                        .then(
                                                Commands.argument(
                                                                "enabled",
                                                                BoolArgumentType.bool()
                                                        )
                                                        .executes(
                                                                context -> setAutomation(
                                                                        context.getSource(),
                                                                        BoolArgumentType.getBool(
                                                                                context,
                                                                                "enabled"
                                                                        )
                                                                )
                                                        )
                                        )
                        )
                        .then(
                                Commands.literal("ue")
                                        .then(
                                                Commands.argument(
                                                                "generation",
                                                                StringArgumentType.word()
                                                        )
                                                        .then(
                                                                Commands.argument(
                                                                                "plmn",
                                                                                StringArgumentType.word()
                                                                        )
                                                                        .then(
                                                                                Commands.argument(
                                                                                                "subscriber",
                                                                                                StringArgumentType.word()
                                                                                        )
                                                                                        .then(
                                                                                                Commands.argument(
                                                                                                                "secret",
                                                                                                                StringArgumentType.greedyString()
                                                                                                        )
                                                                                                        .executes(
                                                                                                                context -> configureUe(
                                                                                                                        context.getSource(),
                                                                                                                        StringArgumentType.getString(context, "generation"),
                                                                                                                        StringArgumentType.getString(context, "plmn"),
                                                                                                                        StringArgumentType.getString(context, "subscriber"),
                                                                                                                        StringArgumentType.getString(context, "secret")
                                                                                                                )
                                                                                                        )
                                                                                        )
                                                                        )
                                                        )
                                        )
                        )
                        .then(
                                Commands.literal("base")
                                        .then(
                                                Commands.argument(
                                                                "generation",
                                                                StringArgumentType.word()
                                                        )
                                                        .then(
                                                                Commands.argument(
                                                                                "pci",
                                                                                IntegerArgumentType.integer(0, 1007)
                                                                        )
                                                                        .then(
                                                                                Commands.argument(
                                                                                                "cell_identity",
                                                                                                LongArgumentType.longArg(1L)
                                                                                        )
                                                                                        .then(
                                                                                                Commands.argument(
                                                                                                                "plmn",
                                                                                                                StringArgumentType.word()
                                                                                                        )
                                                                                                        .executes(
                                                                                                                context -> configureBase(
                                                                                                                        context.getSource(),
                                                                                                                        StringArgumentType.getString(context, "generation"),
                                                                                                                        IntegerArgumentType.getInteger(context, "pci"),
                                                                                                                        LongArgumentType.getLong(context, "cell_identity"),
                                                                                                                        StringArgumentType.getString(context, "plmn")
                                                                                                                )
                                                                                                        )
                                                                                        )
                                                                        )
                                                        )
                                        )
                        )
                        .then(
                                Commands.literal("provision")
                                        .then(
                                                Commands.argument(
                                                                "plmn",
                                                                StringArgumentType.word()
                                                        )
                                                        .then(
                                                                Commands.argument(
                                                                                "subscriber",
                                                                                StringArgumentType.word()
                                                                        )
                                                                        .then(
                                                                                Commands.argument(
                                                                                                "secret",
                                                                                                StringArgumentType.greedyString()
                                                                                        )
                                                                                        .executes(
                                                                                                context -> provision(
                                                                                                        context.getSource(),
                                                                                                        StringArgumentType.getString(context, "plmn"),
                                                                                                        StringArgumentType.getString(context, "subscriber"),
                                                                                                        StringArgumentType.getString(context, "secret")
                                                                                                )
                                                                                        )
                                                                        )
                                                        )
                                        )
                        )
                        .then(
                                Commands.literal("profile")
                                        .then(
                                                Commands.argument(
                                                                "generation",
                                                                StringArgumentType.word()
                                                        )
                                                        .executes(
                                                                context -> profile(
                                                                        context.getSource(),
                                                                        StringArgumentType.getString(
                                                                                context,
                                                                                "generation"
                                                                        )
                                                                )
                                                        )
                                        )
                        )
                        .then(
                                Commands.literal("link")
                                        .then(
                                                Commands.argument(
                                                                "generation",
                                                                StringArgumentType.word()
                                                        )
                                                        .then(
                                                                Commands.argument(
                                                                                "distance_m",
                                                                                DoubleArgumentType.doubleArg(0.1)
                                                                        )
                                                                        .executes(
                                                                                context -> link(
                                                                                        context.getSource(),
                                                                                        StringArgumentType.getString(
                                                                                                context,
                                                                                                "generation"
                                                                                        ),
                                                                                        DoubleArgumentType.getDouble(
                                                                                                context,
                                                                                                "distance_m"
                                                                                        )
                                                                                )
                                                                        )
                                                        )
                                        )
                        )
        );
    }

    private static int help(CommandSourceStack source) {
        line(source, "VS:IA Cellular Full Package", ChatFormatting.AQUA);
        line(source, "Look at a NetworkDevice block within 8 blocks for device commands.", ChatFormatting.GRAY);
        line(source, "/vsiacellular status", ChatFormatting.WHITE);
        line(source, "/vsiacellular auto <true|false>", ChatFormatting.WHITE);
        line(source, "/vsiacellular ue <1g|2g|3g|4g|5g> <plmn> <subscriber> <secret>", ChatFormatting.WHITE);
        line(source, "/vsiacellular base <1g|2g|3g|4g|5g> <pci> <cell_identity> <plmn>", ChatFormatting.WHITE);
        line(source, "/vsiacellular provision <plmn> <subscriber> <secret>", ChatFormatting.WHITE);
        line(source, "/vsiacellular selftest", ChatFormatting.WHITE);
        line(source, "/vsiacellular profile <1g|2g|3g|4g|5g>", ChatFormatting.WHITE);
        line(source, "/vsiacellular link <1g|2g|3g|4g|5g> <distance_m>", ChatFormatting.WHITE);
        line(
                source,
                "Use the same PLMN/subscriber/secret on UE and base-station provisioning.",
                ChatFormatting.GOLD
        );
        line(
                source,
                "This is a deterministic engineering simulation, not a real mobile-network interface.",
                ChatFormatting.DARK_GRAY
        );
        return 1;
    }

    private static int selftest(CommandSourceStack source) {
        CellularSelfTest.Report report =
                CellularSelfTest.run();

        for (CellularSelfTest.Result result : report.results()) {
            line(
                    source,
                    (result.passed() ? "[PASS] " : "[FAIL] ")
                            + result.id()
                            + (result.detail().isBlank() ? "" : " | " + result.detail()),
                    result.passed()
                            ? ChatFormatting.GREEN
                            : ChatFormatting.RED
            );
        }

        line(
                source,
                "Cellular self-test result: "
                        + report.passed()
                        + " passed, "
                        + report.failed()
                        + " failed",
                report.failed() == 0
                        ? ChatFormatting.GREEN
                        : ChatFormatting.RED
        );

        return report.failed() == 0 ? 1 : 0;
    }

    private static int status(CommandSourceStack source) {
        NetworkDeviceBlockEntity device =
                lookedAtDevice(source);

        if (device == null) {
            return 0;
        }

        line(
                source,
                "Cellular device " + device.id(),
                ChatFormatting.AQUA
        );

        line(
                source,
                "Profile="
                        + device.networkProfileId()
                        + " | generation="
                        + device.cellularGeneration().displayName(),
                ChatFormatting.WHITE
        );

        line(
                source,
                "Mode="
                        + device.cellularMode()
                        + " | RAN="
                        + device.cellularUeState()
                        + " | NAS="
                        + device.cellularNasState(),
                ChatFormatting.WHITE
        );

        line(
                source,
                "Automation="
                        + device.cellularAutomationEnabled()
                        + " | serving="
                        + String.valueOf(device.servingCellId()),
                ChatFormatting.WHITE
        );

        line(
                source,
                "Control TX="
                        + device.cellularLastControlTx()
                        + " ("
                        + device.cellularControlTxCount()
                        + ") | RX="
                        + device.cellularLastControlRx()
                        + " ("
                        + device.cellularControlRxCount()
                        + ")",
                ChatFormatting.GRAY
        );

        // CELLULAR_RAR_STATUS_V1
        line(
                source,
                "Wire RX="
                        + device.cellularLastWireRx()
                        + " | target="
                        + device.cellularLastWireTarget()
                        + " | own="
                        + device.cellularLastWireOwn()
                        + " | rejected="
                        + device.cellularRejectedTargetCount()
                        + " | pending="
                        + device.cellularPendingControlCount(),
                device.cellularRejectedTargetCount() == 0
                        ? ChatFormatting.GRAY
                        : ChatFormatting.GOLD
        );

        if (device.cellularRejectedTargetCount() > 0) {
            line(
                    source,
                    "Last rejected="
                            + device.cellularLastRejectedType()
                            + " | target="
                            + device.cellularLastRejectedTarget(),
                    ChatFormatting.GOLD
            );
        }

        line(
                source,
                "Recovery RACH="
                        + device.cellularRandomAccessRetries()
                        + " | RRC="
                        + device.cellularRrcSetupRetries()
                        + " | NAS="
                        + device.cellularNasRegistrationRetries()
                        + " | age="
                        + device.cellularAutomationStateAgeMillis()
                        + " ms | last="
                        + device.cellularLastAutomationAction(),
                ChatFormatting.GRAY
        );

        PduSession session =
                device.cellularPduSession();

        line(
                source,
                session == null
                        ? "PDU session: none"
                        : "PDU session #"
                        + session.sessionId()
                        + " | "
                        + session.ipAddress()
                        + " | DNN="
                        + session.dnn()
                        + " | 5QI="
                        + session.fiveQi(),
                session == null
                        ? ChatFormatting.GRAY
                        : ChatFormatting.GREEN
        );

        return 1;
    }

    private static int setAutomation(
            CommandSourceStack source,
            boolean enabled
    ) {
        NetworkDeviceBlockEntity device =
                lookedAtDevice(source);

        if (device == null) {
            return 0;
        }

        device.setCellularAutomationEnabled(
                enabled
        );

        line(
                source,
                "Cellular automation = " + enabled,
                ChatFormatting.GREEN
        );

        return 1;
    }

    private static int configureUe(
            CommandSourceStack source,
            String rawGeneration,
            String plmn,
            String subscriber,
            String secret
    ) {
        NetworkDeviceBlockEntity device =
                lookedAtDevice(source);

        if (device == null) {
            return 0;
        }

        CellularGeneration generation =
                parseGeneration(rawGeneration);

        if (!selectProfile(
                device,
                generation
        )) {
            source.sendFailure(
                    Component.literal(
                            "Unable to select cellular profile."
                    )
            );
            return 0;
        }

        CellularSimProfile sim =
                CellularSimProfile.fromSecret(
                        plmn,
                        subscriber,
                        secret
                );

        if (!device.configureCellularUe(
                sim
        )) {
            source.sendFailure(
                    Component.literal(
                            "Unable to configure UE."
                    )
            );
            return 0;
        }

        line(
                source,
                "UE configured | SUPI="
                        + sim.supi()
                        + " | ICCID="
                        + sim.iccid()
                        + " | key fingerprint="
                        + sim.fingerprint(),
                ChatFormatting.GREEN
        );

        line(
                source,
                "Now provision the same subscriber on the base station.",
                ChatFormatting.GOLD
        );

        return 1;
    }

    private static int configureBase(
            CommandSourceStack source,
            String rawGeneration,
            int pci,
            long cellIdentity,
            String plmn
    ) {
        NetworkDeviceBlockEntity device =
                lookedAtDevice(source);

        if (device == null) {
            return 0;
        }

        CellularGeneration generation =
                parseGeneration(rawGeneration);

        if (!selectProfile(
                device,
                generation
        )) {
            source.sendFailure(
                    Component.literal(
                            "Unable to select cellular profile."
                    )
            );
            return 0;
        }

        if (!device.configureCellularBaseStation(
                pci,
                cellIdentity,
                plmn
        )) {
            source.sendFailure(
                    Component.literal(
                            "Unable to configure base station."
                    )
            );
            return 0;
        }

        line(
                source,
                generation.displayName()
                        + " base station configured | PCI="
                        + pci
                        + " | NCI/CI="
                        + cellIdentity
                        + " | PLMN="
                        + plmn,
                ChatFormatting.GREEN
        );

        return 1;
    }

    private static int provision(
            CommandSourceStack source,
            String plmn,
            String subscriber,
            String secret
    ) {
        NetworkDeviceBlockEntity device =
                lookedAtDevice(source);

        if (device == null) {
            return 0;
        }

        CellularSimProfile sim =
                CellularSimProfile.fromSecret(
                        plmn,
                        subscriber,
                        secret
                );

        if (!device.provisionCellularSubscriber(
                sim.supi(),
                sim.subscriberKey()
        )) {
            source.sendFailure(
                    Component.literal(
                            "Provisioning failed. The targeted device must be a configured cellular base station."
                    )
            );
            return 0;
        }

        line(
                source,
                "Subscriber provisioned | SUPI="
                        + sim.supi()
                        + " | fingerprint="
                        + sim.fingerprint(),
                ChatFormatting.GREEN
        );

        return 1;
    }

    private static int profile(
            CommandSourceStack source,
            String rawGeneration
    ) {
        CellularGeneration generation =
                parseGeneration(rawGeneration);

        CellularGenerationProfile profile =
                CellularGenerationCatalog.profile(
                        generation
                );

        line(
                source,
                profile.generation().displayName()
                        + " | "
                        + profile.airInterface(),
                ChatFormatting.AQUA
        );

        line(
                source,
                "Multiple access: "
                        + profile.multipleAccess(),
                ChatFormatting.WHITE
        );

        line(
                source,
                "Packet core: "
                        + profile.supportsPacketCore()
                        + " | HARQ: "
                        + profile.supportsHarq(),
                ChatFormatting.WHITE
        );

        line(
                source,
                "Mobility hysteresis="
                        + profile.defaultHysteresisDb()
                        + " dB | TTT="
                        + profile.defaultTimeToTriggerMillis()
                        + " ms",
                ChatFormatting.GRAY
        );

        CellularProtocolConceptCatalog.ProtocolConcept concept =
                CellularProtocolConceptCatalog.forGeneration(
                        generation
                );

        line(
                source,
                "Access: "
                        + concept.synchronizationAndAccess(),
                ChatFormatting.GRAY
        );

        line(
                source,
                "Service: "
                        + concept.serviceModel(),
                ChatFormatting.GRAY
        );

        line(
                source,
                "Simplified flow: "
                        + String.join(
                        " -> ",
                        concept.simplifiedAttachSequence()
                ),
                ChatFormatting.DARK_GRAY
        );

        return 1;
    }

    private static int link(
            CommandSourceStack source,
            String rawGeneration,
            double distanceMeters
    ) {
        CellularGeneration generation =
                parseGeneration(rawGeneration);

        CellularBand band =
                defaultBand(generation);

        double bandwidthHz =
                switch (generation) {
                    case G1_ANALOG -> 25_000.0;
                    case G2_GSM -> 200_000.0;
                    case G3_UMTS -> 5_000_000.0;
                    case G4_LTE -> 20_000_000.0;
                    case G5_NR -> 100_000_000.0;
                };

        int resourceBlocks =
                switch (generation) {
                    case G1_ANALOG, G2_GSM, G3_UMTS -> 1;
                    case G4_LTE -> 100;
                    case G5_NR -> 273;
                };

        CellularMeasurement measurement =
                CellularMeasurementEngine.evaluate(
                        band.centerDownlinkHz(),
                        distanceMeters,
                        46.0,
                        15.0,
                        0.0,
                        bandwidthHz,
                        resourceBlocks,
                        -110.0,
                        5.0,
                        0.0
                );

        line(
                source,
                generation.displayName()
                        + " link @ "
                        + String.format(
                        Locale.ROOT,
                        "%.1f m",
                        distanceMeters
                ),
                ChatFormatting.AQUA
        );

        line(
                source,
                String.format(
                        Locale.ROOT,
                        "PL=%.2f dB | RSRP=%.2f dBm | RSRQ=%.2f dB",
                        measurement.pathLossDb(),
                        measurement.rsrpDbm(),
                        measurement.rsrqDb()
                ),
                ChatFormatting.WHITE
        );

        line(
                source,
                String.format(
                        Locale.ROOT,
                        "SINR=%.2f dB | CQI=%d | TA=%d",
                        measurement.sinrDb(),
                        measurement.cqi(),
                        measurement.timingAdvanceUnits()
                ),
                ChatFormatting.WHITE
        );

        line(
                source,
                String.format(
                        Locale.ROOT,
                        "Shannon upper bound=%.3f Mbit/s",
                        measurement.shannonCapacityBps()
                                / 1_000_000.0
                ),
                ChatFormatting.GOLD
        );

        return 1;
    }

    private static NetworkDeviceBlockEntity lookedAtDevice(
            CommandSourceStack source
    ) {
        try {
            ServerPlayer player =
                    source.getPlayerOrException();

            HitResult result =
                    player.pick(
                            8.0,
                            0.0F,
                            false
                    );

            if (!(result instanceof BlockHitResult blockHit)) {
                source.sendFailure(
                        Component.literal(
                                "Look at a NetworkDevice block within 8 blocks."
                        )
                );
                return null;
            }

            BlockEntity blockEntity =
                    player.serverLevel()
                            .getBlockEntity(
                                    blockHit.getBlockPos()
                            );

            if (!(blockEntity
                    instanceof NetworkDeviceBlockEntity device)) {
                source.sendFailure(
                        Component.literal(
                                "Target block is not a Signality NetworkDevice."
                        )
                );
                return null;
            }

            return device;
        } catch (Exception exception) {
            source.sendFailure(
                    Component.literal(
                            "Unable to resolve targeted network device: "
                                    + exception.getMessage()
                    )
            );
            return null;
        }
    }

    private static boolean selectProfile(
            NetworkDeviceBlockEntity device,
            CellularGeneration generation
    ) {
        String path =
                switch (generation) {
                    case G1_ANALOG -> "cellular_1g";
                    case G2_GSM -> "cellular_2g";
                    case G3_UMTS -> "cellular_3g";
                    case G4_LTE -> "cellular_4g";
                    case G5_NR -> "cellular_5g";
                };

        return device.setNetworkProfile(
                new ResourceLocation(
                        Signality.MODID,
                        path
                )
        );
    }

    private static CellularGeneration parseGeneration(
            String raw
    ) {
        String value =
                raw == null
                        ? ""
                        : raw.toLowerCase(Locale.ROOT);

        return switch (value) {
            case "1", "1g", "g1" ->
                    CellularGeneration.G1_ANALOG;
            case "2", "2g", "g2", "gsm" ->
                    CellularGeneration.G2_GSM;
            case "3", "3g", "g3", "umts" ->
                    CellularGeneration.G3_UMTS;
            case "4", "4g", "g4", "lte" ->
                    CellularGeneration.G4_LTE;
            default ->
                    CellularGeneration.G5_NR;
        };
    }

    private static CellularBand defaultBand(
            CellularGeneration generation
    ) {
        double frequency =
                switch (generation) {
                    case G1_ANALOG, G2_GSM -> 950.0e6;
                    case G3_UMTS -> 2.14e9;
                    case G4_LTE -> 1.84e9;
                    case G5_NR -> 3.5e9;
                };

        CellularBand band =
                CellularBandCatalog.bestMatch(
                        generation,
                        frequency
                );

        if (band != null) {
            return band;
        }

        return CellularBandCatalog.bestMatch(
                CellularGeneration.G5_NR,
                3.5e9
        );
    }

    private static void line(
            CommandSourceStack source,
            String text,
            ChatFormatting color
    ) {
        source.sendSuccess(
                () -> Component.literal(
                        text
                ).withStyle(
                        color
                ),
                false
        );
    }
}

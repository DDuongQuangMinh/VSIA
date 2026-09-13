package com.k1ngtle.vsia.signality.internet.radio;

import com.k1ngtle.vsia.Vsia;
import com.k1ngtle.vsia.signality.engineering.radio.RadioEmission;
import com.k1ngtle.vsia.signality.engineering.radio.RadioLinkQuality;
import com.k1ngtle.vsia.signality.internet.NetworkDeviceBlockEntity;
import com.k1ngtle.vsia.signality.internet.network.NetworkKind;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Mod.EventBusSubscriber(
        modid = Vsia.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class RadioNetworkCommand {
    private RadioNetworkCommand() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("vsiaradio")
                        .requires(source -> source.hasPermission(2))
                        .executes(context -> help(context.getSource()))
                        .then(
                                Commands.literal("status")
                                        .then(
                                                Commands.argument("pos", BlockPosArgument.blockPos())
                                                        .executes(RadioNetworkCommand::status)
                                        )
                        )
                        .then(
                                Commands.literal("band")
                                        .then(
                                                Commands.argument("pos", BlockPosArgument.blockPos())
                                                        .then(
                                                                Commands.argument("band", StringArgumentType.word())
                                                                        .suggests((context, builder) -> {
                                                                            builder.suggest("hf");
                                                                            builder.suggest("vhf");
                                                                            builder.suggest("uhf");
                                                                            return builder.buildFuture();
                                                                        })
                                                                        .executes(RadioNetworkCommand::band)
                                                        )
                                        )
                        )
                        .then(
                                Commands.literal("tune")
                                        .then(
                                                Commands.argument("pos", BlockPosArgument.blockPos())
                                                        .then(
                                                                Commands.argument(
                                                                                "frequency_mhz",
                                                                                DoubleArgumentType.doubleArg(0.001D, 100_000.0D)
                                                                        )
                                                                        .then(
                                                                                Commands.argument(
                                                                                                "emission",
                                                                                                StringArgumentType.word()
                                                                                        )
                                                                                        .suggests((context, builder) -> {
                                                                                            builder.suggest("am");
                                                                                            builder.suggest("fm");
                                                                                            builder.suggest("digital");
                                                                                            return builder.buildFuture();
                                                                                        })
                                                                                        .executes(RadioNetworkCommand::tune)
                                                                        )
                                                        )
                                        )
                        )
                        .then(
                                Commands.literal("packet")
                                        .then(
                                                Commands.argument("source", BlockPosArgument.blockPos())
                                                        .then(
                                                                Commands.argument("target", BlockPosArgument.blockPos())
                                                                        .then(
                                                                                Commands.argument(
                                                                                                "message",
                                                                                                StringArgumentType.greedyString()
                                                                                        )
                                                                                        .executes(RadioNetworkCommand::packet)
                                                                        )
                                                        )
                                        )
                        )
                        .then(
                                Commands.literal("broadcast")
                                        .then(
                                                Commands.argument("source", BlockPosArgument.blockPos())
                                                        .then(
                                                                Commands.argument(
                                                                                "message",
                                                                                StringArgumentType.greedyString()
                                                                        )
                                                                        .executes(RadioNetworkCommand::broadcast)
                                                        )
                                        )
                        )
                        .then(
                                Commands.literal("voice")
                                        .then(
                                                Commands.argument("source", BlockPosArgument.blockPos())
                                                        .then(
                                                                Commands.argument(
                                                                                "text",
                                                                                StringArgumentType.greedyString()
                                                                        )
                                                                        .executes(RadioNetworkCommand::voice)
                                                        )
                                        )
                        )
                        .then(
                                Commands.literal("squelch")
                                        .then(
                                                Commands.argument("pos", BlockPosArgument.blockPos())
                                                        .then(
                                                                Commands.argument(
                                                                                "snr_db",
                                                                                DoubleArgumentType.doubleArg(-40.0D, 80.0D)
                                                                        )
                                                                        .executes(RadioNetworkCommand::squelch)
                                                        )
                                        )
                        )
                        .then(
                                Commands.literal("mesh")
                                        .then(
                                                Commands.argument("pos", BlockPosArgument.blockPos())
                                                        .then(
                                                                Commands.argument("enabled", BoolArgumentType.bool())
                                                                        .executes(RadioNetworkCommand::mesh)
                                                        )
                                        )
                        )
                        .then(
                                Commands.literal("fhss")
                                        .then(
                                                Commands.argument("pos", BlockPosArgument.blockPos())
                                                        .then(
                                                                Commands.argument("enabled", BoolArgumentType.bool())
                                                                        .executes(RadioNetworkCommand::fhss)
                                                        )
                                        )
                        )
                        .then(
                                Commands.literal("repeater")
                                        .then(
                                                Commands.argument("pos", BlockPosArgument.blockPos())
                                                        .then(
                                                                Commands.argument(
                                                                                "input_mhz",
                                                                                DoubleArgumentType.doubleArg(0.001D, 100_000.0D)
                                                                        )
                                                                        .then(
                                                                                Commands.argument(
                                                                                                "output_mhz",
                                                                                                DoubleArgumentType.doubleArg(0.001D, 100_000.0D)
                                                                                        )
                                                                                        .executes(RadioNetworkCommand::repeater)
                                                                        )
                                                        )
                                        )
                        )
                        .then(
                                Commands.literal("linktest")
                                        .then(
                                                Commands.argument("source", BlockPosArgument.blockPos())
                                                        .then(
                                                                Commands.argument("target", BlockPosArgument.blockPos())
                                                                        .executes(RadioNetworkCommand::linkTest)
                                                        )
                                        )
                        )
        );
    }

    private static int help(CommandSourceStack source) {
        source.sendSuccess(
                () -> Component.literal("VSIA full radio test controls")
                        .withStyle(ChatFormatting.AQUA),
                false
        );
        source.sendSuccess(() -> Component.literal("/vsiaradio status <x y z>"), false);
        source.sendSuccess(() -> Component.literal("/vsiaradio band <x y z> <hf|vhf|uhf>"), false);
        source.sendSuccess(() -> Component.literal("/vsiaradio tune <x y z> <MHz> <am|fm|digital>"), false);
        source.sendSuccess(() -> Component.literal("/vsiaradio packet <src> <dst> <message>"), false);
        source.sendSuccess(() -> Component.literal("/vsiaradio broadcast <src> <message>"), false);
        source.sendSuccess(() -> Component.literal("/vsiaradio voice <src> <text>"), false);
        source.sendSuccess(() -> Component.literal("/vsiaradio squelch <pos> <SNR dB>"), false);
        source.sendSuccess(() -> Component.literal("/vsiaradio mesh <pos> <true|false>"), false);
        source.sendSuccess(() -> Component.literal("/vsiaradio fhss <pos> <true|false>"), false);
        source.sendSuccess(() -> Component.literal("/vsiaradio repeater <pos> <input MHz> <output MHz>"), false);
        source.sendSuccess(() -> Component.literal("/vsiaradio linktest <src> <dst>"), false);
        return 1;
    }

    private static int status(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        BlockPos pos = BlockPosArgument.getLoadedBlockPos(context, "pos");
        NetworkDeviceBlockEntity radio = radioAt(source, pos);

        if (radio == null) {
            return 0;
        }

        String frequency = String.format(
                Locale.ROOT,
                "%.6f MHz",
                radio.activeFrequencyHz() / 1_000_000.0D
        );

        source.sendSuccess(
                () -> Component.literal(
                        "Radio " + pos.toShortString()
                                + " | id=" + radio.id()
                                + " | profile=" + radio.networkProfileId()
                                + " | mode=" + radio.radioMode()
                                + " | f=" + frequency
                ).withStyle(ChatFormatting.AQUA),
                false
        );

        RadioLinkQuality quality = radio.lastRadioLinkQuality();
        source.sendSuccess(
                () -> Component.literal(
                        "Last link quality: " + (quality == null ? "none" : quality)
                ),
                false
        );

        byte[] voice = radio.lastReceivedRadioVoice();
        if (voice != null && voice.length > 0) {
            source.sendSuccess(
                    () -> Component.literal(
                            "Last voice payload: "
                                    + new String(voice, StandardCharsets.UTF_8)
                    ),
                    false
            );
        }

        if (radio.lastRfChannelAssessment() != null) {
            source.sendSuccess(
                    () -> Component.literal(
                            "RF assessment: " + radio.lastRfChannelAssessment()
                    ),
                    false
            );
        }

        return 1;
    }

    private static int band(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        BlockPos pos = BlockPosArgument.getLoadedBlockPos(context, "pos");
        NetworkDeviceBlockEntity radio = deviceAt(source, pos);

        if (radio == null) {
            return 0;
        }

        RadioBandPreset preset = RadioBandPreset.fromCommand(
                StringArgumentType.getString(context, "band")
        );

        if (preset == null) {
            source.sendFailure(Component.literal("Band must be hf, vhf or uhf."));
            return 0;
        }

        if (!preset.apply(radio)) {
            source.sendFailure(Component.literal("Failed to apply radio band preset."));
            return 0;
        }

        source.sendSuccess(
                () -> Component.literal(
                        "Radio " + pos.toShortString()
                                + " configured for " + preset.name()
                                + " at "
                                + String.format(Locale.ROOT, "%.4f MHz", preset.frequencyHz() / 1_000_000.0D)
                ).withStyle(ChatFormatting.GREEN),
                false
        );
        return 1;
    }

    private static int tune(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        BlockPos pos = BlockPosArgument.getLoadedBlockPos(context, "pos");
        NetworkDeviceBlockEntity radio = radioAt(source, pos);

        if (radio == null) {
            return 0;
        }

        double frequencyHz =
                DoubleArgumentType.getDouble(context, "frequency_mhz") * 1_000_000.0D;

        RadioEmission emission;
        try {
            emission = RadioEmission.valueOf(
                    StringArgumentType.getString(context, "emission")
                            .toUpperCase(Locale.ROOT)
            );
        } catch (IllegalArgumentException exception) {
            source.sendFailure(Component.literal("Unknown emission. Use am, fm or digital."));
            return 0;
        }

        double bandwidthHz = Math.max(1.0D, radio.networkProfile().bandwidthHz());

        boolean configured = radio.configureRadioTransceiver(
                "MANUAL",
                frequencyHz,
                bandwidthHz,
                emission,
                "100.0"
        );

        if (!configured) {
            source.sendFailure(Component.literal("Radio tune failed."));
            return 0;
        }

        source.sendSuccess(
                () -> Component.literal(
                        "Tuned " + pos.toShortString()
                                + " to "
                                + String.format(Locale.ROOT, "%.6f MHz", frequencyHz / 1_000_000.0D)
                                + " " + emission
                ).withStyle(ChatFormatting.GREEN),
                false
        );
        return 1;
    }

    private static int packet(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        NetworkDeviceBlockEntity tx = radioAt(
                source,
                BlockPosArgument.getLoadedBlockPos(context, "source")
        );
        NetworkDeviceBlockEntity rx = radioAt(
                source,
                BlockPosArgument.getLoadedBlockPos(context, "target")
        );

        if (tx == null || rx == null) {
            return 0;
        }

        String message = StringArgumentType.getString(context, "message");
        CompoundTag payload = testPayload(tx, message, "UNICAST");

        if (!tx.sendRadioPacket(rx.id(), payload)) {
            source.sendFailure(Component.literal("Packet radio TX failed."));
            return 0;
        }

        source.sendSuccess(
                () -> Component.literal(
                        "Packet queued " + tx.id() + " -> " + rx.id()
                                + " | " + message
                ).withStyle(ChatFormatting.GREEN),
                false
        );
        return 1;
    }

    private static int broadcast(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        NetworkDeviceBlockEntity tx = radioAt(
                source,
                BlockPosArgument.getLoadedBlockPos(context, "source")
        );

        if (tx == null) {
            return 0;
        }

        String message = StringArgumentType.getString(context, "message");
        CompoundTag payload = testPayload(tx, message, "BROADCAST");

        if (!tx.sendRadioPacket(null, payload)) {
            source.sendFailure(Component.literal("Radio broadcast failed."));
            return 0;
        }

        source.sendSuccess(
                () -> Component.literal("Radio broadcast queued: " + message)
                        .withStyle(ChatFormatting.GREEN),
                false
        );
        return 1;
    }

    private static int voice(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        NetworkDeviceBlockEntity tx = radioAt(
                source,
                BlockPosArgument.getLoadedBlockPos(context, "source")
        );

        if (tx == null) {
            return 0;
        }

        String text = StringArgumentType.getString(context, "text");

        if (!tx.pressRadioPtt()) {
            source.sendFailure(Component.literal("PTT is already active or the radio is not ready."));
            return 0;
        }

        boolean sent = tx.sendRadioVoice(
                text.getBytes(StandardCharsets.UTF_8),
                true
        );

        if (!sent) {
            tx.releaseRadioPtt();
            source.sendFailure(Component.literal("Voice radio TX failed."));
            return 0;
        }

        source.sendSuccess(
                () -> Component.literal("PTT voice test transmitted: " + text)
                        .withStyle(ChatFormatting.GREEN),
                false
        );
        return 1;
    }

    private static int squelch(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        NetworkDeviceBlockEntity radio = radioAt(
                source,
                BlockPosArgument.getLoadedBlockPos(context, "pos")
        );

        if (radio == null) {
            return 0;
        }

        double threshold = DoubleArgumentType.getDouble(context, "snr_db");
        radio.setRadioSquelchSnrThresholdDb(threshold);

        source.sendSuccess(
                () -> Component.literal(
                        "Radio squelch threshold set to " + threshold + " dB SNR"
                ).withStyle(ChatFormatting.GREEN),
                false
        );
        return 1;
    }

    private static int mesh(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        NetworkDeviceBlockEntity radio = radioAt(
                source,
                BlockPosArgument.getLoadedBlockPos(context, "pos")
        );

        if (radio == null) {
            return 0;
        }

        boolean enabled = BoolArgumentType.getBool(context, "enabled");
        radio.enableRadioMesh(enabled);

        source.sendSuccess(
                () -> Component.literal("Radio mesh " + (enabled ? "enabled" : "disabled"))
                        .withStyle(ChatFormatting.GREEN),
                false
        );
        return 1;
    }

    private static int fhss(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        NetworkDeviceBlockEntity radio = radioAt(
                source,
                BlockPosArgument.getLoadedBlockPos(context, "pos")
        );

        if (radio == null) {
            return 0;
        }

        boolean enabled = BoolArgumentType.getBool(context, "enabled");

        if (!enabled) {
            radio.disableRadioFrequencyHopping();
            source.sendSuccess(
                    () -> Component.literal("Radio FHSS disabled")
                            .withStyle(ChatFormatting.GREEN),
                    false
            );
            return 1;
        }

        double center = radio.activeFrequencyHz();
        double step = Math.max(12_500.0D, radio.networkProfile().bandwidthHz());
        double[] frequencies = new double[]{
                center - 1.5D * step,
                center - 0.5D * step,
                center + 0.5D * step,
                center + 1.5D * step
        };

        radio.enableRadioFrequencyHopping(
                frequencies,
                0x5653494152414449L
        );

        source.sendSuccess(
                () -> Component.literal(
                        "Radio FHSS enabled with four deterministic channels. "
                                + "Apply the same center/band to every peer."
                ).withStyle(ChatFormatting.GREEN),
                false
        );
        return 1;
    }

    private static int repeater(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        BlockPos pos = BlockPosArgument.getLoadedBlockPos(context, "pos");
        NetworkDeviceBlockEntity radio = radioAt(source, pos);

        if (radio == null) {
            return 0;
        }

        double inputHz = DoubleArgumentType.getDouble(context, "input_mhz") * 1_000_000.0D;
        double outputHz = DoubleArgumentType.getDouble(context, "output_mhz") * 1_000_000.0D;
        double bandwidth = Math.max(1.0D, radio.networkProfile().bandwidthHz());

        boolean configured = radio.configureRadioRepeater(
                "REPEATER",
                inputHz,
                outputHz,
                bandwidth,
                RadioEmission.FM,
                "100.0"
        );

        if (!configured) {
            source.sendFailure(Component.literal("Repeater configuration failed."));
            return 0;
        }

        source.sendSuccess(
                () -> Component.literal(
                        "Repeater " + pos.toShortString()
                                + " input="
                                + String.format(Locale.ROOT, "%.4f MHz", inputHz / 1_000_000.0D)
                                + " output="
                                + String.format(Locale.ROOT, "%.4f MHz", outputHz / 1_000_000.0D)
                ).withStyle(ChatFormatting.GREEN),
                false
        );
        return 1;
    }

    private static int linkTest(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        BlockPos sourcePos = BlockPosArgument.getLoadedBlockPos(context, "source");
        BlockPos targetPos = BlockPosArgument.getLoadedBlockPos(context, "target");
        NetworkDeviceBlockEntity tx = radioAt(source, sourcePos);
        NetworkDeviceBlockEntity rx = radioAt(source, targetPos);

        if (tx == null || rx == null) {
            return 0;
        }

        CompoundTag payload = testPayload(
                tx,
                "VSIA-RADIO-LINK-TEST",
                "LINK_TEST"
        );
        payload.putString("target_position", targetPos.toShortString());

        if (!tx.sendRadioPacket(rx.id(), payload)) {
            source.sendFailure(Component.literal("Radio link test TX failed."));
            return 0;
        }

        double distance = tx.positionWorld().distanceTo(rx.positionWorld());
        source.sendSuccess(
                () -> Component.literal(
                        "Radio link test queued | distance="
                                + String.format(Locale.ROOT, "%.2f blocks", distance)
                                + " | inspect target with /vsiaradio status "
                                + targetPos.getX() + " " + targetPos.getY() + " " + targetPos.getZ()
                ).withStyle(ChatFormatting.GREEN),
                false
        );
        return 1;
    }

    private static CompoundTag testPayload(
            NetworkDeviceBlockEntity tx,
            String message,
            String type
    ) {
        CompoundTag payload = new CompoundTag();
        payload.putString("vsia_radio_type", type);
        payload.putString("message", message == null ? "" : message);
        payload.putUUID("source_id", tx.id());
        payload.putLong("created_nanos", System.nanoTime());
        return payload;
    }

    private static NetworkDeviceBlockEntity radioAt(
            CommandSourceStack source,
            BlockPos pos
    ) {
        NetworkDeviceBlockEntity device = deviceAt(source, pos);

        if (device == null) {
            return null;
        }

        if (device.networkProfile().kind() != NetworkKind.RADIO) {
            source.sendFailure(
                    Component.literal(
                            "Device at " + pos.toShortString()
                                    + " is not using a RADIO network profile. "
                                    + "Run /vsiaradio band first."
                    )
            );
            return null;
        }

        return device;
    }

    private static NetworkDeviceBlockEntity deviceAt(
            CommandSourceStack source,
            BlockPos pos
    ) {
        BlockEntity blockEntity = source.getLevel().getBlockEntity(pos);

        if (!(blockEntity instanceof NetworkDeviceBlockEntity device)) {
            source.sendFailure(
                    Component.literal(
                            "No VSIA NetworkDeviceBlockEntity at " + pos.toShortString()
                    )
            );
            return null;
        }

        return device;
    }
}

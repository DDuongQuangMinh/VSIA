package com.k1ngtle.vsia.signality.internet.satellite;

import com.k1ngtle.vsia.Vsia;
import com.k1ngtle.vsia.signality.internet.satellite.device.TemporarySatelliteTerminalBlockEntity;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Locale;

@Mod.EventBusSubscriber(
        modid = Vsia.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class SatelliteCommand {
    private static final SimpleCommandExceptionType NOT_TERMINAL =
            new SimpleCommandExceptionType(
                    Component.literal(
                            "Expected a loaded VS:IA satellite terminal"
                    )
            );

    private SatelliteCommand() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(
            RegisterCommandsEvent event
    ) {
        register(
                event.getDispatcher()
        );
    }

    public static void register(
            CommandDispatcher<CommandSourceStack> dispatcher
    ) {
        dispatcher.register(
                Commands.literal(
                                "vsiasat"
                        )
                        .requires(
                                source ->
                                        source.hasPermission(
                                                2
                                        )
                        )
                        .then(
                                Commands.literal(
                                                "status"
                                        )
                                        .then(
                                                Commands.argument(
                                                                "pos",
                                                                BlockPosArgument.blockPos()
                                                        )
                                                        .executes(
                                                                SatelliteCommand::status
                                                        )
                                        )
                        )
                        .then(
                                Commands.literal(
                                                "linktest"
                                        )
                                        .then(
                                                Commands.argument(
                                                                "source",
                                                                BlockPosArgument.blockPos()
                                                        )
                                                        .then(
                                                                Commands.argument(
                                                                                "target",
                                                                                BlockPosArgument.blockPos()
                                                                        )
                                                                        .executes(
                                                                                SatelliteCommand::linkTest
                                                                        )
                                                        )
                                        )
                        )
                        .then(
                                Commands.literal(
                                                "packet"
                                        )
                                        .then(
                                                Commands.argument(
                                                                "source",
                                                                BlockPosArgument.blockPos()
                                                        )
                                                        .then(
                                                                Commands.argument(
                                                                                "target",
                                                                                BlockPosArgument.blockPos()
                                                                        )
                                                                        .then(
                                                                                Commands.argument(
                                                                                                "message",
                                                                                                StringArgumentType.greedyString()
                                                                                        )
                                                                                        .executes(
                                                                                                SatelliteCommand::packet
                                                                                        )
                                                                        )
                                                        )
                                        )
                        )
        );
    }

    private static int status(
            CommandContext<CommandSourceStack> context
    ) throws CommandSyntaxException {
        BlockPos pos =
                BlockPosArgument
                        .getLoadedBlockPos(
                                context,
                                "pos"
                        );

        TemporarySatelliteTerminalBlockEntity terminal =
                terminal(
                        context,
                        pos
                );

        SatelliteLinkAssessment assessment =
                SatelliteNetworkManager
                        .assessSelf(
                                terminal
                        );

        terminal.setLastAssessment(
                assessment
        );

        context.getSource()
                .sendSuccess(
                        () ->
                                Component.literal(
                                                "Satellite Terminal "
                                                        + pos
                                                        .toShortString()
                                                        + " | band="
                                                        + terminal
                                                        .band()
                                                        .name()
                                                        + " | mask="
                                                        + String.format(
                                                        Locale.ROOT,
                                                        "%.1f deg",
                                                        terminal
                                                                .minimumElevationDeg()
                                                )
                                        )
                                        .withStyle(
                                                ChatFormatting.AQUA
                                        ),
                        false
                );

        sendAssessment(
                context.getSource(),
                assessment
        );

        return 1;
    }

    private static int linkTest(
            CommandContext<CommandSourceStack> context
    ) throws CommandSyntaxException {
        BlockPos sourcePos =
                BlockPosArgument
                        .getLoadedBlockPos(
                                context,
                                "source"
                        );

        BlockPos targetPos =
                BlockPosArgument
                        .getLoadedBlockPos(
                                context,
                                "target"
                        );

        TemporarySatelliteTerminalBlockEntity source =
                terminal(
                        context,
                        sourcePos
                );

        TemporarySatelliteTerminalBlockEntity target =
                terminal(
                        context,
                        targetPos
                );

        SatelliteLinkAssessment assessment =
                SatelliteNetworkManager
                        .assess(
                                source,
                                target
                        );

        source.setLastAssessment(
                assessment
        );

        sendAssessment(
                context.getSource(),
                assessment
        );

        return assessment.visible()
                ? 1
                : 0;
    }

    private static int packet(
            CommandContext<CommandSourceStack> context
    ) throws CommandSyntaxException {
        BlockPos sourcePos =
                BlockPosArgument
                        .getLoadedBlockPos(
                                context,
                                "source"
                        );

        BlockPos targetPos =
                BlockPosArgument
                        .getLoadedBlockPos(
                                context,
                                "target"
                        );

        TemporarySatelliteTerminalBlockEntity source =
                terminal(
                        context,
                        sourcePos
                );

        TemporarySatelliteTerminalBlockEntity target =
                terminal(
                        context,
                        targetPos
                );

        String message =
                StringArgumentType
                        .getString(
                                context,
                                "message"
                        );

        boolean queued =
                SatelliteNetworkManager
                        .sendPacket(
                                source,
                                target,
                                message
                        );

        context.getSource()
                .sendSuccess(
                        () ->
                                Component.literal(
                                                queued
                                                        ? "Satellite packet queued"
                                                        : "Satellite packet not queued"
                                        )
                                        .withStyle(
                                                queued
                                                        ? ChatFormatting.GREEN
                                                        : ChatFormatting.RED
                                        ),
                        false
                );

        return queued
                ? 1
                : 0;
    }

    private static TemporarySatelliteTerminalBlockEntity terminal(
            CommandContext<CommandSourceStack> context,
            BlockPos pos
    ) throws CommandSyntaxException {
        if (context.getSource()
                .getLevel()
                .getBlockEntity(pos)
                instanceof TemporarySatelliteTerminalBlockEntity terminal) {
            return terminal;
        }

        throw NOT_TERMINAL.create();
    }

    private static void sendAssessment(
            CommandSourceStack source,
            SatelliteLinkAssessment assessment
    ) {
        if (!assessment.visible()) {
            source.sendFailure(
                    Component.literal(
                            "No common satellite is above the elevation mask."
                    )
            );

            return;
        }

        source.sendSuccess(
                () ->
                        Component.literal(
                                String.format(
                                        Locale.ROOT,
                                        "%s | elev %.1f/%.1f deg | SNR %.1f/%.1f dB | delay %.2f ms | P=%.3f",
                                        assessment
                                                .satelliteName(),
                                        assessment
                                                .sourceElevationDeg(),
                                        assessment
                                                .targetElevationDeg(),
                                        assessment
                                                .uplinkSnrDb(),
                                        assessment
                                                .downlinkSnrDb(),
                                        assessment
                                                .propagationDelayMs(),
                                        assessment
                                                .packetSuccessProbability()
                                )
                        ),
                false
        );
    }
}

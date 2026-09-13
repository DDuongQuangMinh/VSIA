package com.k1ngtle.vsia.signality.internet.radio.debug;

import com.k1ngtle.vsia.Vsia;
import com.k1ngtle.vsia.signality.internet.radio.portable.PortableRadioEndpoint;
import com.k1ngtle.vsia.signality.internet.radio.portable.PortableRadioService;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = Vsia.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class RadioDevSelfTestCommand {
    private RadioDevSelfTestCommand() {
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
                                "vsiaradiodev"
                        )
                        .requires(
                                source ->
                                        source.hasPermission(
                                                2
                                        )
                        )
                        .then(
                                Commands.literal(
                                                "echo"
                                        )
                                        .then(
                                                Commands.literal(
                                                                "on"
                                                        )
                                                        .executes(
                                                                context ->
                                                                        enableDefault(
                                                                                context
                                                                        )
                                                        )
                                                        .then(
                                                                Commands.argument(
                                                                                "distanceBlocks",
                                                                                DoubleArgumentType.doubleArg(
                                                                                        1.0,
                                                                                        1_000_000.0
                                                                                )
                                                                        )
                                                                        .executes(
                                                                                RadioDevSelfTestCommand::enableAtDistance
                                                                        )
                                                        )
                                        )
                                        .then(
                                                Commands.literal(
                                                                "off"
                                                        )
                                                        .executes(
                                                                RadioDevSelfTestCommand::disable
                                                        )
                                        )
                        )
                        .then(
                                Commands.literal(
                                                "remote"
                                        )
                                        .then(
                                                Commands.literal(
                                                                "here"
                                                        )
                                                        .executes(
                                                                RadioDevSelfTestCommand::remoteHere
                                                        )
                                        )
                                        .then(
                                                Commands.argument(
                                                                "pos",
                                                                BlockPosArgument.blockPos()
                                                        )
                                                        .executes(
                                                                RadioDevSelfTestCommand::remotePosition
                                                        )
                                        )
                        )
                        .then(
                                Commands.literal(
                                                "status"
                                        )
                                        .executes(
                                                RadioDevSelfTestCommand::status
                                        )
                        )
        );
    }

    private static int enableDefault(
            CommandContext<CommandSourceStack> context
    ) throws CommandSyntaxException {
        ServerPlayer player =
                context.getSource()
                        .getPlayerOrException();

        RadioDevSelfTestService.enableDefault(
                player
        );

        sendRoute(
                player
        );

        return 1;
    }

    private static int enableAtDistance(
            CommandContext<CommandSourceStack> context
    ) throws CommandSyntaxException {
        ServerPlayer player =
                context.getSource()
                        .getPlayerOrException();

        double distance =
                DoubleArgumentType.getDouble(
                        context,
                        "distanceBlocks"
                );

        RadioDevSelfTestService.enableAtDistance(
                player,
                distance
        );

        sendRoute(
                player
        );

        return 1;
    }

    private static int disable(
            CommandContext<CommandSourceStack> context
    ) throws CommandSyntaxException {
        ServerPlayer player =
                context.getSource()
                        .getPlayerOrException();

        RadioDevSelfTestService.disable(
                player
        );

        return 1;
    }

    private static int remoteHere(
            CommandContext<CommandSourceStack> context
    ) throws CommandSyntaxException {
        ServerPlayer player =
                context.getSource()
                        .getPlayerOrException();

        RadioDevSelfTestService.setRemote(
                player,
                player.position()
        );

        return 1;
    }

    private static int remotePosition(
            CommandContext<CommandSourceStack> context
    ) throws CommandSyntaxException {
        ServerPlayer player =
                context.getSource()
                        .getPlayerOrException();

        BlockPos pos =
                BlockPosArgument.getLoadedBlockPos(
                        context,
                        "pos"
                );

        RadioDevSelfTestService.setRemote(
                player,
                Vec3.atCenterOf(
                        pos
                )
        );

        return 1;
    }

    private static int status(
            CommandContext<CommandSourceStack> context
    ) throws CommandSyntaxException {
        ServerPlayer player =
                context.getSource()
                        .getPlayerOrException();

        player.sendSystemMessage(
                Component.literal(
                                RadioDevSelfTestService.status(
                                        player
                                )
                        )
                        .withStyle(
                                ChatFormatting.AQUA
                        )
        );

        sendRoute(
                player
        );

        return 1;
    }

    private static void sendRoute(
            ServerPlayer player
    ) {
        InteractionHand hand =
                radioHand(
                        player
                );

        if (hand == null) {
            player.sendSystemMessage(
                    Component.literal(
                                    "[VS:IA DEV] Hold a Temporary Field Radio to inspect the route."
                            )
                            .withStyle(
                                    ChatFormatting.YELLOW
                            )
            );

            return;
        }

        PortableRadioEndpoint endpoint =
                PortableRadioService.endpoint(
                        player,
                        hand
                );

        if (endpoint == null) {
            player.sendSystemMessage(
                    Component.literal(
                                    "[VS:IA DEV] Portable radio endpoint is not ready yet."
                            )
                            .withStyle(
                                    ChatFormatting.YELLOW
                            )
            );

            return;
        }

        player.sendSystemMessage(
                Component.literal(
                                "[VS:IA DEV] "
                                        + RadioDevSelfTestService
                                        .routeStatus(
                                                endpoint
                                        )
                        )
                        .withStyle(
                                ChatFormatting.GRAY
                        )
        );
    }

    private static InteractionHand radioHand(
            ServerPlayer player
    ) {
        if (player.getMainHandItem()
                .getItem()
                instanceof com.k1ngtle.vsia.signality.internet.radio.device.TemporaryRadioItem) {
            return InteractionHand.MAIN_HAND;
        }

        if (player.getOffhandItem()
                .getItem()
                instanceof com.k1ngtle.vsia.signality.internet.radio.device.TemporaryRadioItem) {
            return InteractionHand.OFF_HAND;
        }

        return null;
    }
}

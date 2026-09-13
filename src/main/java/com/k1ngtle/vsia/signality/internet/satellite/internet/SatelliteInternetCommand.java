package com.k1ngtle.vsia.signality.internet.satellite.internet;

import com.k1ngtle.vsia.Vsia;
import com.k1ngtle.vsia.phone.browser.PhoneBrowserServerService;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = Vsia.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class SatelliteInternetCommand {
    private SatelliteInternetCommand() {
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
                                "vsiasatnet"
                        )
                        .then(
                                Commands.literal(
                                                "status"
                                        )
                                        .executes(
                                                SatelliteInternetCommand::status
                                        )
                        )
                        .then(
                                Commands.literal(
                                                "fetch"
                                        )
                                        .then(
                                                Commands.argument(
                                                                "url",
                                                                StringArgumentType.greedyString()
                                                        )
                                                        .executes(
                                                                SatelliteInternetCommand::fetch
                                                        )
                                        )
                        )
        );
    }

    private static int status(
            CommandContext<CommandSourceStack> context
    ) throws CommandSyntaxException {
        ServerPlayer player =
                context.getSource()
                        .getPlayerOrException();

        SatelliteInternetService.PathResult result =
                SatelliteInternetService.resolvePath(
                        player
                );

        if (!result.success()) {
            context.getSource()
                    .sendFailure(
                            Component.literal(
                                    result.error()
                            )
                    );

            return 0;
        }

        SatelliteInternetPath path =
                result.path();

        context.getSource()
                .sendSuccess(
                        () ->
                                Component.literal(
                                                SatelliteInternetService
                                                        .routeSummary(
                                                                path
                                                        )
                                        )
                                        .withStyle(
                                                ChatFormatting.AQUA
                                        ),
                        false
                );

        return 1;
    }

    private static int fetch(
            CommandContext<CommandSourceStack> context
    ) throws CommandSyntaxException {
        ServerPlayer player =
                context.getSource()
                        .getPlayerOrException();

        String url =
                StringArgumentType
                        .getString(
                                context,
                                "url"
                        );

        PhoneBrowserServerService.ServerPage page =
                SatelliteInternetService.fetchWebsite(
                        player,
                        url
                );

        context.getSource()
                .sendSuccess(
                        () ->
                                Component.literal(
                                                page.statusCode()
                                                        + " "
                                                        + page.reason()
                                                        + " | "
                                                        + page.transport()
                                        )
                                        .withStyle(
                                                page.statusCode()
                                                        >= 200
                                                        && page.statusCode()
                                                        < 400
                                                        ? ChatFormatting.GREEN
                                                        : ChatFormatting.RED
                                        ),
                        false
                );

        context.getSource()
                .sendSuccess(
                        () ->
                                Component.literal(
                                        page.routeSummary()
                                ),
                        false
                );

        return page.statusCode()
                >= 200
                && page.statusCode()
                < 400
                ? 1
                : 0;
    }
}

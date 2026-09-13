package com.k1ngtle.vsia.signality.internet.satellite.internet;

import com.k1ngtle.vsia.Vsia;
import com.k1ngtle.vsia.phone.browser.PhoneBrowserServerService;
import com.k1ngtle.vsia.signality.internet.routing.LongHaulRoutePolicy;
import com.k1ngtle.vsia.signality.internet.satellite.SatelliteNetworkManager;
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
                                                SatelliteInternetCommand::statusPolicy
                                        )
                                        .then(
                                                Commands.argument(
                                                                "url",
                                                                StringArgumentType.greedyString()
                                                        )
                                                        .executes(
                                                                SatelliteInternetCommand::statusUrl
                                                        )
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

    private static int statusPolicy(
            CommandContext<CommandSourceStack> context
    ) throws CommandSyntaxException {
        ServerPlayer player =
                context.getSource()
                        .getPlayerOrException();

        int terminals =
                SatelliteNetworkManager
                        .terminals(
                                player.serverLevel()
                        )
                        .size();

        context.getSource()
                .sendSuccess(
                        () ->
                                Component.literal(
                                                "VS:IA long-haul policy: SATELLITE is mandatory at >= 5.000 km / 5000 blocks. Below 5 km, normal terrestrial routing is used. Loaded satellite terminals: "
                                                        + terminals
                                        )
                                        .withStyle(
                                                ChatFormatting.AQUA
                                        ),
                        false
                );

        context.getSource()
                .sendSuccess(
                        () ->
                                Component.literal(
                                        "Inspect a real destination with /vsiasatnet status <domain-or-direct-rack-url>."
                                ),
                        false
                );

        return 1;
    }

    private static int statusUrl(
            CommandContext<CommandSourceStack> context
    ) throws CommandSyntaxException {
        ServerPlayer player =
                context.getSource()
                        .getPlayerOrException();

        String url =
                StringArgumentType.getString(
                        context,
                        "url"
                );

        LongHaulBrowserService.RoutePlan plan =
                LongHaulBrowserService.plan(
                        player,
                        url
                );

        ChatFormatting color =
                !plan.targetResolved()
                        || !plan.physicalDestinationAvailable()
                        || (
                        plan.satelliteRequired()
                                && !plan.backhaulAvailable()
                )
                        ? ChatFormatting.RED
                        : plan.satelliteRequired()
                        ? ChatFormatting.GOLD
                        : ChatFormatting.GREEN;

        context.getSource()
                .sendSuccess(
                        () ->
                                Component.literal(
                                                plan.summary()
                                        )
                                        .withStyle(
                                                color
                                        ),
                        false
                );

        return color == ChatFormatting.RED
                ? 0
                : 1;
    }

    private static int fetch(
            CommandContext<CommandSourceStack> context
    ) throws CommandSyntaxException {
        ServerPlayer player =
                context.getSource()
                        .getPlayerOrException();

        String url =
                StringArgumentType.getString(
                        context,
                        "url"
                );

        PhoneBrowserServerService.ServerPage page =
                LongHaulBrowserService.fetch(
                        player,
                        url,
                        "WIFI"
                );

        context.getSource()
                .sendSuccess(
                        () ->
                                Component.literal(
                                                page.statusCode()
                                                        + " "
                                                        + page.reason()
                                                        + " | local access="
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

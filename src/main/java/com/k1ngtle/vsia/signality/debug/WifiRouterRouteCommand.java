package com.k1ngtle.vsia.signality.debug;

import com.k1ngtle.vsia.Vsia;
import com.k1ngtle.vsia.signality.engineering.wifi.instrument.WifiEngineeringResolution;
import com.k1ngtle.vsia.signality.engineering.wifi.instrument.WifiEngineeringTargetResolver;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;


@Mod.EventBusSubscriber(modid = Vsia.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class WifiRouterRouteCommand {
    private WifiRouterRouteCommand() {}

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("wifiw1routerroute")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("x", IntegerArgumentType.integer())
                        .then(Commands.argument("y", IntegerArgumentType.integer())
                        .then(Commands.argument("z", IntegerArgumentType.integer())
                        .then(Commands.argument("network", StringArgumentType.word())
                        .then(Commands.argument("prefix", IntegerArgumentType.integer(0, 32))
                        .then(Commands.argument("nextHop", StringArgumentType.word())
                        .then(Commands.argument("iface", StringArgumentType.word())
                        .then(Commands.argument("metric", IntegerArgumentType.integer(0))
                        .executes(context -> run(
                                context.getSource(),
                                new BlockPos(
                                        IntegerArgumentType.getInteger(context, "x"),
                                        IntegerArgumentType.getInteger(context, "y"),
                                        IntegerArgumentType.getInteger(context, "z")
                                ),
                                StringArgumentType.getString(context, "network"),
                                IntegerArgumentType.getInteger(context, "prefix"),
                                StringArgumentType.getString(context, "nextHop"),
                                StringArgumentType.getString(context, "iface"),
                                IntegerArgumentType.getInteger(context, "metric")
                        ))))))))))
        );
    }

    private static int run(
            CommandSourceStack source,
            BlockPos pos,
            String network,
            int prefix,
            String nextHop,
            String iface,
            int metric
    ) {
        WifiEngineeringResolution resolution =
                WifiEngineeringTargetResolver.resolve(source.getLevel(), pos);

        if (!resolution.resolved()) {
            source.sendFailure(Component.literal(resolution.failureDetail()));
            return 0;
        }

        String resolvedNextHop =
                "onlink".equalsIgnoreCase(nextHop)
                        ? ""
                        : nextHop;

        boolean ok =
                resolution.target().device()
                        .addWifiLiveRouterRoute(
                                network,
                                prefix,
                                resolvedNextHop,
                                iface,
                                metric
                        );

        if (!ok) {
            source.sendFailure(Component.literal("Router route rejected."));
            return 0;
        }

        source.sendSuccess(
                () -> Component.literal(
                        "Router route added: "
                                + network
                                + "/"
                                + prefix
                                + " via "
                                + (resolvedNextHop.isBlank() ? "on-link" : resolvedNextHop)
                                + " dev "
                                + iface
                                + " metric "
                                + metric
                ),
                false
        );

        return 1;
    }
}

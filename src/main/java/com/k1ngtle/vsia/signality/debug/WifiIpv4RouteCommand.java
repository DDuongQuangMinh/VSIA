package com.k1ngtle.vsia.signality.debug;

import com.k1ngtle.vsia.Vsia;
import com.k1ngtle.vsia.signality.engineering.wifi.instrument.WifiEngineeringResolution;
import com.k1ngtle.vsia.signality.engineering.wifi.instrument.WifiEngineeringTargetResolver;
import com.k1ngtle.vsia.signality.engineering.wifi.ip.routing.Ipv4RouteDecision;
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

@Mod.EventBusSubscriber(
        modid = Vsia.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class WifiIpv4RouteCommand {
    private WifiIpv4RouteCommand() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(
            RegisterCommandsEvent event
    ) {
        register(event.getDispatcher());
    }

    public static void register(
            CommandDispatcher<CommandSourceStack> dispatcher
    ) {
        dispatcher.register(
                Commands.literal("wifiw1route")
                        .requires(source -> source.hasPermission(2))
                        .then(
                                Commands.argument(
                                                "x",
                                                IntegerArgumentType.integer()
                                        )
                                        .then(
                                                Commands.argument(
                                                                "y",
                                                                IntegerArgumentType.integer()
                                                        )
                                                        .then(
                                                                Commands.argument(
                                                                                "z",
                                                                                IntegerArgumentType.integer()
                                                                        )
                                                                        .then(
                                                                                Commands.argument(
                                                                                                "destination",
                                                                                                StringArgumentType.word()
                                                                                        )
                                                                                        .executes(
                                                                                                context -> run(
                                                                                                        context.getSource(),
                                                                                                        new BlockPos(
                                                                                                                IntegerArgumentType.getInteger(context, "x"),
                                                                                                                IntegerArgumentType.getInteger(context, "y"),
                                                                                                                IntegerArgumentType.getInteger(context, "z")
                                                                                                        ),
                                                                                                        StringArgumentType.getString(
                                                                                                                context,
                                                                                                                "destination"
                                                                                                        )
                                                                                                )
                                                                                        )
                                                                        )
                                                        )
                                        )
                        )
        );
    }

    private static int run(
            CommandSourceStack source,
            BlockPos requested,
            String destination
    ) {
        WifiEngineeringResolution resolution =
                WifiEngineeringTargetResolver.resolve(
                        source.getLevel(),
                        requested
                );

        if (!resolution.resolved()) {
            source.sendFailure(
                    Component.literal(
                            resolution.failureDetail()
                    )
            );
            return 0;
        }

        Ipv4RouteDecision route =
                resolution.target()
                        .device()
                        .wifiIpv4RouteDecision(
                                destination
                        );

        source.sendSuccess(
                () -> Component.literal(
                        route.detail()
                                + " | final="
                                + route.destinationIp()
                                + " | next-hop="
                                + route.nextHopIp()
                                + " | metric="
                                + route.metric()
                ),
                false
        );

        return route.reachable() ? 1 : 0;
    }
}

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

@Mod.EventBusSubscriber(
        modid = Vsia.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class WifiRouterTtlProbeCommand {
    private WifiRouterTtlProbeCommand() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(
            RegisterCommandsEvent event
    ) {
        event.getDispatcher().register(
                Commands.literal(
                                "wifiw1ttlprobe"
                        )
                        .requires(
                                source ->
                                        source.hasPermission(2)
                        )
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
                                                                                        .then(
                                                                                                Commands.argument(
                                                                                                                "ttl",
                                                                                                                IntegerArgumentType.integer(
                                                                                                                        1,
                                                                                                                        255
                                                                                                                )
                                                                                                        )
                                                                                                        .executes(
                                                                                                                context -> run(
                                                                                                                        context.getSource(),
                                                                                                                        new BlockPos(
                                                                                                                                IntegerArgumentType.getInteger(context, "x"),
                                                                                                                                IntegerArgumentType.getInteger(context, "y"),
                                                                                                                                IntegerArgumentType.getInteger(context, "z")
                                                                                                                        ),
                                                                                                                        StringArgumentType.getString(context, "destination"),
                                                                                                                        IntegerArgumentType.getInteger(context, "ttl")
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
            BlockPos pos,
            String destination,
            int ttl
    ) {
        WifiEngineeringResolution resolution =
                WifiEngineeringTargetResolver.resolve(
                        source.getLevel(),
                        pos
                );

        if (!resolution.resolved()) {
            source.sendFailure(
                    Component.literal(
                            resolution.failureDetail()
                    )
            );
            return 0;
        }

        boolean started =
                resolution.target()
                        .device()
                        .sendWifiTtlProbe(
                                destination,
                                ttl
                        );

        if (!started) {
            source.sendFailure(
                    Component.literal(
                            "TTL probe could not start."
                    )
            );
            return 0;
        }

        source.sendSuccess(
                () -> Component.literal(
                        "TTL probe started: "
                                + destination
                                + " ttl="
                                + ttl
                ),
                false
        );

        return 1;
    }
}

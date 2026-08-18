package com.k1ngtle.vsia.signality.debug;

import com.k1ngtle.vsia.Vsia;
import com.k1ngtle.vsia.signality.engineering.ExecutionMode;
import com.k1ngtle.vsia.signality.engineering.wifi.instrument.WifiEngineeringResolution;
import com.k1ngtle.vsia.signality.engineering.wifi.instrument.WifiEngineeringTargetResolver;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.ChatFormatting;
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
public final class WifiTcpCarrierModeCommand {
    private WifiTcpCarrierModeCommand() {
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
                                "wifiw1tcpcarrier"
                        )
                        .requires(
                                source ->
                                        source.hasPermission(
                                                2
                                        )
                        )
                        .then(
                                Commands.literal(
                                                "simulation"
                                        )
                                        .then(
                                                coordinates(
                                                        ExecutionMode.SIMULATION
                                                )
                                        )
                        )
                        .then(
                                Commands.literal(
                                                "conformance"
                                        )
                                        .then(
                                                coordinates(
                                                        ExecutionMode.CONFORMANCE
                                                )
                                        )
                        )
        );
    }

    private static com.mojang.brigadier.builder.RequiredArgumentBuilder<
            CommandSourceStack,
            Integer> coordinates(
            ExecutionMode mode
    ) {
        return Commands.argument(
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
                                                .executes(
                                                        context ->
                                                                apply(
                                                                        context.getSource(),
                                                                        new BlockPos(
                                                                                IntegerArgumentType.getInteger(
                                                                                        context,
                                                                                        "x"
                                                                                ),
                                                                                IntegerArgumentType.getInteger(
                                                                                        context,
                                                                                        "y"
                                                                                ),
                                                                                IntegerArgumentType.getInteger(
                                                                                        context,
                                                                                        "z"
                                                                                )
                                                                        ),
                                                                        mode
                                                                )
                                                )
                                )
                );
    }

    private static int apply(
            CommandSourceStack source,
            BlockPos requestedPos,
            ExecutionMode mode
    ) {
        WifiEngineeringResolution resolution =
                WifiEngineeringTargetResolver.resolve(
                        source.getLevel(),
                        requestedPos
                );

        if (!resolution.resolved()) {
            source.sendFailure(
                    Component.literal(
                            resolution.failureDetail()
                    )
            );

            return 0;
        }

        resolution.target()
                .device()
                .setWifiTcpExecutionMode(
                        mode
                );

        source.sendSuccess(
                () ->
                        Component.literal(
                                "Wi-Fi TCP carrier "
                                        + mode
                                        + " | "
                                        + resolution.target()
                                        .routeDescription()
                        ).withStyle(
                                mode == ExecutionMode.CONFORMANCE
                                        ? ChatFormatting.AQUA
                                        : ChatFormatting.GREEN
                        ),
                false
        );

        return 1;
    }
}

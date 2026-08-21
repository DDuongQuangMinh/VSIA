package com.k1ngtle.vsia.signality.debug;

import com.k1ngtle.vsia.Vsia;
import com.k1ngtle.vsia.signality.internet.NetworkDeviceBlockEntity;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = Vsia.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class WifiW119Command {
    private WifiW119Command() {
    }

    @SubscribeEvent
    public static void register(
            RegisterCommandsEvent event
    ) {
        event.getDispatcher().register(
                Commands.literal("wifiw119")
                        .requires(
                                source ->
                                        source.hasPermission(2)
                        )
                        .then(
                                Commands.literal("status")
                                        .then(
                                                positionArgs(
                                                        Operation.STATUS
                                                )
                                        )
                        )
                        .then(
                                Commands.literal("clear")
                                        .then(
                                                positionArgs(
                                                        Operation.CLEAR
                                                )
                                        )
                        )
                        .then(
                                Commands.literal("isolation")
                                        .then(
                                                Commands.argument(
                                                                "enabled",
                                                                BoolArgumentType.bool()
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
                                                                                                        .executes(
                                                                                                                context ->
                                                                                                                        isolation(
                                                                                                                                context.getSource(),
                                                                                                                                new BlockPos(
                                                                                                                                        IntegerArgumentType.getInteger(context, "x"),
                                                                                                                                        IntegerArgumentType.getInteger(context, "y"),
                                                                                                                                        IntegerArgumentType.getInteger(context, "z")
                                                                                                                                ),
                                                                                                                                BoolArgumentType.getBool(context, "enabled")
                                                                                                                        )
                                                                                                        )
                                                                                        )
                                                                        )
                                                        )
                                        )
                        )
        );
    }

    private static com.mojang.brigadier.builder.RequiredArgumentBuilder<
            net.minecraft.commands.CommandSourceStack,
            Integer> positionArgs(
            Operation operation
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
                                                                run(
                                                                        context.getSource(),
                                                                        new BlockPos(
                                                                                IntegerArgumentType.getInteger(context, "x"),
                                                                                IntegerArgumentType.getInteger(context, "y"),
                                                                                IntegerArgumentType.getInteger(context, "z")
                                                                        ),
                                                                        operation
                                                                )
                                                )
                                )
                );
    }

    private static int run(
            net.minecraft.commands.CommandSourceStack source,
            BlockPos position,
            Operation operation
    ) {
        NetworkDeviceBlockEntity device =
                deviceAt(
                        source,
                        position
                );

        if (device == null) {
            return 0;
        }

        if (operation == Operation.CLEAR) {
            device.w119ClearBridgeState();

            source.sendSuccess(
                    () -> Component.literal(
                            "W1.19 AP bridge dynamic state cleared"
                    ).withStyle(
                            ChatFormatting.GREEN
                    ),
                    false
            );

            return 1;
        }

        source.sendSuccess(
                () -> Component.literal(
                        device.w119BridgeStatus()
                ).withStyle(
                        ChatFormatting.AQUA
                ),
                false
        );

        source.sendSuccess(
                () -> Component.literal(
                        "mode="
                                + device.wifiMode()
                                + " stationState="
                                + device.wifiStationState()
                                + " security="
                                + device.wifiSecurityState()
                ),
                false
        );

        return 1;
    }

    private static int isolation(
            net.minecraft.commands.CommandSourceStack source,
            BlockPos position,
            boolean enabled
    ) {
        NetworkDeviceBlockEntity device =
                deviceAt(
                        source,
                        position
                );

        if (device == null) {
            return 0;
        }

        device.w119SetClientIsolation(
                enabled
        );

        source.sendSuccess(
                () -> Component.literal(
                        "W1.19 client isolation="
                                + enabled
                ).withStyle(
                        ChatFormatting.GREEN
                ),
                false
        );

        return 1;
    }

    private static NetworkDeviceBlockEntity deviceAt(
            net.minecraft.commands.CommandSourceStack source,
            BlockPos position
    ) {
        BlockEntity blockEntity =
                source.getLevel()
                        .getBlockEntity(
                                position
                        );

        if (!(blockEntity
                instanceof NetworkDeviceBlockEntity device)) {
            source.sendFailure(
                    Component.literal(
                            "W1.19 target is not a NetworkDeviceBlockEntity. Use the logical AP/rack block position."
                    )
            );

            return null;
        }

        return device;
    }

    private enum Operation {
        STATUS,
        CLEAR
    }
}

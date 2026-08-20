package com.k1ngtle.vsia.signality.debug;

import com.k1ngtle.vsia.Vsia;
import com.k1ngtle.vsia.signality.engineering.wifi.instrument.WifiEngineeringResolution;
import com.k1ngtle.vsia.signality.engineering.wifi.instrument.WifiEngineeringTargetResolver;
import com.k1ngtle.vsia.signality.internet.NetworkDeviceBlockEntity;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = Vsia.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class WifiPmtuCommand {
    private WifiPmtuCommand() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(
            RegisterCommandsEvent event
    ) {
        event.getDispatcher()
                .register(
                        Commands.literal(
                                        "wifiw1pmtu"
                                )
                                .requires(
                                        source ->
                                                source.hasPermission(
                                                        2
                                                )
                                )
                                .then(
                                        Commands.literal(
                                                        "discover"
                                                )
                                                .then(
                                                        xyzDestinationBytes()
                                                )
                                )
                                .then(
                                        Commands.literal(
                                                        "status"
                                                )
                                                .then(
                                                        xyzStatus()
                                                )
                                )
                                .then(
                                        Commands.literal(
                                                        "set-interface"
                                                )
                                                .then(
                                                        xyzInterfaceMtu()
                                                )
                                )
                );
    }

    private static com.mojang.brigadier.builder.ArgumentBuilder<
            net.minecraft.commands.CommandSourceStack, ?>
    xyzDestinationBytes() {
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
                                                .then(
                                                        Commands.argument(
                                                                        "destination",
                                                                        StringArgumentType.word()
                                                                )
                                                                .then(
                                                                        Commands.argument(
                                                                                        "bytes",
                                                                                        IntegerArgumentType.integer(
                                                                                                68,
                                                                                                65535
                                                                                        )
                                                                                )
                                                                                .executes(
                                                                                        context ->
                                                                                                discover(
                                                                                                        context.getSource(),
                                                                                                        pos(context),
                                                                                                        StringArgumentType.getString(context, "destination"),
                                                                                                        IntegerArgumentType.getInteger(context, "bytes")
                                                                                                )
                                                                                )
                                                                )
                                                )
                                )
                );
    }

    private static com.mojang.brigadier.builder.ArgumentBuilder<
            net.minecraft.commands.CommandSourceStack, ?>
    xyzStatus() {
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
                                                                status(
                                                                        context.getSource(),
                                                                        pos(context)
                                                                )
                                                )
                                )
                );
    }

    private static com.mojang.brigadier.builder.ArgumentBuilder<
            net.minecraft.commands.CommandSourceStack, ?>
    xyzInterfaceMtu() {
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
                                                .then(
                                                        Commands.argument(
                                                                        "interface",
                                                                        StringArgumentType.word()
                                                                )
                                                                .then(
                                                                        Commands.argument(
                                                                                        "mtu",
                                                                                        IntegerArgumentType.integer(
                                                                                                68,
                                                                                                65535
                                                                                        )
                                                                                )
                                                                                .executes(
                                                                                        context ->
                                                                                                setInterface(
                                                                                                        context.getSource(),
                                                                                                        pos(context),
                                                                                                        StringArgumentType.getString(context, "interface"),
                                                                                                        IntegerArgumentType.getInteger(context, "mtu")
                                                                                                )
                                                                                )
                                                                )
                                                )
                                )
                );
    }

    private static BlockPos pos(
            com.mojang.brigadier.context.CommandContext<
                    net.minecraft.commands.CommandSourceStack>
                    context
    ) {
        return new BlockPos(
                IntegerArgumentType.getInteger(context, "x"),
                IntegerArgumentType.getInteger(context, "y"),
                IntegerArgumentType.getInteger(context, "z")
        );
    }

    private static int discover(
            net.minecraft.commands.CommandSourceStack source,
            BlockPos requested,
            String destination,
            int bytes
    ) {
        NetworkDeviceBlockEntity device =
                resolve(
                        source,
                        requested
                );

        if (device == null) {
            return 0;
        }

        ServerPlayer player;

        try {
            player =
                    source.getPlayerOrException();
        } catch (Exception exception) {
            source.sendFailure(
                    Component.literal(
                            "Run PMTUD as a player."
                    )
            );
            return 0;
        }

        if (!WifiPmtuManager.start(
                player,
                device,
                destination,
                bytes
        )) {
            source.sendFailure(
                    Component.literal(
                            "PMTUD could not start."
                    )
            );
            return 0;
        }

        source.sendSuccess(
                () ->
                        Component.literal(
                                "PMTUD started "
                                        + destination
                                        + " initial="
                                        + bytes
                                        + " bytes DF=1"
                        ),
                false
        );

        return 1;
    }

    private static int status(
            net.minecraft.commands.CommandSourceStack source,
            BlockPos requested
    ) {
        NetworkDeviceBlockEntity device =
                resolve(
                        source,
                        requested
                );

        if (device == null) {
            return 0;
        }

        source.sendSuccess(
                () ->
                        Component.literal(
                                WifiPmtuManager.status(
                                        device
                                )
                        ),
                false
        );

        return 1;
    }

    private static int setInterface(
            net.minecraft.commands.CommandSourceStack source,
            BlockPos requested,
            String interfaceName,
            int mtu
    ) {
        NetworkDeviceBlockEntity device =
                resolve(
                        source,
                        requested
                );

        if (device == null) {
            return 0;
        }

        if (!device.setWifiRouterInterfaceMtu(
                interfaceName,
                mtu
        )) {
            source.sendFailure(
                    Component.literal(
                            "Unknown router interface "
                                    + interfaceName
                    )
            );
            return 0;
        }

        source.sendSuccess(
                () ->
                        Component.literal(
                                "Router "
                                        + interfaceName
                                        + " MTU="
                                        + mtu
                        ),
                false
        );

        return 1;
    }

    private static NetworkDeviceBlockEntity resolve(
            net.minecraft.commands.CommandSourceStack source,
            BlockPos requested
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
            return null;
        }

        return resolution.target()
                .device();
    }
}

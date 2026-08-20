package com.k1ngtle.vsia.signality.debug;

import com.k1ngtle.vsia.Vsia;
import com.k1ngtle.vsia.signality.engineering.wifi.instrument.WifiEngineeringResolution;
import com.k1ngtle.vsia.signality.engineering.wifi.instrument.WifiEngineeringTargetResolver;
import com.k1ngtle.vsia.signality.engineering.wifi.ip.traceroute.WifiTracerouteSnapshot;
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
public final class WifiTracerouteCommand {
    private WifiTracerouteCommand() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(
            RegisterCommandsEvent event
    ) {
        event.getDispatcher()
                .register(
                        Commands.literal(
                                        "wifiw1traceroute"
                                )
                                .requires(
                                        source ->
                                                source.hasPermission(
                                                        2
                                                )
                                )
                                .then(
                                        Commands.literal(
                                                        "start"
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
                                                                                                                                        "maxHops",
                                                                                                                                        IntegerArgumentType.integer(
                                                                                                                                                1,
                                                                                                                                                64
                                                                                                                                        )
                                                                                                                                )
                                                                                                                                .executes(
                                                                                                                                        context ->
                                                                                                                                                start(
                                                                                                                                                        context.getSource(),
                                                                                                                                                        new BlockPos(
                                                                                                                                                                IntegerArgumentType.getInteger(context, "x"),
                                                                                                                                                                IntegerArgumentType.getInteger(context, "y"),
                                                                                                                                                                IntegerArgumentType.getInteger(context, "z")
                                                                                                                                                        ),
                                                                                                                                                        StringArgumentType.getString(context, "destination"),
                                                                                                                                                        IntegerArgumentType.getInteger(context, "maxHops")
                                                                                                                                                )
                                                                                                                                )
                                                                                                                )
                                                                                                )
                                                                                )
                                                                )
                                                )
                                )
                                .then(
                                        Commands.literal(
                                                        "status"
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
                                                                                                                status(
                                                                                                                        context.getSource(),
                                                                                                                        new BlockPos(
                                                                                                                                IntegerArgumentType.getInteger(context, "x"),
                                                                                                                                IntegerArgumentType.getInteger(context, "y"),
                                                                                                                                IntegerArgumentType.getInteger(context, "z")
                                                                                                                        )
                                                                                                                )
                                                                                                )
                                                                                )
                                                                )
                                                )
                                )
                                .then(
                                        Commands.literal(
                                                        "clear"
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
                                                                                                                clear(
                                                                                                                        context.getSource(),
                                                                                                                        new BlockPos(
                                                                                                                                IntegerArgumentType.getInteger(context, "x"),
                                                                                                                                IntegerArgumentType.getInteger(context, "y"),
                                                                                                                                IntegerArgumentType.getInteger(context, "z")
                                                                                                                        )
                                                                                                                )
                                                                                                )
                                                                                )
                                                                )
                                                )
                                )
                );
    }

    private static int start(
            net.minecraft.commands.CommandSourceStack source,
            BlockPos requested,
            String destination,
            int maxHops
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
                            "Run traceroute as a player."
                    )
            );
            return 0;
        }

        boolean started =
                WifiTracerouteManager.start(
                        player,
                        device,
                        destination,
                        maxHops
                );

        if (!started) {
            source.sendFailure(
                    Component.literal(
                            "Traceroute could not start."
                    )
            );
            return 0;
        }

        source.sendSuccess(
                () ->
                        Component.literal(
                                "Traceroute started: "
                                        + destination
                                        + " max-hops="
                                        + maxHops
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

        WifiTracerouteSnapshot snapshot =
                WifiTracerouteManager.snapshot(
                        device
                );

        source.sendSuccess(
                () ->
                        Component.literal(
                                WifiTracerouteManager.format(
                                        snapshot
                                )
                        ),
                false
        );

        return 1;
    }

    private static int clear(
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

        WifiTracerouteManager.clear(
                device
        );

        source.sendSuccess(
                () ->
                        Component.literal(
                                "Traceroute state cleared."
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

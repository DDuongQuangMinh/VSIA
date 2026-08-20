package com.k1ngtle.vsia.signality.debug;

import com.k1ngtle.vsia.Vsia;
import com.k1ngtle.vsia.signality.engineering.wifi.instrument.WifiEngineeringResolution;
import com.k1ngtle.vsia.signality.engineering.wifi.instrument.WifiEngineeringTargetResolver;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
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
public final class WifiW1107MultiRouterLabCommand {
    private WifiW1107MultiRouterLabCommand() {
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
                                "wifiw1multilab"
                        )
                        .requires(
                                source ->
                                        source.hasPermission(
                                                2
                                        )
                        )
                        .then(
                                Commands.literal(
                                                "setup"
                                        )
                                        .then(
                                                Commands.argument(
                                                                "client",
                                                                BlockPosArgument.blockPos()
                                                        )
                                                        .then(
                                                                Commands.argument(
                                                                                "routerA",
                                                                                BlockPosArgument.blockPos()
                                                                        )
                                                                        .then(
                                                                                Commands.argument(
                                                                                                "routerB",
                                                                                                BlockPosArgument.blockPos()
                                                                                        )
                                                                                        .then(
                                                                                                Commands.argument(
                                                                                                                "server",
                                                                                                                BlockPosArgument.blockPos()
                                                                                                        )
                                                                                                        .executes(
                                                                                                                context ->
                                                                                                                        setup(
                                                                                                                                context.getSource(),
                                                                                                                                BlockPosArgument.getLoadedBlockPos(context, "client"),
                                                                                                                                BlockPosArgument.getLoadedBlockPos(context, "routerA"),
                                                                                                                                BlockPosArgument.getLoadedBlockPos(context, "routerB"),
                                                                                                                                BlockPosArgument.getLoadedBlockPos(context, "server")
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
                                        .executes(
                                                context ->
                                                        status(
                                                                context.getSource()
                                                        )
                                        )
                        )
        );
    }

    private static int setup(
            CommandSourceStack source,
            BlockPos clientRequested,
            BlockPos routerARequested,
            BlockPos routerBRequested,
            BlockPos serverRequested
    ) {
        ServerPlayer player;

        try {
            player =
                    source.getPlayerOrException();
        } catch (Exception exception) {
            source.sendFailure(
                    Component.literal(
                            "Run /wifiw1multilab setup as a player."
                    )
            );
            return 0;
        }

        BlockPos client =
                resolve(
                        source,
                        clientRequested,
                        "client"
                );

        BlockPos routerA =
                resolve(
                        source,
                        routerARequested,
                        "Router A"
                );

        BlockPos routerB =
                resolve(
                        source,
                        routerBRequested,
                        "Router B"
                );

        BlockPos server =
                resolve(
                        source,
                        serverRequested,
                        "server"
                );

        if (client == null
                || routerA == null
                || routerB == null
                || server == null) {
            return 0;
        }

        String result =
                WifiW1107MultiRouterLabManager.setup(
                        player.getUUID(),
                        source.getLevel()
                                .dimension(),
                        client,
                        routerA,
                        routerB,
                        server
                );

        source.sendSuccess(
                () ->
                        Component.literal(
                                result
                        ).withStyle(
                                result.contains(
                                        "READY"
                                )
                                        ? ChatFormatting.AQUA
                                        : ChatFormatting.RED
                        ),
                false
        );

        return result.contains(
                "READY"
        )
                ? 1
                : 0;
    }

    private static int status(
            CommandSourceStack source
    ) {
        ServerPlayer player;

        try {
            player =
                    source.getPlayerOrException();
        } catch (Exception exception) {
            return 0;
        }

        String status =
                WifiW1107MultiRouterLabManager.status(
                        player.getUUID()
                );

        source.sendSuccess(
                () ->
                        Component.literal(
                                status
                        ),
                false
        );

        return 1;
    }

    private static BlockPos resolve(
            CommandSourceStack source,
            BlockPos requested,
            String role
    ) {
        WifiEngineeringResolution resolution =
                WifiEngineeringTargetResolver.resolve(
                        source.getLevel(),
                        requested
                );

        if (!resolution.resolved()) {
            source.sendFailure(
                    Component.literal(
                            "W1.10.7 "
                                    + role
                                    + " resolve failed: "
                                    + resolution.failureDetail()
                    )
            );

            return null;
        }

        return resolution.target()
                .devicePos()
                .immutable();
    }
}

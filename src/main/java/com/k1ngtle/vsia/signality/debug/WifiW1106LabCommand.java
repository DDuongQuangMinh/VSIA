package com.k1ngtle.vsia.signality.debug;

import com.k1ngtle.vsia.Vsia;
import com.k1ngtle.vsia.signality.engineering.wifi.instrument.WifiEngineeringResolution;
import com.k1ngtle.vsia.signality.engineering.wifi.instrument.WifiEngineeringTargetResolver;
import com.k1ngtle.vsia.signality.internet.NetworkDeviceBlockEntity;
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
public final class WifiW1106LabCommand {
    private WifiW1106LabCommand() {
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
                                "wifiw1lab"
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
                                                                                "router",
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
                                                                                                                BlockPosArgument.getLoadedBlockPos(context, "router"),
                                                                                                                BlockPosArgument.getLoadedBlockPos(context, "server")
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
            BlockPos routerRequested,
            BlockPos serverRequested
    ) {
        ServerPlayer player;

        try {
            player =
                    source.getPlayerOrException();
        } catch (Exception exception) {
            source.sendFailure(
                    Component.literal(
                            "Run /wifiw1lab setup as a player."
                    )
            );

            return 0;
        }

        Resolved client =
                resolve(
                        source,
                        clientRequested,
                        "client"
                );

        Resolved router =
                resolve(
                        source,
                        routerRequested,
                        "router"
                );

        Resolved server =
                resolve(
                        source,
                        serverRequested,
                        "server"
                );

        if (client == null
                || router == null
                || server == null) {
            return 0;
        }

        WifiW1106LabManager.start(
                player.getUUID(),
                source.getLevel().dimension(),
                client.pos(),
                router.pos(),
                server.pos(),
                source.getLevel()
                        .getGameTime()
        );

        source.sendSuccess(
                () ->
                        Component.literal(
                                "W1.10.6 auto-lab started | client "
                                        + client.pos().toShortString()
                                        + " | router "
                                        + router.pos().toShortString()
                                        + " | server "
                                        + server.pos().toShortString()
                        ).withStyle(
                                ChatFormatting.AQUA
                        ),
                false
        );

        source.sendSuccess(
                () ->
                        Component.literal(
                                "Automatically configuring AP/stations, IPv4, scan, association and security. Wait for 'W1.10.6 lab READY'."
                        ).withStyle(
                                ChatFormatting.GRAY
                        ),
                false
        );

        return 1;
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

        source.sendSuccess(
                () ->
                        Component.literal(
                                WifiW1106LabManager.status(
                                        player.getUUID()
                                )
                        ),
                false
        );

        return 1;
    }

    private static Resolved resolve(
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
                            "W1.10.6 "
                                    + role
                                    + " resolve failed: "
                                    + resolution.failureDetail()
                    )
            );

            return null;
        }

        NetworkDeviceBlockEntity device =
                resolution.target()
                        .device();

        return new Resolved(
                resolution.target()
                        .devicePos(),
                device
        );
    }

    private record Resolved(
            BlockPos pos,
            NetworkDeviceBlockEntity device
    ) {
    }
}

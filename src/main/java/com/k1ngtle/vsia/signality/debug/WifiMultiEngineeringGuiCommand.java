package com.k1ngtle.vsia.signality.debug;

import com.k1ngtle.vsia.Vsia;
import com.k1ngtle.vsia.network.VsiaNetwork;
import com.k1ngtle.vsia.network.wifi.WifiMultiEngineeringOpenPacket;
import com.k1ngtle.vsia.signality.engineering.wifi.instrument.WifiEngineeringProbe;
import com.k1ngtle.vsia.signality.engineering.wifi.instrument.WifiEngineeringResolution;
import com.k1ngtle.vsia.signality.engineering.wifi.instrument.WifiEngineeringSnapshot;
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Mod.EventBusSubscriber(
        modid = Vsia.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class WifiMultiEngineeringGuiCommand {
    private WifiMultiEngineeringGuiCommand() {
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
                Commands.literal("wifiw1multigui")
                        .requires(source -> source.hasPermission(2))
                        .then(
                                Commands.argument(
                                                "a",
                                                BlockPosArgument.blockPos()
                                        )
                                        .then(
                                                Commands.argument(
                                                                "b",
                                                                BlockPosArgument.blockPos()
                                                        )
                                                        .then(
                                                                Commands.argument(
                                                                                "c",
                                                                                BlockPosArgument.blockPos()
                                                                        )
                                                                        .then(
                                                                                Commands.argument(
                                                                                                "d",
                                                                                                BlockPosArgument.blockPos()
                                                                                        )
                                                                                        .executes(
                                                                                                context -> open(
                                                                                                        context.getSource(),
                                                                                                        List.of(
                                                                                                                BlockPosArgument.getLoadedBlockPos(context, "a"),
                                                                                                                BlockPosArgument.getLoadedBlockPos(context, "b"),
                                                                                                                BlockPosArgument.getLoadedBlockPos(context, "c"),
                                                                                                                BlockPosArgument.getLoadedBlockPos(context, "d")
                                                                                                        )
                                                                                                )
                                                                                        )
                                                                        )
                                                        )
                                        )
                        )
        );
    }

    private static int open(
            CommandSourceStack source,
            List<BlockPos> positions
    ) {
        ServerPlayer player;

        try {
            player = source.getPlayerOrException();
        } catch (Exception exception) {
            source.sendFailure(
                    Component.literal(
                            "This command must be run by a player"
                    )
            );
            return 0;
        }

        if (positions.size() != WifiMultiEngineeringOpenPacket.DEVICE_COUNT) {
            source.sendFailure(
                    Component.literal(
                            "W1.23 multi analyzer requires exactly four Wi-Fi targets"
                    )
            );
            return 0;
        }

        Set<BlockPos> unique = new HashSet<>(positions);
        if (unique.size() != positions.size()) {
            source.sendFailure(
                    Component.literal(
                            "W1.23 multi analyzer requires four distinct target positions"
                    )
            );
            return 0;
        }

        List<WifiEngineeringSnapshot> snapshots =
                new ArrayList<>(positions.size());

        for (int index = 0; index < positions.size(); index++) {
            BlockPos pos = positions.get(index);
            WifiEngineeringResolution resolution =
                    WifiEngineeringTargetResolver.resolve(
                            source.getLevel(),
                            pos
                    );

            if (!resolution.resolved()) {
                char label = (char) ('A' + index);
                source.sendFailure(
                        Component.literal(
                                "Device " + label + " @ "
                                        + pos.toShortString()
                                        + ": "
                                        + resolution.failureDetail()
                        )
                );
                return 0;
            }

            snapshots.add(
                    WifiEngineeringProbe.capture(
                            resolution.target().device()
                    )
            );
        }

        VsiaNetwork.sendToPlayer(
                player,
                new WifiMultiEngineeringOpenPacket(
                        positions,
                        snapshots
                )
        );

        source.sendSuccess(
                () -> Component.literal(
                        "Opened W1.23 four-device Wi-Fi analyzer: "
                                + "A=" + positions.get(0).toShortString()
                                + " | B=" + positions.get(1).toShortString()
                                + " | C=" + positions.get(2).toShortString()
                                + " | D=" + positions.get(3).toShortString()
                ).withStyle(ChatFormatting.AQUA),
                false
        );

        return 1;
    }
}

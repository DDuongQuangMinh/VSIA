package com.k1ngtle.vsia.signality.debug;

import com.k1ngtle.vsia.Vsia;
import com.k1ngtle.vsia.network.VsiaNetwork;
import com.k1ngtle.vsia.network.wifi.WifiMultiEngineeringOpenPacket;
import com.k1ngtle.vsia.signality.api.signal.ISignalReceiver;
import com.k1ngtle.vsia.signality.core.signal.SignalBus;
import com.k1ngtle.vsia.signality.engineering.wifi.instrument.WifiEngineeringDeviceIdentityResolver;
import com.k1ngtle.vsia.signality.engineering.wifi.instrument.WifiEngineeringProbe;
import com.k1ngtle.vsia.signality.engineering.wifi.instrument.WifiEngineeringSnapshot;
import com.k1ngtle.vsia.signality.internet.NetworkDeviceBlockEntity;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

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

        dispatcher.register(
                Commands.literal("wifiw1devices")
                        .requires(source -> source.hasPermission(2))
                        .executes(context ->
                                listLoadedDevices(
                                        context.getSource()
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

        if (positions.size()
                != WifiMultiEngineeringOpenPacket.DEVICE_COUNT) {
            source.sendFailure(
                    Component.literal(
                            "W1.23.3 requires exactly four Wi-Fi targets"
                    )
            );
            return 0;
        }

        Set<BlockPos> uniquePositions =
                new HashSet<>(positions);

        if (uniquePositions.size()
                != positions.size()) {
            source.sendFailure(
                    Component.literal(
                            "W1.23.3 requires four distinct requested world positions"
                    )
            );
            return 0;
        }

        List<WifiEngineeringSnapshot> snapshots =
                new ArrayList<>(positions.size());

        Set<UUID> boundIds =
                new HashSet<>();

        for (int index = 0;
             index < positions.size();
             index++) {
            BlockPos requested =
                    positions.get(index);

            NetworkDeviceBlockEntity device =
                    WifiEngineeringDeviceIdentityResolver
                            .resolveNearWorld(
                                    source.getLevel(),
                                    requested,
                                    boundIds
                            );

            char label =
                    (char) ('A' + index);

            if (device == null) {
                source.sendFailure(
                        Component.literal(
                                "Device "
                                        + label
                                        + " @ "
                                        + requested.toShortString()
                                        + ": no UNIQUE loaded Wi-Fi device within "
                                        + WifiEngineeringDeviceIdentityResolver
                                        .WORLD_ACQUIRE_RADIUS_BLOCKS
                                        + " world blocks. "
                                        + "Use /wifiw1devices to inspect registered VSIA Wi-Fi devices."
                        )
                );

                return 0;
            }

            if (!boundIds.add(device.id())) {
                source.sendFailure(
                        Component.literal(
                                "Device "
                                        + label
                                        + " resolved to duplicate UUID "
                                        + shortUuid(device.id())
                                        + "; analyzer open rejected"
                        )
                );

                return 0;
            }

            snapshots.add(
                    WifiEngineeringProbe.capture(
                            device
                    )
            );

            Vec3 world =
                    device.positionWorld();

            source.sendSuccess(
                    () -> Component.literal(
                            "W1.23.3 "
                                    + label
                                    + " -> UUID "
                                    + shortUuid(device.id())
                                    + " | MAC "
                                    + device.wifiMacAddress()
                                    + " | storage "
                                    + device.getBlockPos()
                                    .toShortString()
                                    + " | world "
                                    + formatWorld(world)
                    ).withStyle(
                            ChatFormatting.GRAY
                    ),
                    false
            );
        }

        if (boundIds.size()
                != WifiMultiEngineeringOpenPacket.DEVICE_COUNT) {
            source.sendFailure(
                    Component.literal(
                            "W1.23.3 internal safety check failed: four unique UUIDs were not acquired"
                    )
            );
            return 0;
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
                        "Opened W1.23.3 analyzer with four UNIQUE persistent UUID targets"
                ).withStyle(
                        ChatFormatting.AQUA
                ),
                false
        );

        return 1;
    }

    private static int listLoadedDevices(
            CommandSourceStack source
    ) {
        ServerLevel level =
                source.getLevel();

        List<NetworkDeviceBlockEntity> devices =
                SignalBus.receiversInLevel(level)
                        .stream()
                        .filter(
                                NetworkDeviceBlockEntity.class::isInstance
                        )
                        .map(
                                NetworkDeviceBlockEntity.class::cast
                        )
                        .filter(
                                WifiEngineeringProbe::supports
                        )
                        .sorted(
                                java.util.Comparator.comparing(
                                        device ->
                                                device.id().toString()
                                )
                        )
                        .toList();

        source.sendSuccess(
                () -> Component.literal(
                        "Loaded/registered Wi-Fi devices: "
                                + devices.size()
                ).withStyle(
                        ChatFormatting.AQUA
                ),
                false
        );

        int index =
                1;

        for (NetworkDeviceBlockEntity device
                : devices) {
            Vec3 world =
                    device.positionWorld();

            String line =
                    "#"
                            + index
                            + " UUID "
                            + shortUuid(device.id())
                            + " | MAC "
                            + device.wifiMacAddress()
                            + " | storage "
                            + device.getBlockPos()
                            .toShortString()
                            + " | world "
                            + formatWorld(world);

            source.sendSuccess(
                    () -> Component.literal(
                            line
                    ),
                    false
            );

            index++;
        }

        if (devices.isEmpty()) {
            source.sendFailure(
                    Component.literal(
                            "No Wi-Fi NetworkDeviceBlockEntity is currently registered in SignalBus"
                    )
            );
        }

        return devices.size();
    }

    private static String shortUuid(
            UUID id
    ) {
        if (id == null) {
            return "n/a";
        }

        String value =
                id.toString();

        return value.substring(
                0,
                Math.min(
                        8,
                        value.length()
                )
        );
    }

    private static String formatWorld(
            Vec3 world
    ) {
        if (world == null) {
            return "n/a";
        }

        return String.format(
                Locale.ROOT,
                "%.2f, %.2f, %.2f",
                world.x,
                world.y,
                world.z
        );
    }
}

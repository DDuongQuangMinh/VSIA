package com.k1ngtle.vsia.signality.debug;

import com.k1ngtle.vsia.Vsia;
import com.k1ngtle.vsia.network.VsiaNetwork;
import com.k1ngtle.vsia.network.wifi.WifiMultiEngineeringOpenPacket;
import com.k1ngtle.vsia.signality.core.signal.SignalBus;
import com.k1ngtle.vsia.signality.engineering.wifi.instrument.WifiEngineeringDeviceIdentityResolver;
import com.k1ngtle.vsia.signality.engineering.wifi.instrument.WifiEngineeringProbe;
import com.k1ngtle.vsia.signality.engineering.wifi.instrument.WifiEngineeringSnapshot;
import com.k1ngtle.vsia.signality.internet.NetworkDeviceBlockEntity;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
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
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    public static void register(
            CommandDispatcher<CommandSourceStack> dispatcher
    ) {
        dispatcher.register(
                Commands.literal("wifiw1multigui")
                        .requires(source -> source.hasPermission(2))
                        .then(
                                Commands.argument("a", BlockPosArgument.blockPos())
                                        .then(
                                                Commands.argument("b", BlockPosArgument.blockPos())
                                                        .then(
                                                                Commands.argument("c", BlockPosArgument.blockPos())
                                                                        .then(
                                                                                Commands.argument("d", BlockPosArgument.blockPos())
                                                                                        .executes(context ->
                                                                                                openByPositions(
                                                                                                        context.getSource(),
                                                                                                        List.of(
                                                                                                                BlockPosArgument.getBlockPos(context, "a"),
                                                                                                                BlockPosArgument.getBlockPos(context, "b"),
                                                                                                                BlockPosArgument.getBlockPos(context, "c"),
                                                                                                                BlockPosArgument.getBlockPos(context, "d")
                                                                                                        )
                                                                                                )
                                                                                        )
                                                                        )
                                                        )
                                        )
                        )
        );

        dispatcher.register(
                Commands.literal("wifiw1multiids")
                        .requires(source -> source.hasPermission(2))
                        .then(
                                Commands.argument("a", StringArgumentType.word())
                                        .then(
                                                Commands.argument("b", StringArgumentType.word())
                                                        .then(
                                                                Commands.argument("c", StringArgumentType.word())
                                                                        .then(
                                                                                Commands.argument("d", StringArgumentType.word())
                                                                                        .executes(context ->
                                                                                                openByPrefixes(
                                                                                                        context.getSource(),
                                                                                                        List.of(
                                                                                                                StringArgumentType.getString(context, "a"),
                                                                                                                StringArgumentType.getString(context, "b"),
                                                                                                                StringArgumentType.getString(context, "c"),
                                                                                                                StringArgumentType.getString(context, "d")
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
                                listLoadedDevices(context.getSource())
                        )
        );
    }

    private static int openByPrefixes(
            CommandSourceStack source,
            List<String> prefixes
    ) {
        ServerPlayer player;

        try {
            player = source.getPlayerOrException();
        } catch (Exception exception) {
            source.sendFailure(
                    Component.literal("This command must be run by a player")
            );
            return 0;
        }

        List<NetworkDeviceBlockEntity> registered =
                registeredDevices(source.getLevel());

        Set<UUID> used = new HashSet<>();
        List<UUID> ids = new ArrayList<>(
                WifiMultiEngineeringOpenPacket.DEVICE_COUNT
        );

        for (int index = 0; index < prefixes.size(); index++) {
            String prefix = normalizePrefix(prefixes.get(index));
            char label = (char) ('A' + index);

            if (prefix.length() < 4) {
                source.sendFailure(
                        Component.literal(
                                "Device "
                                        + label
                                        + ": UUID prefix must contain at least 4 characters"
                        )
                );
                return 0;
            }

            List<NetworkDeviceBlockEntity> matches =
                    registered.stream()
                            .filter(device -> !used.contains(device.id()))
                            .filter(device ->
                                    compactUuid(device.id()).startsWith(prefix)
                            )
                            .toList();

            if (matches.isEmpty()) {
                source.sendFailure(
                        Component.literal(
                                "Device "
                                        + label
                                        + ": no registered Wi-Fi UUID matches "
                                        + prefix
                        )
                );
                return 0;
            }

            if (matches.size() > 1) {
                source.sendFailure(
                        Component.literal(
                                "Device "
                                        + label
                                        + ": UUID prefix "
                                        + prefix
                                        + " is ambiguous; use more characters"
                        )
                );
                return 0;
            }

            NetworkDeviceBlockEntity device = matches.get(0);

            used.add(device.id());
            ids.add(device.id());

            source.sendSuccess(
                    () -> Component.literal(
                            "W1.23.3 "
                                    + label
                                    + " -> UUID "
                                    + device.id()
                                    + " | MAC "
                                    + device.wifiMacAddress()
                                    + " | storage "
                                    + device.getBlockPos().toShortString()
                                    + " | world "
                                    + formatWorld(device.positionWorld())
                    ).withStyle(ChatFormatting.GRAY),
                    false
            );
        }

        VsiaNetwork.sendToPlayer(
                player,
                new WifiMultiEngineeringOpenPacket(ids)
        );

        source.sendSuccess(
                () -> Component.literal(
                        "Opened W1.23.3 analyzer directly by persistent UUID"
                ).withStyle(ChatFormatting.AQUA),
                false
        );

        return 1;
    }

    private static int openByPositions(
            CommandSourceStack source,
            List<BlockPos> positions
    ) {
        ServerPlayer player;

        try {
            player = source.getPlayerOrException();
        } catch (Exception exception) {
            source.sendFailure(
                    Component.literal("This command must be run by a player")
            );
            return 0;
        }

        Set<BlockPos> uniquePositions = new HashSet<>(positions);

        if (positions.size() != WifiMultiEngineeringOpenPacket.DEVICE_COUNT
                || uniquePositions.size() != positions.size()) {
            source.sendFailure(
                    Component.literal(
                            "W1.23.3 requires four distinct positions"
                    )
            );
            return 0;
        }

        List<WifiEngineeringSnapshot> snapshots =
                new ArrayList<>(positions.size());

        Set<UUID> boundIds = new HashSet<>();

        for (int index = 0; index < positions.size(); index++) {
            BlockPos requested = positions.get(index);

            NetworkDeviceBlockEntity device =
                    WifiEngineeringDeviceIdentityResolver.resolveNearWorld(
                            source.getLevel(),
                            requested,
                            boundIds
                    );

            char label = (char) ('A' + index);

            if (device == null) {
                source.sendFailure(
                        Component.literal(
                                "Device "
                                        + label
                                        + " @ "
                                        + requested.toShortString()
                                        + ": no UNIQUE registered Wi-Fi device resolved. "
                                        + "For VS ships use /wifiw1multiids."
                        )
                );
                return 0;
            }

            if (!boundIds.add(device.id())) {
                source.sendFailure(
                        Component.literal(
                                "Duplicate UUID for Device "
                                        + label
                        )
                );
                return 0;
            }

            snapshots.add(WifiEngineeringProbe.capture(device));
        }

        VsiaNetwork.sendToPlayer(
                player,
                new WifiMultiEngineeringOpenPacket(
                        positions,
                        snapshots
                )
        );

        return 1;
    }

    private static int listLoadedDevices(
            CommandSourceStack source
    ) {
        List<NetworkDeviceBlockEntity> devices =
                registeredDevices(source.getLevel());

        source.sendSuccess(
                () -> Component.literal(
                        "Registered Wi-Fi devices: " + devices.size()
                ).withStyle(ChatFormatting.AQUA),
                false
        );

        int index = 1;

        for (NetworkDeviceBlockEntity device : devices) {
            String line =
                    "#"
                            + index
                            + " UUID "
                            + device.id()
                            + " | short "
                            + shortUuid(device.id())
                            + " | MAC "
                            + device.wifiMacAddress()
                            + " | storage "
                            + device.getBlockPos().toShortString()
                            + " | world "
                            + formatWorld(device.positionWorld());

            source.sendSuccess(
                    () -> Component.literal(line),
                    false
            );

            index++;
        }

        return devices.size();
    }

    private static List<NetworkDeviceBlockEntity> registeredDevices(
            ServerLevel level
    ) {
        return SignalBus.receiversInLevel(level)
                .stream()
                .filter(NetworkDeviceBlockEntity.class::isInstance)
                .map(NetworkDeviceBlockEntity.class::cast)
                .filter(WifiEngineeringProbe::supports)
                .filter(device ->
                        !device.isRemoved()
                                && device.getLevel() == level
                )
                .sorted(
                        java.util.Comparator.comparing(
                                device -> device.id().toString()
                        )
                )
                .toList();
    }

    private static String normalizePrefix(String value) {
        return value == null
                ? ""
                : value.trim()
                        .toLowerCase(Locale.ROOT)
                        .replace("-", "");
    }

    private static String compactUuid(UUID id) {
        return id.toString()
                .toLowerCase(Locale.ROOT)
                .replace("-", "");
    }

    private static String shortUuid(UUID id) {
        String value = id.toString();
        return value.substring(0, Math.min(8, value.length()));
    }

    private static String formatWorld(Vec3 world) {
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

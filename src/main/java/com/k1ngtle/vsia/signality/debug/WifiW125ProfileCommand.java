package com.k1ngtle.vsia.signality.debug;

import com.k1ngtle.vsia.Vsia;
import com.k1ngtle.vsia.signality.internet.NetworkDeviceBlockEntity;
import com.k1ngtle.vsia.signality.internet.network.NetworkKind;
import com.k1ngtle.vsia.signality.internet.network.NetworkProfile;
import com.k1ngtle.vsia.signality.internet.network.NetworkProfileRegistry;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Locale;

@Mod.EventBusSubscriber(
        modid = Vsia.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class WifiW125ProfileCommand {
    private WifiW125ProfileCommand() {
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
                                "wifiw125profile"
                        )
                        .requires(
                                source ->
                                        source.hasPermission(
                                                2
                                        )
                        )
                        .then(
                                Commands.literal(
                                                "list"
                                        )
                                        .executes(
                                                context ->
                                                        list(
                                                                context.getSource()
                                                        )
                                        )
                        )
                        .then(
                                Commands.literal(
                                                "set"
                                        )
                                        .then(
                                                Commands.argument(
                                                                "args",
                                                                StringArgumentType.greedyString()
                                                        )
                                                        .executes(
                                                                context ->
                                                                        set(
                                                                                context.getSource(),
                                                                                StringArgumentType.getString(
                                                                                        context,
                                                                                        "args"
                                                                                )
                                                                        )
                                                        )
                                        )
                        )
                        .then(
                                Commands.literal(
                                                "wpa2pair"
                                        )
                                        .then(
                                                Commands.argument(
                                                                "coords",
                                                                StringArgumentType.greedyString()
                                                        )
                                                        .executes(
                                                                context ->
                                                                        wpa2Pair(
                                                                                context.getSource(),
                                                                                StringArgumentType.getString(
                                                                                        context,
                                                                                        "coords"
                                                                                )
                                                                        )
                                                        )
                                        )
                        )
                        .then(
                                Commands.literal(
                                                "show"
                                        )
                                        .then(
                                                Commands.argument(
                                                                "coords",
                                                                StringArgumentType.greedyString()
                                                        )
                                                        .executes(
                                                                context ->
                                                                        show(
                                                                                context.getSource(),
                                                                                StringArgumentType.getString(
                                                                                        context,
                                                                                        "coords"
                                                                                )
                                                                        )
                                                        )
                                        )
                        )
        );
    }

    private static int list(
            CommandSourceStack source
    ) {
        source.sendSuccess(
                () ->
                        Component.literal(
                                "Wi-Fi profiles:"
                        ).withStyle(
                                ChatFormatting.AQUA
                        ),
                false
        );

        int count =
                0;

        for (NetworkProfile profile
                : NetworkProfileRegistry.values()) {
            if (profile.kind()
                    != NetworkKind.WIFI) {
                continue;
            }

            count++;

            line(
                    source,
                    profile.id()
                            + " | "
                            + profile.displayName()
                            + " | "
                            + profile.protocol()
                            + " | "
                            + profile.security()
                            + " | default="
                            + String.format(
                            Locale.ROOT,
                            "%.3f GHz",
                            profile.defaultFrequencyHz()
                                    / 1_000_000_000.0D
                    )
            );
        }

        return count;
    }

    private static int set(
            CommandSourceStack source,
            String raw
    ) {
        String[] parts =
                tokens(
                        raw
                );

        if (parts.length != 4) {
            source.sendFailure(
                    Component.literal(
                            "Usage: /wifiw125profile set <x y z> <profileId|profilePath>"
                    )
            );
            return 0;
        }

        BlockPos pos;

        try {
            pos =
                    pos(
                            parts,
                            0
                    );
        } catch (Exception exception) {
            source.sendFailure(
                    Component.literal(
                            "Coordinates must be integers."
                    )
            );
            return 0;
        }

        NetworkDeviceBlockEntity device =
                device(
                        source,
                        pos
                );

        if (device == null) {
            return 0;
        }

        NetworkProfile profile =
                resolveWifiProfile(
                        parts[3]
                );

        if (profile == null) {
            source.sendFailure(
                    Component.literal(
                            "Unknown Wi-Fi profile: "
                                    + parts[3]
                    )
            );
            return 0;
        }

        if (!device.configureNetworkProfile(
                profile.id()
        )) {
            source.sendFailure(
                    Component.literal(
                            "Device rejected profile "
                                    + profile.id()
                    )
            );
            return 0;
        }

        source.sendSuccess(
                () ->
                        Component.literal(
                                "Profile set: "
                                        + pos.toShortString()
                                        + " -> "
                                        + profile.id()
                                        + " | security="
                                        + profile.security()
                        ).withStyle(
                                ChatFormatting.GREEN
                        ),
                false
        );

        return 1;
    }

    private static int wpa2Pair(
            CommandSourceStack source,
            String raw
    ) {
        String[] parts =
                tokens(
                        raw
                );

        if (parts.length != 6) {
            source.sendFailure(
                    Component.literal(
                            "Usage: /wifiw125profile wpa2pair <STA x y z> <AP x y z>"
                    )
            );
            return 0;
        }

        BlockPos stationPos;
        BlockPos apPos;

        try {
            stationPos =
                    pos(
                            parts,
                            0
                    );

            apPos =
                    pos(
                            parts,
                            3
                    );
        } catch (Exception exception) {
            source.sendFailure(
                    Component.literal(
                            "Coordinates must be integers."
                    )
            );
            return 0;
        }

        NetworkDeviceBlockEntity station =
                device(
                        source,
                        stationPos
                );

        NetworkDeviceBlockEntity ap =
                device(
                        source,
                        apPos
                );

        if (station == null
                || ap == null) {
            return 0;
        }

        NetworkProfile wpa2 =
                NetworkProfileRegistry.values()
                        .stream()
                        .filter(
                                profile ->
                                        profile.kind()
                                                == NetworkKind.WIFI
                                                && "wifi_5".equals(
                                                profile.id()
                                                        .getPath()
                                        )
                                                && "signality:wpa2".equalsIgnoreCase(
                                                profile.security()
                                        )
                        )
                        .findFirst()
                        .orElse(
                                null
                        );

        if (wpa2 == null) {
            source.sendFailure(
                    Component.literal(
                            "Built-in wifi_5 WPA2 profile was not found."
                    )
            );
            return 0;
        }

        boolean stationOk =
                station.configureNetworkProfile(
                        wpa2.id()
                );

        boolean apOk =
                ap.configureNetworkProfile(
                        wpa2.id()
                );

        if (!stationOk
                || !apOk) {
            source.sendFailure(
                    Component.literal(
                            "WPA2 profile selection failed | STA="
                                    + stationOk
                                    + " AP="
                                    + apOk
                    )
            );
            return 0;
        }

        source.sendSuccess(
                () ->
                        Component.literal(
                                "WPA2 pair ready: "
                                        + wpa2.id()
                                        + " / "
                                        + wpa2.displayName()
                                        + " / "
                                        + wpa2.security()
                        ).withStyle(
                                ChatFormatting.GREEN
                        ),
                false
        );

        line(
                source,
                "STA "
                        + stationPos.toShortString()
                        + " | AP "
                        + apPos.toShortString()
                        + " | default="
                        + String.format(
                        Locale.ROOT,
                        "%.3f GHz",
                        wpa2.defaultFrequencyHz()
                                / 1_000_000_000.0D
                )
        );

        line(
                source,
                "Now run: /wifiw125test reset"
        );

        line(
                source,
                "Then run: /wifiw125test live wpa2 "
                        + stationPos.getX()
                        + " "
                        + stationPos.getY()
                        + " "
                        + stationPos.getZ()
                        + " "
                        + apPos.getX()
                        + " "
                        + apPos.getY()
                        + " "
                        + apPos.getZ()
        );

        return 1;
    }

    private static int show(
            CommandSourceStack source,
            String raw
    ) {
        String[] parts =
                tokens(
                        raw
                );

        if (parts.length != 3) {
            source.sendFailure(
                    Component.literal(
                            "Usage: /wifiw125profile show <x y z>"
                    )
            );
            return 0;
        }

        BlockPos pos;

        try {
            pos =
                    pos(
                            parts,
                            0
                    );
        } catch (Exception exception) {
            source.sendFailure(
                    Component.literal(
                            "Coordinates must be integers."
                    )
            );
            return 0;
        }

        NetworkDeviceBlockEntity device =
                device(
                        source,
                        pos
                );

        if (device == null) {
            return 0;
        }

        NetworkProfile profile =
                device.networkProfile();

        line(
                source,
                pos.toShortString()
                        + " | "
                        + profile.id()
                        + " | "
                        + profile.displayName()
                        + " | protocol="
                        + profile.protocol()
                        + " | security="
                        + profile.security()
                        + " | active="
                        + String.format(
                        Locale.ROOT,
                        "%.3f GHz",
                        device.activeFrequencyHz()
                                / 1_000_000_000.0D
                )
        );

        return 1;
    }

    private static NetworkProfile resolveWifiProfile(
            String value
    ) {
        String normalized =
                value == null
                        ? ""
                        : value.trim()
                        .toLowerCase(
                                Locale.ROOT
                        );

        return NetworkProfileRegistry.values()
                .stream()
                .filter(
                        profile ->
                                profile.kind()
                                        == NetworkKind.WIFI
                                        && (
                                        profile.id()
                                                .toString()
                                                .equalsIgnoreCase(
                                                        normalized
                                                )
                                                || profile.id()
                                                .getPath()
                                                .equalsIgnoreCase(
                                                        normalized
                                                )
                                )
                )
                .findFirst()
                .orElse(
                        null
                );
    }

    private static NetworkDeviceBlockEntity device(
            CommandSourceStack source,
            BlockPos pos
    ) {
        BlockEntity blockEntity =
                source.getLevel()
                        .getBlockEntity(
                                pos
                        );

        if (blockEntity
                instanceof NetworkDeviceBlockEntity device) {
            return device;
        }

        source.sendFailure(
                Component.literal(
                        "Expected NetworkDeviceBlockEntity at "
                                + pos.toShortString()
                )
        );

        return null;
    }

    private static String[] tokens(
            String raw
    ) {
        if (raw == null
                || raw.isBlank()) {
            return new String[0];
        }

        return raw.trim()
                .split(
                        "\\s+"
                );
    }

    private static BlockPos pos(
            String[] parts,
            int offset
    ) {
        return new BlockPos(
                Integer.parseInt(
                        parts[offset]
                ),
                Integer.parseInt(
                        parts[offset + 1]
                ),
                Integer.parseInt(
                        parts[offset + 2]
                )
        );
    }

    private static void line(
            CommandSourceStack source,
            String value
    ) {
        source.sendSuccess(
                () ->
                        Component.literal(
                                value
                        ).withStyle(
                                ChatFormatting.GRAY
                        ),
                false
        );
    }
}

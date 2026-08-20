package com.k1ngtle.vsia.signality.debug;

import com.k1ngtle.vsia.Vsia;
import com.k1ngtle.vsia.signality.internet.OSINetworkPacket;
import com.k1ngtle.vsia.signality.internet.server.FirewallBlockEntity;
import com.k1ngtle.vsia.signality.internet.server.FirewallOsSimulator;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod.EventBusSubscriber(
        modid = Vsia.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class WifiW116FirewallValidationCommand {
    private WifiW116FirewallValidationCommand() {
    }

    @SubscribeEvent
    public static void register(
            RegisterCommandsEvent event
    ) {
        event.getDispatcher().register(
                Commands.literal("wifiw1firewall")
                        .requires(source -> source.hasPermission(2))
                        .then(
                                Commands.literal("links")
                                        .then(positionArguments(CommandKind.LINKS))
                        )
                        .then(
                                Commands.literal("preflight")
                                        .then(positionArguments(CommandKind.PREFLIGHT))
                        )
                        .then(
                                Commands.literal("probe")
                                        .then(positionArguments(CommandKind.PROBE))
                        )
        );
    }

    private static com.mojang.brigadier.builder.RequiredArgumentBuilder<
            net.minecraft.commands.CommandSourceStack, Integer> positionArguments(
            CommandKind kind
    ) {
        return Commands.argument("x", IntegerArgumentType.integer())
                .then(
                        Commands.argument("y", IntegerArgumentType.integer())
                                .then(
                                        Commands.argument("z", IntegerArgumentType.integer())
                                                .executes(context ->
                                                        switch (kind) {
                                                            case LINKS -> links(
                                                                    context.getSource(),
                                                                    IntegerArgumentType.getInteger(context, "x"),
                                                                    IntegerArgumentType.getInteger(context, "y"),
                                                                    IntegerArgumentType.getInteger(context, "z")
                                                            );
                                                            case PREFLIGHT -> preflight(
                                                                    context.getSource(),
                                                                    IntegerArgumentType.getInteger(context, "x"),
                                                                    IntegerArgumentType.getInteger(context, "y"),
                                                                    IntegerArgumentType.getInteger(context, "z")
                                                            );
                                                            case PROBE -> probe(
                                                                    context.getSource(),
                                                                    IntegerArgumentType.getInteger(context, "x"),
                                                                    IntegerArgumentType.getInteger(context, "y"),
                                                                    IntegerArgumentType.getInteger(context, "z")
                                                            );
                                                        }
                                                )
                                )
                );
    }

    private static FirewallBlockEntity firewall(
            net.minecraft.commands.CommandSourceStack source,
            int x,
            int y,
            int z
    ) {
        if (source.getLevel().getBlockEntity(
                new BlockPos(x, y, z)
        ) instanceof FirewallBlockEntity firewall) {
            return firewall;
        }

        return null;
    }

    private static int links(
            net.minecraft.commands.CommandSourceStack source,
            int x,
            int y,
            int z
    ) {
        FirewallBlockEntity firewall = firewall(source, x, y, z);

        if (firewall == null) {
            source.sendFailure(
                    Component.literal("W1.16 target is not a FirewallBlockEntity")
            );
            return 0;
        }

        List<BlockPos> links = firewall.getConnectedDevices();

        String lan = links.size() >= 1
                ? links.get(0).toShortString()
                : "NONE";

        String wan = links.size() >= 2
                ? links.get(1).toShortString()
                : "NONE";

        source.sendSuccess(
                () -> Component.literal(
                        "W1.16 LINKS | LAN=" + lan + " | WAN=" + wan
                ).withStyle(ChatFormatting.AQUA),
                false
        );

        if ("NONE".equals(lan) || "NONE".equals(wan)) {
            source.sendSuccess(
                    () -> Component.literal(
                            "W1.16 topology incomplete: both LAN and WAN links are required for real transit."
                    ).withStyle(ChatFormatting.YELLOW),
                    false
            );
        }

        return 1;
    }

    private static int preflight(
            net.minecraft.commands.CommandSourceStack source,
            int x,
            int y,
            int z
    ) {
        FirewallBlockEntity firewall = firewall(source, x, y, z);

        if (firewall == null) {
            source.sendFailure(
                    Component.literal("W1.16 target is not a FirewallBlockEntity")
            );
            return 0;
        }

        FirewallOsSimulator sim = firewall.osSimulators[0];

        FirewallOsSimulator.PortConfig lan =
                sim.portConfigs.get("GigabitEthernet1/1");

        FirewallOsSimulator.PortConfig wan =
                sim.portConfigs.get("GigabitEthernet1/2");

        List<BlockPos> links = firewall.getConnectedDevices();

        source.sendSuccess(
                () -> Component.literal(
                        "W1.16 PREFLIGHT | booted="
                                + sim.isBooted
                                + " | worldLinks="
                                + links.size()
                ).withStyle(ChatFormatting.AQUA),
                false
        );

        source.sendSuccess(
                () -> Component.literal(interfaceStatus("LAN/Gi1/1", lan)),
                false
        );

        source.sendSuccess(
                () -> Component.literal(interfaceStatus("WAN/Gi1/2", wan)),
                false
        );

        if (!sim.isBooted) {
            source.sendSuccess(
                    () -> Component.literal(
                            "BLOCKER: legacy FirewallOsSimulator is not booted."
                    ).withStyle(ChatFormatting.YELLOW),
                    false
            );
        }

        if (lan == null || !lan.up || lan.nameif.isEmpty()) {
            source.sendSuccess(
                    () -> Component.literal(
                            "BLOCKER: Gi1/1 must be UP and have a nameif for real LAN transit."
                    ).withStyle(ChatFormatting.YELLOW),
                    false
            );
        }

        if (wan == null || !wan.up || wan.nameif.isEmpty()) {
            source.sendSuccess(
                    () -> Component.literal(
                            "BLOCKER: Gi1/2 must be UP and have a nameif for real WAN transit."
                    ).withStyle(ChatFormatting.YELLOW),
                    false
            );
        }

        if (links.size() < 2) {
            source.sendSuccess(
                    () -> Component.literal(
                            "BLOCKER: FirewallBlockEntity needs both LAN and WAN world links."
                    ).withStyle(ChatFormatting.YELLOW),
                    false
            );
        }

        return 1;
    }

    private static String interfaceStatus(
            String label,
            FirewallOsSimulator.PortConfig config
    ) {
        if (config == null) {
            return label + "=MISSING";
        }

        return label
                + " up=" + config.up
                + " nameif="
                + (
                config.nameif == null || config.nameif.isEmpty()
                        ? "<empty>"
                        : config.nameif
                )
                + " ip=" + config.ipAddress
                + " mask=" + config.subnetMask
                + " security=" + config.securityLevel;
    }

    private static int probe(
            net.minecraft.commands.CommandSourceStack source,
            int x,
            int y,
            int z
    ) {
        FirewallBlockEntity firewall = firewall(source, x, y, z);

        if (firewall == null) {
            source.sendFailure(
                    Component.literal("W1.16 target is not a FirewallBlockEntity")
            );
            return 0;
        }

        OSINetworkPacket packet = new OSINetworkPacket();

        packet.sourceIp = "192.168.10.100";
        packet.targetIp = "198.51.100.20";
        packet.sourcePort = 51842;
        packet.targetPort = 443;
        packet.ipProtocol = 6;
        packet.applicationProtocol = "TCP";
        packet.ttl = 64;
        packet.payload.putBoolean("tcp_syn", true);

        OSINetworkPacket filtered =
                firewall.osSimulators[0]
                        .w116DirectInspect(
                                packet,
                                "LAN",
                                "WAN"
                        );

        if (filtered == null) {
            source.sendFailure(
                    Component.literal(
                            "W1.16 direct engine probe was dropped by W1.16 policy."
                    )
            );

            source.sendSuccess(
                    () -> Component.literal(
                            firewall.osSimulators[0].w116Status()
                    ),
                    false
            );

            return 0;
        }

        source.sendSuccess(
                () -> Component.literal(
                        "W1.16 DIRECT ENGINE PROBE ACCEPTED | "
                                + filtered.sourceIp
                                + ":"
                                + filtered.sourcePort
                                + " -> "
                                + filtered.targetIp
                                + ":"
                                + filtered.targetPort
                ).withStyle(ChatFormatting.GREEN),
                false
        );

        source.sendSuccess(
                () -> Component.literal(
                        firewall.osSimulators[0].w116Status()
                ),
                false
        );

        return 1;
    }

    private enum CommandKind {
        LINKS,
        PREFLIGHT,
        PROBE
    }
}

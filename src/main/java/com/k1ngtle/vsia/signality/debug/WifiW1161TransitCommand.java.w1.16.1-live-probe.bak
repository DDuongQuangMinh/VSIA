package com.k1ngtle.vsia.signality.debug;

import com.k1ngtle.vsia.Vsia;
import com.k1ngtle.vsia.signality.internet.OSINetworkPacket;
import com.k1ngtle.vsia.signality.internet.server.FirewallBlockEntity;
import com.k1ngtle.vsia.signality.internet.server.FirewallTransitProbeFactory;
import com.k1ngtle.vsia.signality.internet.server.NetworkSwitchBlockEntity;
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
public final class WifiW1161TransitCommand {
    private WifiW1161TransitCommand() {
    }

    @SubscribeEvent
    public static void register(
            RegisterCommandsEvent event
    ) {
        event.getDispatcher().register(
                Commands.literal("wifiw1firewalltransit")
                        .requires(source -> source.hasPermission(2))
                        .then(
                                Commands.literal("status")
                                        .then(firewallPosition(StatusOp.STATUS))
                        )
                        .then(
                                Commands.literal("clear")
                                        .then(firewallPosition(StatusOp.CLEAR))
                        )
                        .then(
                                Commands.literal("unlink")
                                        .then(firewallPosition(StatusOp.UNLINK))
                        )
                        .then(
                                Commands.literal("linklan")
                                        .then(linkArguments(true))
                        )
                        .then(
                                Commands.literal("linkwan")
                                        .then(linkArguments(false))
                        )
                        .then(
                                Commands.literal("probe")
                                        .then(
                                                Commands.argument(
                                                                "fx",
                                                                IntegerArgumentType.integer()
                                                        )
                                                        .then(
                                                                Commands.argument(
                                                                                "fy",
                                                                                IntegerArgumentType.integer()
                                                                        )
                                                                        .then(
                                                                                Commands.argument(
                                                                                                "fz",
                                                                                                IntegerArgumentType.integer()
                                                                                        )
                                                                                        .then(
                                                                                                Commands.literal("lan")
                                                                                                        .executes(
                                                                                                                context -> probe(
                                                                                                                        context.getSource(),
                                                                                                                        pos(
                                                                                                                                context,
                                                                                                                                "fx",
                                                                                                                                "fy",
                                                                                                                                "fz"
                                                                                                                        ),
                                                                                                                        true
                                                                                                                )
                                                                                                        )
                                                                                        )
                                                                                        .then(
                                                                                                Commands.literal("wan")
                                                                                                        .executes(
                                                                                                                context -> probe(
                                                                                                                        context.getSource(),
                                                                                                                        pos(
                                                                                                                                context,
                                                                                                                                "fx",
                                                                                                                                "fy",
                                                                                                                                "fz"
                                                                                                                        ),
                                                                                                                        false
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
            Integer> firewallPosition(
            StatusOp op
    ) {
        return Commands.argument(
                        "fx",
                        IntegerArgumentType.integer()
                )
                .then(
                        Commands.argument(
                                        "fy",
                                        IntegerArgumentType.integer()
                                )
                                .then(
                                        Commands.argument(
                                                        "fz",
                                                        IntegerArgumentType.integer()
                                                )
                                                .executes(
                                                        context -> statusOp(
                                                                context.getSource(),
                                                                pos(
                                                                        context,
                                                                        "fx",
                                                                        "fy",
                                                                        "fz"
                                                                ),
                                                                op
                                                        )
                                                )
                                )
                );
    }

    private static com.mojang.brigadier.builder.RequiredArgumentBuilder<
            net.minecraft.commands.CommandSourceStack,
            Integer> linkArguments(
            boolean lan
    ) {
        return Commands.argument(
                        "fx",
                        IntegerArgumentType.integer()
                )
                .then(
                        Commands.argument(
                                        "fy",
                                        IntegerArgumentType.integer()
                                )
                                .then(
                                        Commands.argument(
                                                        "fz",
                                                        IntegerArgumentType.integer()
                                                )
                                                .then(
                                                        Commands.argument(
                                                                        "tx",
                                                                        IntegerArgumentType.integer()
                                                                )
                                                                .then(
                                                                        Commands.argument(
                                                                                        "ty",
                                                                                        IntegerArgumentType.integer()
                                                                                )
                                                                                .then(
                                                                                        Commands.argument(
                                                                                                        "tz",
                                                                                                        IntegerArgumentType.integer()
                                                                                                )
                                                                                                .executes(
                                                                                                        context -> link(
                                                                                                                context.getSource(),
                                                                                                                pos(
                                                                                                                        context,
                                                                                                                        "fx",
                                                                                                                        "fy",
                                                                                                                        "fz"
                                                                                                                ),
                                                                                                                pos(
                                                                                                                        context,
                                                                                                                        "tx",
                                                                                                                        "ty",
                                                                                                                        "tz"
                                                                                                                ),
                                                                                                                lan
                                                                                                        )
                                                                                                )
                                                                                )
                                                                )
                                                )
                                )
                );
    }

    private static BlockPos pos(
            com.mojang.brigadier.context.CommandContext<
                    net.minecraft.commands.CommandSourceStack> context,
            String x,
            String y,
            String z
    ) {
        return new BlockPos(
                IntegerArgumentType.getInteger(context, x),
                IntegerArgumentType.getInteger(context, y),
                IntegerArgumentType.getInteger(context, z)
        );
    }

    private static FirewallBlockEntity firewall(
            net.minecraft.commands.CommandSourceStack source,
            BlockPos pos
    ) {
        BlockEntity be =
                source.getLevel().getBlockEntity(pos);

        return be instanceof FirewallBlockEntity firewall
                ? firewall
                : null;
    }

    private static int probe(
            net.minecraft.commands.CommandSourceStack source,
            BlockPos firewallPos,
            boolean lan
    ) {
        FirewallBlockEntity firewall =
                firewall(
                        source,
                        firewallPos
                );

        if (firewall == null) {
            source.sendFailure(
                    Component.literal(
                            "W1.16.1 target is not a FirewallBlockEntity"
                    )
            );
            return 0;
        }

        BlockPos switchPos =
                lan
                        ? firewall.getLanConnection()
                        : firewall.getWanConnection();

        if (switchPos == null) {
            source.sendFailure(
                    Component.literal(
                            "W1.16.1 "
                                    + (lan ? "LAN" : "WAN")
                                    + " world link is not configured"
                    )
            );
            return 0;
        }

        BlockEntity be =
                source.getLevel().getBlockEntity(
                        switchPos
                );

        if (!(be instanceof NetworkSwitchBlockEntity networkSwitch)) {
            source.sendFailure(
                    Component.literal(
                            "W1.16.1 "
                                    + (lan ? "LAN" : "WAN")
                                    + " linked device is not a NetworkSwitchBlockEntity"
                    )
            );
            return 0;
        }

        OSINetworkPacket packet =
                lan
                        ? FirewallTransitProbeFactory.lanProbe()
                        : FirewallTransitProbeFactory.wanProbe();

        boolean injected =
                networkSwitch.w1161InjectProbeToward(
                        firewallPos,
                        packet
                );

        if (!injected) {
            source.sendFailure(
                    Component.literal(
                            "W1.16.1 "
                                    + (lan ? "LAN" : "WAN")
                                    + " probe could not traverse switch L2 toward firewall"
                    )
            );
            return 0;
        }

        source.sendSuccess(
                () -> Component.literal(
                        "W1.16.1 "
                                + (lan ? "LAN" : "WAN")
                                + " SWITCH-L2 PROBE injected"
                                + " | switch="
                                + switchPos.toShortString()
                                + " | firewall="
                                + firewallPos.toShortString()
                                + " | "
                                + packet.sourceIp
                                + ":"
                                + packet.sourcePort
                                + " -> "
                                + packet.targetIp
                                + ":"
                                + packet.targetPort
                                + " | ttl="
                                + packet.ttl
                ).withStyle(ChatFormatting.GREEN),
                false
        );

        source.sendSuccess(
                () -> Component.literal(
                        firewall.w1161TransitStatus()
                ).withStyle(ChatFormatting.AQUA),
                false
        );

        return 1;
    }

    private static int link(
            net.minecraft.commands.CommandSourceStack source,
            BlockPos firewallPos,
            BlockPos peerPos,
            boolean lan
    ) {
        FirewallBlockEntity firewall =
                firewall(source, firewallPos);

        if (firewall == null) {
            source.sendFailure(
                    Component.literal(
                            "W1.16.1 target is not a FirewallBlockEntity"
                    )
            );
            return 0;
        }

        BlockEntity peer =
                source.getLevel().getBlockEntity(peerPos);

        if (!(peer instanceof NetworkSwitchBlockEntity networkSwitch)) {
            source.sendFailure(
                    Component.literal(
                            "W1.16.1 peer must be a NetworkSwitchBlockEntity"
                    )
            );
            return 0;
        }

        boolean firewallLinked =
                lan
                        ? firewall.connectLanDevice(peerPos)
                        : firewall.connectWanDevice(peerPos);

        boolean switchLinked =
                networkSwitch.getConnectedDevices().contains(firewallPos)
                        || networkSwitch.connectDevice(firewallPos);

        if (!firewallLinked || !switchLinked) {
            source.sendFailure(
                    Component.literal(
                            "W1.16.1 link failed; check duplicate/full port state"
                    )
            );
            return 0;
        }

        String side =
                lan
                        ? "LAN/Gi1/1"
                        : "WAN/Gi1/2";

        source.sendSuccess(
                () -> Component.literal(
                        "W1.16.1 LINKED "
                                + side
                                + " firewall="
                                + firewallPos.toShortString()
                                + " switch="
                                + peerPos.toShortString()
                ).withStyle(ChatFormatting.GREEN),
                false
        );

        return 1;
    }

    private static int statusOp(
            net.minecraft.commands.CommandSourceStack source,
            BlockPos firewallPos,
            StatusOp op
    ) {
        FirewallBlockEntity firewall =
                firewall(source, firewallPos);

        if (firewall == null) {
            source.sendFailure(
                    Component.literal(
                            "W1.16.1 target is not a FirewallBlockEntity"
                    )
            );
            return 0;
        }

        if (op == StatusOp.CLEAR) {
            firewall.w1161ClearCounters();

            source.sendSuccess(
                    () -> Component.literal(
                            "W1.16.1 transit counters cleared"
                    ),
                    false
            );

            return 1;
        }

        if (op == StatusOp.UNLINK) {
            BlockPos lan =
                    firewall.getLanConnection();

            BlockPos wan =
                    firewall.getWanConnection();

            unlinkPeer(
                    source,
                    firewallPos,
                    lan
            );

            unlinkPeer(
                    source,
                    firewallPos,
                    wan
            );

            firewall.disconnectAllTransitDevices();

            source.sendSuccess(
                    () -> Component.literal(
                            "W1.16.1 LAN/WAN links cleared"
                    ),
                    false
            );

            return 1;
        }

        source.sendSuccess(
                () -> Component.literal(
                        firewall.w1161TransitStatus()
                ).withStyle(ChatFormatting.AQUA),
                false
        );

        return 1;
    }

    private static void unlinkPeer(
            net.minecraft.commands.CommandSourceStack source,
            BlockPos firewallPos,
            BlockPos peerPos
    ) {
        if (peerPos == null) {
            return;
        }

        BlockEntity peer =
                source.getLevel().getBlockEntity(
                        peerPos
                );

        if (peer instanceof NetworkSwitchBlockEntity sw) {
            sw.disconnectDevice(
                    firewallPos
            );
        }
    }

    private enum StatusOp {
        STATUS,
        CLEAR,
        UNLINK
    }
}

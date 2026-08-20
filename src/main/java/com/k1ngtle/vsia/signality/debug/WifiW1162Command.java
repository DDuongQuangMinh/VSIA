package com.k1ngtle.vsia.signality.debug;

import com.k1ngtle.vsia.Vsia;
import com.k1ngtle.vsia.signality.engineering.firewall.Nat44Mapping;
import com.k1ngtle.vsia.signality.engineering.firewall.w1162.W1162FlowFactory;
import com.k1ngtle.vsia.signality.engineering.firewall.w1162.W1162HostProfile;
import com.k1ngtle.vsia.signality.internet.OSINetworkPacket;
import com.k1ngtle.vsia.signality.internet.server.FirewallBlockEntity;
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

import java.util.List;

@Mod.EventBusSubscriber(
        modid = Vsia.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class WifiW1162Command {
    private static final W1162HostProfile LAN_HOST =
            new W1162HostProfile(
                    "LAN-HOST",
                    "192.168.10.20",
                    "255.255.255.0",
                    "192.168.10.1",
                    "02:16:02:00:00:20"
            );

    private static final String WAN_HOST_IP =
            "203.0.113.20";

    private WifiW1162Command() {
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("wifiw1162")
                        .requires(source -> source.hasPermission(2))
                        .then(
                                Commands.literal("status")
                                        .then(positionArgs(Operation.STATUS))
                        )
                        .then(
                                Commands.literal("clear")
                                        .then(positionArgs(Operation.CLEAR))
                        )
                        .then(
                                Commands.literal("mappings")
                                        .then(positionArgs(Operation.MAPPINGS))
                        )
                        .then(
                                Commands.literal("udp")
                                        .then(flowArgs(false))
                        )
                        .then(
                                Commands.literal("tcp")
                                        .then(flowArgs(true))
                        )
                        .then(
                                Commands.literal("mixed")
                                        .then(flowArgsMixed())
                        )
                        .then(
                                Commands.literal("expire")
                                        .then(expireArgs())
                        )
        );
    }

    private static com.mojang.brigadier.builder.RequiredArgumentBuilder<
            net.minecraft.commands.CommandSourceStack,
            Integer> positionArgs(
            Operation operation
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
                                                        context -> runOperation(
                                                                context.getSource(),
                                                                new BlockPos(
                                                                        IntegerArgumentType.getInteger(
                                                                                context,
                                                                                "fx"
                                                                        ),
                                                                        IntegerArgumentType.getInteger(
                                                                                context,
                                                                                "fy"
                                                                        ),
                                                                        IntegerArgumentType.getInteger(
                                                                                context,
                                                                                "fz"
                                                                        )
                                                                ),
                                                                operation
                                                        )
                                                )
                                )
                );
    }

    private static com.mojang.brigadier.builder.RequiredArgumentBuilder<
            net.minecraft.commands.CommandSourceStack,
            Integer> flowArgs(
            boolean tcp
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
                                                                        "count",
                                                                        IntegerArgumentType.integer(
                                                                                1,
                                                                                64
                                                                        )
                                                                )
                                                                .executes(
                                                                        context -> runFlows(
                                                                                context.getSource(),
                                                                                new BlockPos(
                                                                                        IntegerArgumentType.getInteger(
                                                                                                context,
                                                                                                "fx"
                                                                                        ),
                                                                                        IntegerArgumentType.getInteger(
                                                                                                context,
                                                                                                "fy"
                                                                                        ),
                                                                                        IntegerArgumentType.getInteger(
                                                                                                context,
                                                                                                "fz"
                                                                                        )
                                                                                ),
                                                                                IntegerArgumentType.getInteger(
                                                                                        context,
                                                                                        "count"
                                                                                ),
                                                                                tcp
                                                                                        ? Mode.TCP
                                                                                        : Mode.UDP
                                                                        )
                                                                )
                                                )
                                )
                );
    }

    private static com.mojang.brigadier.builder.RequiredArgumentBuilder<
            net.minecraft.commands.CommandSourceStack,
            Integer> flowArgsMixed() {
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
                                                                        "count",
                                                                        IntegerArgumentType.integer(
                                                                                2,
                                                                                64
                                                                        )
                                                                )
                                                                .executes(
                                                                        context -> runFlows(
                                                                                context.getSource(),
                                                                                new BlockPos(
                                                                                        IntegerArgumentType.getInteger(
                                                                                                context,
                                                                                                "fx"
                                                                                        ),
                                                                                        IntegerArgumentType.getInteger(
                                                                                                context,
                                                                                                "fy"
                                                                                        ),
                                                                                        IntegerArgumentType.getInteger(
                                                                                                context,
                                                                                                "fz"
                                                                                        )
                                                                                ),
                                                                                IntegerArgumentType.getInteger(
                                                                                        context,
                                                                                        "count"
                                                                                ),
                                                                                Mode.MIXED
                                                                        )
                                                                )
                                                )
                                )
                );
    }

    private static com.mojang.brigadier.builder.RequiredArgumentBuilder<
            net.minecraft.commands.CommandSourceStack,
            Integer> expireArgs() {
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
                                                                        "seconds",
                                                                        IntegerArgumentType.integer(
                                                                                1,
                                                                                3600
                                                                        )
                                                                )
                                                                .executes(
                                                                        context -> expire(
                                                                                context.getSource(),
                                                                                new BlockPos(
                                                                                        IntegerArgumentType.getInteger(
                                                                                                context,
                                                                                                "fx"
                                                                                        ),
                                                                                        IntegerArgumentType.getInteger(
                                                                                                context,
                                                                                                "fy"
                                                                                        ),
                                                                                        IntegerArgumentType.getInteger(
                                                                                                context,
                                                                                                "fz"
                                                                                        )
                                                                                ),
                                                                                IntegerArgumentType.getInteger(
                                                                                        context,
                                                                                        "seconds"
                                                                                )
                                                                        )
                                                                )
                                                )
                                )
                );
    }

    private static FirewallBlockEntity firewall(
            net.minecraft.commands.CommandSourceStack source,
            BlockPos pos
    ) {
        BlockEntity be =
                source.getLevel().getBlockEntity(
                        pos
                );

        return be instanceof FirewallBlockEntity firewall
                ? firewall
                : null;
    }

    private static int runOperation(
            net.minecraft.commands.CommandSourceStack source,
            BlockPos pos,
            Operation operation
    ) {
        FirewallBlockEntity firewall =
                firewall(
                        source,
                        pos
                );

        if (firewall == null) {
            source.sendFailure(
                    Component.literal(
                            "W1.16.2 target is not a FirewallBlockEntity"
                    )
            );
            return 0;
        }

        switch (operation) {
            case CLEAR -> {
                firewall.w1161ClearCounters();
                firewall.osSimulators[0].w116Reset();

                source.sendSuccess(
                        () -> Component.literal(
                                "W1.16.2 state cleared"
                        ),
                        false
                );

                return 1;
            }

            case MAPPINGS -> {
                List<Nat44Mapping> mappings =
                        firewall.osSimulators[0]
                                .w1162NatMappings();

                if (mappings.isEmpty()) {
                    source.sendSuccess(
                            () -> Component.literal(
                                    "W1.16.2 NAT mappings: NONE"
                            ),
                            false
                    );

                    return 1;
                }

                source.sendSuccess(
                        () -> Component.literal(
                                "W1.16.2 NAT mappings="
                                        + mappings.size()
                        ).withStyle(ChatFormatting.YELLOW),
                        false
                );

                for (Nat44Mapping mapping : mappings) {
                    source.sendSuccess(
                            () -> Component.literal(
                                    mapping.protocol()
                                            + " "
                                            + mapping.insideLocalIp()
                                            + ":"
                                            + mapping.insideLocalPort()
                                            + " <-> "
                                            + mapping.insideGlobalIp()
                                            + ":"
                                            + mapping.insideGlobalPort()
                                            + " <-> "
                                            + mapping.outsideIp()
                                            + ":"
                                            + mapping.outsidePort()
                            ),
                            false
                    );
                }

                return 1;
            }

            case STATUS -> {
                source.sendSuccess(
                        () -> Component.literal(
                                "W1.16.2 FULL PHASE"
                                        + " | host="
                                        + LAN_HOST.ipv4()
                                        + "/"
                                        + LAN_HOST.prefixLength()
                                        + " gw="
                                        + LAN_HOST.defaultGateway()
                                        + " | destination="
                                        + WAN_HOST_IP
                                        + " | nextHop="
                                        + LAN_HOST.nextHop(
                                        WAN_HOST_IP
                                )
                        ).withStyle(ChatFormatting.AQUA),
                        false
                );

                source.sendSuccess(
                        () -> Component.literal(
                                firewall.w1161TransitStatus()
                        ).withStyle(ChatFormatting.AQUA),
                        false
                );

                source.sendSuccess(
                        () -> Component.literal(
                                firewall.osSimulators[0]
                                        .w116Status()
                        ),
                        false
                );

                return 1;
            }
        }

        return 0;
    }

    private static int runFlows(
            net.minecraft.commands.CommandSourceStack source,
            BlockPos firewallPos,
            int count,
            Mode mode
    ) {
        FirewallBlockEntity firewall =
                firewall(
                        source,
                        firewallPos
                );

        if (firewall == null) {
            source.sendFailure(
                    Component.literal(
                            "W1.16.2 target is not a FirewallBlockEntity"
                    )
            );
            return 0;
        }

        BlockPos lanPos =
                firewall.getLanConnection();

        BlockPos wanPos =
                firewall.getWanConnection();

        if (lanPos == null || wanPos == null) {
            source.sendFailure(
                    Component.literal(
                            "W1.16.2 requires both LAN and WAN switch links"
                    )
            );
            return 0;
        }

        BlockEntity lanBe =
                source.getLevel().getBlockEntity(
                        lanPos
                );

        BlockEntity wanBe =
                source.getLevel().getBlockEntity(
                        wanPos
                );

        if (!(lanBe instanceof NetworkSwitchBlockEntity lanSwitch)
                || !(wanBe instanceof NetworkSwitchBlockEntity wanSwitch)) {
            source.sendFailure(
                    Component.literal(
                            "W1.16.2 requires NetworkSwitchBlockEntity on both sides"
                    )
            );
            return 0;
        }

        int successfulOutbound = 0;
        int successfulReturn = 0;

        for (int i = 0; i < count; i++) {
            boolean tcp =
                    mode == Mode.TCP
                            || (
                            mode == Mode.MIXED
                                    && (i & 1) == 1
                    );

            int sourcePort =
                    (tcp ? 52000 : 51000)
                            + i;

            String protocol =
                    tcp
                            ? "TCP"
                            : "UDP";

            OSINetworkPacket outbound =
                    tcp
                            ? W1162FlowFactory.outboundTcpSyn(
                            LAN_HOST,
                            WAN_HOST_IP,
                            sourcePort,
                            443,
                            "FF:FF:FF:FF:FF:FF",
                            i
                    )
                            : W1162FlowFactory.outboundUdp(
                            LAN_HOST,
                            WAN_HOST_IP,
                            sourcePort,
                            443,
                            "FF:FF:FF:FF:FF:FF",
                            i
                    );

            boolean injected =
                    lanSwitch.w1161InjectProbeToward(
                            firewallPos,
                            outbound
                    );

            if (!injected) {
                continue;
            }

            Nat44Mapping mapping =
                    firewall.osSimulators[0]
                            .w1162FindNatMapping(
                                    protocol,
                                    LAN_HOST.ipv4(),
                                    sourcePort,
                                    WAN_HOST_IP,
                                    443
                            );

            if (mapping == null) {
                continue;
            }

            successfulOutbound++;

            OSINetworkPacket reply =
                    W1162FlowFactory.reply(
                            mapping,
                            i
                    );

            if (wanSwitch.w1161InjectProbeToward(
                    firewallPos,
                    reply
            )) {
                successfulReturn++;
            }
        }

        int out = successfulOutbound;
        int ret = successfulReturn;

        source.sendSuccess(
                () -> Component.literal(
                        "W1.16.2 "
                                + mode
                                + " burst"
                                + " | requested="
                                + count
                                + " | outbound="
                                + out
                                + " | returns="
                                + ret
                ).withStyle(
                        out == count && ret == count
                                ? ChatFormatting.GREEN
                                : ChatFormatting.YELLOW
                ),
                false
        );

        source.sendSuccess(
                () -> Component.literal(
                        firewall.w1161TransitStatus()
                ).withStyle(ChatFormatting.AQUA),
                false
        );

        source.sendSuccess(
                () -> Component.literal(
                        firewall.osSimulators[0]
                                .w116Status()
                ),
                false
        );

        return out == count
                && ret == count
                ? 1
                : 0;
    }

    private static int expire(
            net.minecraft.commands.CommandSourceStack source,
            BlockPos firewallPos,
            int seconds
    ) {
        FirewallBlockEntity firewall =
                firewall(
                        source,
                        firewallPos
                );

        if (firewall == null) {
            source.sendFailure(
                    Component.literal(
                            "W1.16.2 target is not a FirewallBlockEntity"
                    )
            );
            return 0;
        }

        long future =
                System.currentTimeMillis()
                        + seconds * 1000L;

        int nat =
                firewall.osSimulators[0]
                        .w1162ExpireNat(
                                future
                        );

        int conn =
                firewall.osSimulators[0]
                        .w1162ExpireConntrack(
                                future
                        );

        source.sendSuccess(
                () -> Component.literal(
                        "W1.16.2 EXPIRE"
                                + " | seconds="
                                + seconds
                                + " | natRemoved="
                                + nat
                                + " | conntrackRemoved="
                                + conn
                                + " | natRemaining="
                                + firewall.osSimulators[0]
                                .w1161NatCount()
                                + " | conntrackRemaining="
                                + firewall.osSimulators[0]
                                .w1161ConntrackCount()
                ).withStyle(ChatFormatting.YELLOW),
                false
        );

        return 1;
    }

    private enum Operation {
        STATUS,
        CLEAR,
        MAPPINGS
    }

    private enum Mode {
        UDP,
        TCP,
        MIXED
    }
}

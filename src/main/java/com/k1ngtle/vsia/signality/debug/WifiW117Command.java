package com.k1ngtle.vsia.signality.debug;

import com.k1ngtle.vsia.Vsia;
import com.k1ngtle.vsia.signality.engineering.firewall.w117.W117ArpFrame;
import com.k1ngtle.vsia.signality.engineering.firewall.w117.W117HostEndpoint;
import com.k1ngtle.vsia.signality.internet.OSINetworkPacket;
import com.k1ngtle.vsia.signality.internet.server.FirewallBlockEntity;
import com.k1ngtle.vsia.signality.internet.server.NetworkSwitchBlockEntity;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
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
public final class WifiW117Command {
    private WifiW117Command() {
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("wifiw117")
                        .requires(source -> source.hasPermission(2))
                        .then(
                                Commands.literal("setup")
                                        .then(positionArgs(Operation.SETUP))
                        )
                        .then(
                                Commands.literal("status")
                                        .then(positionArgs(Operation.STATUS))
                        )
                        .then(
                                Commands.literal("clear")
                                        .then(positionArgs(Operation.CLEAR))
                        )
                        .then(
                                Commands.literal("udp")
                                        .then(positionArgs(Operation.UDP))
                        )
                        .then(
                                Commands.literal("tcp")
                                        .then(positionArgs(Operation.TCP))
                        )
                        .then(
                                Commands.literal("host2host")
                                        .then(positionArgs(Operation.HOST2HOST))
                        )
                        .then(
                                Commands.literal("garp")
                                        .then(positionArgs(Operation.GARP))
                        )
                        .then(
                                Commands.literal("duplicate")
                                        .then(positionArgs(Operation.DUPLICATE))
                        )
        );
    }

    private static com.mojang.brigadier.builder.RequiredArgumentBuilder<
            net.minecraft.commands.CommandSourceStack,
            Integer> positionArgs(
            Operation operation
    ) {
        return Commands.argument("fx", IntegerArgumentType.integer())
                .then(
                        Commands.argument("fy", IntegerArgumentType.integer())
                                .then(
                                        Commands.argument("fz", IntegerArgumentType.integer())
                                                .executes(context -> run(
                                                        context.getSource(),
                                                        new BlockPos(
                                                                IntegerArgumentType.getInteger(context, "fx"),
                                                                IntegerArgumentType.getInteger(context, "fy"),
                                                                IntegerArgumentType.getInteger(context, "fz")
                                                        ),
                                                        operation
                                                ))
                                )
                );
    }

    private static int run(
            net.minecraft.commands.CommandSourceStack source,
            BlockPos firewallPos,
            Operation operation
    ) {
        BlockEntity be =
                source.getLevel().getBlockEntity(firewallPos);

        if (!(be instanceof FirewallBlockEntity firewall)) {
            source.sendFailure(
                    Component.literal(
                            "W1.17 target is not a FirewallBlockEntity"
                    )
            );
            return 0;
        }

        BlockPos lanPos = firewall.getLanConnection();
        BlockPos wanPos = firewall.getWanConnection();

        if (lanPos == null || wanPos == null) {
            source.sendFailure(
                    Component.literal(
                            "W1.17 requires both LAN and WAN switch links"
                    )
            );
            return 0;
        }

        BlockEntity lanBe =
                source.getLevel().getBlockEntity(lanPos);

        BlockEntity wanBe =
                source.getLevel().getBlockEntity(wanPos);

        if (!(lanBe instanceof NetworkSwitchBlockEntity lanSwitch)
                || !(wanBe instanceof NetworkSwitchBlockEntity wanSwitch)) {
            source.sendFailure(
                    Component.literal(
                            "W1.17 requires NetworkSwitchBlockEntity on LAN and WAN"
                    )
            );
            return 0;
        }

        return switch (operation) {
            case SETUP -> setup(source, firewall, lanSwitch, wanSwitch);
            case STATUS -> status(source, firewall, lanSwitch, wanSwitch);
            case CLEAR -> clear(source, firewall, lanSwitch, wanSwitch);
            case UDP -> flow(source, firewall, lanSwitch, wanSwitch, false);
            case TCP -> flow(source, firewall, lanSwitch, wanSwitch, true);
            case HOST2HOST -> hostToHost(source, lanSwitch);
            case GARP -> garp(source, firewall, lanSwitch, wanSwitch);
            case DUPLICATE -> duplicate(source, lanSwitch);
        };
    }

    private static int setup(
            net.minecraft.commands.CommandSourceStack source,
            FirewallBlockEntity firewall,
            NetworkSwitchBlockEntity lanSwitch,
            NetworkSwitchBlockEntity wanSwitch
    ) {
        if (lanSwitch.w117Host("LAN-A") == null) {
            String port =
                    lanSwitch.w117BindHostAuto(
                            new W117HostEndpoint(
                                    "LAN-A",
                                    "192.168.10.20",
                                    "255.255.255.0",
                                    "192.168.10.1",
                                    "02:17:00:00:10:20"
                            )
                    );

            if (port == null) {
                source.sendFailure(
                        Component.literal(
                                "W1.17 could not allocate LAN-A switch port"
                        )
                );
                return 0;
            }
        }

        if (lanSwitch.w117Host("LAN-B") == null) {
            String port =
                    lanSwitch.w117BindHostAuto(
                            new W117HostEndpoint(
                                    "LAN-B",
                                    "192.168.10.30",
                                    "255.255.255.0",
                                    "192.168.10.1",
                                    "02:17:00:00:10:30"
                            )
                    );

            if (port == null) {
                source.sendFailure(
                        Component.literal(
                                "W1.17 could not allocate LAN-B switch port"
                        )
                );
                return 0;
            }
        }

        if (wanSwitch.w117Host("WAN-HOST") == null) {
            String port =
                    wanSwitch.w117BindHostAuto(
                            new W117HostEndpoint(
                                    "WAN-HOST",
                                    "203.0.113.20",
                                    "255.255.255.0",
                                    "203.0.113.10",
                                    "02:17:00:00:71:20"
                            )
                    );

            if (port == null) {
                source.sendFailure(
                        Component.literal(
                                "W1.17 could not allocate WAN-HOST switch port"
                        )
                );
                return 0;
            }
        }

        firewall.w117SendGratuitousArp(
                "GigabitEthernet1/1"
        );

        firewall.w117SendGratuitousArp(
                "GigabitEthernet1/2"
        );

        source.sendSuccess(
                () -> Component.literal(
                        "W1.17 SETUP COMPLETE"
                                + " | LAN-A="
                                + lanSwitch.w117HostPort("LAN-A")
                                + " | LAN-B="
                                + lanSwitch.w117HostPort("LAN-B")
                                + " | WAN-HOST="
                                + wanSwitch.w117HostPort("WAN-HOST")
                ).withStyle(ChatFormatting.GREEN),
                false
        );

        return 1;
    }

    private static int clear(
            net.minecraft.commands.CommandSourceStack source,
            FirewallBlockEntity firewall,
            NetworkSwitchBlockEntity lanSwitch,
            NetworkSwitchBlockEntity wanSwitch
    ) {
        firewall.w1161ClearCounters();
        firewall.w117ClearNeighborState();
        firewall.osSimulators[0].w116Reset();

        lanSwitch.w117ClearHostDynamicState();
        wanSwitch.w117ClearHostDynamicState();

        source.sendSuccess(
                () -> Component.literal(
                        "W1.17 dynamic state cleared"
                ),
                false
        );

        return 1;
    }

    private static int status(
            net.minecraft.commands.CommandSourceStack source,
            FirewallBlockEntity firewall,
            NetworkSwitchBlockEntity lanSwitch,
            NetworkSwitchBlockEntity wanSwitch
    ) {
        source.sendSuccess(
                () -> Component.literal(
                        "W1.17 FULL ETHERNET/ARP/HOST TRANSIT"
                ).withStyle(ChatFormatting.AQUA),
                false
        );

        source.sendSuccess(
                () -> Component.literal(
                        lanSwitch.w117HostStatus()
                ),
                false
        );

        source.sendSuccess(
                () -> Component.literal(
                        wanSwitch.w117HostStatus()
                ),
                false
        );

        source.sendSuccess(
                () -> Component.literal(
                        firewall.w117NeighborStatus()
                ).withStyle(ChatFormatting.YELLOW),
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
                        firewall.osSimulators[0].w116Status()
                ),
                false
        );

        return 1;
    }

    private static int flow(
            net.minecraft.commands.CommandSourceStack source,
            FirewallBlockEntity firewall,
            NetworkSwitchBlockEntity lanSwitch,
            NetworkSwitchBlockEntity wanSwitch,
            boolean tcp
    ) {
        if (lanSwitch.w117Host("LAN-A") == null
                || wanSwitch.w117Host("WAN-HOST") == null) {
            source.sendFailure(
                    Component.literal(
                            "W1.17 hosts not configured. Run /wifiw117 setup first."
                    )
            );
            return 0;
        }

        W117HostEndpoint lan =
                lanSwitch.w117Host("LAN-A");

        W117HostEndpoint wan =
                wanSwitch.w117Host("WAN-HOST");

        String lanPort =
                lanSwitch.w117HostPort("LAN-A");

        long beforeLan =
                lan.deliveredIpv4();

        long beforeWan =
                wan.deliveredIpv4();

        OSINetworkPacket packet =
                createFlowPacket(
                        lan.ipv4(),
                        wan.ipv4(),
                        tcp
                );

        List<OSINetworkPacket> initial =
                lan.sendIpv4(
                        packet,
                        System.currentTimeMillis()
                );

        for (OSINetworkPacket frame : initial) {
            lanSwitch.w117TransmitFromHost(
                    lanPort,
                    frame
            );
        }

        boolean wanDelivered =
                wan.deliveredIpv4() > beforeWan;

        boolean lanReturned =
                lan.deliveredIpv4() > beforeLan;

        source.sendSuccess(
                () -> Component.literal(
                        "W1.17 "
                                + (tcp ? "TCP" : "UDP")
                                + " REAL HOST TRANSIT"
                                + " | WAN-delivered="
                                + wanDelivered
                                + " | LAN-returned="
                                + lanReturned
                ).withStyle(
                        wanDelivered && lanReturned
                                ? ChatFormatting.GREEN
                                : ChatFormatting.YELLOW
                ),
                false
        );

        source.sendSuccess(
                () -> Component.literal(
                        firewall.w117NeighborStatus()
                ),
                false
        );

        source.sendSuccess(
                () -> Component.literal(
                        firewall.w1161TransitStatus()
                ),
                false
        );

        return wanDelivered && lanReturned
                ? 1
                : 0;
    }

    private static int hostToHost(
            net.minecraft.commands.CommandSourceStack source,
            NetworkSwitchBlockEntity lanSwitch
    ) {
        W117HostEndpoint a =
                lanSwitch.w117Host("LAN-A");

        W117HostEndpoint b =
                lanSwitch.w117Host("LAN-B");

        if (a == null || b == null) {
            source.sendFailure(
                    Component.literal(
                            "W1.17 LAN hosts not configured. Run /wifiw117 setup first."
                    )
            );
            return 0;
        }

        long beforeA =
                a.deliveredIpv4();

        long beforeB =
                b.deliveredIpv4();

        OSINetworkPacket packet =
                createFlowPacket(
                        a.ipv4(),
                        b.ipv4(),
                        false
                );

        for (OSINetworkPacket frame :
                a.sendIpv4(
                        packet,
                        System.currentTimeMillis()
                )) {
            lanSwitch.w117TransmitFromHost(
                    lanSwitch.w117HostPort("LAN-A"),
                    frame
            );
        }

        boolean forward =
                b.deliveredIpv4() > beforeB;

        boolean reply =
                a.deliveredIpv4() > beforeA;

        source.sendSuccess(
                () -> Component.literal(
                        "W1.17 HOST2HOST"
                                + " | forward="
                                + forward
                                + " | reply="
                                + reply
                                + " | firewall-bypassed=true"
                ).withStyle(
                        forward && reply
                                ? ChatFormatting.GREEN
                                : ChatFormatting.YELLOW
                ),
                false
        );

        return forward && reply
                ? 1
                : 0;
    }

    private static int garp(
            net.minecraft.commands.CommandSourceStack source,
            FirewallBlockEntity firewall,
            NetworkSwitchBlockEntity lanSwitch,
            NetworkSwitchBlockEntity wanSwitch
    ) {
        W117HostEndpoint lan =
                lanSwitch.w117Host("LAN-A");

        W117HostEndpoint wan =
                wanSwitch.w117Host("WAN-HOST");

        if (lan == null || wan == null) {
            source.sendFailure(
                    Component.literal(
                            "W1.17 hosts not configured"
                    )
            );
            return 0;
        }

        lanSwitch.w117TransmitFromHost(
                lanSwitch.w117HostPort("LAN-A"),
                lan.gratuitousArp()
        );

        wanSwitch.w117TransmitFromHost(
                wanSwitch.w117HostPort("WAN-HOST"),
                wan.gratuitousArp()
        );

        firewall.w117SendGratuitousArp(
                "GigabitEthernet1/1"
        );

        firewall.w117SendGratuitousArp(
                "GigabitEthernet1/2"
        );

        source.sendSuccess(
                () -> Component.literal(
                        "W1.17 gratuitous ARP announcements sent"
                ).withStyle(ChatFormatting.GREEN),
                false
        );

        return 1;
    }

    private static int duplicate(
            net.minecraft.commands.CommandSourceStack source,
            NetworkSwitchBlockEntity lanSwitch
    ) {
        W117HostEndpoint existing =
                lanSwitch.w117Host("LAN-A");

        if (existing == null) {
            source.sendFailure(
                    Component.literal(
                            "W1.17 LAN-A not configured"
                    )
            );
            return 0;
        }

        W117HostEndpoint duplicate =
                lanSwitch.w117Host("LAN-DUP");

        if (duplicate == null) {
            duplicate =
                    new W117HostEndpoint(
                            "LAN-DUP",
                            "192.168.10.20",
                            "255.255.255.0",
                            "192.168.10.1",
                            "02:17:DD:00:10:20"
                    );

            String port =
                    lanSwitch.w117BindHostAuto(
                            duplicate
                    );

            if (port == null) {
                source.sendFailure(
                        Component.literal(
                                "W1.17 could not allocate duplicate test host"
                        )
                );
                return 0;
            }
        }

        lanSwitch.w117TransmitFromHost(
                lanSwitch.w117HostPort("LAN-DUP"),
                duplicate.gratuitousArp()
        );

        boolean detected =
                existing.duplicateIpv4();

        source.sendSuccess(
                () -> Component.literal(
                        "W1.17 DUPLICATE-IP"
                                + " | detected="
                                + detected
                ).withStyle(
                        detected
                                ? ChatFormatting.GREEN
                                : ChatFormatting.RED
                ),
                false
        );

        return detected ? 1 : 0;
    }

    private static OSINetworkPacket createFlowPacket(
            String sourceIp,
            String targetIp,
            boolean tcp
    ) {
        OSINetworkPacket packet =
                new OSINetworkPacket();

        packet.sourceIp = sourceIp;
        packet.targetIp = targetIp;
        packet.sourcePort =
                tcp ? 57000 : 56000;
        packet.targetPort = 443;
        packet.ipProtocol =
                tcp ? 6 : 17;
        packet.applicationProtocol =
                tcp ? "TCP" : "UDP";
        packet.ipPacketLength = 96;
        packet.ttl = 64;
        packet.sessionId =
                tcp
                        ? "W1.17-TCP"
                        : "W1.17-UDP";

        CompoundTag payload =
                new CompoundTag();

        payload.putBoolean(
                "w117_echo_request",
                true
        );

        if (tcp) {
            payload.putBoolean(
                    "tcp_syn",
                    true
            );
        }

        packet.payload = payload;

        return packet;
    }

    private enum Operation {
        SETUP,
        STATUS,
        CLEAR,
        UDP,
        TCP,
        HOST2HOST,
        GARP,
        DUPLICATE
    }
}

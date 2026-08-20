package com.k1ngtle.vsia.signality.debug;

import com.k1ngtle.vsia.Vsia;
import com.k1ngtle.vsia.signality.engineering.firewall.w117.W117HostEndpoint;
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
public final class WifiW118Command {
    private WifiW118Command() {
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("wifiw118")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("setup")
                                .then(positionArgs(Operation.SETUP)))
                        .then(Commands.literal("bootstrap")
                                .then(positionArgs(Operation.BOOTSTRAP)))
                        .then(Commands.literal("dns")
                                .then(positionArgs(Operation.DNS)))
                        .then(Commands.literal("nxdomain")
                                .then(positionArgs(Operation.NXDOMAIN)))
                        .then(Commands.literal("renew")
                                .then(positionArgs(Operation.RENEW)))
                        .then(Commands.literal("status")
                                .then(positionArgs(Operation.STATUS)))
                        .then(Commands.literal("leases")
                                .then(positionArgs(Operation.LEASES)))
                        .then(Commands.literal("clear")
                                .then(positionArgs(Operation.CLEAR)))
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
                            "W1.18 target is not a FirewallBlockEntity"
                    )
            );
            return 0;
        }

        BlockPos lanPos =
                firewall.getLanConnection();

        if (lanPos == null) {
            source.sendFailure(
                    Component.literal(
                            "W1.18 requires the LAN switch link"
                    )
            );
            return 0;
        }

        BlockEntity lanBe =
                source.getLevel().getBlockEntity(lanPos);

        if (!(lanBe instanceof NetworkSwitchBlockEntity lanSwitch)) {
            source.sendFailure(
                    Component.literal(
                            "W1.18 LAN endpoint must be a NetworkSwitchBlockEntity"
                    )
            );
            return 0;
        }

        return switch (operation) {
            case SETUP -> setup(source, lanSwitch);
            case BOOTSTRAP -> bootstrap(source, firewall, lanSwitch);
            case DNS -> dns(source, firewall, lanSwitch, "vsia.test");
            case NXDOMAIN -> dns(source, firewall, lanSwitch, "missing.vsia.test");
            case RENEW -> renew(source, firewall, lanSwitch);
            case STATUS -> status(source, firewall, lanSwitch);
            case LEASES -> leases(source, firewall);
            case CLEAR -> clear(source, firewall, lanSwitch);
        };
    }

    private static int setup(
            net.minecraft.commands.CommandSourceStack source,
            NetworkSwitchBlockEntity lanSwitch
    ) {
        W117HostEndpoint client =
                lanSwitch.w117Host("DHCP-CLIENT");

        if (client == null) {
            client =
                    new W117HostEndpoint(
                            "DHCP-CLIENT",
                            "0.0.0.0",
                            "0.0.0.0",
                            "0.0.0.0",
                            "02:18:00:00:10:64"
                    );

            client.w118EnableDhcp();

            String port =
                    lanSwitch.w117BindHostAuto(client);

            if (port == null) {
                source.sendFailure(
                        Component.literal(
                                "W1.18 could not allocate a free LAN switch port"
                        )
                );
                return 0;
            }
        } else {
            client.w118EnableDhcp();
        }

        String port =
                lanSwitch.w117HostPort("DHCP-CLIENT");

        source.sendSuccess(
                () -> Component.literal(
                        "W1.18 SETUP COMPLETE"
                                + " | client=DHCP-CLIENT"
                                + " | port="
                                + port
                                + " | state=INIT"
                ).withStyle(ChatFormatting.GREEN),
                false
        );

        return 1;
    }

    private static int bootstrap(
            net.minecraft.commands.CommandSourceStack source,
            FirewallBlockEntity firewall,
            NetworkSwitchBlockEntity lanSwitch
    ) {
        W117HostEndpoint client =
                lanSwitch.w117Host("DHCP-CLIENT");

        if (client == null) {
            source.sendFailure(
                    Component.literal(
                            "Run /wifiw118 setup first"
                    )
            );
            return 0;
        }

        String port =
                lanSwitch.w117HostPort("DHCP-CLIENT");

        List<OSINetworkPacket> frames =
                client.w118StartDhcp(
                        System.currentTimeMillis()
                );

        for (OSINetworkPacket frame : frames) {
            lanSwitch.w117TransmitFromHost(
                    port,
                    frame
            );
        }

        boolean bound =
                client.w118DhcpBound();

        source.sendSuccess(
                () -> Component.literal(
                        "W1.18 DHCP BOOTSTRAP"
                                + " | bound="
                                + bound
                                + " | ip="
                                + client.ipv4()
                                + " | mask="
                                + client.subnetMask()
                                + " | gw="
                                + client.defaultGateway()
                                + " | dns="
                                + client.w118DnsServer()
                ).withStyle(
                        bound
                                ? ChatFormatting.GREEN
                                : ChatFormatting.YELLOW
                ),
                false
        );

        source.sendSuccess(
                () -> Component.literal(
                        firewall.w118ServiceStatus()
                ),
                false
        );

        return bound ? 1 : 0;
    }

    private static int dns(
            net.minecraft.commands.CommandSourceStack source,
            FirewallBlockEntity firewall,
            NetworkSwitchBlockEntity lanSwitch,
            String name
    ) {
        W117HostEndpoint client =
                lanSwitch.w117Host("DHCP-CLIENT");

        if (client == null
                || !client.w118DhcpBound()) {
            source.sendFailure(
                    Component.literal(
                            "DHCP-CLIENT must be bound before DNS"
                    )
            );
            return 0;
        }

        String port =
                lanSwitch.w117HostPort("DHCP-CLIENT");

        long now =
                System.currentTimeMillis();

        List<OSINetworkPacket> frames =
                client.w118DnsQuery(
                        name,
                        now
                );

        for (OSINetworkPacket frame : frames) {
            lanSwitch.w117TransmitFromHost(
                    port,
                    frame
            );
        }

        String answer =
                client.w118DnsCachedAnswer(
                        name,
                        System.currentTimeMillis()
                );

        int rcode =
                client.w118DnsRcode(
                        name,
                        System.currentTimeMillis()
                );

        boolean success =
                ("missing.vsia.test".equals(name)
                        && rcode == 3)
                        || (answer != null
                        && !answer.isBlank()
                        && rcode == 0);

        source.sendSuccess(
                () -> Component.literal(
                        "W1.18 DNS"
                                + " | name="
                                + name
                                + " | answer="
                                + (answer == null ? "<none>" : answer)
                                + " | rcode="
                                + rcode
                                + " | success="
                                + success
                ).withStyle(
                        success
                                ? ChatFormatting.GREEN
                                : ChatFormatting.YELLOW
                ),
                false
        );

        source.sendSuccess(
                () -> Component.literal(
                        firewall.w118ServiceStatus()
                ),
                false
        );

        return success ? 1 : 0;
    }

    private static int renew(
            net.minecraft.commands.CommandSourceStack source,
            FirewallBlockEntity firewall,
            NetworkSwitchBlockEntity lanSwitch
    ) {
        W117HostEndpoint client =
                lanSwitch.w117Host("DHCP-CLIENT");

        if (client == null
                || !client.w118DhcpBound()
                || client.w118DhcpConfiguration() == null) {
            source.sendFailure(
                    Component.literal(
                            "DHCP-CLIENT has no active lease"
                    )
            );
            return 0;
        }

        long simulatedT1 =
                client.w118DhcpConfiguration()
                        .t1Millis()
                        + 1L;

        String port =
                lanSwitch.w117HostPort("DHCP-CLIENT");

        for (OSINetworkPacket frame :
                client.tick(simulatedT1)) {
            lanSwitch.w117TransmitFromHost(
                    port,
                    frame
            );
        }

        boolean bound =
                client.w118DhcpBound();

        source.sendSuccess(
                () -> Component.literal(
                        "W1.18 DHCP RENEW"
                                + " | bound="
                                + bound
                                + " | ip="
                                + client.ipv4()
                ).withStyle(
                        bound
                                ? ChatFormatting.GREEN
                                : ChatFormatting.RED
                ),
                false
        );

        source.sendSuccess(
                () -> Component.literal(
                        firewall.w118ServiceStatus()
                ),
                false
        );

        return bound ? 1 : 0;
    }

    private static int status(
            net.minecraft.commands.CommandSourceStack source,
            FirewallBlockEntity firewall,
            NetworkSwitchBlockEntity lanSwitch
    ) {
        W117HostEndpoint client =
                lanSwitch.w117Host("DHCP-CLIENT");

        source.sendSuccess(
                () -> Component.literal(
                        "W1.18 HOST NETWORK BOOTSTRAP"
                ).withStyle(ChatFormatting.AQUA),
                false
        );

        if (client != null) {
            source.sendSuccess(
                    () -> Component.literal(
                            client.w118BootstrapStatus(
                                    System.currentTimeMillis()
                            )
                    ),
                    false
            );
        } else {
            source.sendSuccess(
                    () -> Component.literal(
                            "DHCP-CLIENT not configured"
                    ),
                    false
            );
        }

        source.sendSuccess(
                () -> Component.literal(
                        firewall.w118ServiceStatus()
                ).withStyle(ChatFormatting.YELLOW),
                false
        );

        source.sendSuccess(
                () -> Component.literal(
                        firewall.w117NeighborStatus()
                ),
                false
        );

        return 1;
    }

    private static int leases(
            net.minecraft.commands.CommandSourceStack source,
            FirewallBlockEntity firewall
    ) {
        long now =
                System.currentTimeMillis();

        var leases =
                firewall.w118DhcpServer()
                        .leases(now);

        source.sendSuccess(
                () -> Component.literal(
                        "W1.18 DHCP leases="
                                + leases.size()
                ).withStyle(ChatFormatting.YELLOW),
                false
        );

        for (var entry : leases.entrySet()) {
            source.sendSuccess(
                    () -> Component.literal(
                            entry.getKey()
                                    + " -> "
                                    + entry.getValue().ipv4()
                                    + " remainingMs="
                                    + entry.getValue()
                                    .remainingMillis(now)
                    ),
                    false
            );
        }

        return 1;
    }

    private static int clear(
            net.minecraft.commands.CommandSourceStack source,
            FirewallBlockEntity firewall,
            NetworkSwitchBlockEntity lanSwitch
    ) {
        W117HostEndpoint client =
                lanSwitch.w117Host("DHCP-CLIENT");

        if (client != null) {
            client.clearDynamicState();
        }

        firewall.w118ClearServices();
        firewall.w117ClearNeighborState();

        source.sendSuccess(
                () -> Component.literal(
                        "W1.18 DHCP/DNS dynamic state cleared"
                ),
                false
        );

        return 1;
    }

    private enum Operation {
        SETUP,
        BOOTSTRAP,
        DNS,
        NXDOMAIN,
        RENEW,
        STATUS,
        LEASES,
        CLEAR
    }
}

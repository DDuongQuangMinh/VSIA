package com.k1ngtle.vsia.signality.debug;

import com.k1ngtle.vsia.Vsia;
import com.k1ngtle.vsia.signality.engineering.firewall.Nat44Mapping;
import com.k1ngtle.vsia.signality.engineering.firewall.w1162.W1162ArpCache;
import com.k1ngtle.vsia.signality.engineering.firewall.w1162.W1162FlowFactory;
import com.k1ngtle.vsia.signality.engineering.firewall.w1162.W1162HostProfile;
import com.k1ngtle.vsia.signality.engineering.firewall.w1162.W1162Ipv4;
import com.k1ngtle.vsia.signality.internet.OSINetworkPacket;
import com.k1ngtle.vsia.signality.internet.server.FirewallOsSimulator;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashSet;
import java.util.Set;

@Mod.EventBusSubscriber(
        modid = Vsia.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class WifiW1162TestCommand {
    private WifiW1162TestCommand() {
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("wifiw1162test")
                        .requires(source -> source.hasPermission(2))
                        .executes(context -> run(context.getSource()))
        );
    }

    private static int run(
            net.minecraft.commands.CommandSourceStack source
    ) {
        int passed = 0;

        passed += check(source, "w1162-ipv4-valid",
                W1162Ipv4.valid("192.168.10.20"));
        passed += check(source, "w1162-ipv4-invalid",
                !W1162Ipv4.valid("300.1.1.1"));
        passed += check(source, "w1162-mask-contiguous",
                W1162Ipv4.contiguousMask("255.255.255.0"));
        passed += check(source, "w1162-mask-noncontiguous",
                !W1162Ipv4.contiguousMask("255.0.255.0"));
        passed += check(source, "w1162-prefix24",
                W1162Ipv4.prefixLength("255.255.255.0") == 24);
        passed += check(source, "w1162-network",
                "192.168.10.0".equals(
                        W1162Ipv4.network(
                                "192.168.10.20",
                                "255.255.255.0"
                        )
                ));
        passed += check(source, "w1162-broadcast",
                "192.168.10.255".equals(
                        W1162Ipv4.broadcast(
                                "192.168.10.20",
                                "255.255.255.0"
                        )
                ));

        W1162HostProfile lanHost =
                new W1162HostProfile(
                        "LAN-HOST",
                        "192.168.10.20",
                        "255.255.255.0",
                        "192.168.10.1",
                        "02:16:02:00:00:20"
                );

        passed += check(source, "w1162-host-onlink",
                lanHost.destinationOnLink("192.168.10.50"));
        passed += check(source, "w1162-host-offlink",
                !lanHost.destinationOnLink("203.0.113.20"));
        passed += check(source, "w1162-host-next-hop-onlink",
                "192.168.10.50".equals(
                        lanHost.nextHop("192.168.10.50")
                ));
        passed += check(source, "w1162-host-next-hop-gateway",
                "192.168.10.1".equals(
                        lanHost.nextHop("203.0.113.20")
                ));

        W1162ArpCache arp =
                new W1162ArpCache(1000L);

        arp.learn(
                "192.168.10.1",
                "00:02:4A:0B:01:01",
                100L
        );

        passed += check(source, "w1162-arp-learn",
                arp.size(200L) == 1);
        passed += check(source, "w1162-arp-lookup",
                arp.lookup("192.168.10.1", 200L).isPresent());
        passed += check(source, "w1162-arp-timeout",
                arp.size(1200L) == 0);

        FirewallOsSimulator sim =
                configuredSimulator();

        Set<Integer> patPorts =
                new HashSet<>();

        for (int i = 0; i < 4; i++) {
            int sourcePort = 51000 + i;

            OSINetworkPacket outbound =
                    W1162FlowFactory.outboundUdp(
                            lanHost,
                            "203.0.113.20",
                            sourcePort,
                            443,
                            "FF:FF:FF:FF:FF:FF",
                            i
                    );

            OSINetworkPacket translated =
                    sim.w1161FilterAndRoutePacket(
                            outbound,
                            "GigabitEthernet1/1"
                    );

            passed += check(
                    source,
                    "w1162-udp-outbound-" + i,
                    translated != null
                            && "203.0.113.10".equals(
                            translated.sourceIp
                    )
            );

            Nat44Mapping mapping =
                    sim.w1162FindNatMapping(
                            "UDP",
                            "192.168.10.20",
                            sourcePort,
                            "203.0.113.20",
                            443
                    );

            passed += check(
                    source,
                    "w1162-udp-mapping-" + i,
                    mapping != null
            );

            if (mapping != null) {
                patPorts.add(
                        mapping.insideGlobalPort()
                );

                OSINetworkPacket reply =
                        W1162FlowFactory.reply(
                                mapping,
                                i
                        );

                OSINetworkPacket returned =
                        sim.w1161FilterAndRoutePacket(
                                reply,
                                "GigabitEthernet1/2"
                        );

                passed += check(
                        source,
                        "w1162-udp-return-" + i,
                        returned != null
                                && "192.168.10.20".equals(
                                returned.targetIp
                        )
                                && returned.targetPort
                                == sourcePort
                );
            } else {
                passed += check(
                        source,
                        "w1162-udp-return-" + i,
                        false
                );
            }
        }

        passed += check(source, "w1162-udp-four-nat",
                sim.w1161NatCount() == 4);
        passed += check(source, "w1162-udp-four-conntrack",
                sim.w1161ConntrackCount() == 4);
        passed += check(source, "w1162-pat-unique",
                patPorts.size() == 4);

        FirewallOsSimulator tcpSim =
                configuredSimulator();

        OSINetworkPacket tcpSyn =
                W1162FlowFactory.outboundTcpSyn(
                        lanHost,
                        "203.0.113.20",
                        52000,
                        443,
                        "FF:FF:FF:FF:FF:FF",
                        100
                );

        OSINetworkPacket tcpTranslated =
                tcpSim.w1161FilterAndRoutePacket(
                        tcpSyn,
                        "GigabitEthernet1/1"
                );

        passed += check(source, "w1162-tcp-syn-forward",
                tcpTranslated != null);

        Nat44Mapping tcpMapping =
                tcpSim.w1162FindNatMapping(
                        "TCP",
                        "192.168.10.20",
                        52000,
                        "203.0.113.20",
                        443
                );

        passed += check(source, "w1162-tcp-pat-created",
                tcpMapping != null);

        OSINetworkPacket synAck =
                tcpMapping == null
                        ? null
                        : W1162FlowFactory.reply(
                        tcpMapping,
                        101
                );

        OSINetworkPacket tcpReturn =
                synAck == null
                        ? null
                        : tcpSim.w1161FilterAndRoutePacket(
                        synAck,
                        "GigabitEthernet1/2"
                );

        passed += check(source, "w1162-tcp-synack-return",
                tcpReturn != null
                        && tcpSim.w1161LastPipelineStatus()
                        .contains("ESTABLISHED"));

        long future =
                System.currentTimeMillis()
                        + 600_001L;

        int natExpired =
                tcpSim.w1162ExpireNat(
                        future
                );

        int connExpired =
                tcpSim.w1162ExpireConntrack(
                        future
                );

        passed += check(source, "w1162-nat-expire",
                natExpired >= 1
                        && tcpSim.w1161NatCount() == 0);
        passed += check(source, "w1162-conntrack-expire",
                connExpired >= 1
                        && tcpSim.w1161ConntrackCount() == 0);

        FirewallOsSimulator noRoute =
                configuredSimulator();
        noRoute.routes.clear();
        noRoute.portConfigs.get(
                "GigabitEthernet1/2"
        ).ipAddress = "unassigned";

        OSINetworkPacket orphan =
                W1162FlowFactory.outboundUdp(
                        lanHost,
                        "198.51.100.20",
                        53000,
                        443,
                        "",
                        200
                );

        OSINetworkPacket noRouteResult =
                noRoute.w1161FilterAndRoutePacket(
                        orphan,
                        "GigabitEthernet1/1"
                );

        passed += check(source, "w1162-no-route",
                noRouteResult == null
                        && noRoute.w1161LastPipelineStatus()
                        .startsWith("DROP_NO_ROUTE"));

        int total = 35;
        int failed = total - passed;
        int p = passed;
        int f = failed;

        source.sendSuccess(
                () -> Component.literal(
                        "W1.16.2 Full Phase Result: "
                                + p
                                + " passed, "
                                + f
                                + " failed"
                ).withStyle(
                        f == 0
                                ? ChatFormatting.GREEN
                                : ChatFormatting.RED
                ),
                false
        );

        return f == 0 ? 1 : 0;
    }

    private static FirewallOsSimulator configuredSimulator() {
        FirewallOsSimulator sim =
                new FirewallOsSimulator(
                        92,
                        1,
                        "ASA92_1",
                        () -> {}
                );

        sim.isBooted = true;

        FirewallOsSimulator.PortConfig inside =
                sim.portConfigs.get(
                        "GigabitEthernet1/1"
                );

        inside.up = true;
        inside.nameif = "inside";
        inside.ipAddress = "192.168.10.1";
        inside.subnetMask = "255.255.255.0";
        inside.securityLevel = 100;

        FirewallOsSimulator.PortConfig outside =
                sim.portConfigs.get(
                        "GigabitEthernet1/2"
                );

        outside.up = true;
        outside.nameif = "outside";
        outside.ipAddress = "203.0.113.10";
        outside.subnetMask = "255.255.255.0";
        outside.securityLevel = 0;

        sim.routes.add(
                "route outside 0.0.0.0 0.0.0.0 203.0.113.1"
        );

        sim.w116EnableNat44(
                "203.0.113.10"
        );

        sim.w1161RefreshRib();

        return sim;
    }

    private static int check(
            net.minecraft.commands.CommandSourceStack source,
            String id,
            boolean passed
    ) {
        if (passed) {
            source.sendSuccess(
                    () -> Component.literal(
                            "[PASS] " + id
                    ).withStyle(ChatFormatting.GREEN),
                    false
            );

            return 1;
        }

        source.sendFailure(
                Component.literal(
                        "[FAIL] " + id
                )
        );

        return 0;
    }
}

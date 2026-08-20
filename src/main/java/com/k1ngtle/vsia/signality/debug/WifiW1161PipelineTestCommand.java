package com.k1ngtle.vsia.signality.debug;

import com.k1ngtle.vsia.Vsia;
import com.k1ngtle.vsia.signality.internet.OSINetworkPacket;
import com.k1ngtle.vsia.signality.internet.server.FirewallOsSimulator;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = Vsia.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class WifiW1161PipelineTestCommand {
    private WifiW1161PipelineTestCommand() {
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("wifiw1firewallpipelinetest")
                        .requires(source -> source.hasPermission(2))
                        .executes(context -> run(context.getSource()))
        );
    }

    private static int run(
            net.minecraft.commands.CommandSourceStack source
    ) {
        int passed = 0;

        FirewallOsSimulator sim = configuredSimulator();

        OSINetworkPacket outbound = udp(
                "192.168.10.20",
                51000,
                "198.51.100.20",
                443,
                false
        );

        OSINetworkPacket routed =
                sim.w1161FilterAndRoutePacket(
                        outbound,
                        "GigabitEthernet1/1"
                );

        passed += check(source, "w1161-pipeline-outbound-accepted", routed != null);
        passed += check(source, "w1161-pipeline-outbound-egress",
                "GigabitEthernet1/2".equals(sim.w1161LastEgressInterface()));
        passed += check(source, "w1161-pipeline-snat-ip",
                routed != null && "203.0.113.10".equals(routed.sourceIp));
        passed += check(source, "w1161-pipeline-pat-port",
                routed != null && routed.sourcePort >= 40000 && routed.sourcePort <= 60000);
        passed += check(source, "w1161-pipeline-conntrack-created",
                sim.w1161ConntrackCount() == 1);
        passed += check(source, "w1161-pipeline-nat-created",
                sim.w1161NatCount() == 1);
        passed += check(source, "w1161-pipeline-forward-status",
                sim.w1161LastPipelineStatus().startsWith("FORWARD "));
        passed += check(source, "w1161-pipeline-route-refresh",
                sim.w1161LastPipelineStatus().contains("GigabitEthernet1/2"));

        int patPort = routed == null ? 40000 : routed.sourcePort;

        OSINetworkPacket reply = udp(
                "198.51.100.20",
                443,
                "203.0.113.10",
                patPort,
                true
        );

        OSINetworkPacket returned =
                sim.w1161FilterAndRoutePacket(
                        reply,
                        "GigabitEthernet1/2"
                );

        passed += check(source, "w1161-pipeline-return-accepted", returned != null);
        passed += check(source, "w1161-pipeline-return-egress",
                "GigabitEthernet1/1".equals(sim.w1161LastEgressInterface()));
        passed += check(source, "w1161-pipeline-dnat-ip",
                returned != null && "192.168.10.20".equals(returned.targetIp));
        passed += check(source, "w1161-pipeline-dnat-port",
                returned != null && returned.targetPort == 51000);
        passed += check(source, "w1161-pipeline-nat-reused",
                sim.w1161NatCount() == 1);
        passed += check(source, "w1161-pipeline-return-state",
                sim.w1161LastPipelineStatus().contains("ESTABLISHED"));
        passed += check(source, "w1161-pipeline-no-double-legacy-drop",
                returned != null);

        FirewallOsSimulator noRoute = configuredSimulator();
        noRoute.routes.clear();

        OSINetworkPacket noRoutePacket = udp(
                "192.168.10.20",
                51001,
                "198.51.100.21",
                443,
                false
        );

        OSINetworkPacket noRouteResult =
                noRoute.w1161FilterAndRoutePacket(
                        noRoutePacket,
                        "GigabitEthernet1/1"
                );

        passed += check(source, "w1161-pipeline-no-route-drop",
                noRouteResult == null
                        && noRoute.w1161LastPipelineStatus().startsWith("DROP_NO_ROUTE"));

        FirewallOsSimulator ttlIndependent = configuredSimulator();
        OSINetworkPacket routeProbe = udp(
                "192.168.10.20",
                51002,
                "198.51.100.22",
                443,
                false
        );
        routeProbe.ttl = 64;

        OSINetworkPacket routeProbeResult =
                ttlIndependent.w1161FilterAndRoutePacket(
                        routeProbe,
                        "GigabitEthernet1/1"
                );

        passed += check(source, "w1161-pipeline-preserves-ttl-before-world-hop",
                routeProbeResult != null && routeProbeResult.ttl == 64);

        FirewallOsSimulator down = configuredSimulator();
        down.portConfigs.get("GigabitEthernet1/2").up = false;

        OSINetworkPacket downPacket = udp(
                "192.168.10.20",
                51003,
                "198.51.100.23",
                443,
                false
        );

        OSINetworkPacket downResult =
                down.w1161FilterAndRoutePacket(
                        downPacket,
                        "GigabitEthernet1/1"
                );

        passed += check(source, "w1161-pipeline-egress-down",
                downResult == null
                        && down.w1161LastPipelineStatus().startsWith("DROP_EGRESS_DOWN"));

        int total = 18;
        int failed = total - passed;
        int p = passed;
        int f = failed;

        source.sendSuccess(
                () -> Component.literal(
                        "W1.16.1 Routing/NAT Pipeline Result: "
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
                        90,
                        1,
                        "ASA90_1",
                        () -> {}
                );

        sim.isBooted = true;

        FirewallOsSimulator.PortConfig inside =
                sim.portConfigs.get("GigabitEthernet1/1");
        inside.up = true;
        inside.nameif = "inside";
        inside.ipAddress = "192.168.10.1";
        inside.subnetMask = "255.255.255.0";
        inside.securityLevel = 100;

        FirewallOsSimulator.PortConfig outside =
                sim.portConfigs.get("GigabitEthernet1/2");
        outside.up = true;
        outside.nameif = "outside";
        outside.ipAddress = "203.0.113.10";
        outside.subnetMask = "255.255.255.0";
        outside.securityLevel = 0;

        sim.routes.add(
                "route outside 0.0.0.0 0.0.0.0 203.0.113.1"
        );

        sim.w116EnableNat44("203.0.113.10");
        sim.w1161RefreshRib();

        return sim;
    }

    private static OSINetworkPacket udp(
            String sourceIp,
            int sourcePort,
            String targetIp,
            int targetPort,
            boolean response
    ) {
        OSINetworkPacket packet = new OSINetworkPacket();
        packet.sourceMac = "02:16:01:00:00:20";
        packet.targetMac = "FF:FF:FF:FF:FF:FF";
        packet.sourceIp = sourceIp;
        packet.targetIp = targetIp;
        packet.sourcePort = sourcePort;
        packet.targetPort = targetPort;
        packet.ipProtocol = 17;
        packet.applicationProtocol = "UDP";
        packet.ipPacketLength = 64;
        packet.ttl = 64;
        packet.isResponse = response;
        packet.sessionId = "W1.16.1-PIPELINE";
        packet.payload = new CompoundTag();
        packet.payload.putString("kind", "W1.16.1_PIPELINE_TEST");
        return packet;
    }

    private static int check(
            net.minecraft.commands.CommandSourceStack source,
            String id,
            boolean passed
    ) {
        if (passed) {
            source.sendSuccess(
                    () -> Component.literal("[PASS] " + id)
                            .withStyle(ChatFormatting.GREEN),
                    false
            );
            return 1;
        }

        source.sendFailure(
                Component.literal("[FAIL] " + id)
        );
        return 0;
    }
}

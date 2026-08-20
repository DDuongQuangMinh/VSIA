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
public final class WifiW1161StatefulReturnProbeTestCommand {
    private WifiW1161StatefulReturnProbeTestCommand() {
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("wifiw1firewallreturnprobetest")
                        .requires(source -> source.hasPermission(2))
                        .executes(context -> run(context.getSource()))
        );
    }

    private static int run(
            net.minecraft.commands.CommandSourceStack source
    ) {
        int passed = 0;

        FirewallOsSimulator empty = configuredSimulator();

        passed += check(
                source,
                "w1161-return-no-mapping-null",
                empty.w1161BuildLatestNatReplyProbe() == null
        );

        FirewallOsSimulator sim = configuredSimulator();

        OSINetworkPacket outbound =
                udp(
                        "192.168.10.20",
                        51000,
                        "198.51.100.20",
                        443,
                        false
                );

        OSINetworkPacket translated =
                sim.w1161FilterAndRoutePacket(
                        outbound,
                        "GigabitEthernet1/1"
                );

        passed += check(
                source,
                "w1161-return-outbound-created",
                translated != null
                        && sim.w1161NatCount() == 1
        );

        OSINetworkPacket reply =
                sim.w1161BuildLatestNatReplyProbe();

        passed += check(
                source,
                "w1161-return-probe-created",
                reply != null
        );

        passed += check(
                source,
                "w1161-return-src-ip",
                reply != null
                        && "198.51.100.20".equals(reply.sourceIp)
        );

        passed += check(
                source,
                "w1161-return-src-port",
                reply != null
                        && reply.sourcePort == 443
        );

        passed += check(
                source,
                "w1161-return-global-ip",
                reply != null
                        && "203.0.113.10".equals(reply.targetIp)
        );

        passed += check(
                source,
                "w1161-return-global-port",
                reply != null
                        && reply.targetPort == translated.sourcePort
        );

        passed += check(
                source,
                "w1161-return-response-flag",
                reply != null
                        && reply.isResponse
        );

        passed += check(
                source,
                "w1161-return-ttl64",
                reply != null
                        && reply.ttl == 64
        );

        OSINetworkPacket returned =
                sim.w1161FilterAndRoutePacket(
                        reply,
                        "GigabitEthernet1/2"
                );

        passed += check(
                source,
                "w1161-return-accepted",
                returned != null
        );

        passed += check(
                source,
                "w1161-return-dnat-ip",
                returned != null
                        && "192.168.10.20".equals(returned.targetIp)
        );

        passed += check(
                source,
                "w1161-return-dnat-port",
                returned != null
                        && returned.targetPort == 51000
        );

        passed += check(
                source,
                "w1161-return-egress-lan",
                "GigabitEthernet1/1".equals(
                        sim.w1161LastEgressInterface()
                )
        );

        passed += check(
                source,
                "w1161-return-established",
                sim.w1161LastPipelineStatus()
                        .contains("state=ESTABLISHED")
        );

        passed += check(
                source,
                "w1161-return-dnat-status",
                sim.w1161LastPipelineStatus()
                        .contains("nat=DNAT")
        );

        passed += check(
                source,
                "w1161-return-nat-reused",
                sim.w1161NatCount() == 1
        );

        passed += check(
                source,
                "w1161-return-mapping-summary",
                sim.w1161LatestNatMappingSummary()
                        .contains("192.168.10.20:51000")
        );

        int total = 17;
        int failed = total - passed;
        int p = passed;
        int f = failed;

        source.sendSuccess(
                () -> Component.literal(
                        "W1.16.1 Stateful NAT Return Probe Result: "
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
                        91,
                        1,
                        "ASA91_1",
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
        packet.sessionId = "W1.16.1-RETURN-TEST";
        packet.payload = new CompoundTag();
        packet.payload.putString("kind", "W1.16.1_RETURN_TEST");
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

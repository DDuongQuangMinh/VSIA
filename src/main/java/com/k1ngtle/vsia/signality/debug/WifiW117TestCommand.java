package com.k1ngtle.vsia.signality.debug;

import com.k1ngtle.vsia.Vsia;
import com.k1ngtle.vsia.signality.engineering.firewall.w117.W117ArpFrame;
import com.k1ngtle.vsia.signality.engineering.firewall.w117.W117HostEndpoint;
import com.k1ngtle.vsia.signality.engineering.firewall.w117.W117InterfaceNeighborEngine;
import com.k1ngtle.vsia.signality.engineering.firewall.w117.W117Ipv4;
import com.k1ngtle.vsia.signality.engineering.firewall.w117.W117NeighborCache;
import com.k1ngtle.vsia.signality.internet.OSINetworkPacket;
import com.k1ngtle.vsia.signality.internet.server.FirewallOsSimulator;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod.EventBusSubscriber(
        modid = Vsia.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class WifiW117TestCommand {
    private WifiW117TestCommand() {
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("wifiw117test")
                        .requires(source -> source.hasPermission(2))
                        .executes(context -> run(context.getSource()))
        );
    }

    private static int run(
            net.minecraft.commands.CommandSourceStack source
    ) {
        int passed = 0;

        passed += check(source, "w117-ipv4-valid",
                W117Ipv4.valid("192.168.10.20"));
        passed += check(source, "w117-ipv4-invalid",
                !W117Ipv4.valid("999.1.1.1"));
        passed += check(source, "w117-mask-valid",
                W117Ipv4.contiguousMask("255.255.255.0"));
        passed += check(source, "w117-mask-invalid",
                !W117Ipv4.contiguousMask("255.0.255.0"));
        passed += check(source, "w117-prefix24",
                W117Ipv4.prefixLength("255.255.255.0") == 24);
        passed += check(source, "w117-same-subnet",
                W117Ipv4.sameSubnet(
                        "192.168.10.20",
                        "192.168.10.30",
                        "255.255.255.0"
                ));
        passed += check(source, "w117-different-subnet",
                !W117Ipv4.sameSubnet(
                        "192.168.10.20",
                        "203.0.113.20",
                        "255.255.255.0"
                ));

        OSINetworkPacket request =
                W117ArpFrame.request(
                        "02:17:00:00:10:20",
                        "192.168.10.20",
                        "192.168.10.1",
                        "TEST"
                );

        passed += check(source, "w117-arp-request-detect",
                W117ArpFrame.isRequest(request));
        passed += check(source, "w117-arp-request-broadcast",
                W117ArpFrame.BROADCAST_MAC.equals(
                        request.targetMac
                ));
        passed += check(source, "w117-arp-request-sender-ip",
                "192.168.10.20".equals(
                        W117ArpFrame.senderIp(request)
                ));
        passed += check(source, "w117-arp-request-target-ip",
                "192.168.10.1".equals(
                        W117ArpFrame.targetIp(request)
                ));

        OSINetworkPacket reply =
                W117ArpFrame.reply(
                        "02:FA:01:01:00:01",
                        "192.168.10.1",
                        "02:17:00:00:10:20",
                        "192.168.10.20",
                        "TEST"
                );

        passed += check(source, "w117-arp-reply-detect",
                W117ArpFrame.isReply(reply));
        passed += check(source, "w117-arp-reply-unicast",
                "02:17:00:00:10:20".equals(
                        reply.targetMac
                ));
        passed += check(source, "w117-arp-reply-ip",
                "192.168.10.1".equals(
                        W117ArpFrame.senderIp(reply)
                ));

        OSINetworkPacket garp =
                W117ArpFrame.gratuitous(
                        "02:17:00:00:10:20",
                        "192.168.10.20"
                );

        passed += check(source, "w117-garp-request",
                W117ArpFrame.isRequest(garp));
        passed += check(source, "w117-garp-self-target",
                "192.168.10.20".equals(
                        W117ArpFrame.targetIp(garp)
                ));
        passed += check(source, "w117-garp-flag",
                garp.payload.getBoolean("gratuitous"));

        passed += check(source, "w117-garp-broadcast",
                W117ArpFrame.BROADCAST_MAC.equals(
                        garp.targetMac
                ));

        W117NeighborCache cache =
                new W117NeighborCache(1000L);

        cache.learn(
                "192.168.10.1",
                "02:FA:01:01:00:01",
                100L
        );

        passed += check(source, "w117-cache-learn",
                cache.size(200L) == 1);
        passed += check(source, "w117-cache-lookup",
                cache.lookup("192.168.10.1", 200L).isPresent());
        passed += check(source, "w117-cache-mac",
                cache.lookup("192.168.10.1", 200L)
                        .map(e -> "02:FA:01:01:00:01".equals(e.mac()))
                        .orElse(false));
        passed += check(source, "w117-cache-expire",
                cache.size(1200L) == 0);

        W117HostEndpoint host =
                new W117HostEndpoint(
                        "HOST-A",
                        "192.168.10.20",
                        "255.255.255.0",
                        "192.168.10.1",
                        "02:17:00:00:10:20"
                );

        passed += check(source, "w117-host-next-hop-local",
                "192.168.10.30".equals(
                        host.nextHop("192.168.10.30")
                ));
        passed += check(source, "w117-host-next-hop-gateway",
                "192.168.10.1".equals(
                        host.nextHop("203.0.113.20")
                ));

        OSINetworkPacket queued =
                ipv4(
                        "192.168.10.20",
                        "203.0.113.20",
                        false
                );

        List<OSINetworkPacket> first =
                host.sendIpv4(
                        queued,
                        1000L
                );

        passed += check(source, "w117-host-unresolved-arp",
                first.size() == 1
                        && W117ArpFrame.isRequest(first.get(0)));
        passed += check(source, "w117-host-pending-one",
                host.pendingCount() == 1);

        List<OSINetworkPacket> flush =
                host.receive(
                        W117ArpFrame.reply(
                                "02:FA:01:01:00:01",
                                "192.168.10.1",
                                host.macAddress(),
                                host.ipv4(),
                                "TEST"
                        ),
                        1100L
                );

        passed += check(source, "w117-host-arp-flush",
                flush.size() == 1);
        passed += check(source, "w117-host-flush-l2dst",
                flush.size() == 1
                        && "02:FA:01:01:00:01".equals(
                        flush.get(0).targetMac
                ));
        passed += check(source, "w117-host-pending-zero",
                host.pendingCount() == 0);
        passed += check(source, "w117-host-neighbor-one",
                host.neighborCount(1200L) == 1);

        List<OSINetworkPacket> direct =
                host.sendIpv4(
                        ipv4(
                                "192.168.10.20",
                                "203.0.113.20",
                                false
                        ),
                        1200L
                );

        passed += check(source, "w117-host-cache-fastpath",
                direct.size() == 1
                        && !W117ArpFrame.isArp(direct.get(0)));

        W117HostEndpoint target =
                new W117HostEndpoint(
                        "HOST-B",
                        "192.168.10.30",
                        "255.255.255.0",
                        "192.168.10.1",
                        "02:17:00:00:10:30"
                );

        List<OSINetworkPacket> arpAnswer =
                target.receive(
                        W117ArpFrame.request(
                                host.macAddress(),
                                host.ipv4(),
                                target.ipv4(),
                                "LOCAL"
                        ),
                        2000L
                );

        passed += check(source, "w117-host-answers-arp",
                arpAnswer.size() == 1
                        && W117ArpFrame.isReply(arpAnswer.get(0)));

        W117HostEndpoint duplicate =
                new W117HostEndpoint(
                        "HOST-DUP",
                        "192.168.10.20",
                        "255.255.255.0",
                        "192.168.10.1",
                        "02:17:DD:00:10:20"
                );

        host.receive(
                duplicate.gratuitousArp(),
                2100L
        );

        passed += check(source, "w117-duplicate-ip-detected",
                host.duplicateIpv4());

        W117HostEndpoint retryHost =
                new W117HostEndpoint(
                        "RETRY",
                        "10.0.0.2",
                        "255.255.255.0",
                        "10.0.0.1",
                        "02:17:00:00:00:02"
                );

        retryHost.sendIpv4(
                ipv4(
                        "10.0.0.2",
                        "198.51.100.20",
                        false
                ),
                0L
        );

        passed += check(source, "w117-retry-not-early",
                retryHost.tick(500L).isEmpty());
        passed += check(source, "w117-retry-second-attempt",
                retryHost.tick(1000L).size() == 1);
        passed += check(source, "w117-retry-third-attempt",
                retryHost.tick(2000L).size() == 1);
        passed += check(source, "w117-retry-failure-clears",
                retryHost.tick(3000L).isEmpty()
                        && retryHost.pendingCount() == 0);

        W117InterfaceNeighborEngine routerNeighbors =
                new W117InterfaceNeighborEngine();

        OSINetworkPacket routerQueued =
                ipv4(
                        "203.0.113.10",
                        "203.0.113.20",
                        false
                );

        passed += check(source, "w117-router-queue",
                routerNeighbors.queue(
                        "203.0.113.20",
                        routerQueued,
                        "GigabitEthernet1/2",
                        100L
                ));
        passed += check(source, "w117-router-initial-request",
                routerNeighbors.needsInitialRequest("203.0.113.20"));

        routerNeighbors.markRequestSent(
                "203.0.113.20",
                100L
        );

        passed += check(source, "w117-router-request-marked",
                !routerNeighbors.needsInitialRequest("203.0.113.20"));

        routerNeighbors.learn(
                "203.0.113.20",
                "02:17:00:00:71:20",
                200L
        );

        List<W117InterfaceNeighborEngine.ResolvedPacket> drained =
                routerNeighbors.drainResolved();

        passed += check(source, "w117-router-resolved-drain",
                drained.size() == 1);
        passed += check(source, "w117-router-resolved-mac",
                drained.size() == 1
                        && "02:17:00:00:71:20".equals(
                        drained.get(0).nextHopMac()
                ));
        passed += check(source, "w117-router-pending-zero",
                routerNeighbors.pendingCount() == 0);

        passed += check(source, "w117-router-neighbor-learned",
                routerNeighbors.neighborCount(300L) == 1);

        FirewallOsSimulator sim =
                configuredSimulator();

        passed += check(source, "w117-next-hop-lan-direct",
                "192.168.10.20".equals(
                        sim.w117NextHopIp(
                                "192.168.10.20",
                                "GigabitEthernet1/1"
                        )
                ));
        passed += check(source, "w117-next-hop-wan-direct",
                "203.0.113.20".equals(
                        sim.w117NextHopIp(
                                "203.0.113.20",
                                "GigabitEthernet1/2"
                        )
                ));
        passed += check(source, "w117-next-hop-default-route",
                "203.0.113.1".equals(
                        sim.w117NextHopIp(
                                "198.51.100.20",
                                "GigabitEthernet1/2"
                        )
                ));

        OSINetworkPacket echoRequest =
                ipv4(
                        "192.168.10.20",
                        "192.168.10.30",
                        false
                );

        echoRequest.payload.putBoolean(
                "w117_echo_request",
                true
        );

        target.receive(
                W117ArpFrame.request(
                        host.macAddress(),
                        host.ipv4(),
                        target.ipv4(),
                        "LEARN"
                ),
                3000L
        );

        target.receive(
                W117ArpFrame.reply(
                        host.macAddress(),
                        host.ipv4(),
                        target.macAddress(),
                        target.ipv4(),
                        "LEARN"
                ),
                3001L
        );

        List<OSINetworkPacket> echoReply =
                target.receive(
                        echoRequest,
                        3002L
                );

        passed += check(source, "w117-echo-delivered",
                target.deliveredIpv4() >= 1);
        passed += check(source, "w117-echo-reply-created",
                !echoReply.isEmpty());
        passed += check(source, "w117-echo-reply-swaps-ip",
                !echoReply.isEmpty()
                        && "192.168.10.20".equals(
                        echoReply.get(0).targetIp
                ));

        int total = 50;
        int failed = total - passed;
        int p = passed;
        int f = failed;

        source.sendSuccess(
                () -> Component.literal(
                        "W1.17 Full Phase Result: "
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
                        117,
                        1,
                        "ASA117_1",
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

    private static OSINetworkPacket ipv4(
            String sourceIp,
            String targetIp,
            boolean tcp
    ) {
        OSINetworkPacket packet =
                new OSINetworkPacket();

        packet.sourceIp = sourceIp;
        packet.targetIp = targetIp;
        packet.sourcePort = 56000;
        packet.targetPort = 443;
        packet.ipProtocol = tcp ? 6 : 17;
        packet.applicationProtocol =
                tcp ? "TCP" : "UDP";
        packet.ipPacketLength = 96;
        packet.ttl = 64;
        packet.payload = new CompoundTag();

        return packet;
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

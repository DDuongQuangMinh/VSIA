package com.k1ngtle.vsia.signality.debug;

import com.k1ngtle.vsia.Vsia;
import com.k1ngtle.vsia.signality.engineering.firewall.w117.W117HostEndpoint;
import com.k1ngtle.vsia.signality.engineering.firewall.w118.W118DhcpClient;
import com.k1ngtle.vsia.signality.engineering.firewall.w118.W118DhcpLease;
import com.k1ngtle.vsia.signality.engineering.firewall.w118.W118DhcpMessage;
import com.k1ngtle.vsia.signality.engineering.firewall.w118.W118DhcpRelay;
import com.k1ngtle.vsia.signality.engineering.firewall.w118.W118DhcpServer;
import com.k1ngtle.vsia.signality.engineering.firewall.w118.W118DnsCache;
import com.k1ngtle.vsia.signality.engineering.firewall.w118.W118DnsMessage;
import com.k1ngtle.vsia.signality.engineering.firewall.w118.W118DnsServer;
import com.k1ngtle.vsia.signality.internet.OSINetworkPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod.EventBusSubscriber(
        modid = Vsia.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class WifiW118TestCommand {
    private WifiW118TestCommand() {
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("wifiw118test")
                        .requires(source -> source.hasPermission(2))
                        .executes(context -> run(context.getSource()))
        );
    }

    private static int run(
            net.minecraft.commands.CommandSourceStack source
    ) {
        int passed = 0;

        String clientMac = "02:18:00:00:10:64";
        String serverMac = "02:FA:01:01:00:01";

        OSINetworkPacket discover =
                W118DhcpMessage.discover(
                        clientMac,
                        118001
                );

        passed += check(source, "w118-dhcp-discover-detect",
                W118DhcpMessage.isDhcp(discover));
        passed += check(source, "w118-dhcp-discover-type",
                W118DhcpMessage.type(discover) == W118DhcpMessage.Type.DISCOVER);
        passed += check(source, "w118-dhcp-discover-src-ip",
                "0.0.0.0".equals(discover.sourceIp));
        passed += check(source, "w118-dhcp-discover-dst-ip",
                "255.255.255.255".equals(discover.targetIp));
        passed += check(source, "w118-dhcp-discover-ports",
                discover.sourcePort == 68 && discover.targetPort == 67);
        passed += check(source, "w118-dhcp-discover-mac",
                clientMac.equals(discover.sourceMac));
        passed += check(source, "w118-dhcp-discover-xid",
                W118DhcpMessage.xid(discover) == 118001);

        W118DhcpServer server =
                configuredServer();

        OSINetworkPacket offer =
                server.handle(
                        discover,
                        serverMac,
                        1_000L
                );

        passed += check(source, "w118-dhcp-offer-created",
                offer != null);
        passed += check(source, "w118-dhcp-offer-type",
                offer != null
                        && W118DhcpMessage.type(offer) == W118DhcpMessage.Type.OFFER);
        passed += check(source, "w118-dhcp-offer-ip",
                offer != null
                        && "192.168.10.100".equals(W118DhcpMessage.yourIp(offer)));
        passed += check(source, "w118-dhcp-offer-mask",
                offer != null
                        && "255.255.255.0".equals(W118DhcpMessage.subnetMask(offer)));
        passed += check(source, "w118-dhcp-offer-gateway",
                offer != null
                        && "192.168.10.1".equals(W118DhcpMessage.gateway(offer)));
        passed += check(source, "w118-dhcp-offer-dns",
                offer != null
                        && "192.168.10.1".equals(W118DhcpMessage.dnsServer(offer)));
        passed += check(source, "w118-dhcp-offer-lease",
                offer != null
                        && W118DhcpMessage.leaseSeconds(offer) == 600L);

        W118DhcpClient client =
                new W118DhcpClient();

        OSINetworkPacket clientDiscover =
                client.start(
                        clientMac,
                        2_000L
                );

        passed += check(source, "w118-client-selecting",
                client.state() == W118DhcpClient.State.SELECTING);

        OSINetworkPacket clientOffer =
                server.handle(
                        clientDiscover,
                        serverMac,
                        2_001L
                );

        List<OSINetworkPacket> requestFrames =
                client.receive(
                        clientOffer,
                        clientMac,
                        2_002L
                );

        passed += check(source, "w118-client-requesting",
                client.state() == W118DhcpClient.State.REQUESTING);
        passed += check(source, "w118-client-request-created",
                requestFrames.size() == 1
                        && W118DhcpMessage.type(requestFrames.get(0))
                        == W118DhcpMessage.Type.REQUEST);

        OSINetworkPacket request =
                requestFrames.get(0);

        passed += check(source, "w118-request-ip",
                "192.168.10.100".equals(
                        W118DhcpMessage.requestedIp(request)
                ));
        passed += check(source, "w118-request-server-id",
                "192.168.10.1".equals(
                        W118DhcpMessage.serverId(request)
                ));

        OSINetworkPacket ack =
                server.handle(
                        request,
                        serverMac,
                        2_003L
                );

        passed += check(source, "w118-ack-created",
                ack != null
                        && W118DhcpMessage.type(ack)
                        == W118DhcpMessage.Type.ACK);

        client.receive(
                ack,
                clientMac,
                2_004L
        );

        passed += check(source, "w118-client-bound",
                client.bound());
        passed += check(source, "w118-client-ip-config",
                client.configuration() != null
                        && "192.168.10.100".equals(
                        client.configuration().ipv4()
                ));
        passed += check(source, "w118-client-mask-config",
                client.configuration() != null
                        && "255.255.255.0".equals(
                        client.configuration().subnetMask()
                ));
        passed += check(source, "w118-client-gw-config",
                client.configuration() != null
                        && "192.168.10.1".equals(
                        client.configuration().gateway()
                ));
        passed += check(source, "w118-client-dns-config",
                client.configuration() != null
                        && "192.168.10.1".equals(
                        client.configuration().dnsServer()
                ));
        passed += check(source, "w118-server-one-lease",
                server.leaseCount(2_005L) == 1);

        W118DhcpLease lease =
                server.leaseForMac(
                        clientMac,
                        2_005L
                );

        passed += check(source, "w118-lease-owned",
                lease != null
                        && clientMac.equals(lease.clientMac()));
        passed += check(source, "w118-lease-t1-before-t2",
                lease != null
                        && lease.t1Millis() < lease.t2Millis());
        passed += check(source, "w118-lease-t2-before-expiry",
                lease != null
                        && lease.t2Millis() < lease.endMillis());

        long t1 =
                client.configuration().t1Millis() + 1L;

        List<OSINetworkPacket> renewFrames =
                client.tick(
                        clientMac,
                        t1
                );

        passed += check(source, "w118-t1-renew-state",
                client.state() == W118DhcpClient.State.RENEWING);
        passed += check(source, "w118-t1-renew-request",
                renewFrames.size() == 1
                        && W118DhcpMessage.type(renewFrames.get(0))
                        == W118DhcpMessage.Type.REQUEST);

        OSINetworkPacket renewAck =
                server.handle(
                        renewFrames.get(0),
                        serverMac,
                        t1 + 1L
                );

        client.receive(
                renewAck,
                clientMac,
                t1 + 2L
        );

        passed += check(source, "w118-renew-bound",
                client.bound());
        passed += check(source, "w118-renew-same-ip",
                "192.168.10.100".equals(
                        client.configuration().ipv4()
                ));

        W118DhcpClient noServerClient =
                new W118DhcpClient();

        noServerClient.start(
                "02:18:00:00:10:65",
                0L
        );

        passed += check(source, "w118-retry-not-early",
                noServerClient.tick(
                        "02:18:00:00:10:65",
                        3_000L
                ).isEmpty());
        passed += check(source, "w118-retry-discover",
                noServerClient.tick(
                        "02:18:00:00:10:65",
                        4_000L
                ).size() == 1);

        W118DhcpServer twoClientServer =
                configuredServer();

        OSINetworkPacket d1 =
                W118DhcpMessage.discover(
                        "02:18:00:00:10:71",
                        1
                );

        OSINetworkPacket d2 =
                W118DhcpMessage.discover(
                        "02:18:00:00:10:72",
                        2
                );

        OSINetworkPacket o1 =
                twoClientServer.handle(
                        d1,
                        serverMac,
                        10L
                );

        OSINetworkPacket o2 =
                twoClientServer.handle(
                        d2,
                        serverMac,
                        11L
                );

        passed += check(source, "w118-two-offers-unique",
                o1 != null
                        && o2 != null
                        && !W118DhcpMessage.yourIp(o1)
                        .equals(W118DhcpMessage.yourIp(o2)));

        W118DhcpRelay relay =
                new W118DhcpRelay(
                        "10.1.0.1",
                        "10.0.0.10"
                );

        OSINetworkPacket relayed =
                relay.relayClientToServer(discover);

        passed += check(source, "w118-relay-client-server",
                relayed != null
                        && "10.1.0.1".equals(relayed.sourceIp)
                        && "10.0.0.10".equals(relayed.targetIp));
        passed += check(source, "w118-relay-giaddr",
                relayed != null
                        && "10.1.0.1".equals(
                        relayed.payload.getString("dhcp_giaddr")
                ));

        OSINetworkPacket relayBack =
                relay.relayServerToClient(offer);

        passed += check(source, "w118-relay-server-client",
                relayBack != null
                        && "255.255.255.255".equals(relayBack.targetIp));

        W118DnsMessage.normalize("VSIA.TEST.");
        passed += check(source, "w118-dns-normalize",
                "vsia.test".equals(
                        W118DnsMessage.normalize("VSIA.TEST.")
                ));

        OSINetworkPacket dnsQuery =
                W118DnsMessage.query(
                        "192.168.10.100",
                        "192.168.10.1",
                        53001,
                        118,
                        "vsia.test"
                );

        passed += check(source, "w118-dns-query-detect",
                W118DnsMessage.isDns(dnsQuery));
        passed += check(source, "w118-dns-query-port53",
                dnsQuery.targetPort == 53
                        && dnsQuery.sourcePort == 53001);
        passed += check(source, "w118-dns-query-name",
                "vsia.test".equals(
                        W118DnsMessage.queryName(dnsQuery)
                ));

        W118DnsServer dnsServer =
                new W118DnsServer();

        OSINetworkPacket dnsResponse =
                dnsServer.handle(
                        dnsQuery,
                        "192.168.10.1",
                        100L
                );

        passed += check(source, "w118-dns-response-created",
                dnsResponse != null
                        && W118DnsMessage.isResponse(dnsResponse));
        passed += check(source, "w118-dns-answer",
                dnsResponse != null
                        && "203.0.113.20".equals(
                        W118DnsMessage.answer(dnsResponse)
                ));
        passed += check(source, "w118-dns-rcode-zero",
                dnsResponse != null
                        && W118DnsMessage.rcode(dnsResponse) == 0);

        OSINetworkPacket nxQuery =
                W118DnsMessage.query(
                        "192.168.10.100",
                        "192.168.10.1",
                        53002,
                        119,
                        "missing.vsia.test"
                );

        OSINetworkPacket nxResponse =
                dnsServer.handle(
                        nxQuery,
                        "192.168.10.1",
                        101L
                );

        passed += check(source, "w118-dns-nxdomain",
                nxResponse != null
                        && W118DnsMessage.rcode(nxResponse) == 3
                        && W118DnsMessage.answer(nxResponse).isBlank());

        W118DnsCache cache =
                new W118DnsCache();

        cache.put(
                "vsia.test",
                "203.0.113.20",
                0,
                30L,
                1_000L
        );

        passed += check(source, "w118-dns-cache-hit",
                cache.lookup(
                        "VSIA.TEST",
                        2_000L
                ).isPresent());
        passed += check(source, "w118-dns-cache-answer",
                cache.lookup(
                        "vsia.test",
                        2_000L
                )
                        .map(e -> "203.0.113.20".equals(e.answer()))
                        .orElse(false));
        passed += check(source, "w118-dns-cache-expire",
                cache.lookup(
                        "vsia.test",
                        31_001L
                ).isEmpty());

        W117HostEndpoint host =
                new W117HostEndpoint(
                        "DHCP-HOST",
                        "0.0.0.0",
                        "0.0.0.0",
                        "0.0.0.0",
                        "02:18:00:00:10:80"
                );

        host.w118EnableDhcp();

        passed += check(source, "w118-host-dhcp-enabled",
                host.w118DhcpEnabled());
        passed += check(source, "w118-host-unassigned",
                "0.0.0.0".equals(host.ipv4()));

        List<OSINetworkPacket> hostStart =
                host.w118StartDhcp(10_000L);

        passed += check(source, "w118-host-start-discover",
                hostStart.size() == 1
                        && W118DhcpMessage.type(hostStart.get(0))
                        == W118DhcpMessage.Type.DISCOVER);

        W118DhcpServer hostServer =
                configuredServer();

        OSINetworkPacket hostOffer =
                hostServer.handle(
                        hostStart.get(0),
                        serverMac,
                        10_001L
                );

        List<OSINetworkPacket> hostRequest =
                host.receive(
                        hostOffer,
                        10_002L
                );

        OSINetworkPacket hostAck =
                hostServer.handle(
                        hostRequest.get(0),
                        serverMac,
                        10_003L
                );

        host.receive(
                hostAck,
                10_004L
        );

        passed += check(source, "w118-host-bound",
                host.w118DhcpBound());
        passed += check(source, "w118-host-ip-applied",
                "192.168.10.100".equals(host.ipv4()));
        passed += check(source, "w118-host-gateway-applied",
                "192.168.10.1".equals(host.defaultGateway()));
        passed += check(source, "w118-host-dns-applied",
                "192.168.10.1".equals(host.w118DnsServer()));

        int total = 57;
        int failed = total - passed;
        int p = passed;
        int f = failed;

        source.sendSuccess(
                () -> Component.literal(
                        "W1.18 Full Phase Result: "
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

    private static W118DhcpServer configuredServer() {
        W118DhcpServer server =
                new W118DhcpServer();

        server.configure(
                "192.168.10.1",
                "255.255.255.0",
                "192.168.10.1",
                "192.168.10.1",
                "192.168.10.100",
                "192.168.10.199",
                600L
        );

        return server;
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

package com.k1ngtle.vsia.signality.debug;

import com.k1ngtle.vsia.Vsia;
import com.k1ngtle.vsia.signality.engineering.wifi.ip.ipv6.*;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = Vsia.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class WifiIpv6LabCommand {
    private WifiIpv6LabCommand() {
    }

    @SubscribeEvent
    public static void register(
            RegisterCommandsEvent event
    ) {
        event.getDispatcher().register(
                Commands.literal("wifiw1ipv6lab")
                        .requires(source -> source.hasPermission(2))
                        .then(
                                Commands.literal("demo")
                                        .executes(context -> runDemo(context.getSource()))
                        )
                        .then(
                                Commands.literal("view")
                                        .executes(context -> runView(context.getSource()))
                        )
        );
    }

    private static int runDemo(
            net.minecraft.commands.CommandSourceStack source
    ) {
        String clientMac = "02:11:22:33:44:55";

        Ipv6Prefix clientPrefix =
                Ipv6Prefix.parse("2001:db8:10::/64");

        Ipv6Prefix serverPrefix =
                Ipv6Prefix.parse("2001:db8:20::/64");

        Ipv6Address client =
                SlaacEngine.formAddress(
                        clientPrefix,
                        clientMac
                );

        Ipv6Address server =
                Ipv6Address.parse(
                        "2001:db8:20::20"
                );

        Ipv6Address routerA =
                Ipv6Address.parse("fe80::a");

        Ipv6Address routerB =
                Ipv6Address.parse("fe80::b");

        Ipv6RoutingTable routeA =
                new Ipv6RoutingTable();

        routeA.add(
                new Ipv6Route(
                        serverPrefix,
                        routerB,
                        "transit0",
                        10
                )
        );

        Ipv6RoutingTable routeB =
                new Ipv6RoutingTable();

        routeB.add(
                new Ipv6Route(
                        clientPrefix,
                        routerA,
                        "transit0",
                        10
                )
        );

        byte[] udp =
                RawUdp6Codec.encode(
                        client,
                        server,
                        50000,
                        40001,
                        "VSIA-IPv6".getBytes(
                                java.nio.charset.StandardCharsets.UTF_8
                        )
                );

        byte[] packet =
                RawIpv6Codec.encode(
                        client,
                        server,
                        0,
                        0x115,
                        17,
                        64,
                        udp
                );

        byte[] hop1 =
                RawIpv6Codec.decrementHopLimit(
                        packet
                );

        byte[] hop2 =
                RawIpv6Codec.decrementHopLimit(
                        hop1
                );

        RawIpv6Packet arrived =
                RawIpv6Codec.decode(
                        hop2
                );

        RawUdp6Codec.Decoded udpArrived =
                RawUdp6Codec.decode(
                        arrived.source(),
                        arrived.destination(),
                        arrived.payload()
                );

        source.sendSuccess(
                () -> Component.literal(
                        "W1.15 IPv6 LAB READY"
                ).withStyle(ChatFormatting.AQUA),
                false
        );

        source.sendSuccess(
                () -> Component.literal(
                        "SLAAC client="
                                + client
                                + " prefix="
                                + clientPrefix
                ),
                false
        );

        source.sendSuccess(
                () -> Component.literal(
                        "ND/RS/RA modeled | DAD=PREFERRED"
                ),
                false
        );

        source.sendSuccess(
                () -> Component.literal(
                        "Route A -> "
                                + routeA.lookup(server).egressInterface()
                                + " nextHop="
                                + routeA.lookup(server).nextHop()
                ),
                false
        );

        source.sendSuccess(
                () -> Component.literal(
                        "IPv6 UDP multi-router delivered="
                                + udpArrived.checksumValid()
                                + " hopLimit="
                                + arrived.hopLimit()
                                + " payload="
                                + new String(
                                udpArrived.payload(),
                                java.nio.charset.StandardCharsets.UTF_8
                        )
                ).withStyle(
                        udpArrived.checksumValid()
                                ? ChatFormatting.GREEN
                                : ChatFormatting.RED
                ),
                false
        );

        source.sendSuccess(
                () -> Component.literal(
                        "IPv6 router fragmentation=DISABLED | PTB/PMTUD=ENABLED"
                ),
                false
        );

        return udpArrived.checksumValid()
                && arrived.hopLimit() == 62
                ? 1
                : 0;
    }

    private static int runView(
            net.minecraft.commands.CommandSourceStack source
    ) {
        Ipv6Prefix prefix =
                Ipv6Prefix.parse(
                        "2001:db8:10::/64"
                );

        String mac =
                "02:11:22:33:44:55";

        Ipv6AnalyzerSnapshot snapshot =
                new Ipv6AnalyzerSnapshot(
                        Ipv6Address.linkLocalFromMac(mac),
                        SlaacEngine.formAddress(prefix, mac),
                        prefix,
                        Ipv6Address.parse("fe80::1"),
                        64,
                        1500,
                        1,
                        2,
                        "REACHABLE",
                        "PREFERRED",
                        "UDP6 | ICMPv6 | AAAA | PMTUD"
                );

        source.sendSuccess(
                () -> Component.literal(
                        snapshot.render()
                ).withStyle(ChatFormatting.AQUA),
                false
        );

        return 1;
    }
}

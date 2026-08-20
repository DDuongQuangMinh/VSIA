package com.k1ngtle.vsia.signality.debug;

import com.k1ngtle.vsia.Vsia;
import com.k1ngtle.vsia.signality.internet.OSINetworkPacket;
import com.k1ngtle.vsia.signality.internet.server.FirewallTransitProbeFactory;
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
public final class WifiW1161LiveProbeTestCommand {
    private WifiW1161LiveProbeTestCommand() {
    }

    @SubscribeEvent
    public static void register(
            RegisterCommandsEvent event
    ) {
        event.getDispatcher().register(
                Commands.literal("wifiw1firewallprobetest")
                        .requires(source -> source.hasPermission(2))
                        .executes(context -> run(context.getSource()))
        );
    }

    private static int run(
            net.minecraft.commands.CommandSourceStack source
    ) {
        int passed = 0;

        OSINetworkPacket lan =
                FirewallTransitProbeFactory.lanProbe();

        passed += check(
                source,
                "w1161-probe-lan-src-ip",
                "192.168.10.20".equals(lan.sourceIp)
        );

        passed += check(
                source,
                "w1161-probe-lan-dst-ip",
                "198.51.100.20".equals(lan.targetIp)
        );

        passed += check(
                source,
                "w1161-probe-lan-udp",
                lan.ipProtocol == 17
                        && "UDP".equals(lan.applicationProtocol)
        );

        passed += check(
                source,
                "w1161-probe-lan-ttl",
                lan.ttl == 64
        );

        passed += check(
                source,
                "w1161-probe-lan-broadcast-l2",
                "FF:FF:FF:FF:FF:FF".equalsIgnoreCase(
                        lan.targetMac
                )
        );

        OSINetworkPacket wan =
                FirewallTransitProbeFactory.wanProbe();

        passed += check(
                source,
                "w1161-probe-wan-src-ip",
                "198.51.100.20".equals(wan.sourceIp)
        );

        passed += check(
                source,
                "w1161-probe-wan-dst-ip",
                "203.0.113.10".equals(wan.targetIp)
        );

        passed += check(
                source,
                "w1161-probe-wan-response",
                wan.isResponse
        );

        passed += check(
                source,
                "w1161-probe-wan-udp",
                wan.ipProtocol == 17
        );

        passed += check(
                source,
                "w1161-probe-session-ids",
                !lan.sessionId.equals(wan.sessionId)
        );

        int total = 10;
        int failed = total - passed;
        int p = passed;
        int f = failed;

        source.sendSuccess(
                () -> Component.literal(
                        "W1.16.1 Live Probe Result: "
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

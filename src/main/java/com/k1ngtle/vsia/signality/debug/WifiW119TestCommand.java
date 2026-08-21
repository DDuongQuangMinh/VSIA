package com.k1ngtle.vsia.signality.debug;

import com.k1ngtle.vsia.Vsia;
import com.k1ngtle.vsia.signality.engineering.wifi.bridge.w119.W119ApBridgeEngine;
import com.k1ngtle.vsia.signality.engineering.wifi.bridge.w119.W119BridgeAction;
import com.k1ngtle.vsia.signality.engineering.wifi.bridge.w119.W119BridgePort;
import com.k1ngtle.vsia.signality.engineering.wifi.bridge.w119.W119BridgeTable;
import com.k1ngtle.vsia.signality.engineering.wifi.bridge.w119.W119Mac;
import com.k1ngtle.vsia.signality.engineering.wifi.bridge.w119.W119StationTable;
import com.k1ngtle.vsia.signality.internet.OSINetworkPacket;
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
public final class WifiW119TestCommand {
    private WifiW119TestCommand() {
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("wifiw119test")
                        .requires(source -> source.hasPermission(2))
                        .executes(context -> run(context.getSource()))
        );
    }

    private static int run(
            net.minecraft.commands.CommandSourceStack source
    ) {
        int passed = 0;

        passed += check(source, "w119-mac-normalize-colon",
                "02:11:22:33:44:55".equals(
                        W119Mac.normalize("02:11:22:33:44:55")
                ));

        passed += check(source, "w119-mac-normalize-compact",
                "02:11:22:33:44:55".equals(
                        W119Mac.normalize("021122334455")
                ));

        passed += check(source, "w119-mac-equality",
                W119Mac.equals(
                        "021122334455",
                        "02:11:22:33:44:55"
                ));

        passed += check(source, "w119-broadcast-detect",
                W119Mac.isBroadcast(
                        "ff:ff:ff:ff:ff:ff"
                ));

        passed += check(source, "w119-multicast-detect",
                W119Mac.isMulticast(
                        "01:00:5E:00:00:01"
                ));

        passed += check(source, "w119-unicast-not-group",
                !W119Mac.isGroup(
                        "02:11:22:33:44:55"
                ));

        W119StationTable stations =
                new W119StationTable(1_000L);

        var stationA =
                stations.learn(
                        "02:19:00:00:00:0A",
                        0L
                );

        passed += check(source, "w119-station-learn",
                stations.size(10L) == 1);

        passed += check(source, "w119-station-aid-positive",
                stationA.associationId() > 0);

        var stationAAgain =
                stations.learn(
                        "02:19:00:00:00:0A",
                        100L
                );

        passed += check(source, "w119-station-aid-stable",
                stationA.associationId()
                        == stationAAgain.associationId());

        stations.learn(
                "02:19:00:00:00:0B",
                100L
        );

        passed += check(source, "w119-station-second-aid",
                stations.lookup(
                        "02:19:00:00:00:0B",
                        101L
                )
                        .map(e -> e.associationId()
                                != stationA.associationId())
                        .orElse(false));

        passed += check(source, "w119-station-expire",
                stations.size(1_101L) == 0);

        W119BridgeTable fdb =
                new W119BridgeTable(1_000L);

        fdb.learn(
                "02:19:00:00:00:11",
                W119BridgePort.WIRELESS,
                0L
        );

        passed += check(source, "w119-fdb-wireless-learn",
                fdb.lookup(
                        "02:19:00:00:00:11",
                        10L
                )
                        .map(e -> e.port()
                                == W119BridgePort.WIRELESS)
                        .orElse(false));

        fdb.learn(
                "02:19:00:00:00:12",
                W119BridgePort.DISTRIBUTION_SYSTEM,
                0L
        );

        passed += check(source, "w119-fdb-ds-learn",
                fdb.lookup(
                        "02:19:00:00:00:12",
                        10L
                )
                        .map(e -> e.port()
                                == W119BridgePort.DISTRIBUTION_SYSTEM)
                        .orElse(false));

        fdb.learn(
                W119Mac.BROADCAST,
                W119BridgePort.WIRELESS,
                0L
        );

        passed += check(source, "w119-fdb-no-group-learn",
                fdb.size(10L) == 2);

        passed += check(source, "w119-fdb-expire",
                fdb.size(1_001L) == 0);

        String apMac =
                "02:19:AA:00:00:01";

        String staA =
                "02:19:00:00:00:0A";

        String staB =
                "02:19:00:00:00:0B";

        String wired =
                "02:19:00:00:71:20";

        W119ApBridgeEngine bridge =
                new W119ApBridgeEngine(
                        apMac
                );

        OSINetworkPacket staToWired =
                packet(
                        staA,
                        wired
                );

        var wirelessToDs =
                bridge.wirelessIngress(
                        staToWired,
                        true,
                        false,
                        100L
                );

        passed += check(source, "w119-sta-tods-accepted",
                wirelessToDs.action()
                        == W119BridgeAction.TO_DISTRIBUTION_SYSTEM);

        passed += check(source, "w119-sta-learned-after-data",
                bridge.associated(
                        staA,
                        101L
                ));

        passed += check(source, "w119-sta-fdb-wireless",
                bridge.bridgeTable()
                        .lookup(
                                staA,
                                101L
                        )
                        .map(e -> e.port()
                                == W119BridgePort.WIRELESS)
                        .orElse(false));

        var noToDs =
                bridge.wirelessIngress(
                        packet(staA, wired),
                        false,
                        false,
                        200L
                );

        passed += check(source, "w119-reject-no-tods",
                noToDs.action()
                        == W119BridgeAction.DROP);

        var badFromDs =
                bridge.wirelessIngress(
                        packet(staA, wired),
                        true,
                        true,
                        201L
                );

        passed += check(source, "w119-reject-wds",
                badFromDs.action()
                        == W119BridgeAction.DROP);

        var apLocal =
                bridge.wirelessIngress(
                        packet(staA, apMac),
                        true,
                        false,
                        202L
                );

        passed += check(source, "w119-ap-local",
                apLocal.action()
                        == W119BridgeAction.LOCAL);

        var wirelessBroadcast =
                bridge.wirelessIngress(
                        packet(
                                staA,
                                W119Mac.BROADCAST
                        ),
                        true,
                        false,
                        203L
                );

        passed += check(source, "w119-wireless-broadcast-to-ds",
                wirelessBroadcast.action()
                        == W119BridgeAction.TO_DISTRIBUTION_SYSTEM);

        bridge.wirelessIngress(
                packet(staB, wired),
                true,
                false,
                204L
        );

        var intraBss =
                bridge.wirelessIngress(
                        packet(staA, staB),
                        true,
                        false,
                        205L
                );

        passed += check(source, "w119-intra-bss-controller-handled",
                intraBss.action()
                        == W119BridgeAction.CONTROLLER_HANDLED);

        bridge.setClientIsolation(true);

        var isolated =
                bridge.wirelessIngress(
                        packet(staA, staB),
                        true,
                        false,
                        206L
                );

        passed += check(source, "w119-client-isolation",
                isolated.action()
                        == W119BridgeAction.DROP);

        bridge.setClientIsolation(false);

        passed += check(source, "w119-client-isolation-off",
                !bridge.clientIsolation());

        var dsToSta =
                bridge.distributionIngress(
                        packet(wired, staA),
                        300L
                );

        passed += check(source, "w119-ds-to-associated-sta",
                dsToSta.action()
                        == W119BridgeAction.TO_WIRELESS);

        passed += check(source, "w119-ds-source-learned",
                bridge.bridgeTable()
                        .lookup(
                                wired,
                                301L
                        )
                        .map(e -> e.port()
                                == W119BridgePort.DISTRIBUTION_SYSTEM)
                        .orElse(false));

        var dsBroadcast =
                bridge.distributionIngress(
                        packet(
                                wired,
                                W119Mac.BROADCAST
                        ),
                        302L
                );

        passed += check(source, "w119-ds-broadcast-wireless",
                dsBroadcast.action()
                        == W119BridgeAction.TO_WIRELESS);

        var dsMulticast =
                bridge.distributionIngress(
                        packet(
                                wired,
                                "01:00:5E:00:00:01"
                        ),
                        303L
                );

        passed += check(source, "w119-ds-multicast-wireless",
                dsMulticast.action()
                        == W119BridgeAction.TO_WIRELESS);

        var dsLocal =
                bridge.distributionIngress(
                        packet(wired, apMac),
                        304L
                );

        passed += check(source, "w119-ds-ap-local",
                dsLocal.action()
                        == W119BridgeAction.LOCAL);

        var dsUnknown =
                bridge.distributionIngress(
                        packet(
                                wired,
                                "02:19:00:00:99:99"
                        ),
                        305L
                );

        passed += check(source, "w119-ds-unknown-no-reflection",
                dsUnknown.action()
                        == W119BridgeAction.DROP);

        W119ApBridgeEngine emptyBridge =
                new W119ApBridgeEngine(
                        apMac
                );

        var unknownBeforeLearn =
                emptyBridge.distributionIngress(
                        packet(wired, staA),
                        400L
                );

        passed += check(source, "w119-no-phantom-station",
                unknownBeforeLearn.action()
                        == W119BridgeAction.DROP);

        passed += check(source, "w119-empty-station-count",
                emptyBridge.stationCount(
                        401L
                ) == 0);

        bridge.noteDistributionTransmit();
        bridge.noteWirelessTransmit();

        String status =
                bridge.status(
                        500L
                );

        passed += check(source, "w119-status-stations",
                status.contains(
                        "stations=2"
                ));

        passed += check(source, "w119-status-fdb",
                status.contains(
                        "fdb="
                ));

        passed += check(source, "w119-status-wireless-rx",
                status.contains(
                        "wirelessRx="
                ));

        passed += check(source, "w119-status-ds-rx",
                status.contains(
                        "dsRx="
                ));

        passed += check(source, "w119-status-group-flood",
                status.contains(
                        "groupFloods="
                ));

        passed += check(source, "w119-status-intra-bss",
                status.contains(
                        "intraBss="
                ));

        bridge.noteDistributionFailure();

        passed += check(source, "w119-status-ds-failure",
                bridge.status(501L)
                        .contains(
                                "dsFailures=1"
                        ));

        bridge.clearDynamic();

        passed += check(source, "w119-clear-stations",
                bridge.stationCount(
                        502L
                ) == 0);

        passed += check(source, "w119-clear-fdb",
                bridge.bridgeEntryCount(
                        502L
                ) == 0);

        passed += check(source, "w119-clear-counters",
                bridge.status(502L)
                        .contains(
                                "wirelessRx=0"
                        ));

        int total = 43;
        int failed =
                total - passed;

        int p = passed;
        int f = failed;

        source.sendSuccess(
                () -> Component.literal(
                        "W1.19 Full Phase Result: "
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

    private static OSINetworkPacket packet(
            String sourceMac,
            String targetMac
    ) {
        OSINetworkPacket packet =
                new OSINetworkPacket();

        packet.sourceMac = sourceMac;
        packet.targetMac = targetMac;
        packet.sourceIp = "192.168.10.20";
        packet.targetIp = "203.0.113.20";
        packet.sourcePort = 50000;
        packet.targetPort = 443;
        packet.ipProtocol = 17;
        packet.applicationProtocol = "UDP";
        packet.ipPacketLength = 96;
        packet.ttl = 64;

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
                    ).withStyle(
                            ChatFormatting.GREEN
                    ),
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

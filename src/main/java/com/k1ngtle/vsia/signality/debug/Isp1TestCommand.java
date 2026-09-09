package com.k1ngtle.vsia.signality.debug;

import com.k1ngtle.vsia.Vsia;
import com.k1ngtle.vsia.signality.internet.NetworkDeviceBlockEntity;
import com.k1ngtle.vsia.signality.internet.provider.InternetRegistryResult;
import com.k1ngtle.vsia.signality.internet.provider.InternetRegistrySavedData;
import com.k1ngtle.vsia.signality.internet.provider.Isp1UnitTestSuite;
import com.k1ngtle.vsia.signality.internet.server.ServerRackBlockEntity;
import com.k1ngtle.vsia.signality.engineering.wifi.ip.workflow.WifiRawIpWorkflowSnapshot;
import com.k1ngtle.vsia.signality.engineering.wifi.WifiSecurityState;
import com.k1ngtle.vsia.signality.engineering.wifi.WifiStationState;
import com.k1ngtle.vsia.signality.engineering.wifi.ip.routing.Ipv4Prefix;
import com.k1ngtle.vsia.signality.engineering.wifi.ip.workflow.WifiRawIpWorkflowState;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = Vsia.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class Isp1TestCommand {
    private static BlockPos liveSta;
    private static String liveHost = "";
    private static String liveExpectedIp = "";
    private static long liveStartedTick = -1L;

    private Isp1TestCommand() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("isp1test")
                        .then(
                                Commands.literal("unit")
                                        .executes(context -> unit(context.getSource()))
                        )
                        .then(
                                Commands.literal("live")
                                        .then(
                                                Commands.argument(
                                                                "coords",
                                                                StringArgumentType.greedyString()
                                                        )
                                                        .executes(
                                                                context -> live(
                                                                        context.getSource(),
                                                                        StringArgumentType.getString(
                                                                                context,
                                                                                "coords"
                                                                        )
                                                                )
                                                        )
                                        )
                        )
                        .then(
                                Commands.literal("preflight")
                                        .then(
                                                Commands.argument(
                                                                "coords",
                                                                StringArgumentType.greedyString()
                                                        )
                                                        .executes(
                                                                context -> preflight(
                                                                        context.getSource(),
                                                                        StringArgumentType.getString(
                                                                                context,
                                                                                "coords"
                                                                        )
                                                                )
                                                        )
                                        )
                        )
                        .then(
                                Commands.literal("status")
                                        .executes(context -> status(context.getSource()))
                        )
                        .then(
                                Commands.literal("reset")
                                        .executes(context -> reset(context.getSource()))
                        )
        );
    }

    private static int unit(CommandSourceStack source) {
        var results = Isp1UnitTestSuite.runAll();
        int passed = 0;

        for (var result : results) {
            if (result.passed()) {
                passed++;
            }

            source.sendSuccess(
                    () -> Component.literal(
                            "[" + (result.passed() ? "PASS" : "FAIL") + "] " + result.name()
                    ).withStyle(
                            result.passed() ? ChatFormatting.GREEN : ChatFormatting.RED
                    ),
                    false
            );

            line(source, "  " + result.detail());
        }

        int failed = results.size() - passed;
        int finalPassed = passed;
        int finalFailed = failed;

        source.sendSuccess(
                () -> Component.literal(
                        "ISP1 unit result: " + finalPassed
                                + " passed, " + finalFailed + " failed"
                ).withStyle(
                        finalFailed == 0 ? ChatFormatting.GREEN : ChatFormatting.RED
                ),
                false
        );

        return failed == 0 ? 1 : 0;
    }

    private static int live(
            CommandSourceStack source,
            String raw
    ) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        String[] parts = raw == null ? new String[0] : raw.trim().split("\\s+");

        if (parts.length != 6) {
            source.sendFailure(
                    Component.literal(
                            "Usage: /isp1test live <STA x y z> <ServerRack x y z>"
                    )
            );
            return 0;
        }

        BlockPos staPos;
        BlockPos serverPos;

        try {
            staPos = new BlockPos(
                    Integer.parseInt(parts[0]),
                    Integer.parseInt(parts[1]),
                    Integer.parseInt(parts[2])
            );

            serverPos = new BlockPos(
                    Integer.parseInt(parts[3]),
                    Integer.parseInt(parts[4]),
                    Integer.parseInt(parts[5])
            );
        } catch (NumberFormatException exception) {
            source.sendFailure(Component.literal("Coordinates must be integers."));
            return 0;
        }

        BlockEntity staEntity = source.getLevel().getBlockEntity(staPos);
        BlockEntity serverEntity = source.getLevel().getBlockEntity(serverPos);

        if (!(staEntity instanceof NetworkDeviceBlockEntity sta)) {
            source.sendFailure(Component.literal("STA is not a NetworkDeviceBlockEntity."));
            return 0;
        }

        if (!(serverEntity instanceof ServerRackBlockEntity server)) {
            source.sendFailure(Component.literal("Server target is not a ServerRack."));
            return 0;
        }

        String suffix = player.getUUID()
                .toString()
                .replace("-", "")
                .substring(0, 8);

        String providerId = "isp1test-" + suffix;
        String zone = "isp1test-" + suffix + ".net";
        String host = "www." + zone;

        InternetRegistrySavedData registry =
                InternetRegistrySavedData.get(source.getLevel());

        if (registry.provider(providerId).isEmpty()) {
            InternetRegistryResult provider = registry.registerProvider(
                    player.getUUID(),
                    player.getGameProfile().getName(),
                    providerId,
                    "ISP1 Test Provider " + suffix,
                    System.currentTimeMillis()
            );

            if (!provider.success()) {
                source.sendFailure(Component.literal(provider.message()));
                return 0;
            }
        }

        if (registry.domain(zone).isEmpty()) {
            InternetRegistryResult domain = registry.registerDomain(
                    player.getUUID(),
                    player.getGameProfile().getName(),
                    zone,
                    providerId,
                    System.currentTimeMillis()
            );

            if (!domain.success()) {
                source.sendFailure(Component.literal(domain.message()));
                return 0;
            }
        }

        registry.removeDnsRecord(
                player.getUUID(),
                zone,
                "www",
                "A"
        );

        InternetRegistryResult record = registry.addDnsRecord(
                player.getUUID(),
                zone,
                "www",
                "A",
                server.ipAddress(),
                300
        );

        if (!record.success()) {
            source.sendFailure(Component.literal(record.message()));
            return 0;
        }

        String rackAnswer = server.resolveDns(host, "A");

        if (!server.ipAddress().equals(rackAnswer)) {
            source.sendFailure(
                    Component.literal(
                            "ServerRack did not resolve the player-owned domain. "
                                    + "Install the ISP1 recursive-DNS patch first."
                    )
            );
            return 0;
        }

        if (sta.wifiStationState() != WifiStationState.ASSOCIATED
                || sta.wifiSecurityState() != WifiSecurityState.SECURED) {
            source.sendFailure(
                    Component.literal(
                            "STA is not ready for ISP1 live traffic | state="
                                    + sta.wifiStationState()
                                    + " security="
                                    + sta.wifiSecurityState()
                                    + " ip="
                                    + sta.wifiIpAddress()
                                    + " bssid="
                                    + sta.wifiSelectedBssid()
                    )
            );

            line(
                    source,
                    "Reconnect the STA first, then rerun /isp1test live. "
                            + "Use /isp1test preflight <STA x y z> to inspect it."
            );

            return 0;
        }

        boolean reusedLease =
                Ipv4Prefix.isUsableUnicast(
                        sta.wifiIpAddress()
                );

        boolean workflowStarted =
                reusedLease
                        && sta.startWifiRawHttpWorkflowWithExistingLease(
                        host,
                        "/"
                );

        if (!workflowStarted) {
            reusedLease =
                    false;

            workflowStarted =
                    sta.startWifiRawHttpWorkflow(
                            host,
                            "/"
                    );
        }

        if (!workflowStarted) {
            WifiRawIpWorkflowSnapshot failedWorkflow =
                    sta.wifiRawIpWorkflowSnapshot();

            source.sendFailure(
                    Component.literal(
                            "STA RAW HTTP workflow could not start | state="
                                    + sta.wifiStationState()
                                    + " security="
                                    + sta.wifiSecurityState()
                                    + " ip="
                                    + sta.wifiIpAddress()
                                    + " workflow="
                                    + failedWorkflow.state()
                                    + " detail="
                                    + failedWorkflow.detail()
                    )
            );

            return 0;
        }

        liveSta = staPos.immutable();
        liveHost = host;
        liveExpectedIp = server.ipAddress();
        liveStartedTick = source.getLevel().getGameTime();

        boolean finalReusedLease =
                reusedLease;

        source.sendSuccess(
                () -> Component.literal(
                        "ISP1 live test started: "
                                + host
                                + " -> "
                                + server.ipAddress()
                                + " | network-start="
                                + (
                                finalReusedLease
                                        ? "EXISTING_LEASE"
                                        : "DHCP"
                        )
                ).withStyle(ChatFormatting.GREEN),
                false
        );

        return 1;
    }

    private static int preflight(
            CommandSourceStack source,
            String raw
    ) {
        String[] parts =
                raw == null
                        ? new String[0]
                        : raw.trim()
                        .split("\\s+");

        if (parts.length != 3) {
            source.sendFailure(
                    Component.literal(
                            "Usage: /isp1test preflight <STA x y z>"
                    )
            );
            return 0;
        }

        BlockPos staPos;

        try {
            staPos =
                    new BlockPos(
                            Integer.parseInt(
                                    parts[0]
                            ),
                            Integer.parseInt(
                                    parts[1]
                            ),
                            Integer.parseInt(
                                    parts[2]
                            )
                    );
        } catch (NumberFormatException exception) {
            source.sendFailure(
                    Component.literal(
                            "Coordinates must be integers."
                    )
            );
            return 0;
        }

        BlockEntity entity =
                source.getLevel()
                        .getBlockEntity(
                                staPos
                        );

        if (!(entity instanceof NetworkDeviceBlockEntity sta)) {
            source.sendFailure(
                    Component.literal(
                            "STA position is not a NetworkDeviceBlockEntity."
                    )
            );
            return 0;
        }

        WifiRawIpWorkflowSnapshot workflow =
                sta.wifiRawIpWorkflowSnapshot();

        boolean linkReady =
                sta.wifiStationState()
                        == WifiStationState.ASSOCIATED
                        && sta.wifiSecurityState()
                        == WifiSecurityState.SECURED;

        boolean leaseReady =
                Ipv4Prefix.isUsableUnicast(
                        sta.wifiIpAddress()
                );

        source.sendSuccess(
                () ->
                        Component.literal(
                                "[ISP1 PREFLIGHT] "
                                        + (
                                        linkReady
                                                ? "LINK READY"
                                                : "LINK NOT READY"
                                )
                        ).withStyle(
                                linkReady
                                        ? ChatFormatting.GREEN
                                        : ChatFormatting.RED
                        ),
                false
        );

        line(
                source,
                "STA state="
                        + sta.wifiStationState()
                        + " security="
                        + sta.wifiSecurityState()
        );

        line(
                source,
                "BSSID="
                        + sta.wifiSelectedBssid()
                        + " IPv4="
                        + sta.wifiIpAddress()
                        + " leaseReady="
                        + leaseReady
        );

        line(
                source,
                "RAW workflow="
                        + workflow.state()
                        + " | "
                        + workflow.detail()
        );

        if (!linkReady) {
            line(
                    source,
                    "ISP1 does not create the Wi-Fi link itself. "
                            + "Associate/secure this STA to a working AP first."
            );
        } else if (leaseReady) {
            line(
                    source,
                    "Live ISP1 will reuse the existing IPv4 lease and start at DNS/ARP."
            );
        } else {
            line(
                    source,
                    "Live ISP1 will obtain IPv4 by DHCP before DNS/TCP/HTTP."
            );
        }

        return linkReady
                ? 1
                : 0;
    }

    private static int status(CommandSourceStack source) {
        if (liveSta == null) {
            source.sendFailure(Component.literal("No ISP1 live test active."));
            return 0;
        }

        BlockEntity entity = source.getLevel().getBlockEntity(liveSta);

        if (!(entity instanceof NetworkDeviceBlockEntity sta)) {
            source.sendFailure(Component.literal("ISP1 live-test STA is missing."));
            return 0;
        }

        WifiRawIpWorkflowSnapshot workflow = sta.wifiRawIpWorkflowSnapshot();

        boolean complete = workflow.state() == WifiRawIpWorkflowState.COMPLETE;
        boolean pass = complete
                && workflow.detail().contains(liveHost)
                && workflow.detail().contains(liveExpectedIp)
                && workflow.detail().contains("HTTP 200");

        source.sendSuccess(
                () -> Component.literal(
                        "[ISP1] "
                                + (pass
                                ? "COMPLETE | PASS"
                                : workflow.state() == WifiRawIpWorkflowState.FAILED
                                ? "FAILED | FAIL"
                                : complete
                                ? "COMPLETE | FAIL"
                                : "RUNNING")
                ).withStyle(
                        pass
                                ? ChatFormatting.GREEN
                                : workflow.state() == WifiRawIpWorkflowState.FAILED || complete
                                ? ChatFormatting.RED
                                : ChatFormatting.YELLOW
                ),
                false
        );

        line(source, "Host: " + liveHost + " -> " + liveExpectedIp);
        line(
                source,
                "STA: state="
                        + sta.wifiStationState()
                        + " security="
                        + sta.wifiSecurityState()
                        + " bssid="
                        + sta.wifiSelectedBssid()
        );
        line(source, "STA IPv4: " + sta.wifiIpAddress());
        line(source, "Workflow: " + workflow.state() + " | " + workflow.detail());
        line(
                source,
                "Elapsed: " + Math.max(
                        0L,
                        source.getLevel().getGameTime() - liveStartedTick
                ) + " ticks"
        );

        return pass ? 1 : workflow.state() == WifiRawIpWorkflowState.FAILED ? 0 : 1;
    }

    private static int reset(CommandSourceStack source) {
        liveSta = null;
        liveHost = "";
        liveExpectedIp = "";
        liveStartedTick = -1L;

        source.sendSuccess(
                () -> Component.literal(
                        "ISP1 live test state reset."
                ).withStyle(ChatFormatting.GREEN),
                false
        );

        return 1;
    }

    private static void line(CommandSourceStack source, String value) {
        source.sendSuccess(
                () -> Component.literal(value).withStyle(ChatFormatting.GRAY),
                false
        );
    }
}

package com.k1ngtle.vsia.signality.debug;

import com.k1ngtle.vsia.Vsia;
import com.k1ngtle.vsia.signality.engineering.wifi.integration.w121.W121IntegrationSnapshot;
import com.k1ngtle.vsia.signality.engineering.wifi.integration.w121.W121IntegrationTestManager;
import com.k1ngtle.vsia.signality.engineering.wifi.integration.w121.W121IntegrationTestResult;
import com.k1ngtle.vsia.signality.engineering.wifi.integration.w121.W121IntegrationTestSuite;
import com.k1ngtle.vsia.signality.internet.NetworkDeviceBlockEntity;
import com.k1ngtle.vsia.signality.internet.router.RtAc68uRouterBlockEntity;
import com.k1ngtle.vsia.signality.internet.server.NetworkSwitchBlockEntity;
import com.k1ngtle.vsia.signality.internet.server.ServerRackBlockEntity;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod.EventBusSubscriber(
        modid = Vsia.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class WifiW121IntegrationTestCommand {
    private static final String PERSIST_ROOT =
            "VSIA_W121_PERSISTENCE_PROBE";

    private WifiW121IntegrationTestCommand() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(
            RegisterCommandsEvent event
    ) {
        register(
                event.getDispatcher()
        );
    }

    public static void register(
            CommandDispatcher<CommandSourceStack> dispatcher
    ) {
        dispatcher.register(
                Commands.literal(
                                "wifiw121test"
                        )
                        .requires(
                                source ->
                                        source.hasPermission(
                                                2
                                        )
                        )
                        .then(
                                Commands.literal(
                                                "unit"
                                        )
                                        .executes(
                                                context ->
                                                        runUnit(
                                                                context.getSource()
                                                        )
                                        )
                        )
                        .then(
                                Commands.literal(
                                                "full"
                                        )
                                        .then(
                                                Commands.argument(
                                                                "coords",
                                                                StringArgumentType.greedyString()
                                                        )
                                                        .executes(
                                                                context ->
                                                                        startLive(
                                                                                context.getSource(),
                                                                                StringArgumentType.getString(
                                                                                        context,
                                                                                        "coords"
                                                                                ),
                                                                                false
                                                                        )
                                                        )
                                        )
                        )
                        .then(
                                Commands.literal(
                                                "all"
                                        )
                                        .then(
                                                Commands.argument(
                                                                "coords",
                                                                StringArgumentType.greedyString()
                                                        )
                                                        .executes(
                                                                context ->
                                                                        startLive(
                                                                                context.getSource(),
                                                                                StringArgumentType.getString(
                                                                                        context,
                                                                                        "coords"
                                                                                ),
                                                                                true
                                                                        )
                                                        )
                                        )
                        )
                        .then(
                                Commands.literal(
                                                "status"
                                        )
                                        .executes(
                                                context ->
                                                        status(
                                                                context.getSource()
                                                        )
                                        )
                        )
                        .then(
                                Commands.literal(
                                                "reset"
                                        )
                                        .executes(
                                                context ->
                                                        reset(
                                                                context.getSource()
                                                        )
                                        )
                        )
                        .then(
                                Commands.literal(
                                                "persistmark"
                                        )
                                        .then(
                                                Commands.argument(
                                                                "coords",
                                                                StringArgumentType.greedyString()
                                                        )
                                                        .executes(
                                                                context ->
                                                                        persistMark(
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
                                Commands.literal(
                                                "persistverify"
                                        )
                                        .then(
                                                Commands.argument(
                                                                "station",
                                                                StringArgumentType.greedyString()
                                                        )
                                                        .executes(
                                                                context ->
                                                                        persistVerify(
                                                                                context.getSource(),
                                                                                StringArgumentType.getString(
                                                                                        context,
                                                                                        "station"
                                                                                )
                                                                        )
                                                        )
                                        )
                        )
        );
    }

    private static int runUnit(
            CommandSourceStack source
    ) {
        List<W121IntegrationTestResult> results =
                W121IntegrationTestSuite.runAll();

        int passed =
                0;

        int failed =
                0;

        for (W121IntegrationTestResult result
                : results) {
            if (result.passed()) {
                passed++;

                source.sendSuccess(
                        () ->
                                Component.literal(
                                        "[PASS] "
                                                + result.id()
                                ).withStyle(
                                        ChatFormatting.GREEN
                                ),
                        false
                );
            } else {
                failed++;

                source.sendFailure(
                        Component.literal(
                                "[FAIL] "
                                        + result.id()
                        )
                );
            }

            source.sendSuccess(
                    () ->
                            Component.literal(
                                    "  "
                                            + result.detail()
                            ).withStyle(
                                    ChatFormatting.DARK_GRAY
                            ),
                    false
            );
        }

        int finalPassed =
                passed;

        int finalFailed =
                failed;

        source.sendSuccess(
                () ->
                        Component.literal(
                                "W1.21.3 unit result: "
                                        + finalPassed
                                        + " passed, "
                                        + finalFailed
                                        + " failed"
                        ).withStyle(
                                finalFailed == 0
                                        ? ChatFormatting.GREEN
                                        : ChatFormatting.RED
                        ),
                false
        );

        return failed == 0
                ? 1
                : 0;
    }

    private static int startLive(
            CommandSourceStack source,
            String rawCoords,
            boolean runUnitFirst
    ) {
        if (runUnitFirst
                && !allUnitPassed(
                source
        )) {
            return 0;
        }

        BlockPos[] positions;

        try {
            positions =
                    parsePositions(
                            rawCoords,
                            4
                    );
        } catch (IllegalArgumentException exception) {
            source.sendFailure(
                    Component.literal(
                            exception.getMessage()
                    )
            );

            usage(
                    source
            );

            return 0;
        }

        boolean started =
                W121IntegrationTestManager.start(
                        source.getLevel(),
                        positions[0],
                        positions[1],
                        positions[2],
                        positions[3]
                );

        if (!started) {
            source.sendFailure(
                    Component.literal(
                            "A W1.21.3 live test is already running. Use /wifiw121test status or reset."
                    )
            );

            return 0;
        }

        source.sendSuccess(
                () ->
                        Component.literal(
                                "W1.21.3 live closure test started."
                        ).withStyle(
                                ChatFormatting.GREEN
                        ),
                false
        );

        source.sendSuccess(
                () ->
                        Component.literal(
                                "STA "
                                        + positions[0].toShortString()
                                        + " | AP "
                                        + positions[1].toShortString()
                                        + " | Switch "
                                        + positions[2].toShortString()
                                        + " | Server "
                                        + positions[3].toShortString()
                        ).withStyle(
                                ChatFormatting.GRAY
                        ),
                false
        );

        source.sendSuccess(
                () ->
                        Component.literal(
                                "Use /wifiw121test status to inspect progress."
                        ).withStyle(
                                ChatFormatting.DARK_GRAY
                        ),
                false
        );

        return 1;
    }

    private static boolean allUnitPassed(
            CommandSourceStack source
    ) {
        boolean allPassed =
                true;

        for (W121IntegrationTestResult result
                : W121IntegrationTestSuite.runAll()) {
            if (!result.passed()) {
                allPassed =
                        false;

                source.sendFailure(
                        Component.literal(
                                "[FAIL] "
                                        + result.id()
                                        + " | "
                                        + result.detail()
                        )
                );
            }
        }

        if (allPassed) {
            source.sendSuccess(
                    () ->
                            Component.literal(
                                    "W1.21.3 unit gate passed; starting live test."
                            ).withStyle(
                                    ChatFormatting.GREEN
                            ),
                    false
            );
        }

        return allPassed;
    }

    private static int status(
            CommandSourceStack source
    ) {
        W121IntegrationSnapshot snapshot =
                W121IntegrationTestManager.snapshot();

        if (snapshot == null) {
            source.sendFailure(
                    Component.literal(
                            "No W1.21.3 live test exists."
                    )
            );

            return 0;
        }

        ChatFormatting resultColor =
                snapshot.finished()
                        ? snapshot.passed()
                        ? ChatFormatting.GREEN
                        : ChatFormatting.RED
                        : ChatFormatting.YELLOW;

        source.sendSuccess(
                () ->
                        Component.literal(
                                "[W1.21.3] "
                                        + snapshot.stage()
                                        + " | "
                                        + (
                                        snapshot.finished()
                                                ? snapshot.passed()
                                                ? "PASS"
                                                : "FAIL"
                                                : "RUNNING"
                                )
                        ).withStyle(
                                resultColor
                        ),
                false
        );

        line(
                source,
                "Detail: "
                        + snapshot.detail()
        );

        line(
                source,
                "Failure: "
                        + snapshot.failure()
        );

        line(
                source,
                "Elapsed: "
                        + snapshot.elapsedTicks()
                        + " ticks"
        );

        line(
                source,
                "STA: state="
                        + snapshot.stationState()
                        + " security="
                        + snapshot.securityState()
                        + " ip="
                        + snapshot.stationIp()
                        + " bssid="
                        + snapshot.selectedBssid()
        );

        line(
                source,
                "RAW: "
                        + snapshot.workflowState()
                        + " | "
                        + snapshot.workflowDetail()
        );

        line(
                source,
                "Trace: sta="
                        + snapshot.stationTraceEvents()
                        + " ap="
                        + snapshot.apTraceEvents()
                        + " retries="
                        + snapshot.retries()
                        + " ackRx="
                        + snapshot.ackRx()
        );

        line(
                source,
                "DS: tx="
                        + snapshot.dsTx()
                        + " rx="
                        + snapshot.dsRx()
        );

        line(
                source,
                "Bridge: "
                        + snapshot.apBridgeStatus()
        );

        return snapshot.finished()
                && !snapshot.passed()
                ? 0
                : 1;
    }

    private static int reset(
            CommandSourceStack source
    ) {
        W121IntegrationTestManager.reset();

        source.sendSuccess(
                () ->
                        Component.literal(
                                "W1.21.3 integration runner reset."
                        ),
                false
        );

        return 1;
    }

    private static int persistMark(
            CommandSourceStack source,
            String rawCoords
    ) {
        BlockPos[] positions;

        try {
            positions =
                    parsePositions(
                            rawCoords,
                            4
                    );
        } catch (IllegalArgumentException exception) {
            source.sendFailure(
                    Component.literal(
                            exception.getMessage()
                    )
            );

            return 0;
        }

        ServerLevel level =
                source.getLevel();

        BlockEntity stationEntity =
                level.getBlockEntity(
                        positions[0]
                );

        BlockEntity apEntity =
                level.getBlockEntity(
                        positions[1]
                );

        BlockEntity switchEntity =
                level.getBlockEntity(
                        positions[2]
                );

        BlockEntity serverEntity =
                level.getBlockEntity(
                        positions[3]
                );

        if (!(stationEntity
                instanceof NetworkDeviceBlockEntity station)
                || !(apEntity
                instanceof NetworkDeviceBlockEntity accessPoint)
                || !(switchEntity
                instanceof NetworkSwitchBlockEntity networkSwitch)
                || !(serverEntity
                instanceof ServerRackBlockEntity server)) {
            source.sendFailure(
                    Component.literal(
                            "Persistence mark rejected: supplied topology block types do not match STA/AP/Switch/Server."
                    )
            );

            return 0;
        }

        CompoundTag probe =
                new CompoundTag();

        probe.putLong(
                "ApPos",
                positions[1].asLong()
        );

        probe.putLong(
                "SwitchPos",
                positions[2].asLong()
        );

        probe.putLong(
                "ServerPos",
                positions[3].asLong()
        );

        probe.putString(
                "ApMac",
                accessPoint.wifiMacAddress()
        );

        probe.putString(
                "ServerIp",
                server.ipAddress()
        );

        probe.putBoolean(
                "SwitchHasAp",
                switchConnectedToAp(
                        networkSwitch,
                        positions[1],
                        accessPoint,
                        positions[2]
                )
        );

        probe.putBoolean(
                "SwitchHasServer",
                networkSwitch
                        .getConnectedDevices()
                        .contains(
                                positions[3]
                        )
        );

        probe.putBoolean(
                "DhcpEnabled",
                server.dhcpEnabled()
        );

        probe.putBoolean(
                "DnsEnabled",
                server.dnsEnabled()
        );

        probe.putBoolean(
                "HttpEnabled",
                server.httpEnabled()
        );

        station.getPersistentData()
                .put(
                        PERSIST_ROOT,
                        probe
                );

        station.setChanged();

        source.sendSuccess(
                () ->
                        Component.literal(
                                "W1.21 persistence baseline stored on station "
                                        + positions[0].toShortString()
                                        + ". Save/quit/reload, then run persistverify."
                        ).withStyle(
                                ChatFormatting.GREEN
                        ),
                false
        );

        return 1;
    }

    private static int persistVerify(
            CommandSourceStack source,
            String rawStation
    ) {
        BlockPos[] positions;

        try {
            positions =
                    parsePositions(
                            rawStation,
                            1
                    );
        } catch (IllegalArgumentException exception) {
            source.sendFailure(
                    Component.literal(
                            exception.getMessage()
                    )
            );

            return 0;
        }

        ServerLevel level =
                source.getLevel();

        BlockEntity stationEntity =
                level.getBlockEntity(
                        positions[0]
                );

        if (!(stationEntity
                instanceof NetworkDeviceBlockEntity station)) {
            source.sendFailure(
                    Component.literal(
                            "Persistence verify rejected: station block is missing."
                    )
            );

            return 0;
        }

        if (!station.getPersistentData()
                .contains(
                        PERSIST_ROOT
                )) {
            source.sendFailure(
                    Component.literal(
                            "No W1.21 persistence baseline exists on this station."
                    )
            );

            return 0;
        }

        CompoundTag probe =
                station.getPersistentData()
                        .getCompound(
                                PERSIST_ROOT
                        );

        BlockPos apPos =
                BlockPos.of(
                        probe.getLong(
                                "ApPos"
                        )
                );

        BlockPos switchPos =
                BlockPos.of(
                        probe.getLong(
                                "SwitchPos"
                        )
                );

        BlockPos serverPos =
                BlockPos.of(
                        probe.getLong(
                                "ServerPos"
                        )
                );

        BlockEntity apEntity =
                level.getBlockEntity(
                        apPos
                );

        BlockEntity switchEntity =
                level.getBlockEntity(
                        switchPos
                );

        BlockEntity serverEntity =
                level.getBlockEntity(
                        serverPos
                );

        if (!(apEntity
                instanceof NetworkDeviceBlockEntity accessPoint)
                || !(switchEntity
                instanceof NetworkSwitchBlockEntity networkSwitch)
                || !(serverEntity
                instanceof ServerRackBlockEntity server)) {
            source.sendFailure(
                    Component.literal(
                            "Persistence verify failed: one or more topology entities did not reload."
                    )
            );

            return 0;
        }

        boolean apMac =
                probe.getString(
                        "ApMac"
                ).equalsIgnoreCase(
                        accessPoint.wifiMacAddress()
                );

        boolean serverIp =
                probe.getString(
                        "ServerIp"
                ).equals(
                        server.ipAddress()
                );

        boolean switchAp =
                switchConnectedToAp(
                        networkSwitch,
                        apPos,
                        accessPoint,
                        switchPos
                );

        boolean switchServer =
                networkSwitch
                        .getConnectedDevices()
                        .contains(
                                serverPos
                        );

        boolean services =
                server.dhcpEnabled()
                        == probe.getBoolean(
                        "DhcpEnabled"
                )
                        && server.dnsEnabled()
                        == probe.getBoolean(
                        "DnsEnabled"
                )
                        && server.httpEnabled()
                        == probe.getBoolean(
                        "HttpEnabled"
                );

        boolean passed =
                apMac
                        && serverIp
                        && switchAp
                        && switchServer
                        && services;

        if (!passed) {
            source.sendFailure(
                    Component.literal(
                            "W1.21 persistence verify FAIL | apMac="
                                    + apMac
                                    + " serverIp="
                                    + serverIp
                                    + " switchAp="
                                    + switchAp
                                    + " switchServer="
                                    + switchServer
                                    + " services="
                                    + services
                    )
            );

            return 0;
        }

        source.sendSuccess(
                () ->
                        Component.literal(
                                "W1.21 persistence verify PASS. Re-run /wifiw121test full with the same four positions to prove reconnect + HTTP 200 after reload."
                        ).withStyle(
                                ChatFormatting.GREEN
                        ),
                false
        );

        return 1;
    }

    private static boolean switchConnectedToAp(
            NetworkSwitchBlockEntity networkSwitch,
            BlockPos apPos,
            NetworkDeviceBlockEntity accessPoint,
            BlockPos switchPos
    ) {
        if (networkSwitch
                .getConnectedDevices()
                .contains(
                        apPos
                )) {
            return true;
        }

        if (accessPoint
                instanceof RtAc68uRouterBlockEntity router) {
            return !router
                    .w121InterfaceForPeer(
                            switchPos
                    )
                    .isBlank();
        }

        return false;
    }

    private static BlockPos[] parsePositions(
            String raw,
            int count
    ) {
        String value =
                raw == null
                        ? ""
                        : raw.trim();

        String[] parts =
                value.isBlank()
                        ? new String[0]
                        : value.split(
                        "\\s+"
                );

        int expected =
                count * 3;

        if (parts.length != expected) {
            throw new IllegalArgumentException(
                    "Expected "
                            + expected
                            + " integer coordinate values but received "
                            + parts.length
            );
        }

        BlockPos[] result =
                new BlockPos[
                        count
                        ];

        for (int i = 0;
             i < count;
             i++) {
            int x;
            int y;
            int z;

            try {
                x =
                        Integer.parseInt(
                                parts[
                                        i * 3
                                        ]
                        );

                y =
                        Integer.parseInt(
                                parts[
                                        i * 3 + 1
                                        ]
                        );

                z =
                        Integer.parseInt(
                                parts[
                                        i * 3 + 2
                                        ]
                        );
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException(
                        "All coordinates must be integers"
                );
            }

            result[i] =
                    new BlockPos(
                            x,
                            y,
                            z
                    );
        }

        return result;
    }

    private static void usage(
            CommandSourceStack source
    ) {
        source.sendSuccess(
                () ->
                        Component.literal(
                                "Usage: /wifiw121test full <STA x y z> <AP x y z> <Switch x y z> <Server x y z>"
                        ).withStyle(
                                ChatFormatting.GRAY
                        ),
                false
        );
    }

    private static void line(
            CommandSourceStack source,
            String value
    ) {
        source.sendSuccess(
                () ->
                        Component.literal(
                                value
                        ).withStyle(
                                ChatFormatting.GRAY
                        ),
                false
        );
    }
}

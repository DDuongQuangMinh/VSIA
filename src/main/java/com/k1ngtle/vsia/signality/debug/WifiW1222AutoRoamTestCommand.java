package com.k1ngtle.vsia.signality.debug;

import com.k1ngtle.vsia.Vsia;
import com.k1ngtle.vsia.signality.engineering.wifi.integration.w1222.W1222AutoRoamSnapshot;
import com.k1ngtle.vsia.signality.engineering.wifi.integration.w1222.W1222AutoRoamTestManager;
import com.k1ngtle.vsia.signality.engineering.wifi.integration.w1222.W1222RoamingUnitTestSuite;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod.EventBusSubscriber(
        modid = Vsia.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class WifiW1222AutoRoamTestCommand {
    private WifiW1222AutoRoamTestCommand() {
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
                                "wifiw1222test"
                        )
                        .requires(
                                source ->
                                        source.hasPermission(
                                                2
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
                                                                        start(
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
                                                "unit"
                                        )
                                        .executes(
                                                context ->
                                                        unit(
                                                                context.getSource()
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
                                                "usage"
                                        )
                                        .executes(
                                                context -> {
                                                    usage(
                                                            context.getSource()
                                                    );
                                                    return 1;
                                                }
                                        )
                        )
        );
    }

    private static int start(
            CommandSourceStack source,
            String raw
    ) {
        BlockPos[] positions;

        try {
            positions =
                    parsePositions(
                            raw,
                            5
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

        ServerLevel level =
                source.getLevel();

        boolean started =
                W1222AutoRoamTestManager.start(
                        level,
                        positions[0],
                        positions[1],
                        positions[2],
                        positions[3],
                        positions[4]
                );

        if (!started) {
            source.sendFailure(
                    Component.literal(
                            "A W1.22.2 automatic roaming test is already active. Run /wifiw1222test status or reset first."
                    )
            );

            return 0;
        }

        source.sendSuccess(
                () ->
                        Component.literal(
                                "W1.22.2 automatic roaming closure test started"
                        ).withStyle(
                                ChatFormatting.GREEN
                        ),
                false
        );

        line(
                source,
                "STA "
                        + positions[0].toShortString()
                        + " | AP1 "
                        + positions[1].toShortString()
                        + " | AP2 "
                        + positions[2].toShortString()
                        + " | Switch "
                        + positions[3].toShortString()
                        + " | Server "
                        + positions[4].toShortString()
        );

        line(
                source,
                "The test forces AP1 only for the baseline. After background roaming is enabled, it never calls scanWifi() or roamWifiToBestCandidate() manually."
        );

        line(
                source,
                "Use /wifiw1222test status to inspect progress."
        );

        return 1;
    }

    private static int unit(
            CommandSourceStack source
    ) {
        List<W1222RoamingUnitTestSuite.Result> results =
                W1222RoamingUnitTestSuite.runAll();

        int passed =
                0;

        for (W1222RoamingUnitTestSuite.Result result : results) {
            if (result.passed()) {
                passed++;
            }

            ChatFormatting color =
                    result.passed()
                            ? ChatFormatting.GREEN
                            : ChatFormatting.RED;

            source.sendSuccess(
                    () ->
                            Component.literal(
                                    "["
                                            + (
                                            result.passed()
                                                    ? "PASS"
                                                    : "FAIL"
                                    )
                                            + "] "
                                            + result.name()
                            ).withStyle(
                                    color
                            ),
                    false
            );

            line(
                    source,
                    "  "
                            + result.detail()
            );
        }

        int failed =
                results.size()
                        - passed;

        ChatFormatting color =
                failed == 0
                        ? ChatFormatting.GREEN
                        : ChatFormatting.RED;

        final int finalPassed =
                passed;

        final int finalFailed =
                failed;

        source.sendSuccess(
                () ->
                        Component.literal(
                                "W1.22.2 unit result: "
                                        + finalPassed
                                        + " passed, "
                                        + finalFailed
                                        + " failed"
                        ).withStyle(
                                color
                        ),
                false
        );

        return failed == 0
                ? 1
                : 0;
    }

    private static int status(
            CommandSourceStack source
    ) {
        W1222AutoRoamSnapshot snapshot =
                W1222AutoRoamTestManager.snapshot();

        if (snapshot == null) {
            source.sendFailure(
                    Component.literal(
                            "No W1.22.2 automatic roaming test has been started."
                    )
            );

            usage(
                    source
            );

            return 0;
        }

        ChatFormatting resultColor =
                snapshot.passed()
                        ? ChatFormatting.GREEN
                        : snapshot.finished()
                        ? ChatFormatting.RED
                        : ChatFormatting.YELLOW;

        source.sendSuccess(
                () ->
                        Component.literal(
                                "[W1.22.2] "
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
                        + " | elapsed="
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
                        + " ssid="
                        + snapshot.selectedSsid()
                        + " bssid="
                        + snapshot.selectedBssid()
        );

        line(
                source,
                "AP1="
                        + snapshot.ap1Bssid()
                        + " snr="
                        + db(
                        snapshot.ap1SnrDb()
                )
                        + " dB stations="
                        + snapshot.ap1Stations()
                        + " | AP2="
                        + snapshot.ap2Bssid()
                        + " snr="
                        + db(
                        snapshot.ap2SnrDb()
                )
                        + " dB stations="
                        + snapshot.ap2Stations()
        );

        line(
                source,
                "Candidate="
                        + snapshot.candidateBssid()
                        + " snr="
                        + db(
                        snapshot.candidateSnrDb()
                )
                        + " dB | backgroundRoam="
                        + snapshot.backgroundRoamingEnabled()
                        + " roamScanSeen="
                        + snapshot.associatedRoamScanObserved()
                        + " pendingData="
                        + snapshot.pendingWifiData()
        );

        line(
                source,
                "Wi-Fi diagnostic: "
                        + snapshot.wifiDiagnostic()
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
                        + " ap1="
                        + snapshot.ap1TraceEvents()
                        + " ap2="
                        + snapshot.ap2TraceEvents()
                        + " retries="
                        + snapshot.retries()
                        + " ackRx="
                        + snapshot.ackRx()
        );

        line(
                source,
                "DS: AP1 tx="
                        + snapshot.ap1DsTx()
                        + " rx="
                        + snapshot.ap1DsRx()
                        + " | AP2 tx="
                        + snapshot.ap2DsTx()
                        + " rx="
                        + snapshot.ap2DsRx()
        );

        line(
                source,
                "AP1 Bridge: "
                        + snapshot.ap1BridgeStatus()
        );

        line(
                source,
                "AP2 Bridge: "
                        + snapshot.ap2BridgeStatus()
        );

        return snapshot.finished()
                ? snapshot.passed()
                ? 1
                : 0
                : 1;
    }

    private static int reset(
            CommandSourceStack source
    ) {
        W1222AutoRoamTestManager.reset();

        source.sendSuccess(
                () ->
                        Component.literal(
                                "W1.22.2 automatic roaming test state reset."
                        ).withStyle(
                                ChatFormatting.GREEN
                        ),
                false
        );

        return 1;
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

        if (parts.length
                != expected) {
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
            try {
                result[i] =
                        new BlockPos(
                                Integer.parseInt(
                                        parts[
                                                i * 3
                                                ]
                                ),
                                Integer.parseInt(
                                        parts[
                                                i * 3 + 1
                                                ]
                                ),
                                Integer.parseInt(
                                        parts[
                                                i * 3 + 2
                                                ]
                                )
                        );
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException(
                        "All coordinates must be integers"
                );
            }
        }

        return result;
    }

    private static String db(
            double value
    ) {
        return Double.isFinite(
                value
        )
                ? String.format(
                java.util.Locale.ROOT,
                "%.1f",
                value
        )
                : "n/a";
    }

    private static void usage(
            CommandSourceStack source
    ) {
        line(
                source,
                "Usage: /wifiw1222test full <STA x y z> <AP1 x y z> <AP2 x y z> <Switch x y z> <Server x y z>"
        );

        line(
                source,
                "AP1 and AP2 must use the same switch. Place the STA so AP2 is at least +6 dB stronger than AP1."
        );

        line(
                source,
                "Also available: /wifiw1222test unit | status | reset"
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

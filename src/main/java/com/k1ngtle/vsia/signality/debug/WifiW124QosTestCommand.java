package com.k1ngtle.vsia.signality.debug;

import com.k1ngtle.vsia.Vsia;
import com.k1ngtle.vsia.signality.engineering.wifi.integration.w124.W124QosClassResult;
import com.k1ngtle.vsia.signality.engineering.wifi.integration.w124.W124QosSnapshot;
import com.k1ngtle.vsia.signality.engineering.wifi.integration.w124.W124QosTestManager;
import com.k1ngtle.vsia.signality.engineering.wifi.integration.w124.W124QosUnitTestSuite;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod.EventBusSubscriber(
        modid = Vsia.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class WifiW124QosTestCommand {
    private WifiW124QosTestCommand() {
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
                                "wifiw124test"
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
                                                        unit(
                                                                context.getSource()
                                                        )
                                        )
                        )
                        .then(
                                Commands.literal(
                                                "live"
                                        )
                                        .then(
                                                Commands.argument(
                                                                "coords",
                                                                StringArgumentType.greedyString()
                                                        )
                                                        .executes(
                                                                context ->
                                                                        live(
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

    private static int unit(
            CommandSourceStack source
    ) {
        List<W124QosUnitTestSuite.Result> results =
                W124QosUnitTestSuite.runAll();

        int passed =
                0;

        for (W124QosUnitTestSuite.Result result
                : results) {
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

        final int finalPassed =
                passed;

        final int finalFailed =
                failed;

        source.sendSuccess(
                () ->
                        Component.literal(
                                "W1.24 unit result: "
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

    private static int live(
            CommandSourceStack source,
            String raw
    ) {
        BlockPos[] positions;

        try {
            positions =
                    parsePositions(
                            raw,
                            2
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
                W124QosTestManager.start(
                        source.getLevel(),
                        positions[0],
                        positions[1]
                );

        if (!started) {
            source.sendFailure(
                    Component.literal(
                            "A W1.24 live QoS test is already active. Run status or reset first."
                    )
            );

            return 0;
        }

        source.sendSuccess(
                () ->
                        Component.literal(
                                "W1.24 QoS / traffic-class live test started"
                        ).withStyle(
                                ChatFormatting.GREEN
                        ),
                false
        );

        line(
                source,
                "STA "
                        + positions[0].toShortString()
                        + " | AP "
                        + positions[1].toShortString()
        );

        line(
                source,
                "The test proves VO, VI, BE and BK reach the real MAC scheduler, complete DATA/ACK delivery, then survive a mixed load."
        );

        return 1;
    }

    private static int status(
            CommandSourceStack source
    ) {
        W124QosSnapshot snapshot =
                W124QosTestManager.snapshot();

        if (snapshot == null) {
            source.sendFailure(
                    Component.literal(
                            "No W1.24 live QoS test has been started."
                    )
            );

            usage(
                    source
            );

            return 0;
        }

        ChatFormatting color =
                snapshot.passed()
                        ? ChatFormatting.GREEN
                        : snapshot.finished()
                        ? ChatFormatting.RED
                        : ChatFormatting.YELLOW;

        source.sendSuccess(
                () ->
                        Component.literal(
                                "[W1.24] "
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
                                color
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
                        + " bssid="
                        + snapshot.selectedBssid()
                        + " pending="
                        + snapshot.pendingData()
        );

        for (W124QosClassResult result
                : snapshot.classResults()) {
            line(
                    source,
                    result.category()
                            + ": accepted="
                            + result.accepted()
                            + " ok="
                            + result.successes()
                            + " retry="
                            + result.retries()
                            + " drop="
                            + result.drops()
                            + " defer="
                            + result.deferrals()
                            + " peak="
                            + result.queuePeak()
                            + " macAC="
                            + result.macCategoryObserved()
            );
        }

        line(
                source,
                "Mixed: accepted="
                        + snapshot.mixedAccepted()
                        + " ok="
                        + snapshot.mixedSuccesses()
                        + " retry="
                        + snapshot.mixedRetries()
                        + " drop="
                        + snapshot.mixedDrops()
                        + " defer="
                        + snapshot.mixedDeferrals()
                        + " peak="
                        + snapshot.mixedQueuePeak()
        );

        line(
                source,
                "CW: VO="
                        + snapshot.cwVoice()
                        + " VI="
                        + snapshot.cwVideo()
                        + " BE="
                        + snapshot.cwBestEffort()
                        + " BK="
                        + snapshot.cwBackground()
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
        W124QosTestManager.reset();

        source.sendSuccess(
                () ->
                        Component.literal(
                                "W1.24 QoS test state reset."
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

        for (int index = 0;
             index < count;
             index++) {
            try {
                result[index] =
                        new BlockPos(
                                Integer.parseInt(
                                        parts[
                                                index * 3
                                                ]
                                ),
                                Integer.parseInt(
                                        parts[
                                                index * 3 + 1
                                                ]
                                ),
                                Integer.parseInt(
                                        parts[
                                                index * 3 + 2
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

    private static void usage(
            CommandSourceStack source
    ) {
        line(
                source,
                "/wifiw124test unit"
        );

        line(
                source,
                "/wifiw124test live <STA x y z> <AP x y z>"
        );

        line(
                source,
                "/wifiw124test status | reset"
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

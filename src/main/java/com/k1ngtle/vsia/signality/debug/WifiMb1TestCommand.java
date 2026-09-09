package com.k1ngtle.vsia.signality.debug;

import com.k1ngtle.vsia.Vsia;
import com.k1ngtle.vsia.signality.engineering.wifi.integration.mb1.Mb1LiveSnapshot;
import com.k1ngtle.vsia.signality.engineering.wifi.integration.mb1.Mb1LiveTestManager;
import com.k1ngtle.vsia.signality.engineering.wifi.integration.mb1.Mb1UnitTestSuite;
import com.k1ngtle.vsia.signality.engineering.wifi.smartconnect.WifiBand;
import com.k1ngtle.vsia.signality.engineering.wifi.smartconnect.WifiBandUtil;
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
public final class WifiMb1TestCommand {
    private WifiMb1TestCommand() {
    }

    @SubscribeEvent
    public static void registerCommands(
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
                                "wifimb1test"
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
                                                "live5"
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
                                                                                ),
                                                                                WifiBand.FIVE_GHZ
                                                                        )
                                                        )
                                        )
                        )
                        .then(
                                Commands.literal(
                                                "live24"
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
                                                                                ),
                                                                                WifiBand.TWO_FOUR_GHZ
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
        );
    }

    private static int unit(
            CommandSourceStack source
    ) {
        List<Mb1UnitTestSuite.Result> results =
                Mb1UnitTestSuite.runAll();

        int passed =
                0;

        for (Mb1UnitTestSuite.Result result
                : results) {
            if (result.passed()) {
                passed++;
            }

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
                                    result.passed()
                                            ? ChatFormatting.GREEN
                                            : ChatFormatting.RED
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

        int finalPassed =
                passed;

        int finalFailed =
                failed;

        source.sendSuccess(
                () ->
                        Component.literal(
                                "MB1 unit result: "
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
            String raw,
            WifiBand expectedBand
    ) {
        BlockPos[] positions;

        try {
            positions =
                    parse(
                            raw,
                            3
                    );
        } catch (IllegalArgumentException exception) {
            source.sendFailure(
                    Component.literal(
                            exception.getMessage()
                    )
            );
            return 0;
        }

        if (!Mb1LiveTestManager.start(
                source.getLevel(),
                positions[0],
                positions[1],
                positions[2],
                expectedBand
        )) {
            source.sendFailure(
                    Component.literal(
                            "An MB1 live test is already active. Reset first."
                    )
            );
            return 0;
        }

        source.sendSuccess(
                () ->
                        Component.literal(
                                "MB1 Dual-band / Smart Connect test started"
                        ).withStyle(
                                ChatFormatting.GREEN
                        ),
                false
        );

        line(
                source,
                "STA="
                        + positions[0].toShortString()
                        + " AP24="
                        + positions[1].toShortString()
                        + " AP5="
                        + positions[2].toShortString()
                        + " expected="
                        + WifiBandUtil.displayName(
                        expectedBand
                )
        );

        return 1;
    }

    private static int status(
            CommandSourceStack source
    ) {
        Mb1LiveSnapshot snapshot =
                Mb1LiveTestManager.snapshot();

        if (snapshot == null) {
            source.sendFailure(
                    Component.literal(
                            "No MB1 test active."
                    )
            );
            return 0;
        }

        source.sendSuccess(
                () ->
                        Component.literal(
                                "[MB1] "
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
                                snapshot.passed()
                                        ? ChatFormatting.GREEN
                                        : snapshot.finished()
                                        ? ChatFormatting.RED
                                        : ChatFormatting.YELLOW
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
                "STA: state="
                        + snapshot.stationState()
                        + " security="
                        + snapshot.securityState()
                        + " bssid="
                        + snapshot.selectedBssid()
                        + " freq="
                        + String.format(
                        java.util.Locale.ROOT,
                        "%.3f GHz",
                        snapshot.activeFrequencyHz()
                                / 1_000_000_000.0D
                )
                        + " band="
                        + WifiBandUtil.displayName(
                        snapshot.selectedBand()
                )
        );

        line(
                source,
                "SNR: 2.4="
                        + db(
                        snapshot.twoFourSnrDb()
                )
                        + " dB 5="
                        + db(
                        snapshot.fiveSnrDb()
                )
                        + " dB | decision="
                        + WifiBandUtil.displayName(
                        snapshot.decisionBand()
                )
                        + " "
                        + snapshot.decisionBssid()
                        + " score="
                        + db(
                        snapshot.decisionScore()
                )
        );

        line(
                source,
                "ACK RX="
                        + snapshot.ackRx()
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
        Mb1LiveTestManager.reset();

        source.sendSuccess(
                () ->
                        Component.literal(
                                "MB1 test state reset."
                        ).withStyle(
                                ChatFormatting.GREEN
                        ),
                false
        );

        return 1;
    }

    private static BlockPos[] parse(
            String raw,
            int count
    ) {
        String[] values =
                raw == null
                        ? new String[0]
                        : raw.trim()
                        .split(
                                "\\s+"
                        );

        if (values.length
                != count * 3) {
            throw new IllegalArgumentException(
                    "Expected "
                            + count * 3
                            + " integer coordinates"
            );
        }

        BlockPos[] result =
                new BlockPos[
                        count
                        ];

        for (int i = 0;
             i < count;
             i++) {
            result[i] =
                    new BlockPos(
                            Integer.parseInt(
                                    values[i * 3]
                            ),
                            Integer.parseInt(
                                    values[i * 3 + 1]
                            ),
                            Integer.parseInt(
                                    values[i * 3 + 2]
                            )
                    );
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

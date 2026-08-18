package com.k1ngtle.vsia.signality.debug;

import com.k1ngtle.vsia.Vsia;
import com.k1ngtle.vsia.signality.engineering.wifi.baseband.WifiBasebandTestResult;
import com.k1ngtle.vsia.signality.engineering.wifi.baseband.WifiBasebandTestSuite;
import com.k1ngtle.vsia.signality.engineering.wifi.baseband.WifiWaveformTestResult;
import com.k1ngtle.vsia.signality.engineering.wifi.baseband.WifiWaveformTestSuite;
import com.k1ngtle.vsia.signality.engineering.wifi.ldpc.WifiLdpcTestResult;
import com.k1ngtle.vsia.signality.engineering.wifi.ldpc.WifiLdpcTestSuite;
import com.k1ngtle.vsia.signality.engineering.wifi.ldpc.WifiLdpcStandardTestResult;
import com.k1ngtle.vsia.signality.engineering.wifi.ldpc.WifiLdpcStandardTestSuite;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = Vsia.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class WifiW1Command {
    private WifiW1Command() {
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
                                "wifiw1test"
                        )
                        .requires(
                                source ->
                                        source.hasPermission(
                                                2
                                        )
                        )
                        .executes(
                                context ->
                                        runAll(
                                                context.getSource()
                                        )
                        )
                        .then(
                                Commands.literal(
                                                "all"
                                        )
                                        .executes(
                                                context ->
                                                        runAll(
                                                                context.getSource()
                                                        )
                                        )
                        )
                        .then(
                                Commands.literal(
                                                "baseband"
                                        )
                                        .executes(
                                                context ->
                                                        runBaseband(
                                                                context.getSource()
                                                        )
                                        )
                        )
                        .then(
                                Commands.literal(
                                                "waveform"
                                        )
                                        .executes(
                                                context ->
                                                        runWaveform(
                                                                context.getSource()
                                                        )
                                        )
                        )
                        .then(
                                Commands.literal(
                                                "ldpc"
                                        )
                                        .executes(
                                                context ->
                                                        runLdpc(
                                                                context.getSource()
                                                        )
                                        )
                        )
                        .then(
                                Commands.literal(
                                                "ldpc-standard"
                                        )
                                        .executes(
                                                context ->
                                                        runLdpcStandard(
                                                                context.getSource()
                                                        )
                                        )
                        )
        );
    }

    private static int runAll(
            CommandSourceStack source
    ) {
        Counts counts =
                new Counts();

        header(
                source,
                "Wi-Fi W1.1 Baseband"
        );

        runBasebandTests(
                source,
                counts
        );

        header(
                source,
                "Wi-Fi W1.2 Waveform / Synchronization"
        );

        runWaveformTests(
                source,
                counts
        );

        header(
                source,
                "Wi-Fi W1.3 QC-LDPC"
        );

        runLdpcTests(
                source,
                counts
        );

        header(
                source,
                "Wi-Fi W1.3b Standard LDPC Profile"
        );

        runLdpcStandardTests(
                source,
                counts
        );

        summary(
                source,
                counts
        );

        return counts.failed == 0
                ? 1
                : 0;
    }

    private static int runBaseband(
            CommandSourceStack source
    ) {
        Counts counts =
                new Counts();

        header(
                source,
                "Wi-Fi W1.1 Baseband"
        );

        runBasebandTests(
                source,
                counts
        );

        summary(
                source,
                counts
        );

        return counts.failed == 0
                ? 1
                : 0;
    }

    private static int runWaveform(
            CommandSourceStack source
    ) {
        Counts counts =
                new Counts();

        header(
                source,
                "Wi-Fi W1.2 Waveform / Synchronization"
        );

        runWaveformTests(
                source,
                counts
        );

        summary(
                source,
                counts
        );

        return counts.failed == 0
                ? 1
                : 0;
    }

    private static int runLdpc(
            CommandSourceStack source
    ) {
        Counts counts =
                new Counts();

        header(
                source,
                "Wi-Fi W1.3 QC-LDPC"
        );

        runLdpcTests(
                source,
                counts
        );

        summary(
                source,
                counts
        );

        return counts.failed == 0
                ? 1
                : 0;
    }

    private static int runLdpcStandard(
            CommandSourceStack source
    ) {
        Counts counts =
                new Counts();

        header(
                source,
                "Wi-Fi W1.3b Standard LDPC Profile"
        );

        runLdpcStandardTests(
                source,
                counts
        );

        summary(
                source,
                counts
        );

        return counts.failed == 0
                ? 1
                : 0;
    }

    private static void runLdpcStandardTests(
            CommandSourceStack source,
            Counts counts
    ) {
        for (WifiLdpcStandardTestResult result
                : WifiLdpcStandardTestSuite.runAll()) {
            emit(
                    source,
                    result.id(),
                    result.passed(),
                    result.detail(),
                    counts
            );
        }
    }

    private static void runLdpcTests(
            CommandSourceStack source,
            Counts counts
    ) {
        for (WifiLdpcTestResult result
                : WifiLdpcTestSuite.runAll()) {
            emit(
                    source,
                    result.id(),
                    result.passed(),
                    result.detail(),
                    counts
            );
        }
    }

    private static void runBasebandTests(
            CommandSourceStack source,
            Counts counts
    ) {
        for (WifiBasebandTestResult result
                : WifiBasebandTestSuite.runAll()) {
            emit(
                    source,
                    result.id(),
                    result.passed(),
                    result.detail(),
                    counts
            );
        }
    }

    private static void runWaveformTests(
            CommandSourceStack source,
            Counts counts
    ) {
        for (WifiWaveformTestResult result
                : WifiWaveformTestSuite.runAll()) {
            emit(
                    source,
                    result.id(),
                    result.passed(),
                    result.detail(),
                    counts
            );
        }
    }

    private static void emit(
            CommandSourceStack source,
            String id,
            boolean passed,
            String detail,
            Counts counts
    ) {
        if (passed) {
            counts.passed++;

            source.sendSuccess(
                    () ->
                            Component.literal(
                                    "[PASS] "
                                            + id
                            ).withStyle(
                                    ChatFormatting.GREEN
                            ),
                    false
            );
        } else {
            counts.failed++;

            source.sendFailure(
                    Component.literal(
                            "[FAIL] "
                                    + id
                    ).withStyle(
                            ChatFormatting.RED
                    )
            );
        }

        source.sendSuccess(
                () ->
                        Component.literal(
                                "  "
                                        + detail
                        ).withStyle(
                                ChatFormatting.DARK_GRAY
                        ),
                false
        );
    }

    private static void header(
            CommandSourceStack source,
            String title
    ) {
        source.sendSuccess(
                () ->
                        Component.literal(
                                title
                        ).withStyle(
                                ChatFormatting.AQUA
                        ),
                false
        );
    }

    private static void summary(
            CommandSourceStack source,
            Counts counts
    ) {
        source.sendSuccess(
                () ->
                        Component.literal(
                                "Result: "
                                        + counts.passed
                                        + " passed, "
                                        + counts.failed
                                        + " failed"
                        ).withStyle(
                                counts.failed == 0
                                        ? ChatFormatting.GREEN
                                        : ChatFormatting.RED
                        ),
                false
        );
    }

    private static final class Counts {
        private int passed;
        private int failed;
    }
}

package com.k1ngtle.vsia.signality.debug;

import com.k1ngtle.vsia.Vsia;
import com.k1ngtle.vsia.signality.engineering.wifi.integration.w125.W125LiveSnapshot;
import com.k1ngtle.vsia.signality.engineering.wifi.integration.w125.W125LiveTestManager;
import com.k1ngtle.vsia.signality.engineering.wifi.integration.w125.W125SecurityUnitTestSuite;
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

@Mod.EventBusSubscriber(
        modid = Vsia.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class WifiW125TestCommand {
    private WifiW125TestCommand() {
    }

    @SubscribeEvent
    public static void registerCommands(
            RegisterCommandsEvent event
    ) {
        CommandDispatcher<CommandSourceStack> dispatcher =
                event.getDispatcher();

        dispatcher.register(
                Commands.literal(
                                "wifiw125test"
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
                                                                "args",
                                                                StringArgumentType.greedyString()
                                                        )
                                                        .executes(
                                                                context ->
                                                                        live(
                                                                                context.getSource(),
                                                                                StringArgumentType.getString(
                                                                                        context,
                                                                                        "args"
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
        );
    }

    private static int unit(
            CommandSourceStack source
    ) {
        var results =
                W125SecurityUnitTestSuite.runAll();

        int passed =
                0;

        for (var result
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

        int p =
                passed;

        int f =
                failed;

        source.sendSuccess(
                () ->
                        Component.literal(
                                "W1.25 unit result: "
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

        return failed == 0
                ? 1
                : 0;
    }

    private static int live(
            CommandSourceStack source,
            String raw
    ) {
        String[] values =
                raw.trim()
                        .split(
                                "\\s+"
                        );

        if (values.length != 7) {
            source.sendFailure(
                    Component.literal(
                            "Usage: /wifiw125test live <open|wpa|wpa2|wpa3> <STA x y z> <AP x y z>"
                    )
            );
            return 0;
        }

        String protocol =
                values[0];

        BlockPos station =
                new BlockPos(
                        Integer.parseInt(
                                values[1]
                        ),
                        Integer.parseInt(
                                values[2]
                        ),
                        Integer.parseInt(
                                values[3]
                        )
                );

        BlockPos ap =
                new BlockPos(
                        Integer.parseInt(
                                values[4]
                        ),
                        Integer.parseInt(
                                values[5]
                        ),
                        Integer.parseInt(
                                values[6]
                        )
                );

        if (!W125LiveTestManager.start(
                source.getLevel(),
                station,
                ap,
                protocol
        )) {
            source.sendFailure(
                    Component.literal(
                            "A W1.25 live test is already active."
                    )
            );
            return 0;
        }

        source.sendSuccess(
                () ->
                        Component.literal(
                                "W1.25 security/protocol abstraction live test started"
                        ).withStyle(
                                ChatFormatting.GREEN
                        ),
                false
        );

        return 1;
    }

    private static int status(
            CommandSourceStack source
    ) {
        W125LiveSnapshot snapshot =
                W125LiveTestManager.snapshot();

        if (snapshot == null) {
            source.sendFailure(
                    Component.literal(
                            "No W1.25 live test active."
                    )
            );
            return 0;
        }

        source.sendSuccess(
                () ->
                        Component.literal(
                                "[W1.25] "
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
                "Protocol="
                        + snapshot.protocolId()
                        + " | "
                        + snapshot.detail()
        );

        line(
                source,
                "STA: state="
                        + snapshot.stationState()
                        + " securityState="
                        + snapshot.securityState()
                        + " selectedSecurity="
                        + snapshot.selectedSecurity()
                        + " ackRx="
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
        W125LiveTestManager.reset();

        source.sendSuccess(
                () ->
                        Component.literal(
                                "W1.25 test state reset."
                        ).withStyle(
                                ChatFormatting.GREEN
                        ),
                false
        );

        return 1;
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

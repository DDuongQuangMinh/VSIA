package com.k1ngtle.vsia.signality.debug;

import com.k1ngtle.vsia.Vsia;
import com.k1ngtle.vsia.signality.engineering.wifi.integration.w127.W127Snapshot;
import com.k1ngtle.vsia.signality.engineering.wifi.integration.w127.W127TestManager;
import com.k1ngtle.vsia.signality.engineering.wifi.integration.w127.W127UnitTestSuite;
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
public final class WifiW127TestCommand {
    private WifiW127TestCommand() {
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
                                "wifiw127test"
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
                                                "internet"
                                        )
                                        .then(
                                                Commands.argument(
                                                                "args",
                                                                StringArgumentType.greedyString()
                                                        )
                                                        .executes(
                                                                context ->
                                                                        internet(
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
                                                "wifi"
                                        )
                                        .then(
                                                Commands.argument(
                                                                "coords",
                                                                StringArgumentType.greedyString()
                                                        )
                                                        .executes(
                                                                context ->
                                                                        wifi(
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
                                                "closure"
                                        )
                                        .executes(
                                                context ->
                                                        closure(
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
                                                                context.getSource(),
                                                                false
                                                        )
                                        )
                                        .then(
                                                Commands.literal(
                                                                "all"
                                                        )
                                                        .executes(
                                                                context ->
                                                                        reset(
                                                                                context.getSource(),
                                                                                true
                                                                        )
                                                        )
                                        )
                        )
        );
    }

    private static int unit(
            CommandSourceStack source
    ) {
        var results =
                W127UnitTestSuite.runAll();

        int passed = 0;

        for (var result : results) {
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
                                    )
                                    .withStyle(
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
                                        "W1.27 unit result: "
                                                + finalPassed
                                                + " passed, "
                                                + finalFailed
                                                + " failed"
                                )
                                .withStyle(
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

    private static int internet(
            CommandSourceStack source,
            String raw
    ) {
        String[] parts =
                split(
                        raw
                );

        if (parts.length != 17) {
            source.sendFailure(
                    Component.literal(
                            "Usage: /wifiw127test internet "
                                    + "<Resolver xyz> <Root xyz> <TLD xyz> "
                                    + "<Primary xyz> <Secondary xyz> "
                                    + "<zone> <hostname>"
                    )
            );
            return 0;
        }

        BlockPos[] positions =
                positions(
                        source,
                        parts,
                        5
                );

        if (positions == null) {
            return 0;
        }

        if (!W127TestManager.startInternet(
                source.getLevel(),
                positions[0],
                positions[1],
                positions[2],
                positions[3],
                positions[4],
                parts[15],
                parts[16]
        )) {
            source.sendFailure(
                    Component.literal(
                            "A W1.27 test is already active. Use /wifiw127test reset first."
                    )
            );
            return 0;
        }

        source.sendSuccess(
                () ->
                        Component.literal(
                                        "W1.27 — Stress / Failure / Edge Cases Internet fault suite started"
                                )
                                .withStyle(
                                        ChatFormatting.GREEN
                                ),
                false
        );

        line(
                source,
                "Resolver="
                        + positions[0].toShortString()
                        + " Root="
                        + positions[1].toShortString()
                        + " TLD="
                        + positions[2].toShortString()
                        + " Primary="
                        + positions[3].toShortString()
                        + " Secondary="
                        + positions[4].toShortString()
        );

        line(
                source,
                "Zone="
                        + parts[15]
                        + " Host="
                        + parts[16]
        );

        line(
                source,
                "This suite intentionally causes packet loss, outages, DNSSEC rejection and incomplete transfers, then proves recovery."
        );

        return 1;
    }

    private static int wifi(
            CommandSourceStack source,
            String raw
    ) {
        String[] parts =
                split(
                        raw
                );

        if (parts.length != 18) {
            source.sendFailure(
                    Component.literal(
                            "Usage: /wifiw127test wifi "
                                    + "<STA1 xyz> <STA2 xyz> <AP1 xyz> <AP2 xyz> <Switch xyz> <Server xyz>"
                    )
            );
            return 0;
        }

        BlockPos[] positions =
                positions(
                        source,
                        parts,
                        6
                );

        if (positions == null) {
            return 0;
        }

        if (!W127TestManager.startWifiRegression(
                source.getLevel(),
                positions[0],
                positions[1],
                positions[2],
                positions[3],
                positions[4],
                positions[5]
        )) {
            source.sendFailure(
                    Component.literal(
                            "Could not start the W1.26.4 post-fault regression. Finish/reset the active W1.27 suite first."
                    )
            );
            return 0;
        }

        source.sendSuccess(
                () ->
                        Component.literal(
                                        "W1.27 post-fault Wi-Fi regression started through W1.26.4"
                                )
                                .withStyle(
                                        ChatFormatting.GREEN
                                ),
                false
        );

        line(
                source,
                "The regression must again prove 2 WPA2 STAs, DHCP uniqueness, concurrent HTTP 200, QoS and AP handoff."
        );

        return 1;
    }

    private static int status(
            CommandSourceStack source
    ) {
        W127Snapshot snapshot =
                W127TestManager.snapshot();

        source.sendSuccess(
                () ->
                        Component.literal(
                                        "[W1.27] "
                                                + snapshot.stage()
                                                + " | "
                                                + (
                                                snapshot.finished()
                                                        ? snapshot.passed()
                                                        ? "PASS"
                                                        : "FAIL"
                                                        : "RUNNING"
                                        )
                                )
                                .withStyle(
                                        snapshot.finished()
                                                ? snapshot.passed()
                                                ? ChatFormatting.GREEN
                                                : ChatFormatting.RED
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
                "Failure: "
                        + snapshot.failure()
                        + " | elapsed="
                        + snapshot.elapsedTicks()
                        + " ticks"
        );

        if (!snapshot.resolution()
                .isBlank()) {
            line(
                    source,
                    "DNS: "
                            + snapshot.resolution()
            );
        }

        if (!snapshot.transfer()
                .isBlank()) {
            line(
                    source,
                    "XFR: "
                            + snapshot.transfer()
            );
        }

        line(
                source,
                "Faults: "
                        + snapshot.faults()
                        .compact()
        );

        line(
                source,
                "Internet suite="
                        + snapshot.internetSuitePassed()
                        + " | W1.26.4 post-fault regression="
                        + snapshot.wifiRegressionPassed()
        );

        line(
                source,
                "Wi-Fi regression: "
                        + snapshot.wifiRegressionDetail()
        );

        return snapshot.finished()
                ? snapshot.passed()
                ? 1
                : 0
                : 1;
    }

    private static int closure(
            CommandSourceStack source
    ) {
        boolean internet =
                W127TestManager.internetSuitePassed();

        boolean wifi =
                W127TestManager.wifiRegressionPassed();

        boolean closure =
                W127TestManager.closurePassed();

        source.sendSuccess(
                () ->
                        Component.literal(
                                        closure
                                                ? "[W1.27] COMPLETE | PASS"
                                                : "[W1.27] CLOSURE NOT READY"
                                )
                                .withStyle(
                                        closure
                                                ? ChatFormatting.GREEN
                                                : ChatFormatting.YELLOW
                                ),
                false
        );

        line(
                source,
                "Internet destructive/recovery suite="
                        + internet
        );

        line(
                source,
                "W1.26.4 post-fault Wi-Fi regression="
                        + wifi
        );

        line(
                source,
                "Wi-Fi detail="
                        + W127TestManager.wifiRegressionDetail()
        );

        return closure
                ? 1
                : 0;
    }

    private static int reset(
            CommandSourceStack source,
            boolean all
    ) {
        if (all) {
            W127TestManager.resetAll();
        } else {
            W127TestManager.resetCurrent();
        }

        source.sendSuccess(
                () ->
                        Component.literal(
                                        all
                                                ? "W1.27 complete test state and closure flags reset."
                                                : "W1.27 active test/fault state reset; completed-suite flags retained."
                                )
                                .withStyle(
                                        ChatFormatting.GREEN
                                ),
                false
        );

        return 1;
    }

    private static BlockPos[] positions(
            CommandSourceStack source,
            String[] parts,
            int count
    ) {
        BlockPos[] positions =
                new BlockPos[count];

        try {
            for (int index = 0;
                 index < count;
                 index++) {
                positions[index] =
                        new BlockPos(
                                Integer.parseInt(
                                        parts[index * 3]
                                ),
                                Integer.parseInt(
                                        parts[index * 3 + 1]
                                ),
                                Integer.parseInt(
                                        parts[index * 3 + 2]
                                )
                        );
            }
        } catch (NumberFormatException exception) {
            source.sendFailure(
                    Component.literal(
                            "All device coordinates must be integers."
                    )
            );
            return null;
        }

        return positions;
    }

    private static String[] split(
            String raw
    ) {
        return raw == null
                || raw.isBlank()
                ? new String[0]
                : raw.trim()
                .split(
                        "\\s+"
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
                                )
                                .withStyle(
                                        ChatFormatting.GRAY
                                ),
                false
        );
    }
}

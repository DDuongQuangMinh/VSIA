package com.k1ngtle.vsia.signality.debug;

import com.k1ngtle.vsia.Vsia;
import com.k1ngtle.vsia.network.VsiaNetwork;
import com.k1ngtle.vsia.network.wifi.WifiMultiEngineeringOpenPacket;
import com.k1ngtle.vsia.signality.engineering.wifi.integration.w123.W123ClosureUnitTestSuite;
import com.k1ngtle.vsia.signality.engineering.wifi.integration.w123.W123ContentionSnapshot;
import com.k1ngtle.vsia.signality.engineering.wifi.integration.w123.W123ContentionTestManager;
import com.k1ngtle.vsia.signality.engineering.wifi.integration.w123.W123IdentityAudit;
import com.k1ngtle.vsia.signality.engineering.wifi.integration.w123.W123ShipTrackingStore;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;
import java.util.UUID;

@Mod.EventBusSubscriber(
        modid = Vsia.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class WifiW123ClosureTestCommand {
    private WifiW123ClosureTestCommand() {
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
                                "wifiw123test"
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
                                                "contention"
                                        )
                                        .then(
                                                Commands.argument(
                                                                "coords",
                                                                StringArgumentType.greedyString()
                                                        )
                                                        .executes(
                                                                context ->
                                                                        contention(
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
                                                "identity"
                                        )
                                        .then(
                                                Commands.argument(
                                                                "coords",
                                                                StringArgumentType.greedyString()
                                                        )
                                                        .executes(
                                                                context ->
                                                                        identity(
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
                                                "multiopen"
                                        )
                                        .then(
                                                Commands.argument(
                                                                "coords",
                                                                StringArgumentType.greedyString()
                                                        )
                                                        .executes(
                                                                context ->
                                                                        identity(
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
                                                "shipmark"
                                        )
                                        .then(
                                                Commands.argument(
                                                                "coords",
                                                                StringArgumentType.greedyString()
                                                        )
                                                        .executes(
                                                                context ->
                                                                        shipMark(
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
                                                "shipverify"
                                        )
                                        .then(
                                                Commands.argument(
                                                                "coords",
                                                                StringArgumentType.greedyString()
                                                        )
                                                        .executes(
                                                                context ->
                                                                        shipVerify(
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
        List<W123ClosureUnitTestSuite.Result> results =
                W123ClosureUnitTestSuite.runAll();

        int passed =
                0;

        for (W123ClosureUnitTestSuite.Result result
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
                                "W1.23 unit result: "
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

    private static int contention(
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
            return 0;
        }

        boolean started =
                W123ContentionTestManager.start(
                        source.getLevel(),
                        positions[0],
                        positions[1]
                );

        if (!started) {
            source.sendFailure(
                    Component.literal(
                            "A W1.23 contention test is already active. Run status or reset first."
                    )
            );
            return 0;
        }

        source.sendSuccess(
                () ->
                        Component.literal(
                                "W1.23 contention/EDCA closure test started"
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
                "The test runs 32-frame normal load, then 96 synchronous submissions against the 64-frame MAC contention queue."
        );

        return 1;
    }

    private static int identity(
            CommandSourceStack source,
            String raw,
            boolean openGui
    ) {
        BlockPos[] positions;

        try {
            positions =
                    parsePositions(
                            raw,
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

        W123IdentityAudit.Result result =
                W123IdentityAudit.audit(
                        source.getLevel(),
                        List.of(
                                positions
                        )
                );

        for (W123IdentityAudit.Entry entry
                : result.entries()) {
            line(
                    source,
                    "W1.23.3 "
                            + entry.label()
                            + " UUID="
                            + entry.deviceId()
                            + " MAC="
                            + entry.macAddress()
                            + " storage="
                            + entry.storagePosition()
                            .toShortString()
                            + " world="
                            + formatWorld(
                            entry.worldPosition()
                    )
                            + " tcpOwner="
                            + entry.tcpSchedulerOwned()
                            + " vmOwner="
                            + entry.vmSchedulerOwned()
                            + " detailedRF="
                            + entry.detailedPropagation()
            );
        }

        if (!result.passed()) {
            source.sendFailure(
                    Component.literal(
                            "[W1.23.3] IDENTITY FAIL: "
                                    + result.detail()
                    )
            );
            return 0;
        }

        source.sendSuccess(
                () ->
                        Component.literal(
                                "[W1.23.3] IDENTITY PASS | "
                                        + result.detail()
                        ).withStyle(
                                ChatFormatting.GREEN
                        ),
                false
        );

        if (!openGui) {
            return 1;
        }

        ServerPlayer player;

        try {
            player =
                    source.getPlayerOrException();
        } catch (Exception exception) {
            source.sendFailure(
                    Component.literal(
                            "multiopen must be run by a player"
                    )
            );
            return 0;
        }

        List<UUID> ids =
                result.entries()
                        .stream()
                        .map(
                                W123IdentityAudit.Entry::deviceId
                        )
                        .toList();

        VsiaNetwork.sendToPlayer(
                player,
                new WifiMultiEngineeringOpenPacket(
                        ids
                )
        );

        source.sendSuccess(
                () ->
                        Component.literal(
                                "Opened existing W1.23.3 four-device analyzer by persistent UUID"
                        ).withStyle(
                                ChatFormatting.AQUA
                        ),
                false
        );

        return 1;
    }

    private static int shipMark(
            CommandSourceStack source,
            String raw
    ) {
        BlockPos[] positions;

        try {
            positions =
                    parsePositions(
                            raw,
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

        W123ShipTrackingStore.Mark mark =
                W123ShipTrackingStore.mark(
                        source.getLevel(),
                        positions[0]
                );

        if (mark == null) {
            source.sendFailure(
                    Component.literal(
                            "No live Wi-Fi device resolved at/near the requested world position"
                    )
            );
            return 0;
        }

        source.sendSuccess(
                () ->
                        Component.literal(
                                "W1.23 ship tracking mark saved"
                        ).withStyle(
                                ChatFormatting.GREEN
                        ),
                false
        );

        line(
                source,
                "UUID="
                        + mark.deviceId()
                        + " storage="
                        + mark.storagePosition()
                        .toShortString()
                        + " world="
                        + formatWorld(
                        mark.initialWorldPosition()
                )
        );

        line(
                source,
                "Now move/rotate the VS ship by at least 0.25 blocks, target the device at its NEW visible world position, then run shipverify."
        );

        return 1;
    }

    private static int shipVerify(
            CommandSourceStack source,
            String raw
    ) {
        BlockPos[] positions;

        try {
            positions =
                    parsePositions(
                            raw,
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

        W123ShipTrackingStore.VerifyResult result =
                W123ShipTrackingStore.verify(
                        source.getLevel(),
                        positions[0]
                );

        ChatFormatting color =
                result.passed()
                        ? ChatFormatting.GREEN
                        : ChatFormatting.RED;

        source.sendSuccess(
                () ->
                        Component.literal(
                                "[W1.23 SHIP] "
                                        + (
                                        result.passed()
                                                ? "PASS"
                                                : "FAIL"
                                )
                        ).withStyle(
                                color
                        ),
                false
        );

        line(
                source,
                result.detail()
        );

        line(
                source,
                "UUID="
                        + result.deviceId()
                        + " storage="
                        + (
                        result.storagePosition() == null
                                ? "n/a"
                                : result.storagePosition()
                                .toShortString()
                )
                        + " initialWorld="
                        + formatWorld(
                        result.initialWorldPosition()
                )
                        + " currentWorld="
                        + formatWorld(
                        result.currentWorldPosition()
                )
                        + " displacement="
                        + String.format(
                        java.util.Locale.ROOT,
                        "%.2f",
                        result.worldDisplacementBlocks()
                )
                        + " blocks"
        );

        return result.passed()
                ? 1
                : 0;
    }

    private static int status(
            CommandSourceStack source
    ) {
        W123ContentionSnapshot snapshot =
                W123ContentionTestManager.snapshot();

        if (snapshot == null) {
            source.sendFailure(
                    Component.literal(
                            "No W1.23 contention test has been started."
                    )
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
                                "[W1.23] "
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

        line(
                source,
                "Burst: moderate="
                        + snapshot.acceptedModerate()
                        + "/32 saturation="
                        + snapshot.acceptedSaturation()
                        + "/96"
        );

        line(
                source,
                "Contention: q="
                        + snapshot.queueDepth()
                        + "/"
                        + snapshot.queueCapacity()
                        + " peak="
                        + snapshot.queuePeak()
                        + " enq="
                        + snapshot.enqueued()
                        + " attempts="
                        + snapshot.attempts()
                        + " ok="
                        + snapshot.successes()
                        + " retry="
                        + snapshot.retries()
                        + " drop="
                        + snapshot.drops()
                        + " defer="
                        + snapshot.deferrals()
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

        line(
                source,
                "RF: detailed="
                        + snapshot.detailedPropagation()
                        + " RSSI="
                        + db(
                        snapshot.receivedPowerDbm()
                )
                        + " dBm SNR="
                        + db(
                        snapshot.snrDb()
                )
                        + " dB correctedSINR="
                        + db(
                        snapshot.correctedSinrDb()
                )
                        + " dB"
        );

        line(
                source,
                "Trace: STA="
                        + snapshot.stationTraceEvents()
                        + " AP="
                        + snapshot.apTraceEvents()
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
        W123ContentionTestManager.reset();
        W123ShipTrackingStore.clear();

        source.sendSuccess(
                () ->
                        Component.literal(
                                "W1.23 closure test state reset."
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
                "/wifiw123test unit"
        );

        line(
                source,
                "/wifiw123test contention <STA x y z> <AP x y z>"
        );

        line(
                source,
                "/wifiw123test identity <A x y z> <B x y z> <C x y z> <D x y z>"
        );

        line(
                source,
                "/wifiw123test multiopen <A x y z> <B x y z> <C x y z> <D x y z>"
        );

        line(
                source,
                "/wifiw123test shipmark <device world x y z>"
        );

        line(
                source,
                "/wifiw123test shipverify <device NEW world x y z>"
        );

        line(
                source,
                "/wifiw123test status | reset"
        );
    }

    private static String formatWorld(
            Vec3 value
    ) {
        if (value == null) {
            return "n/a";
        }

        return String.format(
                java.util.Locale.ROOT,
                "%.2f,%.2f,%.2f",
                value.x,
                value.y,
                value.z
        );
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

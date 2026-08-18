package com.k1ngtle.vsia.signality.debug;

import com.k1ngtle.vsia.signality.engineering.conformance.ComponentConformanceStatus;
import com.k1ngtle.vsia.signality.engineering.conformance.ConformanceStatusRegistry;
import com.k1ngtle.vsia.signality.engineering.conformance.EngineeringLabSuite;
import com.k1ngtle.vsia.signality.engineering.conformance.KnownAnswerResult;
import com.k1ngtle.vsia.signality.engineering.conformance.KnownAnswerSuite;
import com.k1ngtle.vsia.signality.engineering.conformance.PacketInspection;
import com.k1ngtle.vsia.signality.engineering.conformance.PacketInspector;
import com.k1ngtle.vsia.signality.engineering.conformance.LabCheckResult;
import com.k1ngtle.vsia.signality.engineering.conformance.RfEngineeringLab;
import com.k1ngtle.vsia.signality.engineering.conformance.RfLabResult;
import com.k1ngtle.vsia.signality.engineering.conformance.RfLabScenario;
import com.k1ngtle.vsia.signality.engineering.conformance.StandardCatalog;
import com.k1ngtle.vsia.signality.engineering.conformance.StandardReference;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public final class SignalityLabCommand {

    private SignalityLabCommand() {
    }

    public static void register(
            CommandDispatcher<CommandSourceStack> dispatcher
    ) {
        dispatcher.register(
                Commands.literal(
                                "signalitylab"
                        )
                        .requires(
                                source ->
                                        source.hasPermission(
                                                2
                                        )
                        )
                        .executes(
                                context ->
                                        help(
                                                context.getSource()
                                        )
                        )
                        .then(
                                Commands.literal(
                                                "help"
                                        )
                                        .executes(
                                                context ->
                                                        help(
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
                                                "standards"
                                        )
                                        .executes(
                                                context ->
                                                        standards(
                                                                context.getSource()
                                                        )
                                        )
                        )
                        .then(
                                Commands.literal(
                                                "kat"
                                        )
                                        .executes(
                                                context ->
                                                        kat(
                                                                context.getSource()
                                                        )
                                        )
                        )
                        .then(
                                Commands.literal(
                                                "all"
                                        )
                                        .executes(
                                                context ->
                                                        all(
                                                                context.getSource()
                                                        )
                                        )
                        )
                        .then(
                                Commands.literal(
                                                "inspect"
                                        )
                                        .then(
                                                Commands.argument(
                                                                "hex",
                                                                StringArgumentType.greedyString()
                                                        )
                                                        .executes(
                                                                context ->
                                                                        inspect(
                                                                                context.getSource(),
                                                                                StringArgumentType.getString(
                                                                                        context,
                                                                                        "hex"
                                                                                )
                                                                        )
                                                        )
                                        )
                        )
                        .then(
                                Commands.literal(
                                                "rf"
                                        )
                                        .then(
                                                Commands.argument(
                                                                "frequency_mhz",
                                                                DoubleArgumentType.doubleArg(
                                                                        0.001
                                                                )
                                                        )
                                                        .then(
                                                                Commands.argument(
                                                                                "distance_m",
                                                                                DoubleArgumentType.doubleArg(
                                                                                        0.001
                                                                                )
                                                                        )
                                                                        .then(
                                                                                Commands.argument(
                                                                                                "bandwidth_mhz",
                                                                                                DoubleArgumentType.doubleArg(
                                                                                                        0.000001
                                                                                                )
                                                                                        )
                                                                                        .then(
                                                                                                Commands.argument(
                                                                                                                "tx_dbm",
                                                                                                                DoubleArgumentType.doubleArg(
                                                                                                                        -300.0,
                                                                                                                        300.0
                                                                                                                )
                                                                                                        )
                                                                                                        .then(
                                                                                                                Commands.argument(
                                                                                                                                "noise_figure_db",
                                                                                                                                DoubleArgumentType.doubleArg(
                                                                                                                                        0.0,
                                                                                                                                        100.0
                                                                                                                                )
                                                                                                                        )
                                                                                                                        .executes(
                                                                                                                                context ->
                                                                                                                                        rf(
                                                                                                                                                context.getSource(),
                                                                                                                                                DoubleArgumentType.getDouble(
                                                                                                                                                        context,
                                                                                                                                                        "frequency_mhz"
                                                                                                                                                ),
                                                                                                                                                DoubleArgumentType.getDouble(
                                                                                                                                                        context,
                                                                                                                                                        "distance_m"
                                                                                                                                                ),
                                                                                                                                                DoubleArgumentType.getDouble(
                                                                                                                                                        context,
                                                                                                                                                        "bandwidth_mhz"
                                                                                                                                                ),
                                                                                                                                                DoubleArgumentType.getDouble(
                                                                                                                                                        context,
                                                                                                                                                        "tx_dbm"
                                                                                                                                                ),
                                                                                                                                                DoubleArgumentType.getDouble(
                                                                                                                                                        context,
                                                                                                                                                        "noise_figure_db"
                                                                                                                                                )
                                                                                                                                        )
                                                                                                                        )
                                                                                                        )
                                                                                        )
                                                                        )
                                                        )
                                        )
                        )
                        .then(
                                Commands.literal(
                                                "sweep"
                                        )
                                        .then(
                                                Commands.argument(
                                                                "frequency_mhz",
                                                                DoubleArgumentType.doubleArg(
                                                                        0.001
                                                                )
                                                        )
                                                        .then(
                                                                Commands.argument(
                                                                                "bandwidth_mhz",
                                                                                DoubleArgumentType.doubleArg(
                                                                                        0.000001
                                                                                )
                                                                        )
                                                                        .then(
                                                                                Commands.argument(
                                                                                                "tx_dbm",
                                                                                                DoubleArgumentType.doubleArg(
                                                                                                        -300.0,
                                                                                                        300.0
                                                                                                )
                                                                                        )
                                                                                        .then(
                                                                                                Commands.argument(
                                                                                                                "noise_figure_db",
                                                                                                                DoubleArgumentType.doubleArg(
                                                                                                                        0.0,
                                                                                                                        100.0
                                                                                                                )
                                                                                                        )
                                                                                                        .executes(
                                                                                                                context ->
                                                                                                                        sweep(
                                                                                                                                context.getSource(),
                                                                                                                                DoubleArgumentType.getDouble(
                                                                                                                                        context,
                                                                                                                                        "frequency_mhz"
                                                                                                                                ),
                                                                                                                                DoubleArgumentType.getDouble(
                                                                                                                                        context,
                                                                                                                                        "bandwidth_mhz"
                                                                                                                                ),
                                                                                                                                DoubleArgumentType.getDouble(
                                                                                                                                        context,
                                                                                                                                        "tx_dbm"
                                                                                                                                ),
                                                                                                                                DoubleArgumentType.getDouble(
                                                                                                                                        context,
                                                                                                                                        "noise_figure_db"
                                                                                                                                )
                                                                                                                        )
                                                                                                        )
                                                                                        )
                                                                        )
                                                        )
                                        )
                        )
        );
    }

    private static int help(
            CommandSourceStack source
    ) {
        title(
                source,
                "Signality Engineering / Conformance Lab"
        );

        line(
                source,
                "/signalitylab status",
                ChatFormatting.WHITE
        );

        line(
                source,
                "/signalitylab standards",
                ChatFormatting.WHITE
        );

        line(
                source,
                "/signalitylab kat",
                ChatFormatting.WHITE
        );

        line(
                source,
                "/signalitylab all",
                ChatFormatting.WHITE
        );

        line(
                source,
                "/signalitylab inspect <hex bytes>",
                ChatFormatting.WHITE
        );

        line(
                source,
                "/signalitylab rf <MHz> <distance m> <bandwidth MHz> <tx dBm> <NF dB>",
                ChatFormatting.WHITE
        );

        line(
                source,
                "/signalitylab sweep <MHz> <bandwidth MHz> <tx dBm> <NF dB>",
                ChatFormatting.WHITE
        );

        line(
                source,
                "CONFORMANCE is never implied by a PASS unless the component status explicitly says CONFORMANCE.",
                ChatFormatting.GRAY
        );

        return 1;
    }

    private static int status(
            CommandSourceStack source
    ) {
        title(
                source,
                "Implementation Status"
        );

        for (ComponentConformanceStatus status
                : ConformanceStatusRegistry.all()) {
            ChatFormatting color =
                    switch (status.level()) {
                        case SIMULATION ->
                                ChatFormatting.YELLOW;

                        case CONFORMANCE_PREP ->
                                ChatFormatting.AQUA;

                        case CONFORMANCE ->
                                ChatFormatting.GREEN;
                    };

            line(
                    source,
                    status.component()
                            + " = "
                            + status.level()
                            + " ["
                            + status.referenceId()
                            + "]",
                    color
            );

            line(
                    source,
                    "  "
                            + status.note(),
                    ChatFormatting.DARK_GRAY
            );
        }

        return 1;
    }

    private static int standards(
            CommandSourceStack source
    ) {
        title(
                source,
                "Pinned Standards / Source Artifacts"
        );

        for (StandardReference reference
                : StandardCatalog.all()) {
            line(
                    source,
                    reference.id()
                            + " | "
                            + reference.document()
                            + " | "
                            + reference.revision(),
                    ChatFormatting.AQUA
            );

            line(
                    source,
                    "  scope: "
                            + reference.scope(),
                    ChatFormatting.GRAY
            );

            line(
                    source,
                    "  artifact: "
                            + reference.sourceArtifact(),
                    ChatFormatting.DARK_GRAY
            );
        }

        return 1;
    }

    private static int kat(
            CommandSourceStack source
    ) {
        title(
                source,
                "Known-answer Tests"
        );

        int failed =
                0;

        for (KnownAnswerResult result
                : KnownAnswerSuite.runAll()) {
            if (result.passed()) {
                line(
                        source,
                        "[PASS] "
                                + result.id(),
                        ChatFormatting.GREEN
                );
            } else {
                failed++;

                line(
                        source,
                        "[FAIL] "
                                + result.id(),
                        ChatFormatting.RED
                );
            }

            line(
                    source,
                    "  expected="
                            + result.expected(),
                    ChatFormatting.DARK_GRAY
            );

            line(
                    source,
                    "  actual="
                            + result.actual(),
                    ChatFormatting.DARK_GRAY
            );
        }

        summary(
                source,
                failed
        );

        return failed == 0
                ? 1
                : 0;
    }

    private static int all(
            CommandSourceStack source
    ) {
        title(
                source,
                "Deterministic Engineering Lab"
        );

        int failed =
                0;

        int passed =
                0;

        for (LabCheckResult result
                : EngineeringLabSuite
                .runDeterministicChecks()) {

            if (result.passed()) {
                passed++;

                line(
                        source,
                        "[PASS] "
                                + result.id(),
                        ChatFormatting.GREEN
                );
            } else {
                failed++;

                line(
                        source,
                        "[FAIL] "
                                + result.id(),
                        ChatFormatting.RED
                );
            }

            line(
                    source,
                    "  "
                            + result.detail(),
                    ChatFormatting.DARK_GRAY
            );
        }

        line(
                source,
                "Result: "
                        + passed
                        + " passed, "
                        + failed
                        + " failed",
                failed == 0
                        ? ChatFormatting.GREEN
                        : ChatFormatting.RED
        );

        return failed == 0
                ? 1
                : 0;
    }

    private static int inspect(
            CommandSourceStack source,
            String hex
    ) {
        try {
            PacketInspection inspection =
                    PacketInspector.inspect(
                            PacketInspector.parseHex(
                                    hex
                            )
                    );

            title(
                    source,
                    "Packet Inspector"
            );

            line(
                    source,
                    "Length = "
                            + inspection.lengthBytes()
                            + " byte(s)",
                    ChatFormatting.WHITE
            );

            line(
                    source,
                    "Hex = "
                            + inspection.hex(),
                    ChatFormatting.AQUA
            );

            line(
                    source,
                    String.format(
                            "CRC32 = %08X",
                            inspection.crc32()
                    ),
                    ChatFormatting.GREEN
            );

            line(
                    source,
                    "SHA-256 = "
                            + inspection.sha256(),
                    ChatFormatting.GOLD
            );

            line(
                    source,
                    "This reports generic byte-level checks only; it does not imply a protocol frame is standards-conformant.",
                    ChatFormatting.DARK_GRAY
            );

            return 1;
        } catch (Exception exception) {
            source.sendFailure(
                    Component.literal(
                            "Packet inspection failed: "
                                    + exception.getMessage()
                    ).withStyle(
                            ChatFormatting.RED
                    )
            );

            return 0;
        }
    }

    private static int rf(
            CommandSourceStack source,
            double frequencyMhz,
            double distanceMeters,
            double bandwidthMhz,
            double txDbm,
            double noiseFigureDb
    ) {
        RfLabScenario scenario =
                new RfLabScenario(
                        frequencyMhz
                                * 1_000_000.0,
                        distanceMeters,
                        bandwidthMhz
                                * 1_000_000.0,
                        txDbm,
                        0.0,
                        0.0,
                        noiseFigureDb,
                        0.0
                );

        RfLabResult result =
                RfEngineeringLab.evaluate(
                        scenario
                );

        title(
                source,
                "RF Link Budget"
        );

        line(
                source,
                String.format(
                        "f = %.6f MHz",
                        frequencyMhz
                ),
                ChatFormatting.WHITE
        );

        line(
                source,
                String.format(
                        "distance = %.3f m",
                        distanceMeters
                ),
                ChatFormatting.WHITE
        );

        line(
                source,
                String.format(
                        "bandwidth = %.6f MHz",
                        bandwidthMhz
                ),
                ChatFormatting.WHITE
        );

        line(
                source,
                String.format(
                        "FSPL = %.3f dB",
                        result.pathLossDb()
                ),
                ChatFormatting.AQUA
        );

        line(
                source,
                String.format(
                        "RX power = %.3f dBm",
                        result.receivedPowerDbm()
                ),
                ChatFormatting.AQUA
        );

        line(
                source,
                String.format(
                        "Noise floor = %.3f dBm",
                        result.noiseFloorDbm()
                ),
                ChatFormatting.AQUA
        );

        line(
                source,
                String.format(
                        "SNR = %.3f dB",
                        result.snrDb()
                ),
                result.snrDb()
                        >= 0.0
                        ? ChatFormatting.GREEN
                        : ChatFormatting.RED
        );

        line(
                source,
                String.format(
                        "Shannon capacity = %.6f Mbit/s",
                        result.shannonCapacityBps()
                                / 1_000_000.0
                ),
                ChatFormatting.GOLD
        );

        line(
                source,
                "Assumptions: free-space loss, 0 dBi TX/RX gains, 0 dB additional loss, 290 K.",
                ChatFormatting.DARK_GRAY
        );

        return 1;
    }

    private static int sweep(
            CommandSourceStack source,
            double frequencyMhz,
            double bandwidthMhz,
            double txDbm,
            double noiseFigureDb
    ) {
        double[] distances =
                new double[]{
                        1.0,
                        3.0,
                        10.0,
                        30.0,
                        100.0,
                        300.0,
                        1000.0,
                        3000.0,
                        10000.0
                };

        title(
                source,
                "RF Distance Sweep"
        );

        for (double distance
                : distances) {
            RfLabResult result =
                    RfEngineeringLab.evaluate(
                            new RfLabScenario(
                                    frequencyMhz
                                            * 1_000_000.0,
                                    distance,
                                    bandwidthMhz
                                            * 1_000_000.0,
                                    txDbm,
                                    0.0,
                                    0.0,
                                    noiseFigureDb,
                                    0.0
                            )
                    );

            ChatFormatting color =
                    result.snrDb()
                            >= 20.0
                            ? ChatFormatting.GREEN
                            : result.snrDb()
                            >= 0.0
                            ? ChatFormatting.YELLOW
                            : ChatFormatting.RED;

            line(
                    source,
                    String.format(
                            "%8.1f m | RX %8.2f dBm | SNR %7.2f dB | C %10.3f Mbit/s",
                            distance,
                            result.receivedPowerDbm(),
                            result.snrDb(),
                            result.shannonCapacityBps()
                                    / 1_000_000.0
                    ),
                    color
            );
        }

        return 1;
    }

    private static void summary(
            CommandSourceStack source,
            int failed
    ) {
        line(
                source,
                failed == 0
                        ? "Result: all known-answer tests passed."
                        : "Result: "
                        + failed
                        + " known-answer test(s) failed.",
                failed == 0
                        ? ChatFormatting.GREEN
                        : ChatFormatting.RED
        );
    }

    private static void title(
            CommandSourceStack source,
            String text
    ) {
        line(
                source,
                "=== "
                        + text
                        + " ===",
                ChatFormatting.GOLD
        );
    }

    private static void line(
            CommandSourceStack source,
            String text,
            ChatFormatting color
    ) {
        source.sendSuccess(
                () -> Component.literal(
                        text
                ).withStyle(
                        color
                ),
                false
        );
    }
}

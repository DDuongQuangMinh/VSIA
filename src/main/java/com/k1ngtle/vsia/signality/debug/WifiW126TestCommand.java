package com.k1ngtle.vsia.signality.debug;

import com.k1ngtle.vsia.Vsia;
import com.k1ngtle.vsia.signality.engineering.wifi.integration.w126.W126Snapshot;
import com.k1ngtle.vsia.signality.engineering.wifi.integration.w126.W126TestManager;
import com.k1ngtle.vsia.signality.engineering.wifi.integration.w126.W126UnitTestSuite;
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
public final class WifiW126TestCommand {
    private WifiW126TestCommand() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("wifiw126test")
                        .requires(source -> source.hasPermission(2))
                        .then(
                                Commands.literal("unit")
                                        .executes(context -> unit(context.getSource()))
                        )
                        .then(
                                Commands.literal("full")
                                        .then(
                                                Commands.argument("coords", StringArgumentType.greedyString())
                                                        .executes(
                                                                context -> full(
                                                                        context.getSource(),
                                                                        StringArgumentType.getString(context, "coords")
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
        var results = W126UnitTestSuite.runAll();
        int passed = 0;

        for (var result : results) {
            if (result.passed()) passed++;

            source.sendSuccess(
                    () -> Component.literal(
                            "[" + (result.passed() ? "PASS" : "FAIL") + "] " + result.name()
                    ).withStyle(
                            result.passed()
                                    ? ChatFormatting.GREEN
                                    : ChatFormatting.RED
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
                        "W1.26 unit result: " + finalPassed + " passed, " + finalFailed + " failed"
                ).withStyle(
                        finalFailed == 0
                                ? ChatFormatting.GREEN
                                : ChatFormatting.RED
                ),
                false
        );

        return failed == 0 ? 1 : 0;
    }

    private static int full(CommandSourceStack source, String raw) {
        String[] parts = raw == null || raw.isBlank()
                ? new String[0]
                : raw.trim().split("\\s+");

        if (parts.length != 18) {
            source.sendFailure(
                    Component.literal(
                            "Usage: /wifiw126test full <STA1 x y z> <STA2 x y z> <AP1 x y z> <AP2 x y z> <Switch x y z> <Server x y z>"
                    )
            );
            return 0;
        }

        BlockPos[] positions = new BlockPos[6];

        try {
            for (int index = 0; index < 6; index++) {
                positions[index] = new BlockPos(
                        Integer.parseInt(parts[index * 3]),
                        Integer.parseInt(parts[index * 3 + 1]),
                        Integer.parseInt(parts[index * 3 + 2])
                );
            }
        } catch (NumberFormatException exception) {
            source.sendFailure(Component.literal("All W1.26 coordinates must be integers."));
            return 0;
        }

        if (!W126TestManager.start(
                source.getLevel(),
                positions[0],
                positions[1],
                positions[2],
                positions[3],
                positions[4],
                positions[5]
        )) {
            source.sendFailure(Component.literal("A W1.26 test is already active. Reset first."));
            return 0;
        }

        source.sendSuccess(
                () -> Component.literal(
                        "Wi-Fi 1 / W1.26.4 — Wi-Fi Integration & Scale Validation started"
                ).withStyle(ChatFormatting.GREEN),
                false
        );

        line(
                source,
                "STA1=" + positions[0].toShortString()
                        + " STA2=" + positions[1].toShortString()
                        + " AP1=" + positions[2].toShortString()
                        + " AP2=" + positions[3].toShortString()
                        + " Switch=" + positions[4].toShortString()
                        + " Server=" + positions[5].toShortString()
        );

        line(source, "AP2 must be at least +6 dB stronger than AP1 at STA1.");

        return 1;
    }

    private static int status(CommandSourceStack source) {
        W126Snapshot snapshot = W126TestManager.snapshot();

        if (snapshot == null) {
            source.sendFailure(Component.literal("No W1.26 test active."));
            return 0;
        }

        source.sendSuccess(
                () -> Component.literal(
                        "[W1.26.4] " + snapshot.stage() + " | "
                                + (snapshot.finished()
                                ? snapshot.passed() ? "PASS" : "FAIL"
                                : "RUNNING")
                ).withStyle(
                        snapshot.passed()
                                ? ChatFormatting.GREEN
                                : snapshot.finished()
                                ? ChatFormatting.RED
                                : ChatFormatting.YELLOW
                ),
                false
        );

        line(source, "Detail: " + snapshot.detail());
        line(source, "Failure: " + snapshot.failure() + " | elapsed=" + snapshot.elapsedTicks() + " ticks");
        line(source, "STA1: " + snapshot.sta1State() + "/" + snapshot.sta1Security()
                + " ip=" + snapshot.sta1Ip()
                + " bssid=" + snapshot.sta1Bssid()
                + " pending=" + snapshot.sta1Pending()
                + " ack=" + snapshot.sta1AckRx());
        line(source, "STA2: " + snapshot.sta2State() + "/" + snapshot.sta2Security()
                + " ip=" + snapshot.sta2Ip()
                + " bssid=" + snapshot.sta2Bssid()
                + " pending=" + snapshot.sta2Pending()
                + " ack=" + snapshot.sta2AckRx());
        line(source, "HTTP1: " + snapshot.sta1Workflow() + " | " + snapshot.sta1WorkflowDetail());
        line(source, "HTTP2: " + snapshot.sta2Workflow() + " | " + snapshot.sta2WorkflowDetail());
        line(source, "QoS: STA1 ok=" + snapshot.sta1QosSuccesses() + " drop=" + snapshot.sta1QosDrops()
                + " | STA2 ok=" + snapshot.sta2QosSuccesses() + " drop=" + snapshot.sta2QosDrops());
        line(source, "AP ownership: AP1=" + snapshot.ap1Stations() + " AP2=" + snapshot.ap2Stations());
        line(source, "DS: AP1 tx=" + snapshot.ap1DsTx() + " rx=" + snapshot.ap1DsRx()
                + " | AP2 tx=" + snapshot.ap2DsTx() + " rx=" + snapshot.ap2DsRx());
        line(source, "ISP1: " + snapshot.providerIpv4() + " match=" + snapshot.providerDomainMatches());

        return snapshot.finished() ? snapshot.passed() ? 1 : 0 : 1;
    }

    private static int reset(CommandSourceStack source) {
        W126TestManager.reset();

        source.sendSuccess(
                () -> Component.literal("W1.26 test state reset.").withStyle(ChatFormatting.GREEN),
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

package com.k1ngtle.vsia.signality.debug;

import com.k1ngtle.vsia.Vsia;
import com.k1ngtle.vsia.network.VsiaNetwork;
import com.k1ngtle.vsia.network.wifi.WifiEngineeringSnapshotPacket;
import com.k1ngtle.vsia.signality.engineering.wifi.instrument.WifiEngineeringGuiTestResult;
import com.k1ngtle.vsia.signality.engineering.wifi.instrument.WifiEngineeringGuiTestSuite;
import com.k1ngtle.vsia.signality.engineering.wifi.instrument.WifiEngineeringProbe;
import com.k1ngtle.vsia.signality.engineering.wifi.instrument.WifiEngineeringResolution;
import com.k1ngtle.vsia.signality.engineering.wifi.instrument.WifiEngineeringResolvedTarget;
import com.k1ngtle.vsia.signality.engineering.wifi.instrument.WifiEngineeringTargetResolver;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = Vsia.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class WifiEngineeringGuiCommand {
    private WifiEngineeringGuiCommand() {
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
                                "wifiw1gui"
                        )
                        .requires(
                                source ->
                                        source.hasPermission(
                                                2
                                        )
                        )
                        .then(
                                Commands.argument(
                                                "pos",
                                                BlockPosArgument
                                                        .blockPos()
                                        )
                                        .executes(
                                                context ->
                                                        open(
                                                                context.getSource(),
                                                                BlockPosArgument
                                                                        .getLoadedBlockPos(
                                                                                context,
                                                                                "pos"
                                                                        )
                                                        )
                                        )
                        )
        );

        dispatcher.register(
                Commands.literal(
                                "wifiw1guitest"
                        )
                        .requires(
                                source ->
                                        source.hasPermission(
                                                2
                                        )
                        )
                        .executes(
                                context ->
                                        runTests(
                                                context.getSource()
                                        )
                        )
        );
    }

    private static int open(
            CommandSourceStack source,
            BlockPos pos
    ) {
        ServerPlayer player;

        try {
            player =
                    source.getPlayerOrException();
        } catch (Exception exception) {
            source.sendFailure(
                    Component.literal(
                            "This command must be run by a player"
                    )
            );

            return 0;
        }

        WifiEngineeringResolution resolution =
                WifiEngineeringTargetResolver.resolve(
                        source.getLevel(),
                        pos
                );

        if (!resolution.resolved()) {
            source.sendFailure(
                    Component.literal(
                            resolution.failureDetail()
                    )
            );

            return 0;
        }

        WifiEngineeringResolvedTarget target =
                resolution.target();

        VsiaNetwork.sendToPlayer(
                player,
                new WifiEngineeringSnapshotPacket(
                        pos,
                        WifiEngineeringProbe.capture(
                                target.device()
                        ),
                        true
                )
        );

        source.sendSuccess(
                () ->
                        Component.literal(
                                target.direct()
                                        ? "Opened Wi-Fi engineering analyzer for direct device "
                                                + target.devicePos()
                                                .toShortString()
                                        : "Opened Wi-Fi engineering analyzer via "
                                                + target.hops()
                                                + " infrastructure hop(s): "
                                                + target.routeDescription()
                        ).withStyle(
                                ChatFormatting.AQUA
                        ),
                false
        );

        return 1;
    }

    private static int runTests(
            CommandSourceStack source
    ) {
        int passed =
                0;

        int failed =
                0;

        for (WifiEngineeringGuiTestResult result
                : WifiEngineeringGuiTestSuite.runAll()) {
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
                                "Result: "
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
}

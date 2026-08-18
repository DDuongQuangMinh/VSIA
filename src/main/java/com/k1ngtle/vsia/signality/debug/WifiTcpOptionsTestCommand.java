package com.k1ngtle.vsia.signality.debug;

import com.k1ngtle.vsia.Vsia;
import com.k1ngtle.vsia.signality.engineering.wifi.tcp.options.TcpLiveOptionsTestResult;
import com.k1ngtle.vsia.signality.engineering.wifi.tcp.options.TcpLiveOptionsTestSuite;
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
public final class WifiTcpOptionsTestCommand {
    private WifiTcpOptionsTestCommand() {
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
                                "wifiw1tcpoptiontest"
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

    private static int runTests(
            CommandSourceStack source
    ) {
        int passed = 0;
        int failed = 0;

        for (TcpLiveOptionsTestResult result
                : TcpLiveOptionsTestSuite.runAll()) {
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

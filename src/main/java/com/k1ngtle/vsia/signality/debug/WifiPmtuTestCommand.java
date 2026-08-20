package com.k1ngtle.vsia.signality.debug;

import com.k1ngtle.vsia.Vsia;
import com.k1ngtle.vsia.signality.engineering.wifi.ip.mtu.WifiPmtuTestResult;
import com.k1ngtle.vsia.signality.engineering.wifi.ip.mtu.WifiPmtuTestSuite;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = Vsia.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class WifiPmtuTestCommand {
    private WifiPmtuTestCommand() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(
            RegisterCommandsEvent event
    ) {
        event.getDispatcher()
                .register(
                        Commands.literal(
                                        "wifiw1pmtutest"
                                )
                                .requires(
                                        source ->
                                                source.hasPermission(
                                                        2
                                                )
                                )
                                .executes(
                                        context -> {
                                            int passed =
                                                    0;

                                            int failed =
                                                    0;

                                            for (WifiPmtuTestResult result
                                                    : WifiPmtuTestSuite.runAll()) {
                                                if (result.passed()) {
                                                    passed++;
                                                } else {
                                                    failed++;
                                                }

                                                context.getSource()
                                                        .sendSuccess(
                                                                () ->
                                                                        Component.literal(
                                                                                (result.passed()
                                                                                        ? "[PASS] "
                                                                                        : "[FAIL] ")
                                                                                        + result.name()
                                                                        ).withStyle(
                                                                                result.passed()
                                                                                        ? ChatFormatting.GREEN
                                                                                        : ChatFormatting.RED
                                                                        ),
                                                                false
                                                        );
                                            }

                                            int finalPassed =
                                                    passed;

                                            int finalFailed =
                                                    failed;

                                            context.getSource()
                                                    .sendSuccess(
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
                                )
                );
    }
}

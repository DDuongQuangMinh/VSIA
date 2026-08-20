package com.k1ngtle.vsia.signality.debug;

import com.k1ngtle.vsia.Vsia;
import com.k1ngtle.vsia.signality.engineering.wifi.ip.raw.WifiRawFragmentTestResult;
import com.k1ngtle.vsia.signality.engineering.wifi.ip.raw.WifiRawFragmentTestSuite;
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
public final class WifiRawFragmentTestCommand {
    private WifiRawFragmentTestCommand() {
    }

    @SubscribeEvent
    public static void register(
            RegisterCommandsEvent event
    ) {
        event.getDispatcher()
                .register(
                        Commands.literal(
                                        "wifiw1rawfragtest"
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

                                            for (WifiRawFragmentTestResult result
                                                    : WifiRawFragmentTestSuite.runAll()) {
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

                                            int p =
                                                    passed;

                                            int f =
                                                    failed;

                                            context.getSource()
                                                    .sendSuccess(
                                                            () ->
                                                                    Component.literal(
                                                                            "Result: "
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
                                )
                );
    }
}

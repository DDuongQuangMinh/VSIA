package com.k1ngtle.vsia.signality.debug;

import com.k1ngtle.vsia.Vsia;
import com.k1ngtle.vsia.signality.engineering.wifi.ip.router.RouterMultiHopTestResult;
import com.k1ngtle.vsia.signality.engineering.wifi.ip.router.RouterMultiHopTestSuite;
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
public final class WifiMultiRouterTestCommand {
    private WifiMultiRouterTestCommand() {
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
                                "wifiw1multiroutertest"
                        )
                        .requires(
                                source ->
                                        source.hasPermission(
                                                2
                                        )
                        )
                        .executes(
                                context ->
                                        run(
                                                context.getSource()
                                        )
                        )
        );
    }

    private static int run(
            CommandSourceStack source
    ) {
        int passed =
                0;

        int failed =
                0;

        for (RouterMultiHopTestResult result
                : RouterMultiHopTestSuite.runAll()) {
            if (result.passed()) {
                passed++;
            } else {
                failed++;
            }

            source.sendSuccess(
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

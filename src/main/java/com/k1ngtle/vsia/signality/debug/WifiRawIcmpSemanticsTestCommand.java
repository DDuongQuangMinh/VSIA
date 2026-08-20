package com.k1ngtle.vsia.signality.debug;

import com.k1ngtle.vsia.Vsia;
import com.k1ngtle.vsia.signality.engineering.wifi.ip.raw.RawIcmpSemanticsTestResult;
import com.k1ngtle.vsia.signality.engineering.wifi.ip.raw.RawIcmpSemanticsTestSuite;
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
public final class WifiRawIcmpSemanticsTestCommand {
    private WifiRawIcmpSemanticsTestCommand() {
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
                        "wifiw1icmptest"
                )
                        .requires(
                                source ->
                                        source.hasPermission(2)
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
        int passed = 0;
        int failed = 0;

        for (RawIcmpSemanticsTestResult result
                : RawIcmpSemanticsTestSuite.runAll()) {
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
                                        + " | "
                                        + result.detail()
                        )
                );
            }
        }

        int p = passed;
        int f = failed;

        source.sendSuccess(
                () ->
                        Component.literal(
                                "W1.13 Result: "
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
}

package com.k1ngtle.vsia.signality.debug;

import com.k1ngtle.vsia.Vsia;
import com.k1ngtle.vsia.signality.engineering.wifi.ip.router.RouterTestResult;
import com.k1ngtle.vsia.signality.engineering.wifi.ip.router.RouterTestSuite;
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
public final class WifiRouterTestCommand {
    private WifiRouterTestCommand() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(
            RegisterCommandsEvent event
    ) {
        register(event.getDispatcher());
    }

    public static void register(
            CommandDispatcher<CommandSourceStack> dispatcher
    ) {
        dispatcher.register(
                Commands.literal("wifiw1routertest")
                        .requires(source -> source.hasPermission(2))
                        .executes(context -> run(context.getSource()))
        );
    }

    private static int run(CommandSourceStack source) {
        int passed = 0;
        int failed = 0;

        for (RouterTestResult result : RouterTestSuite.runAll()) {
            if (result.passed()) {
                passed++;
                source.sendSuccess(
                        () -> Component.literal(
                                "[PASS] " + result.id()
                        ).withStyle(ChatFormatting.GREEN),
                        false
                );
            } else {
                failed++;
                source.sendFailure(
                        Component.literal(
                                "[FAIL] " + result.id()
                        )
                );
            }
        }

        int p = passed;
        int f = failed;

        source.sendSuccess(
                () -> Component.literal(
                        "Result: " + p
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

        return failed == 0 ? 1 : 0;
    }
}

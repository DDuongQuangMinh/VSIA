package com.k1ngtle.vsia.signality.debug;

import com.k1ngtle.vsia.Vsia;
import com.k1ngtle.vsia.signality.engineering.wifi.dns.DnsRawTestResult;
import com.k1ngtle.vsia.signality.engineering.wifi.dns.DnsRawTestSuite;
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
public final class WifiRawDnsTestCommand {
    private WifiRawDnsTestCommand() {
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
                Commands.literal("wifiw1dnstest")
                        .requires(source -> source.hasPermission(2))
                        .executes(context -> runTests(context.getSource()))
        );
    }

    private static int runTests(
            CommandSourceStack source
    ) {
        int passed = 0;
        int failed = 0;

        for (DnsRawTestResult result
                : DnsRawTestSuite.runAll()) {
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

        return failed == 0 ? 1 : 0;
    }
}

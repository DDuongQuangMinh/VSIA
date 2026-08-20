package com.k1ngtle.vsia.signality.debug;

import com.k1ngtle.vsia.Vsia;
import com.k1ngtle.vsia.signality.engineering.firewall.transit.FirewallTransitTestResult;
import com.k1ngtle.vsia.signality.engineering.firewall.transit.FirewallTransitTestSuite;
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
public final class WifiW1161TransitTestCommand {
    private WifiW1161TransitTestCommand() {
    }

    @SubscribeEvent
    public static void register(
            RegisterCommandsEvent event
    ) {
        event.getDispatcher().register(
                Commands.literal("wifiw1firewalltransittest")
                        .requires(source -> source.hasPermission(2))
                        .executes(context -> run(context.getSource()))
        );
    }

    private static int run(
            net.minecraft.commands.CommandSourceStack source
    ) {
        int passed=0;
        int failed=0;

        for (FirewallTransitTestResult result :
                FirewallTransitTestSuite.runAll()) {
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
                                "[FAIL] "
                                        + result.id()
                                        + " | "
                                        + result.detail()
                        )
                );
            }
        }

        int p=passed;
        int f=failed;

        source.sendSuccess(
                () -> Component.literal(
                        "W1.16.1 Result: "
                                + p
                                + " passed, "
                                + f
                                + " failed"
                ).withStyle(
                        f==0
                                ? ChatFormatting.GREEN
                                : ChatFormatting.RED
                ),
                false
        );

        return f==0 ? 1 : 0;
    }
}

package com.k1ngtle.vsia.signality.debug;

import com.k1ngtle.vsia.Vsia;
import com.k1ngtle.vsia.signality.internet.server.FirewallCliEnhancer;
import com.k1ngtle.vsia.signality.internet.server.FirewallOsSimulator;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod.EventBusSubscriber(
        modid = Vsia.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class FirewallOsFidelityTestCommand {
    private FirewallOsFidelityTestCommand() {
    }

    @SubscribeEvent
    public static void register(
            RegisterCommandsEvent event
    ) {
        event.getDispatcher().register(
                Commands.literal("wifiw1firewallostest")
                        .requires(source -> source.hasPermission(2))
                        .executes(context -> run(context.getSource()))
        );
    }

    private static int run(
            net.minecraft.commands.CommandSourceStack source
    ) {
        int passed = 0;
        int failed = 0;

        FirewallOsSimulator sim =
                new FirewallOsSimulator(
                        99,
                        1,
                        "ASA99_1",
                        () -> {
                        }
                );

        passed += check(source, "boot-on-cli",
                !sim.isBooted
                        && execute(sim, "enable")
                        && sim.isBooted);

        passed += check(source, "enable-mode",
                sim.cliMode
                        == FirewallOsSimulator.CliMode.PRIVILEGED);

        execute(sim, "configure terminal");

        passed += check(source, "configure-terminal",
                sim.cliMode
                        == FirewallOsSimulator.CliMode.CONFIG);

        execute(sim, "interface gi1/1");
        execute(sim, "nameif inside");
        execute(sim, "ip address 192.168.10.1 255.255.255.0");

        passed += check(source, "gi-alias",
                sim.cliMode
                        == FirewallOsSimulator.CliMode.CONFIG_IF);

        passed += check(source, "inside-nameif",
                "inside".equals(
                        sim.portConfigs
                                .get("GigabitEthernet1/1")
                                .nameif
                ));

        passed += check(source, "inside-security-default",
                sim.portConfigs
                        .get("GigabitEthernet1/1")
                        .securityLevel == 100);

        execute(sim, "end");

        passed += check(source, "end-from-interface",
                sim.cliMode
                        == FirewallOsSimulator.CliMode.PRIVILEGED);

        execute(sim, "configure terminal");
        execute(sim, "end");

        passed += check(source, "end-from-global-config",
                sim.cliMode
                        == FirewallOsSimulator.CliMode.PRIVILEGED);

        execute(sim, "configure terminal");
        execute(sim, "do show route");

        passed += check(source, "do-show",
                sim.cliMode
                        == FirewallOsSimulator.CliMode.CONFIG);

        execute(sim, "interface gi1/1");
        execute(sim, "no ip address");

        passed += check(source, "no-ip-address",
                "unassigned".equals(
                        sim.portConfigs
                                .get("GigabitEthernet1/1")
                                .ipAddress
                ));

        execute(sim, "nameif inside");
        execute(sim, "no nameif");

        passed += check(source, "no-nameif",
                sim.portConfigs
                        .get("GigabitEthernet1/1")
                        .nameif
                        .isEmpty());

        execute(sim, "end");
        execute(sim, "write memory");

        passed += check(source, "write-memory",
                !FirewallCliEnhancer
                        .startupConfig(sim)
                        .isBlank());

        execute(sim, "show startup-config");

        passed += check(source, "show-startup-config",
                sim.cliLines.stream().anyMatch(
                        line -> line.contains("ASA Version 9.14")
                ));

        execute(sim, "show history");

        passed += check(source, "show-history",
                FirewallCliEnhancer
                        .history(sim)
                        .size() >= 5);

        List<String> paste =
                FirewallCliEnhancer.splitPaste(
                        "enable\r\n"
                                + "configure terminal\r\n"
                                + "!\r\n"
                                + "interface gi1/2\r\n"
                                + " nameif outside\r\n"
                                + " exit\r\n"
                );

        passed += check(source, "paste-crlf",
                paste.size() == 6);

        passed += check(source, "paste-comments",
                paste.contains("!"));

        passed += check(source, "paste-trim",
                paste.contains("nameif outside"));

        String previous =
                FirewallCliEnhancer.historyPrevious(
                        sim,
                        ""
                );

        passed += check(source, "history-up",
                previous != null
                        && !previous.isBlank());

        String next =
                FirewallCliEnhancer.historyNext(sim);

        passed += check(source, "history-down",
                next != null);

        String snapshot =
                FirewallCliEnhancer.terminalSnapshot(sim);

        passed += check(source, "terminal-copy-snapshot",
                snapshot.contains(sim.getPrompt()));

        execute(sim, "show nameif");

        passed += check(source, "show-nameif",
                sim.cliLines.stream().anyMatch(
                        line -> line.contains("Interface")
                                && line.contains("Name")
                ));

        execute(sim, "show clock");

        passed += check(source, "show-clock",
                !sim.cliLines.isEmpty());

        sim.isBooted = false;
        sim.bootStartTime =
                System.currentTimeMillis() - 5000L;

        FirewallCliEnhancer.advanceBoot(
                sim,
                System.currentTimeMillis()
        );

        passed += check(source, "auto-boot",
                sim.isBooted);

        execute(sim, "reload");

        passed += check(source, "reload-booting",
                !sim.isBooted
                        && sim.cliMode
                        == FirewallOsSimulator.CliMode.EXEC);

        int total = 24;
        failed = total - passed;

        int finalPassed = passed;
        int finalFailed = failed;

        source.sendSuccess(
                () -> Component.literal(
                        "Firewall OS Fidelity Result: "
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

        return failed == 0 ? 1 : 0;
    }

    private static boolean execute(
            FirewallOsSimulator sim,
            String command
    ) {
        sim.executeCliCore(
                command,
                true
        );
        return true;
    }

    private static int check(
            net.minecraft.commands.CommandSourceStack source,
            String id,
            boolean passed
    ) {
        if (passed) {
            source.sendSuccess(
                    () -> Component.literal(
                            "[PASS] firewall-os-" + id
                    ).withStyle(ChatFormatting.GREEN),
                    false
            );
            return 1;
        }

        source.sendFailure(
                Component.literal(
                        "[FAIL] firewall-os-" + id
                )
        );
        return 0;
    }
}

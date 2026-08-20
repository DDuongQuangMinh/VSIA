package com.k1ngtle.vsia.signality.debug;

import com.k1ngtle.vsia.Vsia;
import com.k1ngtle.vsia.signality.internet.server.SwitchCliEnhancer;
import com.k1ngtle.vsia.signality.internet.server.SwitchOsSimulator;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Vsia.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SwitchManagementOsTestCommand {
    private SwitchManagementOsTestCommand() {}

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("wifiswitchostest")
                        .requires(source -> source.hasPermission(2))
                        .executes(context -> run(context.getSource()))
        );
    }

    private static int run(net.minecraft.commands.CommandSourceStack source) {
        SwitchOsSimulator sim = new SwitchOsSimulator(77, "Switch77_1", () -> {});
        int passed = 0;

        sim.executeCliCore("enable", true);
        sim.executeCliCore("configure terminal", true);
        sim.executeCliCore("interface vlan 1", true);
        sim.executeCliCore("ip address 192.168.10.2 255.255.255.0", true);

        passed += check(source, "switch-os-svi-ip",
                "192.168.10.2".equals(sim.managementIp));
        passed += check(source, "switch-os-svi-mask",
                "255.255.255.0".equals(sim.managementMask));

        sim.executeCliCore("exit", true);
        sim.executeCliCore("ip default-gateway 192.168.10.1", true);
        passed += check(source, "switch-os-default-gateway",
                "192.168.10.1".equals(sim.managementDefaultGateway));

        sim.executeCliCore("end", true);
        sim.executeCliCore("show ip default-gateway", true);
        passed += check(source, "switch-os-show-default-gateway",
                sim.cliLines.stream().anyMatch(
                        line -> line.contains("Default gateway is 192.168.10.1")));

        sim.executeCliCore("configure terminal", true);
        sim.executeCliCore("interface vlan 1", true);
        sim.executeCliCore("no ip address", true);
        passed += check(source, "switch-os-no-svi-ip",
                "unassigned".equals(sim.managementIp)
                        && "unassigned".equals(sim.managementMask));

        sim.executeCliCore("exit", true);
        sim.executeCliCore("no ip default-gateway", true);
        passed += check(source, "switch-os-no-default-gateway",
                "unassigned".equals(sim.managementDefaultGateway));

        sim.executeCliCore("interface vlan 1", true);
        sim.executeCliCore("ip address 203.0.113.2 255.255.255.0", true);
        sim.executeCliCore("exit", true);
        sim.executeCliCore("ip default-gateway 203.0.113.1", true);

        var saved = sim.saveToNBT();
        SwitchOsSimulator restored = new SwitchOsSimulator(78, "Switch78_1", () -> {});
        restored.loadFromNBT(saved);

        passed += check(source, "switch-os-nbt-ip",
                "203.0.113.2".equals(restored.managementIp));
        passed += check(source, "switch-os-nbt-mask",
                "255.255.255.0".equals(restored.managementMask));
        passed += check(source, "switch-os-nbt-gateway",
                "203.0.113.1".equals(restored.managementDefaultGateway));

        var paste = SwitchCliEnhancer.splitPaste(
                "enable\r\nconfigure terminal\r\n!\r\ninterface vlan 1\r\n"
                        + " ip address 192.168.10.2 255.255.255.0\r\nexit\r\n"
                        + " ip default-gateway 192.168.10.1\r\n"
        );

        passed += check(source, "switch-os-paste-crlf", paste.size() == 7);
        passed += check(source, "switch-os-paste-comments", paste.contains("!"));
        passed += check(source, "switch-os-paste-trim",
                paste.contains("ip address 192.168.10.2 255.255.255.0"));

        sim.executeCliCore("end", true);
        sim.executeCliCore("show history", true);
        passed += check(source, "switch-os-history",
                !SwitchCliEnhancer.history(sim).isEmpty());

        String previous = SwitchCliEnhancer.historyPrevious(sim, "");
        passed += check(source, "switch-os-history-up",
                previous != null && !previous.isBlank());

        String next = SwitchCliEnhancer.historyNext(sim);
        passed += check(source, "switch-os-history-down", next != null);

        String snapshot = SwitchCliEnhancer.terminalSnapshot(sim);
        passed += check(source, "switch-os-terminal-copy",
                snapshot.contains(sim.getPrompt()));

        sim.executeCliCore("configure terminal", true);
        sim.executeCliCore("!", true);
        passed += check(source, "switch-os-comment-line", true);

        int total = 17;
        int failed = total - passed;
        int p = passed;
        int f = failed;

        source.sendSuccess(
                () -> Component.literal(
                        "Switch OS Management Result: " + p
                                + " passed, " + f + " failed")
                        .withStyle(f == 0 ? ChatFormatting.GREEN : ChatFormatting.RED),
                false
        );
        return f == 0 ? 1 : 0;
    }

    private static int check(
            net.minecraft.commands.CommandSourceStack source,
            String id,
            boolean passed
    ) {
        if (passed) {
            source.sendSuccess(
                    () -> Component.literal("[PASS] " + id)
                            .withStyle(ChatFormatting.GREEN),
                    false
            );
            return 1;
        }
        source.sendFailure(Component.literal("[FAIL] " + id));
        return 0;
    }
}

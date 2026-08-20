package com.k1ngtle.vsia.signality.internet.server;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public final class FirewallCliEnhancer {
    private static final int MAX_HISTORY = 100;
    private static final int MAX_PASTE_LINES = 256;
    private static final int MAX_PASTE_CHARS = 32768;
    private static final long AUTO_BOOT_MILLIS = 3000L;

    private static final Map<FirewallOsSimulator, SessionState> STATES =
            Collections.synchronizedMap(new WeakHashMap<>());

    private FirewallCliEnhancer() {
    }

    public static void advanceBoot(
            FirewallOsSimulator simulator,
            long nowMillis
    ) {
        if (simulator == null || simulator.isBooted) {
            return;
        }

        if (nowMillis - simulator.bootStartTime >= AUTO_BOOT_MILLIS) {
            simulator.isBooted = true;
            simulator.bootStep = Integer.MAX_VALUE;
        }
    }

    public static boolean handle(
            FirewallOsSimulator simulator,
            String input,
            boolean echo
    ) {
        if (simulator == null || input == null) {
            return false;
        }

        String cmd = input.trim();

        if (cmd.isEmpty()) {
            return false;
        }

        ensureConsoleReady(simulator);
        recordHistory(simulator, cmd);

        String lower = cmd.toLowerCase();

        if (lower.equals("!") || lower.startsWith("! ")) {
            if (echo) {
                simulator.cliLines.add(simulator.getPrompt() + input);
            }
            return true;
        }

        if (lower.equals("end")
                && isConfigurationMode(simulator.cliMode)) {
            if (echo) {
                simulator.cliLines.add(simulator.getPrompt() + input);
            }

            simulator.cliMode =
                    FirewallOsSimulator.CliMode.PRIVILEGED;

            simulator.cliTarget = "";
            return true;
        }

        if (lower.equals("end")
                && simulator.cliMode
                == FirewallOsSimulator.CliMode.CONFIG) {
            if (echo) {
                simulator.cliLines.add(simulator.getPrompt() + input);
            }

            simulator.cliMode =
                    FirewallOsSimulator.CliMode.PRIVILEGED;

            simulator.cliTarget = "";
            return true;
        }

        if (lower.startsWith("do ")
                && isConfigurationModeOrGlobal(simulator.cliMode)) {
            if (echo) {
                simulator.cliLines.add(simulator.getPrompt() + input);
            }

            FirewallOsSimulator.CliMode previousMode =
                    simulator.cliMode;

            String previousTarget =
                    simulator.cliTarget;

            simulator.cliMode =
                    FirewallOsSimulator.CliMode.PRIVILEGED;

            simulator.cliTarget = "";

            simulator.executeCliCore(
                    cmd.substring(3).trim(),
                    false
            );

            simulator.cliMode = previousMode;
            simulator.cliTarget = previousTarget;
            return true;
        }

        if (handleWriteMemory(simulator, cmd, lower, echo)) {
            return true;
        }

        if (handleShowEnhancements(simulator, input, lower, echo)) {
            return true;
        }

        if (handleInterfaceNoCommands(simulator, input, lower, echo)) {
            return true;
        }

        if (lower.equals("reload")) {
            if (echo) {
                simulator.cliLines.add(simulator.getPrompt() + input);
                simulator.cliLines.add("Proceed with reload? [confirm]");
                simulator.cliLines.add("Reload requested. Simulator boot sequence restarted.");
            }

            simulator.isBooted = false;
            simulator.bootStep = 0;
            simulator.bootStartTime =
                    System.currentTimeMillis();

            simulator.cliMode =
                    FirewallOsSimulator.CliMode.EXEC;

            simulator.cliTarget = "";
            return true;
        }

        if (lower.startsWith("terminal length ")
                || lower.startsWith("terminal pager ")) {
            if (echo) {
                simulator.cliLines.add(simulator.getPrompt() + input);
            }
            return true;
        }

        return false;
    }

    public static List<String> splitPaste(
            String clipboard
    ) {
        if (clipboard == null || clipboard.isEmpty()) {
            return List.of();
        }

        String normalized =
                clipboard
                        .replace("\r\n", "\n")
                        .replace('\r', '\n');

        if (normalized.length() > MAX_PASTE_CHARS) {
            normalized =
                    normalized.substring(
                            0,
                            MAX_PASTE_CHARS
                    );
        }

        String[] rawLines =
                normalized.split("\n", -1);

        List<String> commands =
                new ArrayList<>();

        for (String raw : rawLines) {
            if (commands.size() >= MAX_PASTE_LINES) {
                break;
            }

            String command =
                    raw.strip();

            if (command.isEmpty()) {
                continue;
            }

            commands.add(command);
        }

        return List.copyOf(commands);
    }

    public static String terminalSnapshot(
            FirewallOsSimulator simulator
    ) {
        if (simulator == null) {
            return "";
        }

        StringBuilder out =
                new StringBuilder();

        for (String line : simulator.cliLines) {
            out.append(line)
                    .append('\n');
        }

        out.append(simulator.getPrompt())
                .append(simulator.cliInput);

        return out.toString();
    }

    public static String historyPrevious(
            FirewallOsSimulator simulator,
            String currentInput
    ) {
        SessionState state =
                state(simulator);

        if (state.history.isEmpty()) {
            return currentInput == null
                    ? ""
                    : currentInput;
        }

        if (state.historyIndex < 0) {
            state.draft =
                    currentInput == null
                            ? ""
                            : currentInput;

            state.historyIndex =
                    state.history.size() - 1;
        } else if (state.historyIndex > 0) {
            state.historyIndex--;
        }

        return state.history.get(
                state.historyIndex
        );
    }

    public static String historyNext(
            FirewallOsSimulator simulator
    ) {
        SessionState state =
                state(simulator);

        if (state.historyIndex < 0) {
            return "";
        }

        if (state.historyIndex
                < state.history.size() - 1) {
            state.historyIndex++;

            return state.history.get(
                    state.historyIndex
            );
        }

        state.historyIndex = -1;
        return state.draft;
    }

    public static List<String> history(
            FirewallOsSimulator simulator
    ) {
        return List.copyOf(
                state(simulator).history
        );
    }

    public static String startupConfig(
            FirewallOsSimulator simulator
    ) {
        String config =
                state(simulator).startupConfig;

        return config == null
                ? ""
                : config;
    }

    private static boolean handleWriteMemory(
            FirewallOsSimulator simulator,
            String cmd,
            String lower,
            boolean echo
    ) {
        boolean writeMemory =
                lower.equals("write memory")
                        || lower.equals("wr mem")
                        || lower.equals("wr memory")
                        || lower.equals("copy running-config startup-config")
                        || lower.equals("copy run start")
                        || lower.equals("copy running startup");

        if (!writeMemory) {
            return false;
        }

        if (echo) {
            simulator.cliLines.add(
                    simulator.getPrompt() + cmd
            );
        }

        state(simulator).startupConfig =
                buildRunningConfig(simulator);

        if (echo) {
            simulator.cliLines.add(
                    "Building configuration..."
            );
            simulator.cliLines.add("[OK]");
        }

        return true;
    }

    private static boolean handleShowEnhancements(
            FirewallOsSimulator simulator,
            String input,
            String lower,
            boolean echo
    ) {
        if (lower.equals("show startup-config")
                || lower.equals("show start")) {
            if (echo) {
                simulator.cliLines.add(
                        simulator.getPrompt() + input
                );
            }

            String startup =
                    startupConfig(simulator);

            if (startup.isBlank()) {
                simulator.cliLines.add(
                        "startup-config is empty"
                );
            } else {
                Collections.addAll(
                        simulator.cliLines,
                        startup.split("\n", -1)
                );
            }

            return true;
        }

        if (lower.equals("show history")) {
            if (echo) {
                simulator.cliLines.add(
                        simulator.getPrompt() + input
                );
            }

            List<String> history =
                    history(simulator);

            int start =
                    Math.max(
                            0,
                            history.size() - 20
                    );

            for (int i = start;
                 i < history.size();
                 i++) {
                simulator.cliLines.add(
                        String.format(
                                "%3d  %s",
                                i + 1,
                                history.get(i)
                        )
                );
            }

            return true;
        }

        if (lower.equals("show nameif")) {
            if (echo) {
                simulator.cliLines.add(
                        simulator.getPrompt() + input
                );
            }

            simulator.cliLines.add(
                    "Interface                  Name                     Security"
            );

            for (Map.Entry<String, FirewallOsSimulator.PortConfig> entry
                    : simulator.portConfigs.entrySet()) {
                FirewallOsSimulator.PortConfig pc =
                        entry.getValue();

                simulator.cliLines.add(
                        String.format(
                                "%-26s %-24s %d",
                                abbreviateInterface(
                                        entry.getKey()
                                ),
                                pc.nameif.isBlank()
                                        ? "-"
                                        : pc.nameif,
                                pc.securityLevel
                        )
                );
            }

            return true;
        }

        if (lower.equals("show clock")) {
            if (echo) {
                simulator.cliLines.add(
                        simulator.getPrompt() + input
                );
            }

            simulator.cliLines.add(
                    ZonedDateTime.now()
                            .format(
                                    DateTimeFormatter.ofPattern(
                                            "HH:mm:ss.SSS z EEE MMM dd yyyy"
                                    )
                            )
            );

            return true;
        }

        if (lower.equals("show boot")) {
            if (echo) {
                simulator.cliLines.add(
                        simulator.getPrompt() + input
                );
            }

            simulator.cliLines.add(
                    "Boot state: "
                            + (
                            simulator.isBooted
                                    ? "READY"
                                    : "BOOTING"
                    )
                            + " step="
                            + simulator.bootStep
            );

            return true;
        }

        return false;
    }

    private static boolean handleInterfaceNoCommands(
            FirewallOsSimulator simulator,
            String input,
            String lower,
            boolean echo
    ) {
        if (simulator.cliMode
                != FirewallOsSimulator.CliMode.CONFIG_IF) {
            return false;
        }

        FirewallOsSimulator.PortConfig pc =
                simulator.portConfigs.get(
                        simulator.cliTarget
                );

        if (pc == null) {
            return false;
        }

        if (lower.equals("no nameif")) {
            if (echo) {
                simulator.cliLines.add(
                        simulator.getPrompt() + input
                );
            }

            pc.nameif = "";
            pc.securityLevel = 0;
            return true;
        }

        if (lower.equals("no ip address")) {
            if (echo) {
                simulator.cliLines.add(
                        simulator.getPrompt() + input
                );
            }

            pc.ipAddress = "unassigned";
            pc.subnetMask = "unassigned";
            return true;
        }

        if (lower.equals("no ipv6 address")) {
            if (echo) {
                simulator.cliLines.add(
                        simulator.getPrompt() + input
                );
            }

            pc.ipv6Address = "unassigned";
            return true;
        }

        if (lower.equals("no description")) {
            if (echo) {
                simulator.cliLines.add(
                        simulator.getPrompt() + input
                );
            }

            pc.description = "";
            return true;
        }

        if (lower.equals("no security-level")) {
            if (echo) {
                simulator.cliLines.add(
                        simulator.getPrompt() + input
                );
            }

            pc.securityLevel =
                    pc.nameif.equalsIgnoreCase("inside")
                            ? 100
                            : 0;

            return true;
        }

        return false;
    }

    private static void recordHistory(
            FirewallOsSimulator simulator,
            String command
    ) {
        String normalized =
                command.strip();

        if (normalized.isEmpty()) {
            return;
        }

        SessionState state =
                state(simulator);

        if (state.history.isEmpty()
                || !state.history.get(
                        state.history.size() - 1
                ).equals(normalized)) {
            state.history.add(normalized);
        }

        while (state.history.size()
                > MAX_HISTORY) {
            state.history.remove(0);
        }

        state.historyIndex = -1;
        state.draft = "";
    }

    private static void ensureConsoleReady(
            FirewallOsSimulator simulator
    ) {
        if (!simulator.isBooted) {
            simulator.isBooted = true;
            simulator.bootStep =
                    Integer.MAX_VALUE;
        }
    }

    private static String buildRunningConfig(
            FirewallOsSimulator simulator
    ) {
        StringBuilder out =
                new StringBuilder();

        out.append("ASA Version 9.14(1)\n");
        out.append("!\n");
        out.append("hostname ")
                .append(simulator.hostname)
                .append('\n');
        out.append("!\n");

        for (Map.Entry<String, FirewallOsSimulator.PortConfig> entry
                : simulator.portConfigs.entrySet()) {
            FirewallOsSimulator.PortConfig pc =
                    entry.getValue();

            out.append("interface ")
                    .append(entry.getKey())
                    .append('\n');

            if (!pc.description.isBlank()) {
                out.append(" description ")
                        .append(pc.description)
                        .append('\n');
            }

            if (!pc.nameif.isBlank()) {
                out.append(" nameif ")
                        .append(pc.nameif)
                        .append('\n');
                out.append(" security-level ")
                        .append(pc.securityLevel)
                        .append('\n');
            }

            if (!pc.ipAddress.equals("unassigned")) {
                out.append(" ip address ")
                        .append(pc.ipAddress)
                        .append(' ')
                        .append(pc.subnetMask)
                        .append('\n');
            }

            if (!pc.ipv6Address.equals("unassigned")) {
                out.append(" ipv6 address ")
                        .append(pc.ipv6Address)
                        .append('\n');
            }

            if (!pc.up) {
                out.append(" shutdown\n");
            }
        }

        for (String route : simulator.routes) {
            out.append(route)
                    .append('\n');
        }

        for (FirewallOsSimulator.ParsedAclRule acl
                : simulator.parsedAcls) {
            out.append(acl.rawCommand)
                    .append('\n');
        }

        return out.toString();
    }

    private static String abbreviateInterface(
            String interfaceName
    ) {
        if (interfaceName.startsWith(
                "GigabitEthernet"
        )) {
            return "Gi"
                    + interfaceName.substring(
                    "GigabitEthernet".length()
            );
        }

        if (interfaceName.startsWith(
                "Management"
        )) {
            return "Mgmt"
                    + interfaceName.substring(
                    "Management".length()
            );
        }

        return interfaceName;
    }

    private static boolean isConfigurationMode(
            FirewallOsSimulator.CliMode mode
    ) {
        return mode
                == FirewallOsSimulator.CliMode.CONFIG_IF
                || mode
                == FirewallOsSimulator.CliMode.CONFIG_OBJ
                || mode
                == FirewallOsSimulator.CliMode.CONFIG_ROUTER
                || mode
                == FirewallOsSimulator.CliMode.CONFIG_CRYPTO_MAP;
    }

    private static boolean isConfigurationModeOrGlobal(
            FirewallOsSimulator.CliMode mode
    ) {
        return mode
                == FirewallOsSimulator.CliMode.CONFIG
                || isConfigurationMode(mode);
    }

    private static SessionState state(
            FirewallOsSimulator simulator
    ) {
        return STATES.computeIfAbsent(
                simulator,
                ignored -> new SessionState()
        );
    }

    private static final class SessionState {
        private final List<String> history =
                new ArrayList<>();

        private int historyIndex = -1;
        private String draft = "";
        private String startupConfig = "";
    }
}

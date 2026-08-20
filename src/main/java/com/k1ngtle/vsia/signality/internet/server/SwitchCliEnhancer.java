package com.k1ngtle.vsia.signality.internet.server;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public final class SwitchCliEnhancer {
    private static final int MAX_HISTORY = 100;
    private static final int MAX_PASTE_LINES = 256;
    private static final int MAX_PASTE_CHARS = 32768;

    private static final Map<SwitchOsSimulator, SessionState> STATES =
            Collections.synchronizedMap(new WeakHashMap<>());

    private SwitchCliEnhancer() {}

    public static List<String> splitPaste(String clipboard) {
        if (clipboard == null || clipboard.isEmpty()) return List.of();

        String normalized = clipboard
                .replace("\r\n", "\n")
                .replace('\r', '\n');

        if (normalized.length() > MAX_PASTE_CHARS) {
            normalized = normalized.substring(0, MAX_PASTE_CHARS);
        }

        List<String> commands = new ArrayList<>();
        for (String raw : normalized.split("\n", -1)) {
            if (commands.size() >= MAX_PASTE_LINES) break;
            String command = raw.strip();
            if (!command.isEmpty()) commands.add(command);
        }
        return List.copyOf(commands);
    }

    public static boolean handlePreParse(
            SwitchOsSimulator sim,
            String input,
            boolean echo
    ) {
        if (sim == null || input == null) return false;

        String cmd = input.trim();
        if (cmd.isEmpty()) return false;

        recordHistory(sim, cmd);
        String lower = cmd.toLowerCase();

        if (lower.equals("!") || lower.startsWith("! ")) {
            if (echo) sim.cliLines.add(sim.getPrompt() + input);
            return true;
        }

        if (sim.cliMode == SwitchOsSimulator.CliMode.CONFIG_IF
                && sim.cliTarget.startsWith("VLAN")) {
            if (lower.startsWith("ip address ")) {
                if (echo) sim.cliLines.add(sim.getPrompt() + input);
                String[] t = cmd.split("\\s+");
                if (t.length < 3) {
                    sim.cliLines.add("% Incomplete command.");
                    return true;
                }
                sim.managementIp = t[2];
                sim.managementMask = t.length >= 4 ? t[3] : "255.255.255.0";
                return true;
            }

            if (lower.equals("no ip address")) {
                if (echo) sim.cliLines.add(sim.getPrompt() + input);
                sim.managementIp = "unassigned";
                sim.managementMask = "unassigned";
                return true;
            }
        }

        if (sim.cliMode == SwitchOsSimulator.CliMode.CONFIG) {
            if (lower.startsWith("ip default-gateway ")) {
                if (echo) sim.cliLines.add(sim.getPrompt() + input);
                String[] t = cmd.split("\\s+");
                if (t.length < 3) {
                    sim.cliLines.add("% Incomplete command.");
                    return true;
                }
                sim.managementDefaultGateway = t[2];
                return true;
            }

            if (lower.equals("no ip default-gateway")) {
                if (echo) sim.cliLines.add(sim.getPrompt() + input);
                sim.managementDefaultGateway = "unassigned";
                return true;
            }
        }

        if (sim.cliMode == SwitchOsSimulator.CliMode.PRIVILEGED) {
            if (lower.equals("show ip default-gateway")) {
                if (echo) sim.cliLines.add(sim.getPrompt() + input);
                sim.cliLines.add("Default gateway is " + sim.managementDefaultGateway);
                return true;
            }

            if (lower.equals("show interface vlan 1")
                    || lower.equals("show interfaces vlan 1")) {
                if (echo) sim.cliLines.add(sim.getPrompt() + input);
                sim.cliLines.add("Vlan1 is up, line protocol is up");
                sim.cliLines.add("  Internet address is "
                        + sim.managementIp + " " + sim.managementMask);
                return true;
            }

            if (lower.equals("show history")) {
                if (echo) sim.cliLines.add(sim.getPrompt() + input);
                List<String> h = history(sim);
                int start = Math.max(0, h.size() - 20);
                for (int i = start; i < h.size(); i++) {
                    sim.cliLines.add(String.format("%3d  %s", i + 1, h.get(i)));
                }
                return true;
            }
        }

        return false;
    }

    public static String historyPrevious(
            SwitchOsSimulator sim,
            String currentInput
    ) {
        SessionState s = state(sim);
        if (s.history.isEmpty()) return currentInput == null ? "" : currentInput;

        if (s.historyIndex < 0) {
            s.draft = currentInput == null ? "" : currentInput;
            s.historyIndex = s.history.size() - 1;
        } else if (s.historyIndex > 0) {
            s.historyIndex--;
        }
        return s.history.get(s.historyIndex);
    }

    public static String historyNext(SwitchOsSimulator sim) {
        SessionState s = state(sim);
        if (s.historyIndex < 0) return "";
        if (s.historyIndex < s.history.size() - 1) {
            s.historyIndex++;
            return s.history.get(s.historyIndex);
        }
        s.historyIndex = -1;
        return s.draft;
    }

    public static String terminalSnapshot(SwitchOsSimulator sim) {
        if (sim == null) return "";
        StringBuilder out = new StringBuilder();
        for (String line : sim.cliLines) out.append(line).append('\n');
        out.append(sim.getPrompt()).append(sim.cliInput);
        return out.toString();
    }

    public static List<String> history(SwitchOsSimulator sim) {
        return List.copyOf(state(sim).history);
    }

    private static void recordHistory(SwitchOsSimulator sim, String command) {
        SessionState s = state(sim);
        if (s.history.isEmpty()
                || !s.history.get(s.history.size() - 1).equals(command)) {
            s.history.add(command);
        }
        while (s.history.size() > MAX_HISTORY) s.history.remove(0);
        s.historyIndex = -1;
        s.draft = "";
    }

    private static SessionState state(SwitchOsSimulator sim) {
        return STATES.computeIfAbsent(sim, ignored -> new SessionState());
    }

    private static final class SessionState {
        private final List<String> history = new ArrayList<>();
        private int historyIndex = -1;
        private String draft = "";
    }
}

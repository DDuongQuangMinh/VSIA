package com.k1ngtle.vsia.signality.internet.router;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

public final class RouterOsSimulator {
    public enum CliMode {
        EXEC,
        PRIVILEGED,
        GLOBAL_CONFIG,
        INTERFACE_CONFIG
    }

    public static class RouteEntry {
        public String network;
        public String mask;
        public String nextHop;

        public RouteEntry(String network, String mask, String nextHop) {
            this.network = network;
            this.mask = mask;
            this.nextHop = nextHop;
        }
    }

    public String displayName = "RT-AC68U";
    public String hostname = "Router";

    // Core legacy variables exactly as expected by RtAc68uRouterBlockEntity
    public String wlanIp = "192.168.1.1";
    public String wlanMask = "255.255.255.0";
    public String wlanGateway = "";
    public boolean wlanAdminUp = true;
    public boolean forwardingEnabled = true;

    public String lan0Ip = "192.168.1.1";
    public String lan0Mask = "255.255.255.0";
    public String lan1Ip = "192.168.2.1";
    public String lan1Mask = "255.255.255.0";

    public final List<RouteEntry> staticRoutes = new ArrayList<>();

    public CliMode cliMode = CliMode.EXEC;
    public String cliTarget = "";
    public String cliInput = "";
    public int cliCursorPos = 0;
    public int cliScrollOffset = 0;

    public final List<String> iosCommands = new ArrayList<>();
    public final List<String> cliLines = new ArrayList<>();
    public final List<String> history = new ArrayList<>();

    public boolean isBooted = false;
    public long bootStartTime;
    public int bootStep = 0;

    public transient Runnable guiCallback;

    // W1.21 FULL V3 REAL PING CALLBACK
    private transient Function<String, Boolean> livePingTransmitter;

    public void setLivePingTransmitter(
            Function<String, Boolean> transmitter
    ) {
        this.livePingTransmitter = transmitter;
    }

    // W1.21 FULL V5.1 LIVE DIAGNOSTICS
    private transient Supplier<List<String>> liveEthernetDiagnostics;
    private transient Supplier<List<String>> liveArpDiagnostics;

    public void setLiveEthernetDiagnostics(Supplier<List<String>> supplier) {
        this.liveEthernetDiagnostics = supplier;
    }

    public void setLiveArpDiagnostics(Supplier<List<String>> supplier) {
        this.liveArpDiagnostics = supplier;
    }


    public RouterOsSimulator(Runnable guiCallback) {
        this.guiCallback = guiCallback;
        this.bootStartTime = System.currentTimeMillis();
    }

    // Required by RtAc68uRouterBlockEntity logic!
    public static int maskToPrefix(String mask) {
        String[] p = mask.split("\\.");
        if (p.length != 4) return 24;
        int bits = 0;
        try {
            for (String part : p) bits += Integer.bitCount(Integer.parseInt(part) & 0xFF);
        } catch (NumberFormatException ex) {
            return 24;
        }
        return bits;
    }

    public String getPrompt() {
        return switch (cliMode) {
            case EXEC -> hostname + ">";
            case PRIVILEGED -> hostname + "#";
            case GLOBAL_CONFIG -> hostname + "(config)#";
            case INTERFACE_CONFIG -> hostname + "(config-if)#";
        };
    }

    public void appendGuiCommand(String cmd, String targetContext) {
        if (targetContext.equals("Settings")) {
            iosCommands.add(hostname + "(config)#" + cmd);
        } else {
            iosCommands.add(hostname + "(config-if)#" + cmd);
        }
        while (iosCommands.size() > 8) iosCommands.remove(0);
        if (guiCallback != null) guiCallback.run();
    }

    public void executeCliCore(String input, boolean echo) {
        if (input.endsWith("?")) {
            if (echo) { cliLines.add(getPrompt() + input); cliScrollOffset = 0; }
            String prefix = input.substring(0, input.length() - 1);
            showHelp(prefix.trim().toLowerCase(), prefix.endsWith(" "), cliMode);
            if (guiCallback != null) guiCallback.run();
            return;
        }

        String cmd = input.trim();
        if (cmd.isEmpty()) { if (echo) { cliLines.add(getPrompt()); cliScrollOffset = 0; } return; }

        String lower = cmd.toLowerCase();
        boolean isDo = false;
        if (lower.startsWith("do ") && (cliMode == CliMode.GLOBAL_CONFIG || cliMode == CliMode.INTERFACE_CONFIG)) {
            isDo = true;
            lower = lower.substring(3).trim();
            cmd = cmd.substring(3).trim();
        }

        CliMode executionMode = isDo ? CliMode.PRIVILEGED : cliMode;
        if (echo) { cliLines.add(getPrompt() + input); cliScrollOffset = 0; }

        history.add(input);
        while (history.size() > 100) history.remove(0);

        if (lower.equals("help")) {
            showHelp("", false, executionMode);
            return;
        }

        String[] tokens = lower.split("\\s+");
        String first = resolveAlias(tokens[0], executionMode);

        if (first == null) {
            if (echo) appendInvalidMarker(input, tokens[0]);
            return;
        } else if (first.equals("AMBIGUOUS")) {
            if (echo) cliLines.add("% Ambiguous command:  \"" + tokens[0] + "\"");
            return;
        }

        if (executionMode == CliMode.EXEC) {
            if (first.equals("enable")) cliMode = CliMode.PRIVILEGED;
            else if (first.equals("ping") && tokens.length > 1) runPing(tokens[1], echo);
            else if (first.equals("ping")) cliLines.add("% Incomplete command.");
            else if (first.equals("exit") || first.equals("logout")) { /* No-op */ }
            else if (echo) appendInvalidMarker(input, tokens[0]);
        }
        else if (executionMode == CliMode.PRIVILEGED) {
            if (first.equals("configure")) {
                if (tokens.length > 1 && "terminal".startsWith(tokens[1])) {
                    if (echo) cliLines.add("Enter configuration commands, one per line.  End with CNTL/Z.");
                    cliMode = CliMode.GLOBAL_CONFIG;
                } else if (tokens.length == 1) {
                    if (echo) {
                        cliLines.add("Configuring from terminal, memory, or network [terminal]?");
                        cliLines.add("Enter configuration commands, one per line.  End with CNTL/Z.");
                    }
                    cliMode = CliMode.GLOBAL_CONFIG;
                } else if (echo) appendInvalidMarker(input, tokens[1]);
            }
            else if (first.equals("disable") || first.equals("exit")) cliMode = CliMode.EXEC;
            else if (first.equals("write") || first.equals("copy")) {
                if (echo) { cliLines.add("Building configuration..."); cliLines.add("[OK]"); }
            }
            else if (first.equals("show")) {
                if (tokens.length == 1) { cliLines.add("% Incomplete command."); return; }
                String second = resolveAlias(tokens[1], executionMode, first);

                if (second == null) { if (echo) appendInvalidMarker(input, tokens[1]); }
                else if (second.equals("AMBIGUOUS")) { if (echo) cliLines.add("% Ambiguous command:  \"" + tokens[1] + "\""); }
                else if (second.equals("version")) runShowVersion(echo);
                else if (second.equals("running-config")) runShowRun(echo);
                // W1.21 FULL V6.2 SHOW ETHERNET COUNTERS
                else if (second.equals("ethernet")) {
                    if (tokens.length > 2
                            && "counters".startsWith(tokens[2])) {
                        runLiveDiagnostic(
                                echo,
                                "Routed Ethernet / Counters",
                                liveEthernetDiagnostics
                        );
                    } else {
                        runLiveDiagnostic(
                                echo,
                                "Routed Ethernet",
                                liveEthernetDiagnostics
                        );
                    }
                }
                else if (second.equals("arp")) runLiveDiagnostic(echo, "ARP / Pending", liveArpDiagnostics);
                else if (second.equals("interfaces")) runLiveDiagnostic(echo, "Routed Ethernet", liveEthernetDiagnostics);
                else if (second.equals("ip")) {
                    if (tokens.length > 2 && "interface".startsWith(tokens[2])) runShowIpIntBrief(echo);
                    else if (tokens.length > 2 && "route".startsWith(tokens[2])) runShowRoute(echo);
                    else if (echo) appendInvalidMarker(input, tokens[2]);
                }
                else if (echo) appendInvalidMarker(input, tokens[1]);
            }
            else if (first.equals("ping") && tokens.length > 1) runPing(tokens[1], echo);
            else if (echo) appendInvalidMarker(input, tokens[0]);
        }
        else if (executionMode == CliMode.GLOBAL_CONFIG) {
            if (first.equals("interface")) {
                if (tokens.length == 1) { cliLines.add("% Incomplete command."); return; }
                String rawIface = cmd.substring(tokens[0].length()).trim().replaceAll("\\s+", "");
                String lowerIface = rawIface.toLowerCase();
                String iface = rawIface;

                if (lowerIface.startsWith("gi") || lowerIface.startsWith("gigabitethernet")) {
                    String num = lowerIface.replace("gigabitethernet", "").replace("gi", "");
                    iface = "GigabitEthernet" + num;
                } else if (lowerIface.startsWith("do") || lowerIface.startsWith("dot11radio") || lowerIface.startsWith("wlan")) {
                    iface = "Dot11Radio0";
                }

                if (iface.equals("GigabitEthernet0/0/0") || iface.equals("GigabitEthernet0/0/1") || iface.equals("Dot11Radio0")) {
                    cliMode = CliMode.INTERFACE_CONFIG; cliTarget = iface;
                } else if (echo) appendInvalidMarker(input, tokens[1]);
            }
            else if (first.equals("hostname")) {
                if (tokens.length > 1) hostname = cmd.substring(tokens[0].length()).trim();
                else cliLines.add("% Incomplete command.");
            }
            else if (first.equals("ip")) {
                if (tokens.length > 1 && "routing".startsWith(tokens[1])) forwardingEnabled = true;
                else if (tokens.length > 3 && "route".startsWith(tokens[1])) {
                    staticRoutes.removeIf(r -> r.network.equals(tokens[2]) && r.mask.equals(tokens[3]));
                    staticRoutes.add(new RouteEntry(tokens[2], tokens[3], tokens[4]));
                }
                else if (tokens.length > 2 && "default-gateway".startsWith(tokens[1])) wlanGateway = tokens[2];
                else if (echo) appendInvalidMarker(input, tokens[1]);
            }
            else if (first.equals("no")) {
                if (tokens.length > 2 && "ip".startsWith(tokens[1]) && "routing".startsWith(tokens[2])) forwardingEnabled = false;
                else if (tokens.length > 2 && "ip".startsWith(tokens[1]) && "default-gateway".startsWith(tokens[2])) wlanGateway = "";
                else if (tokens.length > 4 && "ip".startsWith(tokens[1]) && "route".startsWith(tokens[2])) {
                    staticRoutes.removeIf(r -> r.network.equals(tokens[3]) && r.mask.equals(tokens[4]) && r.nextHop.equals(tokens[5]));
                }
                else if (echo) appendInvalidMarker(input, tokens[1]);
            }
            else if (first.equals("exit") || first.equals("end")) { cliMode = CliMode.PRIVILEGED; }
            else if (echo) appendInvalidMarker(input, tokens[0]);
        }
        else if (executionMode == CliMode.INTERFACE_CONFIG) {
            if (first.equals("exit")) { cliMode = CliMode.GLOBAL_CONFIG; return; }
            if (first.equals("end")) { cliMode = CliMode.PRIVILEGED; return; }

            if (first.equals("shutdown")) {
                if (cliTarget.equals("Dot11Radio0")) wlanAdminUp = false;
            }
            else if (first.equals("no") && tokens.length > 1 && "shutdown".startsWith(tokens[1])) {
                if (cliTarget.equals("Dot11Radio0")) wlanAdminUp = true;
            }
            else if (first.equals("ip") && tokens.length >= 3 && "address".startsWith(tokens[1])) {
                String ip = tokens[2];
                String mask = tokens.length > 3 ? tokens[3] : "255.255.255.0";
                if (cliTarget.equals("GigabitEthernet0/0/0")) { lan0Ip = ip; lan0Mask = mask; }
                else if (cliTarget.equals("GigabitEthernet0/0/1")) { lan1Ip = ip; lan1Mask = mask; }
                else if (cliTarget.equals("Dot11Radio0")) { wlanIp = ip; wlanMask = mask; }
            }
            else if (first.equals("no") && tokens.length > 2 && "ip".startsWith(tokens[1]) && "address".startsWith(tokens[2])) {
                if (cliTarget.equals("GigabitEthernet0/0/0")) { lan0Ip = "0.0.0.0"; lan0Mask = "0.0.0.0"; }
                else if (cliTarget.equals("GigabitEthernet0/0/1")) { lan1Ip = "0.0.0.0"; lan1Mask = "0.0.0.0"; }
                else if (cliTarget.equals("Dot11Radio0")) { wlanIp = "0.0.0.0"; wlanMask = "0.0.0.0"; }
            }
            else if (echo) appendInvalidMarker(input, tokens[0]);
        }

        if (guiCallback != null) guiCallback.run();
    }

    public void handleAutocomplete() {
        if (cliInput.isEmpty()) return;
        String lower = cliInput.toLowerCase();

        String[] options = getOptionsForPrefix(lower, cliMode);
        if (options.length == 1) {
            String[] tokens = lower.split(" ", -1);
            if (tokens.length > 1) {
                String lastToken = tokens[tokens.length - 1];
                if (!lastToken.isEmpty() && options[0].startsWith(lastToken)) {
                    cliInput = cliInput.substring(0, cliInput.lastIndexOf(lastToken)) + options[0] + " ";
                    cliCursorPos = cliInput.length();
                    if (guiCallback != null) guiCallback.run();
                }
            } else if (!lower.endsWith(" ")) {
                cliInput = options[0] + " ";
                cliCursorPos = cliInput.length();
                if (guiCallback != null) guiCallback.run();
            }
        } else if (options.length > 1) {
            String match = findCommonPrefix("", options);
            if (match != null) {
                String[] tokens = lower.split(" ", -1);
                String lastToken = tokens[tokens.length - 1];
                if (match.length() > lastToken.length()) {
                    cliInput = cliInput.substring(0, cliInput.lastIndexOf(lastToken)) + match;
                    cliCursorPos = cliInput.length();
                    if (guiCallback != null) guiCallback.run();
                }
            }
        }
    }

    private void appendInvalidMarker(String input, String faultyWord) {
        cliLines.add(getPrompt() + input);
        int idx = input.toLowerCase().indexOf(faultyWord);
        if (idx == -1) idx = input.length();
        StringBuilder marker = new StringBuilder();
        for(int i=0; i<getPrompt().length() + idx; i++) marker.append(" ");
        marker.append("^");
        cliLines.add(marker.toString());
        cliLines.add("% Invalid input detected at '^' marker.");
    }

    private String resolveAlias(String token, CliMode mode, String... context) {
        String fullPrefix = String.join(" ", context) + (context.length > 0 ? " " : "") + token;
        String[] opts = getOptionsForPrefix(fullPrefix, mode);
        if (opts.length == 1) {
            String[] split = opts[0].split(" ");
            if (split.length > context.length) return split[context.length];
            return token;
        }
        if (opts.length > 1) {
            for (String opt : opts) {
                String[] split = opt.split(" ");
                if (split.length > context.length && split[context.length].equals(token)) return token;
            }
            String commonWord = null;
            boolean ambiguous = false;
            for (String opt : opts) {
                String[] split = opt.split(" ");
                if (split.length > context.length) {
                    if (commonWord == null) {
                        commonWord = split[context.length];
                    } else if (!commonWord.equals(split[context.length])) {
                        ambiguous = true;
                        break;
                    }
                }
            }
            if (!ambiguous && commonWord != null) return commonWord;
            return "AMBIGUOUS";
        }
        return null;
    }

    private String[] getOptionsForPrefix(String prefix, CliMode mode) {
        if (prefix.startsWith("do ") && (mode == CliMode.GLOBAL_CONFIG || mode == CliMode.INTERFACE_CONFIG)) {
            String subPrefix = prefix.substring(3);
            String[] subOpts = getOptionsForPrefix(subPrefix, CliMode.PRIVILEGED);
            for (int i = 0; i < subOpts.length; i++) subOpts[i] = "do " + subOpts[i];
            return subOpts;
        }

        List<String> matches = new ArrayList<>();
        List<String> allContexts = new ArrayList<>();

        if (mode == CliMode.EXEC) {
            allContexts.addAll(List.of("enable", "ping", "show version", "show running-config", "show ip interface brief", "show ip route", "show ethernet", "show arp", "show interfaces", "exit", "logout", "help"));
        } else if (mode == CliMode.PRIVILEGED) {
            allContexts.addAll(List.of("configure terminal", "disable", "exit", "write memory", "copy running-config startup-config", "show version", "show running-config", "show ip interface brief", "show ip route", "show ethernet", "show arp", "show interfaces", "ping", "help"));
        } else if (mode == CliMode.GLOBAL_CONFIG) {
            allContexts.addAll(List.of("interface GigabitEthernet0/0/0", "interface GigabitEthernet0/0/1", "interface Dot11Radio0", "hostname", "ip routing", "no ip routing", "ip default-gateway", "no ip default-gateway", "ip route", "no ip route", "exit", "end", "do", "help"));
        } else if (mode == CliMode.INTERFACE_CONFIG) {
            allContexts.addAll(List.of("ip address", "no ip address", "shutdown", "no shutdown", "exit", "end", "do", "help"));
        }

        for (String ctx : allContexts) {
            if (ctx.startsWith(prefix)) matches.add(ctx);
        }
        return matches.toArray(new String[0]);
    }

    private String findCommonPrefix(String input, String[] options) {
        if (options.length == 0) return null;
        String common = options[0];
        for (int i = 1; i < options.length; i++) {
            int j = 0;
            while (j < common.length() && j < options[i].length() && common.charAt(j) == options[i].charAt(j)) j++;
            common = common.substring(0, j);
        }
        return common.isEmpty() ? null : common;
    }

    private void showHelp(String prefix, boolean spaceBefore, CliMode mode) {
        List<String> matches = new ArrayList<>();
        String[] opts = getOptionsForPrefix(prefix, mode);

        if (opts.length == 0) {
            if (prefix.isEmpty()) {
                cliLines.add("Exec commands:");
                cliLines.add("  enable       Turn on privileged commands");
                cliLines.add("  exit         Exit from the EXEC");
                cliLines.add("  ping         Send echo messages");
                cliLines.add("  show         Show running system information");
            } else {
                cliLines.add("% Unrecognized command");
            }
            return;
        }

        if (spaceBefore) {
            for (String opt : opts) {
                String[] split = opt.split(" ");
                int prefixSpaces = prefix.trim().split(" ").length;
                if (split.length > prefixSpaces) matches.add("  " + split[prefixSpaces]);
                else if (split.length == prefixSpaces && opt.equals(prefix.trim())) matches.add("  <cr>");
            }
        } else {
            for (String opt : opts) {
                String[] split = opt.split(" ");
                matches.add("  " + split[split.length - 1]);
            }
        }

        if (matches.isEmpty()) cliLines.add("% Unrecognized command");
        else cliLines.addAll(matches.stream().distinct().toList());
    }

    private void runShowRun(boolean echo) {
        if (!echo) return;
        cliLines.add("Building configuration...");
        cliLines.add("");
        cliLines.add("version 15.0");
        cliLines.add("hostname " + hostname);
        cliLines.add("!");
        cliLines.add(forwardingEnabled ? "ip routing" : "no ip routing");
        cliLines.add("!");

        cliLines.add("interface GigabitEthernet0/0/0");
        if (!lan0Ip.equals("0.0.0.0") && !lan0Ip.equals("unassigned")) cliLines.add(" ip address " + lan0Ip + " " + lan0Mask);
        else cliLines.add(" no ip address");
        cliLines.add(" no shutdown");
        cliLines.add("!");

        cliLines.add("interface GigabitEthernet0/0/1");
        if (!lan1Ip.equals("0.0.0.0") && !lan1Ip.equals("unassigned")) cliLines.add(" ip address " + lan1Ip + " " + lan1Mask);
        else cliLines.add(" no ip address");
        cliLines.add(" no shutdown");
        cliLines.add("!");

        cliLines.add("interface Dot11Radio0");
        if (!wlanIp.equals("0.0.0.0") && !wlanIp.equals("unassigned")) cliLines.add(" ip address " + wlanIp + " " + wlanMask);
        else cliLines.add(" no ip address");
        if (!wlanAdminUp) cliLines.add(" shutdown");
        cliLines.add("!");

        if (!wlanGateway.isEmpty() && !wlanGateway.equals("unassigned")) {
            cliLines.add("ip default-gateway " + wlanGateway);
            cliLines.add("!");
        }
        for (RouteEntry route : staticRoutes) {
            cliLines.add("ip route " + route.network + " " + route.mask + " " + route.nextHop);
        }
        cliLines.add("end");
    }

    private void runShowIpIntBrief(boolean echo) {
        if (!echo) return;
        cliLines.add("Interface              IP-Address      OK? Method Status                Protocol");
        cliLines.add(String.format("%-22s %-15s YES manual %-21s %s", "GigabitEthernet0/0/0", lan0Ip, "up", "up"));
        cliLines.add(String.format("%-22s %-15s YES manual %-21s %s", "GigabitEthernet0/0/1", lan1Ip, "up", "up"));
        cliLines.add(String.format("%-22s %-15s YES manual %-21s %s", "Dot11Radio0", wlanIp, wlanAdminUp ? "up" : "administratively down", wlanAdminUp ? "up" : "down"));
    }

    private void runShowRoute(boolean echo) {
        if (!echo) return;

        // W1.21 FULL V3 NORMALIZED ROUTE DISPLAY
        cliLines.add("Codes: C - connected, S - static");
        cliLines.add("");

        appendConnectedRoute(lan0Ip, lan0Mask, "GigabitEthernet0/0/0");
        appendConnectedRoute(lan1Ip, lan1Mask, "GigabitEthernet0/0/1");
        if (wlanAdminUp) appendConnectedRoute(wlanIp, wlanMask, "Dot11Radio0");

        for (RouteEntry route : staticRoutes) {
            cliLines.add("S    " + normalizeNetwork(route.network, route.mask)
                    + "/" + maskToPrefix(route.mask) + " via " + route.nextHop);
        }

        if (!wlanGateway.isBlank() && !wlanGateway.equals("unassigned")) {
            cliLines.add("S*   0.0.0.0/0 via " + wlanGateway);
        }
    }

    private void appendConnectedRoute(String ip, String mask, String iface) {
        if (ip == null || ip.isBlank() || ip.equals("0.0.0.0") || ip.equals("unassigned")) return;
        cliLines.add("C    " + normalizeNetwork(ip, mask) + "/" + maskToPrefix(mask)
                + " is directly connected, " + iface);
    }

    private static String normalizeNetwork(String ip, String mask) {
        try {
            String[] a = ip.split("\\.");
            String[] m = mask.split("\\.");
            if (a.length != 4 || m.length != 4) return ip;
            StringBuilder out = new StringBuilder();
            for (int i = 0; i < 4; i++) {
                int octet = Integer.parseInt(a[i]) & 0xFF;
                int maskOctet = Integer.parseInt(m[i]) & 0xFF;
                if (i > 0) out.append('.');
                out.append(octet & maskOctet);
            }
            return out.toString();
        } catch (RuntimeException ex) {
            return ip;
        }
    }

    private void runLiveDiagnostic(
            boolean echo,
            String title,
            Supplier<List<String>> supplier
    ) {
        if (!echo) return;
        cliLines.add(title);
        cliLines.add("------------------------------");
        if (supplier == null) {
            cliLines.add("% Live diagnostics unavailable.");
            return;
        }
        List<String> lines = supplier.get();
        if (lines == null || lines.isEmpty()) {
            cliLines.add("No data.");
            return;
        }
        cliLines.addAll(lines);
    }

    private void runShowVersion(boolean echo) {
        if (!echo) return;
        cliLines.add("VSIA Router OS Software, RT-AC68U Software");
        cliLines.add("System image: vsia:rt_ac68u_router");
        cliLines.add("Configuration register is persistent");
    }

    private void runPing(String target, boolean echo) {
        if (!echo) return;

        // W1.21 FULL V3 REAL ROUTER PING
        cliLines.add("Type escape sequence to abort.");
        cliLines.add(
                "Sending live ICMP Echo probe to " + target + ", timeout is 2 seconds:"
        );

        if (livePingTransmitter == null) {
            cliLines.add("% Live router ping transport is unavailable.");
            cliLines.add("Success rate is 0 percent (0/1)");
            return;
        }

        boolean accepted;
        try {
            accepted = Boolean.TRUE.equals(livePingTransmitter.apply(target));
        } catch (RuntimeException ex) {
            accepted = false;
        }

        if (accepted) {
            cliLines.add(".");
            cliLines.add("Live ICMP probe injected into the real network stack.");
            cliLines.add("Reply processing is asynchronous; inspect live diagnostics.");
        } else {
            cliLines.add(".");
            cliLines.add("% Live ICMP probe could not be injected.");
            cliLines.add("Success rate is 0 percent (0/1)");
        }
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("DisplayName", displayName);
        tag.putString("Hostname", hostname);
        tag.putString("WlanIp", wlanIp);
        tag.putString("WlanMask", wlanMask);
        tag.putString("WlanGateway", wlanGateway);
        tag.putBoolean("WlanAdminUp", wlanAdminUp);
        tag.putBoolean("Forwarding", forwardingEnabled);
        tag.putString("Lan0Ip", lan0Ip);
        tag.putString("Lan0Mask", lan0Mask);
        tag.putString("Lan1Ip", lan1Ip);
        tag.putString("Lan1Mask", lan1Mask);

        ListTag routes = new ListTag();
        for (RouteEntry route : staticRoutes) {
            CompoundTag r = new CompoundTag();
            r.putString("Network", route.network);
            r.putString("Mask", route.mask);
            r.putString("NextHop", route.nextHop);
            routes.add(r);
        }
        tag.put("Routes", routes);
        return tag;
    }

    public void load(CompoundTag tag) {
        if (tag.contains("DisplayName")) displayName = tag.getString("DisplayName");
        if (tag.contains("Hostname")) hostname = tag.getString("Hostname");
        if (tag.contains("WlanIp")) wlanIp = tag.getString("WlanIp");
        if (tag.contains("WlanMask")) wlanMask = tag.getString("WlanMask");
        if (tag.contains("WlanGateway")) wlanGateway = tag.getString("WlanGateway");
        if (tag.contains("WlanAdminUp")) wlanAdminUp = tag.getBoolean("WlanAdminUp");
        if (tag.contains("Forwarding")) forwardingEnabled = tag.getBoolean("Forwarding");
        if (tag.contains("Lan0Ip")) lan0Ip = tag.getString("Lan0Ip");
        if (tag.contains("Lan0Mask")) lan0Mask = tag.getString("Lan0Mask");
        if (tag.contains("Lan1Ip")) lan1Ip = tag.getString("Lan1Ip");
        if (tag.contains("Lan1Mask")) lan1Mask = tag.getString("Lan1Mask");

        staticRoutes.clear();
        if (tag.contains("Routes", Tag.TAG_LIST)) {
            ListTag list = tag.getList("Routes", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag r = list.getCompound(i);
                staticRoutes.add(new RouteEntry(r.getString("Network"), r.contains("Mask") ? r.getString("Mask") : "255.255.255.0", r.getString("NextHop")));
            }
        }
    }    // W1.21 FULL V4 REAL PING REPLY REPORT
    public void noteLivePingReply(
            String sourceIp
    ) {
        cliLines.add(
                "! Reply from "
                        + sourceIp
        );
        cliLines.add(
                "Success rate is 100 percent (1/1)"
        );
    }


}
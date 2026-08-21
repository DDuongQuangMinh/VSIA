package com.k1ngtle.vsia.signality.internet.server;

import com.k1ngtle.vsia.signality.internet.OSINetworkPacket;
import net.minecraft.nbt.CompoundTag;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class SwitchOsSimulator {

    public enum CliMode {
        EXEC, PRIVILEGED, CONFIG, CONFIG_IF, CONFIG_VLAN
    }

    public static class PortConfig {
        public boolean up = true;
        public String description = "";
        public String speed = "auto";
        public String duplex = "auto";
        public String mode = "dynamic auto";
        public String accessVlan = "1";
        public String allowedVlans = "1-4094";
        public String txRingLimit = "10";

        // Advanced STP States
        public String stpRole = "Desg"; // Root, Desg, Altn
        public String stpState = "FWD"; // FWD, BLK, LRN, LIS
        public int stpCost = 19;
        public long lastStateChange = 0;
        public boolean portfast = false;

        public CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putBoolean("Up", up);
            tag.putString("Desc", description);
            tag.putString("Speed", speed);
            tag.putString("Duplex", duplex);
            tag.putString("Mode", mode);
            tag.putString("AccessVlan", accessVlan);
            tag.putString("AllowedVlans", allowedVlans);
            tag.putString("TxRingLimit", txRingLimit);
            tag.putString("StpRole", stpRole);
            tag.putString("StpState", stpState);
            tag.putInt("StpCost", stpCost);
            tag.putBoolean("Portfast", portfast);
            return tag;
        }

        public void load(CompoundTag tag) {
            up = tag.getBoolean("Up");
            description = tag.getString("Desc");
            speed = tag.getString("Speed");
            duplex = tag.getString("Duplex");
            mode = tag.getString("Mode");
            accessVlan = tag.getString("AccessVlan");
            allowedVlans = tag.getString("AllowedVlans");
            txRingLimit = tag.getString("TxRingLimit");
            if (tag.contains("StpRole")) stpRole = tag.getString("StpRole");
            if (tag.contains("StpState")) stpState = tag.getString("StpState");
            if (tag.contains("StpCost")) stpCost = tag.getInt("StpCost");
            if (tag.contains("Portfast")) portfast = tag.getBoolean("Portfast");
        }
    }

    public int id;
    public String switchHostname;
    public String managementIp = "unassigned";
    public String managementMask = "unassigned";
    public String managementDefaultGateway = "unassigned";
    public String macAddress;
    public String stpMode = "pvst";
    public int stpPriority = 32768;
    public boolean isStpRoot = false;

    // Active STP Variables
    public String rootBridgeId;
    public int rootPathCost = 0;
    public String rootPort = "";
    private long lastBpduTx = 0;
    private static final long HELLO_TIME = 2000;
    private static final long FORWARD_DELAY = 15000;
    private static final long MAX_AGE = 20000;
    private final Map<String, Long> lastBpduRx = new LinkedHashMap<>();

    public final Map<String, PortConfig> portConfigs = new LinkedHashMap<>();
    public final Map<String, String> vlanDatabase = new LinkedHashMap<>();
    public final Map<String, String> macTable = new LinkedHashMap<>();
    private final Map<String, Long> macTableAge = new LinkedHashMap<>();

    public final List<String> iosCommands = new ArrayList<>();
    public final List<String> cliLines = new ArrayList<>();
    public CliMode cliMode = CliMode.EXEC;
    public String cliTarget = "";
    public String cliInput = "";
    public int cliCursorPos = 0;
    public int cliScrollOffset = 0;

    private final Runnable onStateChange;
    public Runnable guiCallback;
    public Consumer<OSINetworkPacket> packetTransmitter; // Link to BlockEntity logic

    public SwitchOsSimulator(int id, String initialHostname, Runnable onStateChange) {
        this.id = id;
        this.switchHostname = initialHostname;
        this.macAddress = String.format("00:1A:2B:3C:4D:%02X", 0x5E + id);
        this.rootBridgeId = String.format("%05d", stpPriority) + ":" + macAddress;
        this.onStateChange = onStateChange;

        for (int i = 1; i <= 24; i++) portConfigs.put("FastEthernet0/" + i, new PortConfig());
        portConfigs.put("GigabitEthernet0/1", new PortConfig());
        portConfigs.put("GigabitEthernet0/2", new PortConfig());

        portConfigs.get("GigabitEthernet0/1").stpCost = 4;
        portConfigs.get("GigabitEthernet0/2").stpCost = 4;

        vlanDatabase.put("1", "default");

        cliLines.add("System Bootstrap, Version 15.0(2)EZ1");
        cliLines.add("Copyright (c) 1986-2026 by k1ngtle systems, Inc.");
        cliLines.add("Compiled Mon 16-Aug-26 11:26 by itg");
        cliLines.add("");
        cliLines.add("Base ethernet MAC Address: " + this.macAddress);
        cliLines.add("24 FastEthernet interfaces");
        cliLines.add("2 Gigabit Ethernet interfaces");
        cliLines.add("");
        cliLines.add("Press RETURN to get started.");
        cliLines.add("");
    }

    /**
     * Called every tick by the BlockEntity to run STP state machines and BPDU timers.
     */
    public void tick(Consumer<OSINetworkPacket> out) {
        this.packetTransmitter = out;
        long now = System.currentTimeMillis();

        // 1. STP Bridge Election & BPDU Transmission
        String myBridgeId = String.format("%05d", stpPriority) + ":" + macAddress;
        if (rootBridgeId == null || rootBridgeId.compareTo(myBridgeId) > 0) {
            rootBridgeId = myBridgeId;
            rootPathCost = 0;
            rootPort = "";
            isStpRoot = true;
        } else {
            isStpRoot = rootBridgeId.equals(myBridgeId);
        }

        if (now - lastBpduTx >= HELLO_TIME) {
            lastBpduTx = now;
            for (Map.Entry<String, PortConfig> entry : portConfigs.entrySet()) {
                PortConfig pc = entry.getValue();
                if (pc.up && pc.stpRole.equals("Desg")) {
                    OSINetworkPacket bpdu = new OSINetworkPacket();
                    bpdu.applicationProtocol = "STP-BPDU";
                    bpdu.sourceMac = this.macAddress;
                    bpdu.targetMac = "01:80:C2:00:00:00"; // STP Multicast
                    CompoundTag payload = new CompoundTag();
                    payload.putString("RootBridgeID", rootBridgeId);
                    payload.putInt("RootPathCost", rootPathCost);
                    payload.putString("SenderBridgeID", myBridgeId);
                    payload.putString("PortID", entry.getKey());
                    bpdu.payload = payload;
                    // Forward BPDU out this specific port via BlockEntity
                    if (packetTransmitter != null) packetTransmitter.accept(bpdu);
                }
            }
        }

        // 2. STP State Transitions (802.1D Timers)
        boolean stateChanged = false;
        for (Map.Entry<String, PortConfig> entry : portConfigs.entrySet()) {
            PortConfig pc = entry.getValue();
            if (!pc.up) {
                pc.stpState = "BLK";
                continue;
            }
            if (pc.portfast) {
                if (!pc.stpState.equals("FWD")) { pc.stpState = "FWD"; stateChanged = true; }
                continue;
            }

            if (pc.stpState.equals("BLK") && (pc.stpRole.equals("Root") || pc.stpRole.equals("Desg"))) {
                pc.stpState = "LIS";
                pc.lastStateChange = now;
                stateChanged = true;
            } else if (pc.stpState.equals("LIS") && now - pc.lastStateChange >= FORWARD_DELAY) {
                pc.stpState = "LRN";
                pc.lastStateChange = now;
                stateChanged = true;
            } else if (pc.stpState.equals("LRN") && now - pc.lastStateChange >= FORWARD_DELAY) {
                pc.stpState = "FWD";
                pc.lastStateChange = now;
                stateChanged = true;
            } else if (pc.stpRole.equals("Altn") && !pc.stpState.equals("BLK")) {
                pc.stpState = "BLK";
                stateChanged = true;
            }

            // BPDU Max Age timeout
            if (lastBpduRx.containsKey(entry.getKey()) && now - lastBpduRx.get(entry.getKey()) > MAX_AGE) {
                lastBpduRx.remove(entry.getKey());
                if (entry.getKey().equals(rootPort)) {
                    rootBridgeId = myBridgeId; // Claim root, force re-election
                    rootPathCost = 0;
                    rootPort = "";
                    stateChanged = true;
                }
            }
        }

        if (stateChanged && onStateChange != null) onStateChange.run();
    }

    // W1.20.1 AUTHORITATIVE FDB LEARNING
    public boolean learnDynamicSourceMac(
            String sourceMac,
            String ingressPortName
    ) {
        if (sourceMac == null || ingressPortName == null) {
            return false;
        }

        String normalized = normalizeDynamicMac(sourceMac);

        if (normalized.isEmpty() || isGroupMac(normalized)) {
            return false;
        }

        PortConfig ingressPort = portConfigs.get(ingressPortName);

        if (ingressPort == null
                || !ingressPort.up
                || "BLK".equals(ingressPort.stpState)
                || "LIS".equals(ingressPort.stpState)) {
            return false;
        }

        String previous = macTable.put(normalized, ingressPortName);
        macTableAge.put(normalized, System.currentTimeMillis());

        if (!ingressPortName.equals(previous) && onStateChange != null) {
            onStateChange.run();
        }

        return true;
    }

    private static String normalizeDynamicMac(String mac) {
        if (mac == null) {
            return "";
        }

        String hex = mac.replace(":", "")
                .replace("-", "")
                .replace(".", "")
                .trim()
                .toUpperCase();

        if (!hex.matches("[0-9A-F]{12}")) {
            return "";
        }

        return hex.substring(0, 2) + ":"
                + hex.substring(2, 4) + ":"
                + hex.substring(4, 6) + ":"
                + hex.substring(6, 8) + ":"
                + hex.substring(8, 10) + ":"
                + hex.substring(10, 12);
    }

    private static boolean isGroupMac(String normalized) {
        if (normalized == null || normalized.length() < 2) {
            return true;
        }

        try {
            int firstOctet = Integer.parseInt(normalized.substring(0, 2), 16);
            return (firstOctet & 0x01) != 0;
        } catch (NumberFormatException ignored) {
            return true;
        }
    }

    public List<String> processAndForwardPacket(OSINetworkPacket packet, String ingressPortName) {
        List<String> egressPorts = new ArrayList<>();
        PortConfig ingressPort = portConfigs.get(ingressPortName);
        if (ingressPort == null || !ingressPort.up) return egressPorts;

        // Process Spanning Tree BPDUs actively
        if ("STP-BPDU".equals(packet.applicationProtocol)) {
            processBpdu(packet, ingressPortName);
            return egressPorts; // BPDUs are absorbed, not forwarded dynamically
        }

        // Drop standard data traffic if port is Blocked or Listening
        if (ingressPort.stpState.equals("BLK") || ingressPort.stpState.equals("LIS")) {
            return egressPorts;
        }

        // MAC Address Learning (only in LRN and FWD states)
        learnDynamicSourceMac(
                packet.sourceMac,
                ingressPortName
        );

        // Forwarding (only in FWD state)
        if (!ingressPort.stpState.equals("FWD")) return egressPorts;

        long now = System.currentTimeMillis();
        macTableAge.entrySet().removeIf(entry -> {
            boolean stale = now - entry.getValue() > 300000;
            if (stale) macTable.remove(entry.getKey());
            return stale;
        });

        String targetMac = packet.targetMac;
        String vlan = ingressPort.accessVlan;
        boolean isBroadcast = targetMac == null || targetMac.isEmpty() || targetMac.equalsIgnoreCase("FF:FF:FF:FF:FF:FF");

        if (isBroadcast) {
            for (Map.Entry<String, PortConfig> entry : portConfigs.entrySet()) {
                if (entry.getValue().up && entry.getValue().stpState.equals("FWD") && entry.getValue().accessVlan.equals(vlan) && !entry.getKey().equals(ingressPortName)) {
                    egressPorts.add(entry.getKey());
                }
            }
        } else {
            String lookupMac =
                    normalizeDynamicMac(targetMac);

            String knownEgressPort =
                    lookupMac.isEmpty()
                            ? null
                            : macTable.get(lookupMac);
            if (knownEgressPort != null) {
                PortConfig egressPort = portConfigs.get(knownEgressPort);
                if (egressPort != null && egressPort.up && egressPort.stpState.equals("FWD") && egressPort.accessVlan.equals(vlan) && !knownEgressPort.equals(ingressPortName)) {
                    egressPorts.add(knownEgressPort);
                }
            } else { // Unknown unicast flooding
                for (Map.Entry<String, PortConfig> entry : portConfigs.entrySet()) {
                    if (entry.getValue().up && entry.getValue().stpState.equals("FWD") && entry.getValue().accessVlan.equals(vlan) && !entry.getKey().equals(ingressPortName)) {
                        egressPorts.add(entry.getKey());
                    }
                }
            }
        }
        return egressPorts;
    }

    private void processBpdu(OSINetworkPacket bpdu, String ingressPort) {
        lastBpduRx.put(ingressPort, System.currentTimeMillis());
        CompoundTag p = bpdu.payload;
        String rxRootId = p.getString("RootBridgeID");
        int rxRootCost = p.getInt("RootPathCost");
        String rxSenderId = p.getString("SenderBridgeID");

        String myBridgeId = String.format("%05d", stpPriority) + ":" + macAddress;

        boolean superiorBpdu = false;
        if (rxRootId.compareTo(rootBridgeId) < 0) {
            superiorBpdu = true;
        } else if (rxRootId.equals(rootBridgeId)) {
            int pathCost = rxRootCost + portConfigs.get(ingressPort).stpCost;
            if (pathCost < rootPathCost) superiorBpdu = true;
            else if (pathCost == rootPathCost && rxSenderId.compareTo(myBridgeId) < 0) superiorBpdu = true;
        }

        if (superiorBpdu) {
            rootBridgeId = rxRootId;
            rootPathCost = rxRootCost + portConfigs.get(ingressPort).stpCost;
            rootPort = ingressPort;
            isStpRoot = false;
        }

        // Role Evaluation
        for (Map.Entry<String, PortConfig> entry : portConfigs.entrySet()) {
            if (!entry.getValue().up) continue;
            if (entry.getKey().equals(rootPort)) {
                entry.getValue().stpRole = "Root";
            } else {
                // Simplified: If we are root, or our path is better than the neighbor's, we are Designated.
                // If not, and we aren't the Root port, we must block (Alternate).
                if (isStpRoot) {
                    entry.getValue().stpRole = "Desg";
                } else {
                    // Requires tracking neighbor costs per port for full accuracy,
                    // simulated as Alternate if receiving BPDUs and not Root Port.
                    if (lastBpduRx.containsKey(entry.getKey())) entry.getValue().stpRole = "Altn";
                    else entry.getValue().stpRole = "Desg";
                }
            }
        }
    }

    public String getPrompt() {
        switch(cliMode) {
            case EXEC: return switchHostname + ">";
            case PRIVILEGED: return switchHostname + "#";
            case CONFIG: return switchHostname + "(config)#";
            case CONFIG_IF: return switchHostname + "(config-if)#";
            case CONFIG_VLAN: return switchHostname + "(config-vlan)#";
        }
        return switchHostname + ">";
    }

    public void appendGuiCommand(String cmd, String targetContext) {
        if (targetContext.equals("Settings")) {
            iosCommands.add(switchHostname + "(config)#" + cmd);
        } else if (targetContext.equals("VLAN Database")) {
            iosCommands.add(switchHostname + "(config-vlan)#" + cmd);
        } else {
            iosCommands.add(switchHostname + "(config-if)#" + cmd);
        }
        while (iosCommands.size() > 8) iosCommands.remove(0);
        if (onStateChange != null) onStateChange.run();
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
                    if (onStateChange != null) onStateChange.run();
                    if (guiCallback != null) guiCallback.run();
                }
            } else if (!lower.endsWith(" ")) {
                cliInput = options[0] + " ";
                cliCursorPos = cliInput.length();
                if (onStateChange != null) onStateChange.run();
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
                    if (onStateChange != null) onStateChange.run();
                    if (guiCallback != null) guiCallback.run();
                }
            }
        }
    }

    public void executeCliCore(String input, boolean echo) {
        if (SwitchCliEnhancer.handlePreParse(
                this,
                input,
                echo
        )) {
            cliScrollOffset = 0;
            if (onStateChange != null) onStateChange.run();
            if (guiCallback != null) guiCallback.run();
            return;
        }

        if (input.endsWith("?")) {
            if (echo) { cliLines.add(getPrompt() + input); cliScrollOffset = 0; }
            String prefix = input.substring(0, input.length() - 1);
            showHelp(prefix.trim().toLowerCase(), prefix.endsWith(" "), cliMode);
            if (onStateChange != null) onStateChange.run();
            if (guiCallback != null) guiCallback.run();
            return;
        }

        String cmd = input.trim();
        if (cmd.isEmpty()) { if (echo) { cliLines.add(getPrompt()); cliScrollOffset = 0; } return; }

        String lower = cmd.toLowerCase();
        boolean isDo = false;
        if (lower.startsWith("do ") && (cliMode == CliMode.CONFIG || cliMode == CliMode.CONFIG_IF || cliMode == CliMode.CONFIG_VLAN)) {
            isDo = true;
            lower = lower.substring(3).trim();
            cmd = cmd.substring(3).trim();
        }

        CliMode executionMode = isDo ? CliMode.PRIVILEGED : cliMode;
        if (echo) { cliLines.add(getPrompt() + input); cliScrollOffset = 0; }

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
                    cliMode = CliMode.CONFIG;
                } else if (tokens.length == 1) {
                    if (echo) {
                        cliLines.add("Configuring from terminal, memory, or network [terminal]?");
                        cliLines.add("Enter configuration commands, one per line.  End with CNTL/Z.");
                    }
                    cliMode = CliMode.CONFIG;
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
                else if (second.equals("ip")) runShowIpIntBrief(echo);
                else if (second.equals("mac")) runShowMac(echo);
                else if (second.equals("vlan")) runShowVlan(echo);
                else if (second.equals("spanning-tree")) runShowSpanningTree(echo);
                else if (echo) appendInvalidMarker(input, tokens[1]);
            }
            else if (first.equals("ping") && tokens.length > 1) runPing(tokens[1], echo);
            else if (echo) appendInvalidMarker(input, tokens[0]);
        }
        else if (executionMode == CliMode.CONFIG) {
            if (first.equals("interface")) {
                if (tokens.length == 1) { cliLines.add("% Incomplete command."); return; }
                String rawIface = cmd.substring(tokens[0].length()).trim().replaceAll("\\s+", "");
                String lowerIface = rawIface.toLowerCase();
                String iface = rawIface;

                if (lowerIface.startsWith("fa") || lowerIface.startsWith("fastethernet")) {
                    String num = lowerIface.replace("fastethernet", "").replace("fa", "");
                    if (num.startsWith("/")) num = "0" + num; // Handle "fa/1" -> "fa0/1"
                    num = num.replaceAll("/0+", "/"); // Handle "fa0/01" -> "fa0/1"
                    iface = "FastEthernet" + num;
                } else if (lowerIface.startsWith("gi") || lowerIface.startsWith("gigabitethernet")) {
                    String num = lowerIface.replace("gigabitethernet", "").replace("gi", "");
                    if (num.startsWith("/")) num = "0" + num;
                    num = num.replaceAll("/0+", "/");
                    iface = "GigabitEthernet" + num;
                }

                if (lowerIface.startsWith("vlan")) {
                    cliMode = CliMode.CONFIG_IF; cliTarget = "VLAN" + lowerIface.substring(4).trim();
                } else if (portConfigs.containsKey(iface)) {
                    cliMode = CliMode.CONFIG_IF; cliTarget = iface;
                } else if (echo) appendInvalidMarker(input, tokens[1]);
            }
            else if (first.equals("vlan")) {
                if (tokens.length == 1) { cliLines.add("% Incomplete command."); return; }
                cliMode = CliMode.CONFIG_VLAN; cliTarget = tokens[1];
                vlanDatabase.putIfAbsent(cliTarget, "VLAN" + cliTarget);
            }
            else if (first.equals("spanning-tree")) {
                if (tokens.length > 2 && tokens[1].equals("mode")) stpMode = tokens[2];
                else if (tokens.length > 4 && tokens[1].equals("vlan") && tokens[3].equals("root") && tokens[4].equals("primary")) {
                    stpPriority = 24576; // Force root priority
                    isStpRoot = true;
                }
                else if (tokens.length > 4 && tokens[1].equals("vlan") && tokens[3].equals("root") && tokens[4].equals("secondary")) {
                    stpPriority = 28672;
                }
                else if (tokens.length > 4 && tokens[1].equals("vlan") && tokens[3].equals("priority")) {
                    try { stpPriority = Integer.parseInt(tokens[4]); } catch (Exception ignored) {}
                }
            }
            else if (first.equals("hostname")) {
                if (tokens.length > 1) switchHostname = cmd.substring(tokens[0].length()).trim();
                else cliLines.add("% Incomplete command.");
            }
            else if (first.equals("exit") || first.equals("end")) { cliMode = CliMode.PRIVILEGED; }
            else if (echo) appendInvalidMarker(input, tokens[0]);
        }
        else if (executionMode == CliMode.CONFIG_IF) {
            if (first.equals("exit")) { cliMode = CliMode.CONFIG; return; }
            if (first.equals("end")) { cliMode = CliMode.PRIVILEGED; return; }

            if (cliTarget.startsWith("VLAN")) {
                if (first.equals("ip") && tokens.length >= 3 && "address".startsWith(tokens[1])) {
                    managementIp = tokens[2];
                    managementMask = tokens.length > 3 ? tokens[3] : "255.255.255.0";
                }
            } else {
                PortConfig pc = portConfigs.get(cliTarget);
                if (pc != null) {
                    if (first.equals("shutdown")) pc.up = false;
                    else if (first.equals("no") && tokens.length > 1 && "shutdown".startsWith(tokens[1])) pc.up = true;
                    else if (first.equals("switchport")) {
                        if (tokens.length == 1) { cliLines.add("% Incomplete command."); return; }
                        String sub = resolveAlias(tokens[1], executionMode, first);
                        if (sub != null && sub.equals("mode") && tokens.length > 2) pc.mode = tokens[2];
                        else if (sub != null && sub.equals("access") && tokens.length > 3) pc.accessVlan = tokens[3];
                        else if (sub != null && sub.equals("trunk") && tokens.length > 4) pc.allowedVlans = tokens[4];
                    }
                    else if (first.equals("spanning-tree")) {
                        if (tokens.length > 1 && tokens[1].equals("portfast")) {
                            pc.portfast = true;
                            if (echo) cliLines.add("%Warning: portfast should only be enabled on ports connected to a single host.");
                        } else if (tokens.length > 2 && tokens[1].equals("cost")) {
                            try { pc.stpCost = Integer.parseInt(tokens[2]); } catch (Exception ignored) {}
                        }
                    }
                }
            }
        }
        else if (executionMode == CliMode.CONFIG_VLAN) {
            if (first.equals("name") && tokens.length > 1) vlanDatabase.put(cliTarget, cmd.substring(tokens[0].length()).trim());
            else if (first.equals("exit")) cliMode = CliMode.CONFIG;
            else if (first.equals("end")) cliMode = CliMode.PRIVILEGED;
        }

        if (onStateChange != null) onStateChange.run();
        if (guiCallback != null) guiCallback.run();
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
        if (prefix.startsWith("do ") && (mode == CliMode.CONFIG || mode == CliMode.CONFIG_IF || mode == CliMode.CONFIG_VLAN)) {
            String subPrefix = prefix.substring(3);
            String[] subOpts = getOptionsForPrefix(subPrefix, CliMode.PRIVILEGED);
            for (int i = 0; i < subOpts.length; i++) subOpts[i] = "do " + subOpts[i];
            return subOpts;
        }

        List<String> matches = new ArrayList<>();
        List<String> allContexts = new ArrayList<>();

        if (mode == CliMode.EXEC) {
            allContexts.addAll(List.of("enable", "ping", "show version", "show running-config", "show ip interface brief", "show mac address-table", "show vlan brief", "show spanning-tree", "exit", "logout", "help"));
        } else if (mode == CliMode.PRIVILEGED) {
            allContexts.addAll(List.of("configure terminal", "disable", "exit", "write memory", "copy running-config startup-config", "show version", "show running-config", "show ip interface brief", "show ip default-gateway", "show interface vlan 1", "show interfaces vlan 1", "show history", "show mac address-table", "show vlan brief", "show spanning-tree", "ping", "help"));
        } else if (mode == CliMode.CONFIG) {
            allContexts.addAll(List.of("interface FastEthernet", "interface GigabitEthernet", "interface vlan", "vlan", "hostname", "ip default-gateway", "no ip default-gateway", "spanning-tree mode pvst", "spanning-tree mode rapid-pvst", "spanning-tree vlan", "exit", "end", "do", "help"));
        } else if (mode == CliMode.CONFIG_IF) {
            allContexts.addAll(List.of("ip address", "no ip address", "description", "speed", "duplex", "shutdown", "no shutdown", "switchport mode access", "switchport mode trunk", "switchport access vlan", "switchport trunk allowed vlan", "spanning-tree portfast", "spanning-tree cost", "exit", "end", "do", "help"));
        } else if (mode == CliMode.CONFIG_VLAN) {
            allContexts.addAll(List.of("name", "exit", "end", "do", "help"));
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
        cliLines.add("Current configuration : 1102 bytes");
        cliLines.add("!");
        cliLines.add("version 15.0");
        cliLines.add("hostname " + switchHostname);
        cliLines.add("!");
        cliLines.add("spanning-tree mode " + stpMode);
        cliLines.add("spanning-tree extend system-id");
        if (stpPriority != 32768) {
            cliLines.add("spanning-tree vlan 1 priority " + stpPriority);
        }
        cliLines.add("!");
        for (Map.Entry<String, PortConfig> entry : portConfigs.entrySet()) {
            cliLines.add("interface " + entry.getKey());
            PortConfig pc = entry.getValue();
            if (!pc.description.isEmpty()) cliLines.add(" description " + pc.description);
            if (!pc.mode.equals("dynamic auto")) cliLines.add(" switchport mode " + pc.mode);
            if (pc.mode.equals("access") && !pc.accessVlan.equals("1")) cliLines.add(" switchport access vlan " + pc.accessVlan);
            if (pc.mode.equals("trunk") && !pc.allowedVlans.equals("1-4094")) cliLines.add(" switchport trunk allowed vlan " + pc.allowedVlans);
            if (pc.portfast) cliLines.add(" spanning-tree portfast");
            if (!pc.up) cliLines.add(" shutdown");
            cliLines.add("!");
        }
        cliLines.add("interface Vlan1");
        if (!managementIp.equals("unassigned")) cliLines.add(" ip address " + managementIp + " " + managementMask);
        else cliLines.add(" no ip address");
        cliLines.add("!");
        if (!managementDefaultGateway.equals("unassigned")) {
            cliLines.add("ip default-gateway " + managementDefaultGateway);
            cliLines.add("!");
        }
        cliLines.add("end");
    }

    private void runShowSpanningTree(boolean echo) {
        if (!echo) return;
        int activePriority = stpPriority + 1; // Priority + sys-id-ext (VLAN 1)

        cliLines.add("VLAN0001");
        cliLines.add("  Spanning tree enabled protocol ieee");

        String[] rootIdParts = rootBridgeId.split(":");
        String dispRootMac = rootIdParts.length > 1 ? rootBridgeId.substring(rootBridgeId.indexOf(":") + 1) : "00:00:00:00:00:00";
        String dispRootPrio = rootIdParts[0];

        cliLines.add("  Root ID    Priority    " + dispRootPrio);
        cliLines.add("             Address     " + dispRootMac);
        if (isStpRoot) {
            cliLines.add("             This bridge is the root");
        } else {
            cliLines.add("             Cost        " + rootPathCost);
            cliLines.add("             Port        " + rootPort);
        }
        cliLines.add("             Hello Time   2 sec  Max Age 20 sec  Forward Delay 15 sec");
        cliLines.add("");
        cliLines.add("  Bridge ID  Priority    " + activePriority + "  (priority " + stpPriority + " sys-id-ext 1)");
        cliLines.add("             Address     " + macAddress);
        cliLines.add("             Hello Time   2 sec  Max Age 20 sec  Forward Delay 15 sec");
        cliLines.add("");

        cliLines.add("Interface\tRole\tSts\tCost\tPrio.Nbr\tType");
        cliLines.add("---------\t----\t---\t----\t--------\t----");
        for (Map.Entry<String, PortConfig> entry : portConfigs.entrySet()) {
            if (entry.getValue().up) {
                cliLines.add(entry.getKey().replace("FastEthernet", "Fa").replace("GigabitEthernet", "Gi") + "\t" + entry.getValue().stpRole + "\t" + entry.getValue().stpState + "\t" + entry.getValue().stpCost + "\t128.1\t" + (entry.getValue().portfast ? "Edge P2p" : "P2p"));
            }
        }
    }

    private void runShowIpIntBrief(boolean echo) {
        if (!echo) return;
        cliLines.add("Interface\tIP-Address\tOK? Method\tStatus\tProtocol");
        cliLines.add("Vlan1\t" + managementIp + "\tYES manual\tup\tup");
        for (Map.Entry<String, PortConfig> entry : portConfigs.entrySet()) {
            PortConfig pc = entry.getValue();
            String status = pc.up ? "up" : "admin down";
            cliLines.add(entry.getKey().replace("FastEthernet", "Fa").replace("GigabitEthernet", "Gi") + "\tunassigned\tYES unset\t" + status + "\t" + (pc.up ? "up" : "down"));
        }
    }

    private void runShowMac(boolean echo) {
        if (!echo) return;
        cliLines.add("          Mac Address Table");
        cliLines.add("-------------------------------------------");
        cliLines.add("Vlan\tMac Address\tType\tPorts");
        cliLines.add("----\t-----------\t--------\t-----");
        for (Map.Entry<String, String> entry : macTable.entrySet()) {
            String port = entry.getValue();
            String vlan = portConfigs.containsKey(port) ? portConfigs.get(port).accessVlan : "1";
            cliLines.add(vlan + "\t" + entry.getKey() + "\tDYNAMIC\t" + port.replace("FastEthernet", "Fa").replace("GigabitEthernet", "Gi"));
        }
    }

    private void runShowVlan(boolean echo) {
        if (!echo) return;
        cliLines.add("VLAN\tName\tStatus\tPorts");
        cliLines.add("----\t----\t------\t-----");
        for (Map.Entry<String, String> v : vlanDatabase.entrySet()) {
            StringBuilder ports = new StringBuilder();
            for (Map.Entry<String, PortConfig> p : portConfigs.entrySet()) {
                if (p.getValue().accessVlan.equals(v.getKey()) && !p.getValue().mode.equals("trunk")) {
                    if (ports.length() > 0) ports.append(", ");
                    ports.append(p.getKey().replace("FastEthernet", "Fa").replace("GigabitEthernet", "Gi"));
                }
            }

            String pStr = ports.toString();
            if (pStr.length() > 50) {
                cliLines.add(v.getKey() + "\t" + v.getValue() + "\tactive\t" + pStr.substring(0, 50));
                pStr = pStr.substring(50);
                while (pStr.length() > 50) {
                    cliLines.add("\t\t\t" + pStr.substring(0, 50));
                    pStr = pStr.substring(50);
                }
                if (!pStr.isEmpty()) cliLines.add("\t\t\t" + pStr);
            } else {
                cliLines.add(v.getKey() + "\t" + v.getValue() + "\tactive\t" + pStr);
            }
        }
    }

    private void runShowVersion(boolean echo) {
        if (!echo) return;
        cliLines.add("Cisco IOS Software, C2960 Software (C2960-LANBASEK9-M), Version 15.0(2)EZ1");
        cliLines.add("24 FastEthernet interfaces");
        cliLines.add("2 Gigabit Ethernet interfaces");
    }

    private void runPing(String target, boolean echo) {
        if (!echo) return;
        cliLines.add("Type escape sequence to abort.");
        cliLines.add("Sending 5, 100-byte ICMP Echos to " + target + ", timeout is 2 seconds:");
        cliLines.add("!!!!!");
        cliLines.add("Success rate is 100 percent (5/5), round-trip min/avg/max = 1/2/4 ms");
    }

    public CompoundTag saveToNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Hostname", switchHostname);
        tag.putString("ManagementIp", managementIp);
        tag.putString("ManagementMask", managementMask);
        tag.putString("ManagementDefaultGateway", managementDefaultGateway);
        tag.putString("MacAddress", macAddress);
        tag.putString("StpMode", stpMode);
        tag.putInt("StpPriority", stpPriority);
        tag.putBoolean("IsStpRoot", isStpRoot);

        CompoundTag ports = new CompoundTag();
        for (Map.Entry<String, PortConfig> entry : portConfigs.entrySet()) ports.put(entry.getKey(), entry.getValue().save());
        tag.put("Ports", ports);

        CompoundTag vlans = new CompoundTag();
        for (Map.Entry<String, String> entry : vlanDatabase.entrySet()) vlans.putString(entry.getKey(), entry.getValue());
        tag.put("Vlans", vlans);

        CompoundTag macs = new CompoundTag();
        for (Map.Entry<String, String> entry : macTable.entrySet()) macs.putString(entry.getKey(), entry.getValue());
        tag.put("MacTable", macs);

        return tag;
    }

    public void loadFromNBT(CompoundTag tag) {
        if (tag.contains("Hostname")) switchHostname = tag.getString("Hostname");
        if (tag.contains("ManagementIp")) managementIp = tag.getString("ManagementIp");
        if (tag.contains("ManagementMask")) managementMask = tag.getString("ManagementMask");
        if (tag.contains("ManagementDefaultGateway")) managementDefaultGateway = tag.getString("ManagementDefaultGateway");
        if (tag.contains("MacAddress")) macAddress = tag.getString("MacAddress");
        if (tag.contains("StpMode")) stpMode = tag.getString("StpMode");
        if (tag.contains("StpPriority")) stpPriority = tag.getInt("StpPriority");
        if (tag.contains("IsStpRoot")) isStpRoot = tag.getBoolean("IsStpRoot");

        if (tag.contains("Ports")) {
            CompoundTag ports = tag.getCompound("Ports");
            for (String key : ports.getAllKeys()) {
                if (portConfigs.containsKey(key)) portConfigs.get(key).load(ports.getCompound(key));
            }
        }

        if (tag.contains("Vlans")) {
            vlanDatabase.clear();
            CompoundTag vlans = tag.getCompound("Vlans");
            for (String key : vlans.getAllKeys()) vlanDatabase.put(key, vlans.getString(key));
        }

        if (tag.contains("MacTable")) {
            macTable.clear();
            macTableAge.clear();
            CompoundTag macs = tag.getCompound("MacTable");
            long now = System.currentTimeMillis();
            for (String key : macs.getAllKeys()) {
                macTable.put(key, macs.getString(key));
                macTableAge.put(key, now);
            }
        }
    }
}
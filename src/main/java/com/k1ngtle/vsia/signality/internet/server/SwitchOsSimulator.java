package com.k1ngtle.vsia.signality.internet.server;

import com.k1ngtle.vsia.signality.internet.OSINetworkPacket;
import net.minecraft.nbt.CompoundTag;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SwitchOsSimulator {

    public enum CliMode {
        EXEC, PRIVILEGED, CONFIG, CONFIG_IF, CONFIG_VLAN
    }

    public static class PortConfig {
        public boolean up = true;
        public String speed = "Auto";
        public String duplex = "Auto";
        public String accessVlan = "1";
        public String txRingLimit = "10";

        public CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putBoolean("Up", up);
            tag.putString("Speed", speed);
            tag.putString("Duplex", duplex);
            tag.putString("AccessVlan", accessVlan);
            tag.putString("TxRingLimit", txRingLimit);
            return tag;
        }

        public void load(CompoundTag tag) {
            up = tag.getBoolean("Up");
            speed = tag.getString("Speed");
            duplex = tag.getString("Duplex");
            accessVlan = tag.getString("AccessVlan");
            txRingLimit = tag.getString("TxRingLimit");
        }
    }

    public int id;
    public String switchHostname;
    public String managementIp = "unassigned";
    public String managementMask = "unassigned";
    public String macAddress;

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

    public SwitchOsSimulator(int id, String initialHostname, Runnable onStateChange) {
        this.id = id;
        this.switchHostname = initialHostname;
        this.macAddress = String.format("00:1A:2B:3C:4D:%02X", 0x5E + id);
        this.onStateChange = onStateChange;

        for (int i = 1; i <= 24; i++) portConfigs.put("FastEthernet0/" + i, new PortConfig());
        portConfigs.put("GigabitEthernet0/1", new PortConfig());
        portConfigs.put("GigabitEthernet0/2", new PortConfig());

        vlanDatabase.put("1", "default");
        vlanDatabase.put("1002", "fddi-default");
        vlanDatabase.put("1003", "token-ring-default");
        vlanDatabase.put("1004", "fddinet-default");
        vlanDatabase.put("1005", "trnet-default");

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

    public List<String> processAndForwardPacket(OSINetworkPacket packet, String ingressPortName) {
        List<String> egressPorts = new ArrayList<>();
        PortConfig ingressPort = portConfigs.get(ingressPortName);

        if (ingressPort == null || !ingressPort.up) return egressPorts;

        if (packet.sourceMac != null && !packet.sourceMac.isEmpty()) {
            macTable.put(packet.sourceMac, ingressPortName);
            macTableAge.put(packet.sourceMac, System.currentTimeMillis());
            if (onStateChange != null) onStateChange.run();
        }

        long now = System.currentTimeMillis();
        macTableAge.entrySet().removeIf(entry -> {
            boolean stale = now - entry.getValue() > 300000;
            if (stale) macTable.remove(entry.getKey());
            return stale;
        });

        String targetMac = packet.targetMac;
        String vlan = ingressPort.accessVlan;
        boolean isBroadcast = targetMac == null || targetMac.isEmpty() || targetMac.equalsIgnoreCase("FF:FF:FF:FF:FF:FF") || targetMac.equalsIgnoreCase("00:00:00:00:00:00");

        if (isBroadcast) {
            for (Map.Entry<String, PortConfig> entry : portConfigs.entrySet()) {
                if (entry.getValue().up && entry.getValue().accessVlan.equals(vlan) && !entry.getKey().equals(ingressPortName)) {
                    egressPorts.add(entry.getKey());
                }
            }
        } else {
            String knownEgressPort = macTable.get(targetMac);
            if (knownEgressPort != null) {
                PortConfig egressPort = portConfigs.get(knownEgressPort);
                if (egressPort != null && egressPort.up && egressPort.accessVlan.equals(vlan) && !knownEgressPort.equals(ingressPortName)) {
                    egressPorts.add(knownEgressPort);
                }
            } else {
                for (Map.Entry<String, PortConfig> entry : portConfigs.entrySet()) {
                    if (entry.getValue().up && entry.getValue().accessVlan.equals(vlan) && !entry.getKey().equals(ingressPortName)) {
                        egressPorts.add(entry.getKey());
                    }
                }
            }
        }
        return egressPorts;
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

    public void handleAutocomplete() {
        if (cliInput.isEmpty()) return;
        String lower = cliInput.toLowerCase();

        if (cliMode == CliMode.CONFIG && (lower.startsWith("int ") || lower.startsWith("interface "))) {
            String[] parts = cliInput.split(" ", -1);
            if (parts.length == 2 && !parts[1].isEmpty()) {
                String iface = parts[1].toLowerCase();
                if (iface.startsWith("fa")) cliInput = "interface FastEthernet" + cliInput.substring(cliInput.toLowerCase().indexOf("fa") + 2);
                else if (iface.startsWith("gi")) cliInput = "interface GigabitEthernet" + cliInput.substring(cliInput.toLowerCase().indexOf("gi") + 2);
                else if (iface.startsWith("vl")) cliInput = "interface VLAN" + cliInput.substring(cliInput.toLowerCase().indexOf("vl") + 2);
                else if (!lower.startsWith("interface ")) cliInput = "interface " + parts[1];
                cliCursorPos = cliInput.length();
                if (onStateChange != null) onStateChange.run();
                return;
            } else if (parts.length == 2 && parts[1].isEmpty()) {
                cliInput = "interface ";
                cliCursorPos = cliInput.length();
                if (onStateChange != null) onStateChange.run();
                return;
            }
        }

        String[] tokens = lower.split(" ", -1);
        if (tokens.length == 1) {
            String[] options = new String[0];
            if (cliMode == CliMode.EXEC) options = new String[]{"enable", "ping", "help", "exit"};
            else if (cliMode == CliMode.PRIVILEGED) options = new String[]{"configure", "disable", "exit", "write", "show", "help"};
            else if (cliMode == CliMode.CONFIG) options = new String[]{"interface", "vlan", "hostname", "exit", "help"};
            else if (cliMode == CliMode.CONFIG_IF) {
                if (cliTarget.startsWith("VLAN")) options = new String[]{"ip", "exit", "help"};
                else options = new String[]{"speed", "duplex", "shutdown", "no", "switchport", "tx-ring-limit", "exit", "help"};
            }
            else if (cliMode == CliMode.CONFIG_VLAN) options = new String[]{"name", "exit", "help"};

            String match = findCommonPrefix(tokens[0], options);
            if (match != null && match.length() > tokens[0].length()) {
                cliInput = match + " ";
                cliCursorPos = cliInput.length();
                if (onStateChange != null) onStateChange.run();
            }
        } else if (tokens.length == 2) {
            String first = tokens[0];
            String second = tokens[1];
            String[] options = new String[0];

            if (cliMode == CliMode.PRIVILEGED) {
                if ("configure".startsWith(first)) options = new String[]{"terminal"};
                else if ("write".startsWith(first)) options = new String[]{"memory"};
                else if ("show".startsWith(first)) options = new String[]{"version", "ip", "mac", "vlan"};
            } else if (cliMode == CliMode.CONFIG_IF) {
                if ("no".startsWith(first)) options = new String[]{"shutdown"};
                else if ("switchport".startsWith(first)) options = new String[]{"access"};
                else if ("ip".startsWith(first)) options = new String[]{"address"};
            }

            String match = findCommonPrefix(second, options);
            if (match != null && match.length() > second.length()) {
                String expFirst = first;
                if (cliMode == CliMode.PRIVILEGED) {
                    if ("configure".startsWith(first)) expFirst = "configure";
                    else if ("write".startsWith(first)) expFirst = "write";
                    else if ("show".startsWith(first)) expFirst = "show";
                } else if (cliMode == CliMode.CONFIG_IF) {
                    if ("no".startsWith(first)) expFirst = "no";
                    else if ("switchport".startsWith(first)) expFirst = "switchport";
                    else if ("ip".startsWith(first)) expFirst = "ip";
                }
                cliInput = expFirst + " " + match + " ";
                cliCursorPos = cliInput.length();
                if (onStateChange != null) onStateChange.run();
            }
        } else if (tokens.length == 3) {
            String first = tokens[0];
            String second = tokens[1];
            String third = tokens[2];
            String[] options = new String[0];

            if (cliMode == CliMode.PRIVILEGED && "show".startsWith(first)) {
                if ("ip".startsWith(second)) options = new String[]{"interface"};
                else if ("mac".startsWith(second)) options = new String[]{"address-table"};
            } else if (cliMode == CliMode.CONFIG_IF && "switchport".startsWith(first) && "access".startsWith(second)) {
                options = new String[]{"vlan"};
            }

            String match = findCommonPrefix(third, options);
            if (match != null && match.length() > third.length()) {
                String expFirst = first; String expSecond = second;
                if (cliMode == CliMode.PRIVILEGED) { expFirst = "show"; if ("ip".startsWith(second)) expSecond = "ip"; else if ("mac".startsWith(second)) expSecond = "mac"; }
                else if (cliMode == CliMode.CONFIG_IF) { expFirst = "switchport"; expSecond = "access"; }

                cliInput = expFirst + " " + expSecond + " " + match + " ";
                cliCursorPos = cliInput.length();
                if (onStateChange != null) onStateChange.run();
            }
        } else if (tokens.length == 4) {
            String first = tokens[0]; String second = tokens[1]; String third = tokens[2]; String fourth = tokens[3];
            if (cliMode == CliMode.PRIVILEGED && "show".startsWith(first) && "ip".startsWith(second) && "interface".startsWith(third)) {
                String match = findCommonPrefix(fourth, new String[]{"brief"});
                if (match != null && match.length() > fourth.length()) {
                    cliInput = "show ip interface " + match + " ";
                    cliCursorPos = cliInput.length();
                    if (onStateChange != null) onStateChange.run();
                }
            }
        }
    }

    private String findCommonPrefix(String input, String[] options) {
        String common = null;
        for (String opt : options) {
            if (opt.startsWith(input)) {
                if (common == null) common = opt;
                else {
                    int i = 0;
                    while (i < common.length() && i < opt.length() && common.charAt(i) == opt.charAt(i)) i++;
                    common = common.substring(0, i);
                }
            }
        }
        return common;
    }

    public void appendGuiCommand(String command, String selectedConfigItem) {
        String pre = "Switch(config";
        if (portConfigs.containsKey(selectedConfigItem)) pre += "-if)# ";
        else if (selectedConfigItem.equals("VLAN Database")) pre += "-vlan)# ";
        else pre += ")# ";

        iosCommands.add(pre + command);
        if (iosCommands.size() > 10) iosCommands.remove(0);
        executeCliCore(command, false);
    }

    public void executeCliCore(String input, boolean echo) {
        if (input.endsWith("?")) {
            if (echo) { cliLines.add(getPrompt() + input); cliScrollOffset = 0; }
            String prefix = input.substring(0, input.length() - 1);
            boolean spaceBefore = prefix.endsWith(" ");
            prefix = prefix.trim().toLowerCase();

            if (prefix.isEmpty()) {
                showRootHelp();
            } else if (spaceBefore) {
                showContextHelp(prefix);
            } else {
                showPrefixHelp(prefix);
            }
            if (onStateChange != null) onStateChange.run();
            return;
        }

        String cmd = input.trim();
        if (cmd.isEmpty() && echo) { cliLines.add(getPrompt()); cliScrollOffset = 0; return; }

        String lower = cmd.toLowerCase();
        if (lower.equals("help")) {
            if (echo) { cliLines.add(getPrompt() + cmd); cliScrollOffset = 0; }
            showRootHelp();
            if (onStateChange != null) onStateChange.run();
            return;
        }

        if (echo) { cliLines.add(getPrompt() + cmd); cliScrollOffset = 0; }

        if (cliMode == CliMode.EXEC) {
            if (lower.equals("en") || lower.equals("enable")) cliMode = CliMode.PRIVILEGED;
            else if (lower.startsWith("ping ")) {
                if (echo) {
                    cliLines.add("Type escape sequence to abort.");
                    cliLines.add("Sending 5, 100-byte ICMP Echos to " + cmd.substring(5) + ", timeout is 2 seconds:");
                    cliLines.add("!!!!!");
                    cliLines.add("Success rate is 100 percent (5/5), round-trip min/avg/max = 1/2/4 ms");
                }
            }
            else if (echo && !lower.isEmpty()) cliLines.add("% Invalid input detected");

        } else if (cliMode == CliMode.PRIVILEGED) {
            if (lower.equals("conf t") || lower.equals("configure terminal")) cliMode = CliMode.CONFIG;
            else if (lower.equals("disable") || lower.equals("exit")) cliMode = CliMode.EXEC;
            else if (lower.equals("write memory") || lower.equals("wr")) {
                if (echo) { cliLines.add("Building configuration..."); cliLines.add("[OK]"); }
            }
            else if (lower.startsWith("show version") && echo) {
                cliLines.add("Cisco IOS Software, C2960 Software (C2960-LANBASEK9-M), Version 15.0(2)EZ1");
                cliLines.add("24 FastEthernet interfaces");
                cliLines.add("2 Gigabit Ethernet interfaces");
            }
            else if (lower.equals("show ip int brief") || lower.equals("show ip interface brief")) {
                if (echo) {
                    cliLines.add("Interface              IP-Address      OK? Method Status                Protocol");
                    cliLines.add(String.format("%-22s %-15s YES manual %-21s %s", "Vlan1", managementIp, "up", "up"));
                    for (Map.Entry<String, PortConfig> entry : portConfigs.entrySet()) {
                        PortConfig pc = entry.getValue();
                        String stat = pc.up ? "up" : "administratively down";
                        String prot = pc.up ? "up" : "down";
                        cliLines.add(String.format("%-22s %-15s YES unset  %-21s %s", entry.getKey().replace("Ethernet", "Eth"), "unassigned", stat, prot));
                    }
                }
            }
            else if (lower.startsWith("show mac address-table") || lower.startsWith("show mac-address-table")) {
                if (echo) {
                    cliLines.add("          Mac Address Table");
                    cliLines.add("-------------------------------------------");
                    cliLines.add("Vlan    Mac Address       Type        Ports");
                    cliLines.add("----    -----------       --------    -----");
                    for (Map.Entry<String, String> entry : macTable.entrySet()) {
                        String mac = entry.getKey();
                        String port = entry.getValue();
                        String vlan = portConfigs.containsKey(port) ? portConfigs.get(port).accessVlan : "1";
                        cliLines.add(String.format("%-7s %-17s %-11s %s", vlan, mac, "DYNAMIC", port.replace("Ethernet", "Eth")));
                    }
                    if (macTable.isEmpty()) cliLines.add("No MAC addresses learned.");
                }
            }
            else if (lower.startsWith("show vlan") && echo) {
                cliLines.add("VLAN Name                             Status    Ports");
                cliLines.add("---- -------------------------------- --------- -------------------------------");
                for (Map.Entry<String, String> v : vlanDatabase.entrySet()) {
                    StringBuilder ports = new StringBuilder();
                    for (Map.Entry<String, PortConfig> p : portConfigs.entrySet()) {
                        if (p.getValue().accessVlan.equals(v.getKey())) {
                            if (ports.length() > 0) ports.append(", ");
                            ports.append(p.getKey().replace("Ethernet", "Eth"));
                        }
                    }
                    cliLines.add(String.format("%-4s %-32s active    %s", v.getKey(), v.getValue(), ports.toString()));
                }
            }
            else if (echo && !lower.isEmpty()) cliLines.add("% Invalid input detected");

        } else if (cliMode == CliMode.CONFIG) {
            if (lower.startsWith("int ") || lower.startsWith("interface ")) {
                String iface = cmd.substring(lower.startsWith("int ") ? 4 : 10).trim();
                if (iface.toLowerCase().startsWith("fa")) iface = "FastEthernet" + iface.substring(2);
                if (iface.toLowerCase().startsWith("gi")) iface = "GigabitEthernet" + iface.substring(2);

                if (iface.toLowerCase().startsWith("vlan")) {
                    cliMode = CliMode.CONFIG_IF; cliTarget = "VLAN" + iface.substring(4).trim();
                } else if (portConfigs.containsKey(iface)) {
                    cliMode = CliMode.CONFIG_IF; cliTarget = iface;
                } else if (echo) cliLines.add("% Invalid interface");
            }
            else if (lower.startsWith("vlan ")) {
                cliMode = CliMode.CONFIG_VLAN; cliTarget = cmd.substring(5).trim();
                vlanDatabase.putIfAbsent(cliTarget, "VLAN" + cliTarget);
            }
            else if (lower.startsWith("hostname ")) { switchHostname = cmd.substring(9).trim(); }
            else if (lower.equals("exit")) { cliMode = CliMode.PRIVILEGED; }
            else if (echo && !lower.isEmpty()) cliLines.add("% Invalid input detected");

        } else if (cliMode == CliMode.CONFIG_IF) {
            if (cliTarget.startsWith("VLAN")) {
                if (lower.startsWith("ip address ")) {
                    String[] parts = lower.substring(11).trim().split("\\s+");
                    if (parts.length >= 2) { managementIp = parts[0]; managementMask = parts[1]; }
                } else if (lower.equals("exit")) cliMode = CliMode.CONFIG;
                else if (echo && !lower.isEmpty()) cliLines.add("% Invalid input detected");
            } else {
                PortConfig pc = portConfigs.get(cliTarget);
                if (pc != null) {
                    if (lower.startsWith("speed ")) pc.speed = lower.substring(6).trim();
                    else if (lower.startsWith("duplex ")) pc.duplex = lower.substring(7).trim();
                    else if (lower.equals("shutdown")) pc.up = false;
                    else if (lower.equals("no shutdown")) pc.up = true;
                    else if (lower.startsWith("switchport access vlan ")) pc.accessVlan = lower.substring(23).trim();
                    else if (lower.startsWith("tx-ring-limit ")) pc.txRingLimit = lower.substring(14).trim();
                    else if (lower.equals("exit")) cliMode = CliMode.CONFIG;
                    else if (echo && !lower.isEmpty()) cliLines.add("% Invalid input detected");
                }
            }
        } else if (cliMode == CliMode.CONFIG_VLAN) {
            if (lower.startsWith("name ")) vlanDatabase.put(cliTarget, cmd.substring(5).trim());
            else if (lower.equals("exit")) cliMode = CliMode.CONFIG;
            else if (echo && !lower.isEmpty()) cliLines.add("% Invalid input detected");
        }

        if (onStateChange != null) onStateChange.run();
    }

    private void showRootHelp() {
        if (cliMode == CliMode.EXEC) {
            cliLines.add("Exec commands:");
            cliLines.add("  enable  Turn on privileged commands");
            cliLines.add("  ping    Send echo messages");
            cliLines.add("  help    Description of the interactive help system");
            cliLines.add("  exit    Exit from the EXEC");
        } else if (cliMode == CliMode.PRIVILEGED) {
            cliLines.add("Privileged Exec commands:");
            cliLines.add("  configure  Enter configuration mode");
            cliLines.add("  disable    Turn off privileged commands");
            cliLines.add("  exit       Exit from the EXEC");
            cliLines.add("  show       Show running system information");
            cliLines.add("  write      Write running configuration to memory");
        } else if (cliMode == CliMode.CONFIG) {
            cliLines.add("Configure commands:");
            cliLines.add("  exit       Exit from configure mode");
            cliLines.add("  hostname   Set system's network name");
            cliLines.add("  interface  Select an interface to configure");
            cliLines.add("  vlan       VLAN commands");
        } else if (cliMode == CliMode.CONFIG_IF) {
            if (cliTarget.startsWith("VLAN")) {
                cliLines.add("Interface configuration commands:");
                cliLines.add("  exit        Exit from interface configuration mode");
                cliLines.add("  ip          Interface Internet Protocol config commands");
            } else {
                cliLines.add("Interface configuration commands:");
                cliLines.add("  duplex      Configure duplex operation.");
                cliLines.add("  exit        Exit from interface configuration mode");
                cliLines.add("  shutdown    Shutdown the selected interface");
                cliLines.add("  no          Negate a command or set its defaults");
                cliLines.add("  speed       Configure speed operation.");
                cliLines.add("  switchport  Set switching mode characteristics");
                cliLines.add("  tx-ring-limit Configure transmit ring limit");
            }
        } else if (cliMode == CliMode.CONFIG_VLAN) {
            cliLines.add("VLAN configuration commands:");
            cliLines.add("  exit        Exit from VLAN configuration mode");
            cliLines.add("  name        Ascii name of the VLAN");
        }
    }

    private void showContextHelp(String prefix) {
        if (prefix.equals("show")) {
            cliLines.add("  ip        IP information");
            cliLines.add("  mac       MAC address table");
            cliLines.add("  vlan      VLAN status");
            cliLines.add("  version   System hardware and software status");
            cliLines.add("Example: show ip int brief");
        } else if (prefix.equals("show ip")) {
            cliLines.add("  interface  IP interface status and configuration");
        } else if (prefix.equals("show ip interface") || prefix.equals("show ip int")) {
            cliLines.add("  brief      Brief summary of IP status and configuration");
        } else if (prefix.equals("show mac")) {
            cliLines.add("  address-table  MAC forwarding table");
        } else if (prefix.equals("configure") || prefix.equals("conf")) {
            cliLines.add("  terminal  Configure from the terminal");
        } else if (prefix.equals("interface") || prefix.equals("int")) {
            cliLines.add("  FastEthernet      FastEthernet IEEE 802.3");
            cliLines.add("  GigabitEthernet   GigabitEthernet IEEE 802.3z");
            cliLines.add("  Vlan              Catalyst Vlans");
            cliLines.add("Example: interface fa0/1");
        } else if (prefix.equals("ip")) {
            cliLines.add("  address  Set the IP address of an interface");
            cliLines.add("Example: ip address 192.168.1.10 255.255.255.0");
        } else if (prefix.equals("switchport")) {
            cliLines.add("  access  Set access mode characteristics of the interface");
        } else if (prefix.equals("switchport access")) {
            cliLines.add("  vlan  Set VLAN when interface is in access mode");
        } else if (prefix.equals("ping")) {
            cliLines.add("  WORD  Ping destination address or hostname");
        } else if (prefix.equals("hostname")) {
            cliLines.add("  WORD  This system's network name");
        } else if (prefix.equals("vlan")) {
            cliLines.add("  <1-4094>  VLAN ID");
        } else if (prefix.equals("name")) {
            cliLines.add("  WORD  Ascii name of the VLAN");
        } else if (prefix.equals("speed")) {
            cliLines.add("  10    Force 10 Mbps operation");
            cliLines.add("  100   Force 100 Mbps operation");
            cliLines.add("  1000  Force 1000 Mbps operation");
            cliLines.add("  auto  Enable AUTO speed configuration");
        } else if (prefix.equals("duplex")) {
            cliLines.add("  auto  Enable AUTO duplex configuration");
            cliLines.add("  full  Force full duplex operation");
            cliLines.add("  half  Force half-duplex operation");
        } else {
            cliLines.add("% Unrecognized command");
        }
    }

    private void showPrefixHelp(String prefix) {
        List<String> matches = new ArrayList<>();
        String[] execOpts = {"enable", "ping", "help", "exit"};
        String[] privOpts = {"configure", "disable", "exit", "write", "show", "help"};
        String[] confOpts = {"interface", "vlan", "hostname", "exit", "help"};
        String[] ifOpts = {"speed", "duplex", "shutdown", "no", "switchport", "tx-ring-limit", "exit", "help", "ip"};
        String[] vlanOpts = {"name", "exit", "help"};

        String[] toCheck = switch(cliMode) {
            case EXEC -> execOpts;
            case PRIVILEGED -> privOpts;
            case CONFIG -> confOpts;
            case CONFIG_IF -> ifOpts;
            case CONFIG_VLAN -> vlanOpts;
        };

        for (String opt : toCheck) {
            if (opt.startsWith(prefix)) matches.add(opt);
        }

        if (matches.isEmpty()) cliLines.add("% Unrecognized command");
        else cliLines.add(String.join("  ", matches));
    }

    public CompoundTag saveToNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Hostname", switchHostname);
        tag.putString("ManagementIp", managementIp);
        tag.putString("ManagementMask", managementMask);
        tag.putString("MacAddress", macAddress);

        CompoundTag ports = new CompoundTag();
        for (Map.Entry<String, PortConfig> entry : portConfigs.entrySet()) {
            ports.put(entry.getKey(), entry.getValue().save());
        }
        tag.put("Ports", ports);

        CompoundTag vlans = new CompoundTag();
        for (Map.Entry<String, String> entry : vlanDatabase.entrySet()) {
            vlans.putString(entry.getKey(), entry.getValue());
        }
        tag.put("Vlans", vlans);

        CompoundTag macs = new CompoundTag();
        for (Map.Entry<String, String> entry : macTable.entrySet()) {
            macs.putString(entry.getKey(), entry.getValue());
        }
        tag.put("MacTable", macs);

        return tag;
    }

    public void loadFromNBT(CompoundTag tag) {
        if (tag.contains("Hostname")) switchHostname = tag.getString("Hostname");
        if (tag.contains("ManagementIp")) managementIp = tag.getString("ManagementIp");
        if (tag.contains("ManagementMask")) managementMask = tag.getString("ManagementMask");
        if (tag.contains("MacAddress")) macAddress = tag.getString("MacAddress");

        if (tag.contains("Ports")) {
            CompoundTag ports = tag.getCompound("Ports");
            for (String key : ports.getAllKeys()) {
                if (portConfigs.containsKey(key)) {
                    portConfigs.get(key).load(ports.getCompound(key));
                }
            }
        }

        if (tag.contains("Vlans")) {
            vlanDatabase.clear();
            CompoundTag vlans = tag.getCompound("Vlans");
            for (String key : vlans.getAllKeys()) {
                vlanDatabase.put(key, vlans.getString(key));
            }
        }

        if (tag.contains("MacTable")) {
            macTable.clear();
            macTableAge.clear();
            CompoundTag macs = tag.getCompound("MacTable");
            long now = System.currentTimeMillis();
            for (String key : macs.getAllKeys()) {
                macTable.put(key, macs.getString(key));
                macTableAge.put(key, now); // Reset age on world load
            }
        }
    }
}
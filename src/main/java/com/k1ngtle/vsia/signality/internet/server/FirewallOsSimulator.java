package com.k1ngtle.vsia.signality.internet.server;

import com.k1ngtle.vsia.signality.internet.OSINetworkPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class FirewallOsSimulator {

    public enum CliMode {
        EXEC, PRIVILEGED, CONFIG, CONFIG_IF, CONFIG_OBJ
    }

    public static class PortConfig {
        public boolean up = true;
        public String speed = "Auto";
        public String duplex = "Auto";
        public String ipAddress = "unassigned";
        public String subnetMask = "unassigned";
        public String ipv6Address = "unassigned";
        public String nameif = "";
        public int securityLevel = 0;

        public CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putBoolean("Up", up);
            tag.putString("Speed", speed);
            tag.putString("Duplex", duplex);
            tag.putString("IpAddress", ipAddress);
            tag.putString("SubnetMask", subnetMask);
            tag.putString("Ipv6Address", ipv6Address);
            tag.putString("NameIf", nameif);
            tag.putInt("SecurityLevel", securityLevel);
            return tag;
        }

        public void load(CompoundTag tag) {
            up = tag.getBoolean("Up");
            speed = tag.getString("Speed");
            duplex = tag.getString("Duplex");
            ipAddress = tag.getString("IpAddress");
            subnetMask = tag.getString("SubnetMask");
            if (tag.contains("Ipv6Address")) ipv6Address = tag.getString("Ipv6Address");
            nameif = tag.getString("NameIf");
            securityLevel = tag.getInt("SecurityLevel");
        }
    }

    public static class ParsedAclRule {
        public String name;
        public boolean permit;
        public String protocol;
        public long srcIp, srcMask;
        public long dstIp, dstMask;
        public int dstPort = -1;
        public String rawCommand;

        public boolean matches(long sIp, long dIp, String proto, int dPort) {
            if (!protocol.equals("ip") && !protocol.equalsIgnoreCase(proto)) return false;
            if ((sIp & srcMask) != (srcIp & srcMask)) return false;
            if ((dIp & dstMask) != (dstIp & dstMask)) return false;
            if (dstPort != -1 && dstPort != dPort) return false;
            return true;
        }
    }

    public static class NetworkObject {
        public String name;
        public String subnet = "";
        public String natCommand = "";
    }

    private static class ConnectionState {
        long srcIp, dstIp;
        int srcPort, dstPort;
        String protocol;
        long lastActivityMillis;
    }

    public final int id;
    public String displayName;
    public String hostname;
    public String macAddress;

    public final Map<String, PortConfig> portConfigs = new LinkedHashMap<>();

    public final List<ParsedAclRule> parsedAcls = new ArrayList<>();
    public final Map<String, String> accessGroups = new LinkedHashMap<>();
    public final List<String> routes = new ArrayList<>();
    public final Map<String, NetworkObject> networkObjects = new LinkedHashMap<>();
    private final List<ConnectionState> connectionTable = new ArrayList<>();

    public final List<String> asaCommands = new ArrayList<>();
    public final List<String> cliLines = new ArrayList<>();
    public boolean isBooted = false;
    public long bootStartTime;
    public int bootStep = 0;
    public CliMode cliMode = CliMode.EXEC;
    public String cliTarget = "";
    public String cliInput = "";
    public int cliCursorPos = 0;
    public int cliScrollOffset = 0;

    private final Runnable onStateChange;

    public FirewallOsSimulator(int rackId, int fwId, String initialHostname, Runnable onStateChange) {
        this.id = fwId;
        this.hostname = initialHostname;
        this.displayName = rackId + "_" + fwId;
        this.bootStartTime = System.currentTimeMillis();
        this.onStateChange = onStateChange;
        this.macAddress = String.format("0002.4A0B.%02X%02X", rackId, fwId);

        for (int i = 1; i <= 8; i++) {
            portConfigs.put("GigabitEthernet1/" + i, new PortConfig());
        }
        portConfigs.put("Management1/1", new PortConfig());

        asaCommands.add("INFO: Starting SW-DRBG health test...");
        asaCommands.add("INFO: SW-DRBG health test passed.");
        asaCommands.add("");
        asaCommands.add("Type help or '?' for a list of available commands.");
        asaCommands.add("");
    }

    public OSINetworkPacket filterAndRoutePacket(OSINetworkPacket packet, String ingressPortName) {
        if (!isBooted) return null;

        PortConfig ingressPort = portConfigs.get(ingressPortName);
        if (ingressPort == null || !ingressPort.up || ingressPort.nameif.isEmpty()) return null;

        long pSrcIp = ipToLong(packet.sourceIp);
        long pDstIp = ipToLong(packet.targetIp);

        maintainConnectionTable();
        for (ConnectionState conn : connectionTable) {
            if (conn.protocol.equalsIgnoreCase(packet.applicationProtocol) &&
                    ((conn.srcIp == pSrcIp && conn.dstIp == pDstIp && conn.srcPort == packet.sourcePort && conn.dstPort == packet.targetPort) ||
                            (conn.dstIp == pSrcIp && conn.srcIp == pDstIp && conn.dstPort == packet.sourcePort && conn.srcPort == packet.targetPort))) {

                conn.lastActivityMillis = System.currentTimeMillis();
                return routePacket(packet);
            }
        }

        String egressPortName = lookupRoute(packet.targetIp);
        if (egressPortName == null) return null;

        PortConfig egressPort = portConfigs.get(egressPortName);
        if (egressPort == null || !egressPort.up || egressPort.nameif.isEmpty()) return null;

        boolean permittedBySecurityLevel = ingressPort.securityLevel > egressPort.securityLevel;

        boolean permittedByAcl = false;
        boolean aclApplied = false;

        String appliedAclName = accessGroups.get(ingressPort.nameif);
        if (appliedAclName != null) {
            aclApplied = true;
            for (ParsedAclRule rule : parsedAcls) {
                if (rule.name.equals(appliedAclName)) {
                    if (rule.matches(pSrcIp, pDstIp, packet.applicationProtocol, packet.targetPort)) {
                        permittedByAcl = rule.permit;
                        break;
                    }
                }
            }
        }

        boolean finalPermit;
        if (aclApplied) {
            finalPermit = permittedByAcl;
        } else {
            finalPermit = permittedBySecurityLevel;
        }

        if (!finalPermit) {
            return null;
        }

        ConnectionState newState = new ConnectionState();
        newState.srcIp = pSrcIp;
        newState.dstIp = pDstIp;
        newState.srcPort = packet.sourcePort;
        newState.dstPort = packet.targetPort;
        newState.protocol = packet.applicationProtocol;
        newState.lastActivityMillis = System.currentTimeMillis();
        connectionTable.add(newState);

        return applyNatAndRoute(packet, ingressPort, egressPort);
    }

    private String lookupRoute(String targetIp) {
        long tIp = ipToLong(targetIp);
        for (Map.Entry<String, PortConfig> entry : portConfigs.entrySet()) {
            PortConfig pc = entry.getValue();
            if (!pc.up || pc.ipAddress.equals("unassigned")) continue;

            long pIp = ipToLong(pc.ipAddress);
            long mask = ipToLong(pc.subnetMask);
            if ((tIp & mask) == (pIp & mask)) return entry.getKey();
        }

        for (String route : routes) {
            String[] parts = route.trim().split("\\s+");
            if (parts.length >= 4 && parts[0].equals("route")) {
                String egressIf = parts[1];
                long net = ipToLong(parts[2]);
                long mask = ipToLong(parts[3]);
                if ((tIp & mask) == (net & mask)) {
                    for (Map.Entry<String, PortConfig> entry : portConfigs.entrySet()) {
                        if (entry.getValue().nameif.equals(egressIf)) return entry.getKey();
                    }
                }
            }
        }

        for (String route : routes) {
            if (route.contains("0.0.0.0 0.0.0.0")) {
                String[] parts = route.trim().split("\\s+");
                for (Map.Entry<String, PortConfig> entry : portConfigs.entrySet()) {
                    if (entry.getValue().nameif.equals(parts[1])) return entry.getKey();
                }
            }
        }
        return null;
    }

    private OSINetworkPacket applyNatAndRoute(OSINetworkPacket packet, PortConfig in, PortConfig out) {
        for (NetworkObject obj : networkObjects.values()) {
            if (obj.natCommand.contains("(" + in.nameif + "," + out.nameif + ") dynamic interface")) {
                long pSrcIp = ipToLong(packet.sourceIp);
                if (!obj.subnet.isEmpty()) {
                    String[] subParts = obj.subnet.split("\\s+");
                    long objNet = ipToLong(subParts[0]);
                    long objMask = ipToLong(subParts[1]);
                    if ((pSrcIp & objMask) == (objNet & objMask)) {
                        packet.sourceIp = out.ipAddress;
                    }
                }
            }
        }
        return routePacket(packet);
    }

    private OSINetworkPacket routePacket(OSINetworkPacket packet) {
        return packet;
    }

    private void maintainConnectionTable() {
        long now = System.currentTimeMillis();
        Iterator<ConnectionState> it = connectionTable.iterator();
        while (it.hasNext()) {
            ConnectionState conn = it.next();
            if (now - conn.lastActivityMillis > 60000) {
                it.remove();
            }
        }
    }

    public String getPrompt() {
        switch (cliMode) {
            case EXEC: return hostname + ">";
            case PRIVILEGED: return hostname + "#";
            case CONFIG: return hostname + "(config)#";
            case CONFIG_IF: return hostname + "(config-if)#";
            case CONFIG_OBJ: return hostname + "(config-network-object)#";
        }
        return hostname + ">";
    }

    public void handleAutocomplete() {
        if (cliInput.isEmpty()) return;
        String lower = cliInput.toLowerCase();

        if (cliMode == CliMode.CONFIG && (lower.startsWith("int ") || lower.startsWith("interface "))) {
            String[] parts = cliInput.split(" ", -1);
            if (parts.length == 2 && !parts[1].isEmpty()) {
                String iface = parts[1].toLowerCase();
                if (iface.startsWith("gi")) cliInput = "interface GigabitEthernet1/" + cliInput.substring(cliInput.toLowerCase().indexOf("gi") + 2);
                else if (iface.startsWith("ma")) cliInput = "interface Management1/1";
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
            else if (cliMode == CliMode.CONFIG) options = new String[]{"interface", "hostname", "access-list", "access-group", "object", "route", "exit", "help"};
            else if (cliMode == CliMode.CONFIG_IF) options = new String[]{"nameif", "security-level", "ip", "shutdown", "no", "exit", "help"};
            else if (cliMode == CliMode.CONFIG_OBJ) options = new String[]{"subnet", "nat", "exit", "help"};

            String match = findCommonPrefix(tokens[0], options);
            if (match != null && match.length() > tokens[0].length()) {
                cliInput = match + " ";
                cliCursorPos = cliInput.length();
                if (onStateChange != null) onStateChange.run();
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
        String pre = hostname + "(config)# ";
        if (portConfigs.containsKey(selectedConfigItem)) {
            pre = hostname + "(config-if)# ";
        }
        asaCommands.add(pre + command);
        if (asaCommands.size() > 8) asaCommands.remove(0);
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
                    cliLines.add("Exec commands:");
                    cliLines.add("  enable  Turn on privileged commands");
                    cliLines.add("  help    Description of the interactive help system");
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
                cliLines.add("Cisco Adaptive Security Appliance Software Version 9.14(1)");
                cliLines.add("Hardware:   ASA5506, 4096 MB RAM, CPU Atom C2000 1250 MHz");
            }
            else if ((lower.equals("show interface ip brief") || lower.equals("show int ip brief")) && echo) {
                cliLines.add("Interface                  IP-Address      OK? Method Status                Protocol");
                for (Map.Entry<String, PortConfig> entry : portConfigs.entrySet()) {
                    PortConfig pc = entry.getValue();
                    String ip = pc.ipAddress.equals("unassigned") ? "unassigned     " : String.format("%-15s", pc.ipAddress);
                    String status = pc.up ? "up                    " : "administratively down ";
                    cliLines.add(String.format("%-26s %s YES unset  %s %s", entry.getKey(), ip, status, pc.up ? "up" : "down"));
                }
            }
            else if (lower.equals("show access-list") && echo) {
                if (parsedAcls.isEmpty()) cliLines.add("No access lists configured.");
                for (ParsedAclRule acl : parsedAcls) cliLines.add(acl.rawCommand);
            }
            else if (lower.equals("show running-config") && echo) {
                cliLines.add("Building configuration...");
                cliLines.add("hostname " + hostname);
                for (NetworkObject obj : networkObjects.values()) {
                    cliLines.add("object network " + obj.name);
                    if (!obj.subnet.isEmpty()) cliLines.add(" subnet " + obj.subnet);
                    if (!obj.natCommand.isEmpty()) cliLines.add(" nat " + obj.natCommand);
                }
                for (Map.Entry<String, PortConfig> entry : portConfigs.entrySet()) {
                    cliLines.add("interface " + entry.getKey());
                    PortConfig pc = entry.getValue();
                    if (!pc.nameif.isEmpty()) cliLines.add(" nameif " + pc.nameif);
                    if (!pc.nameif.isEmpty()) cliLines.add(" security-level " + pc.securityLevel);
                    if (!pc.ipAddress.equals("unassigned")) cliLines.add(" ip address " + pc.ipAddress + " " + pc.subnetMask);
                    if (!pc.up) cliLines.add(" shutdown");
                }
                for (ParsedAclRule acl : parsedAcls) cliLines.add(acl.rawCommand);
                for (Map.Entry<String, String> ag : accessGroups.entrySet()) cliLines.add("access-group " + ag.getValue() + " in interface " + ag.getKey());
                for (String route : routes) cliLines.add(route);
            }
            else if (echo && !lower.isEmpty()) cliLines.add("% Invalid input detected");

        } else if (cliMode == CliMode.CONFIG) {
            if (lower.startsWith("int ") || lower.startsWith("interface ")) {
                String iface = cmd.substring(lower.startsWith("int ") ? 4 : 10).trim();
                if (iface.toLowerCase().startsWith("gi")) iface = "GigabitEthernet" + iface.substring(2);
                else if (iface.toLowerCase().startsWith("ma")) iface = "Management1/1";
                if (portConfigs.containsKey(iface)) { cliMode = CliMode.CONFIG_IF; cliTarget = iface; }
                else if (echo) cliLines.add("ERROR: Invalid interface");
            }
            else if (lower.startsWith("hostname ")) { hostname = cmd.substring(9).trim(); }
            else if (lower.startsWith("access-list ")) {
                parseAclRule(cmd);
            }
            else if (lower.startsWith("access-group ")) {
                String[] parts = cmd.substring(13).trim().split("\\s+");
                if (parts.length >= 4 && parts[1].equalsIgnoreCase("in") && parts[2].equalsIgnoreCase("interface")) {
                    accessGroups.put(parts[3], parts[0]);
                }
            }
            else if (lower.startsWith("object network ")) {
                String name = cmd.substring(15).trim();
                cliMode = CliMode.CONFIG_OBJ;
                cliTarget = name;
                networkObjects.putIfAbsent(name, new NetworkObject());
                networkObjects.get(name).name = name;
            }
            else if (lower.startsWith("route ")) { routes.add(cmd); }
            else if (lower.equals("exit")) { cliMode = CliMode.PRIVILEGED; }
            else if (echo && !lower.isEmpty()) cliLines.add("% Invalid input detected");

        } else if (cliMode == CliMode.CONFIG_IF) {
            PortConfig pc = portConfigs.get(cliTarget);
            if (pc != null) {
                if (lower.startsWith("nameif ")) {
                    String name = cmd.substring(7).trim();
                    pc.nameif = name;
                    pc.securityLevel = name.equalsIgnoreCase("inside") ? 100 : 0;
                    if (echo) cliLines.add("INFO: Security level for \"" + name + "\" set to " + pc.securityLevel + " by default.");
                } else if (lower.startsWith("security-level ")) {
                    try { pc.securityLevel = Integer.parseInt(cmd.substring(15).trim()); } catch (Exception ignored) {}
                } else if (lower.startsWith("ip address ")) {
                    String[] parts = cmd.substring(11).trim().split("\\s+");
                    if (parts.length >= 2) { pc.ipAddress = parts[0]; pc.subnetMask = parts[1]; }
                } else if (lower.equals("shutdown")) { pc.up = false; }
                else if (lower.equals("no shutdown")) { pc.up = true; }
                else if (lower.equals("exit")) { cliMode = CliMode.CONFIG; }
                else if (echo && !lower.isEmpty()) cliLines.add("% Invalid input detected");
            }
        } else if (cliMode == CliMode.CONFIG_OBJ) {
            NetworkObject obj = networkObjects.get(cliTarget);
            if (obj != null) {
                if (lower.startsWith("subnet ")) { obj.subnet = cmd.substring(7).trim(); }
                else if (lower.startsWith("nat ")) { obj.natCommand = cmd.substring(4).trim(); }
                else if (lower.equals("exit")) { cliMode = CliMode.CONFIG; }
                else if (echo && !lower.isEmpty()) cliLines.add("% Invalid input detected");
            }
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
            cliLines.add("  access-group  Bind an access list to an interface");
            cliLines.add("  access-list   Add an access list entry");
            cliLines.add("  exit          Exit from configure mode");
            cliLines.add("  hostname      Set system's network name");
            cliLines.add("  interface     Select an interface to configure");
            cliLines.add("  object        Configure a network object");
            cliLines.add("  route         Configure a static route");
        } else if (cliMode == CliMode.CONFIG_IF) {
            cliLines.add("Interface configuration commands:");
            cliLines.add("  exit            Exit from interface configuration mode");
            cliLines.add("  ip              Interface Internet Protocol config commands");
            cliLines.add("  nameif          Assign a name to the interface");
            cliLines.add("  no              Negate a command or set its defaults");
            cliLines.add("  security-level  Specify the security level of the interface");
            cliLines.add("  shutdown        Shutdown the selected interface");
        } else if (cliMode == CliMode.CONFIG_OBJ) {
            cliLines.add("Network object commands:");
            cliLines.add("  exit    Exit from object configuration mode");
            cliLines.add("  nat     Configure NAT operations");
            cliLines.add("  subnet  Configure a subnet");
        }
    }

    private void showContextHelp(String prefix) {
        if (prefix.equals("show")) {
            cliLines.add("  access-list      Display access list information");
            cliLines.add("  interface        Show interface status and configuration");
            cliLines.add("  running-config   Current operating configuration");
            cliLines.add("  version          System hardware and software status");
            cliLines.add("Example: show interface ip brief");
        } else if (prefix.equals("show interface") || prefix.equals("show int")) {
            cliLines.add("  ip  Show IP interface status");
        } else if (prefix.equals("show interface ip") || prefix.equals("show int ip")) {
            cliLines.add("  brief  Brief summary of IP status and configuration");
        } else if (prefix.equals("configure") || prefix.equals("conf")) {
            cliLines.add("  terminal  Configure from the terminal");
        } else if (prefix.equals("interface") || prefix.equals("int")) {
            cliLines.add("  GigabitEthernet  GigabitEthernet IEEE 802.3z");
            cliLines.add("  Management       Management interface");
            cliLines.add("Example: interface GigabitEthernet1/1");
        } else if (prefix.equals("object")) {
            cliLines.add("  network  Configure a network object");
        } else if (prefix.equals("object network")) {
            cliLines.add("  WORD  Network object name");
            cliLines.add("Example: object network inside_subnet");
        } else if (prefix.equals("access-list")) {
            cliLines.add("  WORD  Access list name");
            cliLines.add("Example: access-list OUTSIDE_IN permit tcp any any eq 80");
        } else if (prefix.equals("access-group")) {
            cliLines.add("  WORD  Access group name");
            cliLines.add("Example: access-group OUTSIDE_IN in interface GigabitEthernet1/1");
        } else if (prefix.equals("ip")) {
            cliLines.add("  address  Set the IP address of an interface");
        } else if (prefix.equals("ip address")) {
            cliLines.add("  A.B.C.D  IP address");
            cliLines.add("Example: ip address 10.0.0.1 255.255.255.0");
        } else if (prefix.equals("nameif")) {
            cliLines.add("  WORD  Name of the interface");
        } else if (prefix.equals("security-level")) {
            cliLines.add("  <0-100>  Security level of the interface");
        } else if (prefix.equals("route")) {
            cliLines.add("  WORD  Interface name (e.g., outside)");
            cliLines.add("Example: route outside 0.0.0.0 0.0.0.0 192.168.1.1");
        } else if (prefix.equals("ping")) {
            cliLines.add("  WORD  Ping destination address or hostname");
        } else if (prefix.equals("hostname")) {
            cliLines.add("  WORD  This system's network name");
        } else if (prefix.equals("subnet")) {
            cliLines.add("  A.B.C.D  Network address");
            cliLines.add("Example: subnet 10.0.0.0 255.255.255.0");
        } else if (prefix.equals("nat")) {
            cliLines.add("  (if_name,if_name)  NAT interface mapping");
            cliLines.add("Example: nat (inside,outside) dynamic interface");
        } else {
            cliLines.add("% Unrecognized command");
        }
    }

    private void showPrefixHelp(String prefix) {
        List<String> matches = new ArrayList<>();
        String[] execOpts = {"enable", "ping", "help", "exit"};
        String[] privOpts = {"configure", "disable", "exit", "write", "show", "help"};
        String[] confOpts = {"access-group", "access-list", "exit", "hostname", "interface", "object", "route", "help"};
        String[] ifOpts = {"exit", "ip", "nameif", "no", "security-level", "shutdown", "help"};
        String[] objOpts = {"exit", "nat", "subnet", "help"};

        String[] toCheck = switch(cliMode) {
            case EXEC -> execOpts;
            case PRIVILEGED -> privOpts;
            case CONFIG -> confOpts;
            case CONFIG_IF -> ifOpts;
            case CONFIG_OBJ -> objOpts;
        };

        for (String opt : toCheck) {
            if (opt.startsWith(prefix)) matches.add(opt);
        }

        if (matches.isEmpty()) cliLines.add("% Unrecognized command");
        else cliLines.add(String.join("  ", matches));
    }

    private void parseAclRule(String cmd) {
        try {
            String[] parts = cmd.split("\\s+");
            if (parts.length < 6) return;

            ParsedAclRule rule = new ParsedAclRule();
            rule.rawCommand = cmd;
            rule.name = parts[1];
            rule.permit = parts[3].equalsIgnoreCase("permit");
            rule.protocol = parts[4].toLowerCase();

            int idx = 5;
            if (parts[idx].equalsIgnoreCase("any")) { rule.srcIp = 0; rule.srcMask = 0; idx++; }
            else if (parts[idx].equalsIgnoreCase("host")) { rule.srcIp = ipToLong(parts[idx+1]); rule.srcMask = 0xFFFFFFFFL; idx += 2; }
            else { rule.srcIp = ipToLong(parts[idx]); rule.srcMask = ipToLong(parts[idx+1]); idx += 2; }

            if (idx < parts.length) {
                if (parts[idx].equalsIgnoreCase("any")) { rule.dstIp = 0; rule.dstMask = 0; idx++; }
                else if (parts[idx].equalsIgnoreCase("host")) { rule.dstIp = ipToLong(parts[idx+1]); rule.dstMask = 0xFFFFFFFFL; idx += 2; }
                else { rule.dstIp = ipToLong(parts[idx]); rule.dstMask = ipToLong(parts[idx+1]); idx += 2; }
            }

            if (idx < parts.length - 1 && parts[idx].equalsIgnoreCase("eq")) {
                rule.dstPort = Integer.parseInt(parts[idx+1]);
            }

            parsedAcls.add(rule);
        } catch (Exception ignored) { }
    }

    private long ipToLong(String ip) {
        try {
            String[] parts = ip.split("\\.");
            long result = 0;
            for (String part : parts) {
                result = (result << 8) | Integer.parseInt(part);
            }
            return result;
        } catch (Exception e) {
            return 0;
        }
    }

    public CompoundTag saveToNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("IsBooted", isBooted);
        tag.putString("Hostname", hostname);
        tag.putString("DisplayName", displayName);

        CompoundTag ports = new CompoundTag();
        for (Map.Entry<String, PortConfig> entry : portConfigs.entrySet()) {
            ports.put(entry.getKey(), entry.getValue().save());
        }
        tag.put("Ports", ports);

        ListTag acls = new ListTag();
        for (ParsedAclRule rule : parsedAcls) {
            CompoundTag rt = new CompoundTag();
            rt.putString("Raw", rule.rawCommand);
            acls.add(rt);
        }
        tag.put("Acls", acls);

        CompoundTag groups = new CompoundTag();
        for (Map.Entry<String, String> entry : accessGroups.entrySet()) {
            groups.putString(entry.getKey(), entry.getValue());
        }
        tag.put("AccessGroups", groups);

        ListTag rts = new ListTag();
        for (String r : routes) {
            CompoundTag rt = new CompoundTag();
            rt.putString("R", r);
            rts.add(rt);
        }
        tag.put("Routes", rts);

        CompoundTag objs = new CompoundTag();
        for (NetworkObject obj : networkObjects.values()) {
            CompoundTag ot = new CompoundTag();
            ot.putString("Subnet", obj.subnet);
            ot.putString("Nat", obj.natCommand);
            objs.put(obj.name, ot);
        }
        tag.put("NetworkObjects", objs);

        return tag;
    }

    public void loadFromNBT(CompoundTag tag) {
        if (tag.contains("IsBooted")) {
            isBooted = tag.getBoolean("IsBooted");
            if (isBooted && cliLines.isEmpty()) {
                cliLines.add("System Bootstrap, Version 2.1(0)FW");
                cliLines.add("Platform ASA-5506-X, 4096 MB RAM, CPU Atom C2000");
                cliLines.add("System resumed from sleep.");
                cliLines.add("");
            }
        }
        if (tag.contains("Hostname")) hostname = tag.getString("Hostname");
        if (tag.contains("DisplayName")) displayName = tag.getString("DisplayName");

        if (tag.contains("Ports")) {
            CompoundTag ports = tag.getCompound("Ports");
            for (String key : ports.getAllKeys()) {
                if (portConfigs.containsKey(key)) {
                    portConfigs.get(key).load(ports.getCompound(key));
                }
            }
        }

        if (tag.contains("Acls")) {
            parsedAcls.clear();
            ListTag acls = tag.getList("Acls", Tag.TAG_COMPOUND);
            for (int i = 0; i < acls.size(); i++) {
                parseAclRule(acls.getCompound(i).getString("Raw"));
            }
        }

        if (tag.contains("AccessGroups")) {
            accessGroups.clear();
            CompoundTag groups = tag.getCompound("AccessGroups");
            for (String key : groups.getAllKeys()) {
                accessGroups.put(key, groups.getString(key));
            }
        }

        if (tag.contains("Routes")) {
            routes.clear();
            ListTag rts = tag.getList("Routes", Tag.TAG_COMPOUND);
            for (int i = 0; i < rts.size(); i++) routes.add(rts.getCompound(i).getString("R"));
        }

        if (tag.contains("NetworkObjects")) {
            networkObjects.clear();
            CompoundTag objs = tag.getCompound("NetworkObjects");
            for (String key : objs.getAllKeys()) {
                NetworkObject no = new NetworkObject();
                no.name = key;
                CompoundTag ot = objs.getCompound(key);
                no.subnet = ot.getString("Subnet");
                no.natCommand = ot.getString("Nat");
                networkObjects.put(key, no);
            }
        }
    }
}
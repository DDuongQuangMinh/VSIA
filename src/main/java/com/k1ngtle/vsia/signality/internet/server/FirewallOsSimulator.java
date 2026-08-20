package com.k1ngtle.vsia.signality.internet.server;

import com.k1ngtle.vsia.signality.internet.OSINetworkPacket;
import com.k1ngtle.vsia.signality.engineering.firewall.FirewallDecision;
import com.k1ngtle.vsia.signality.engineering.firewall.FirewallPacketView;
import com.k1ngtle.vsia.signality.engineering.firewall.FirewallW116Adapter;
import com.k1ngtle.vsia.signality.engineering.firewall.StatefulFirewallEngine;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class FirewallOsSimulator {

    public enum CliMode {
        EXEC, PRIVILEGED, CONFIG, CONFIG_IF, CONFIG_OBJ, CONFIG_ROUTER, CONFIG_CRYPTO_MAP
    }

    public static class PortConfig {
        public boolean up = true;
        public String speed = "auto";
        public String duplex = "auto";
        public String ipAddress = "unassigned";
        public String subnetMask = "unassigned";
        public String ipv6Address = "unassigned";
        public String nameif = "";
        public String description = "";
        public int securityLevel = 0;
        public boolean cryptoIkev1Enabled = false;

        public CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putBoolean("Up", up);
            tag.putString("Speed", speed);
            tag.putString("Duplex", duplex);
            tag.putString("IpAddress", ipAddress);
            tag.putString("SubnetMask", subnetMask);
            tag.putString("Ipv6Address", ipv6Address);
            tag.putString("NameIf", nameif);
            tag.putString("Desc", description);
            tag.putInt("SecurityLevel", securityLevel);
            tag.putBoolean("CryptoIkev1Enabled", cryptoIkev1Enabled);
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
            description = tag.getString("Desc");
            securityLevel = tag.getInt("SecurityLevel");
            if (tag.contains("CryptoIkev1Enabled")) cryptoIkev1Enabled = tag.getBoolean("CryptoIkev1Enabled");
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

    public static class CryptoMap {
        public String name;
        public int seq;
        public String matchAcl = "";
        public String peerIp = "";
        public String transformSet = "";
        public String appliedInterface = "";
        public String pfsGroup = "";
        public int saLifetime = 28800; // default 8 hours
    }

    private static class ConnectionState {
        long srcIp, dstIp;
        int srcPort, dstPort;
        String protocol;
        long lastActivityMillis;
    }

    // IPsec ISAKMP Security Association Engine
    private static class IpsecSA {
        String peerIp;
        String cryptoMapName;
        long establishedTime;
        long lifetime;
        String state; // MM1 -> MM6, QM1 -> QM3, ESTABLISHED
        int pktsEncaps, pktsDecaps;
    }

    // Routing Tables
    private static class RouteEntry {
        long network;
        long mask;
        String nextHopIp;
        String egressInterface;
        int metric;
        String protocol; // "STATIC", "OSPF", "BGP"
    }

    public int id;
    public String displayName;
    public String hostname;
    public String macAddress;

    public final Map<String, PortConfig> portConfigs = new LinkedHashMap<>();

    public final List<ParsedAclRule> parsedAcls = new ArrayList<>();
    public final Map<String, String> accessGroups = new LinkedHashMap<>();
    public final List<String> routes = new ArrayList<>();
    public final Map<String, NetworkObject> networkObjects = new LinkedHashMap<>();

    // Advanced Routing & VPN state
    public int ospfProcessId = -1;
    public List<String> ospfNetworks = new ArrayList<>();
    public int bgpAsn = -1;
    public List<String> bgpNeighbors = new ArrayList<>();
    private final List<RouteEntry> rib = new ArrayList<>();
    private long lastRoutingCalculation = 0;

    public Map<String, String> transformSets = new LinkedHashMap<>();
    public List<CryptoMap> cryptoMaps = new ArrayList<>();
    public String isakmpPolicy = "";
    public String isakmpKey = "";
    private final List<IpsecSA> activeSAs = new ArrayList<>();

    private final List<ConnectionState> connectionTable = new ArrayList<>();
    private final List<String> xlateTable = new ArrayList<>();

    private StatefulFirewallEngine w116Firewall =
            StatefulFirewallEngine.permissiveCompatibilityEngine();

    private String w116LastDecision =
            "W1.16 STATEFUL FIREWALL READY";


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
    public Runnable guiCallback;
    public Consumer<OSINetworkPacket> packetTransmitter;

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
    }

    /**
     * Called every tick to process routing algorithms and VPN state machines
     */
    public void tick(Consumer<OSINetworkPacket> out) {
        this.packetTransmitter = out;
        long now = System.currentTimeMillis();

        FirewallCliEnhancer.advanceBoot(
                this,
                now
        );

        // 1. Process IPsec ISAKMP Negotiation States
        Iterator<IpsecSA> saIt = activeSAs.iterator();
        while (saIt.hasNext()) {
            IpsecSA sa = saIt.next();
            if (sa.state.equals("ESTABLISHED") && now - sa.establishedTime > sa.lifetime * 1000L) {
                saIt.remove(); // SA expired, will renegotiate on next packet
            } else if (!sa.state.equals("ESTABLISHED")) {
                // Simulate IKEv1 6-way Main Mode + 3-way Quick Mode exchange delays
                if (now - sa.establishedTime > 500) {
                    switch (sa.state) {
                        case "MM1": sa.state = "MM2"; sa.establishedTime = now; break;
                        case "MM2": sa.state = "MM3"; sa.establishedTime = now; break;
                        case "MM3": sa.state = "MM4"; sa.establishedTime = now; break;
                        case "MM4": sa.state = "MM5"; sa.establishedTime = now; break;
                        case "MM5": sa.state = "MM6"; sa.establishedTime = now; break;
                        case "MM6": sa.state = "QM1"; sa.establishedTime = now; break;
                        case "QM1": sa.state = "QM2"; sa.establishedTime = now; break;
                        case "QM2": sa.state = "QM3"; sa.establishedTime = now; break;
                        case "QM3":
                            sa.state = "ESTABLISHED";
                            sa.establishedTime = now;
                            if (onStateChange != null) onStateChange.run();
                            break;
                    }
                }
            }
        }

        // 2. Dijkstra SPF / BGP Route Calculation (Simulated every 10 seconds)
        if (now - lastRoutingCalculation > 10000) {
            lastRoutingCalculation = now;
            recalculateRIB();
        }
    }

    private void recalculateRIB() {
        rib.clear();
        // Add Connected Routes
        for (Map.Entry<String, PortConfig> entry : portConfigs.entrySet()) {
            PortConfig pc = entry.getValue();
            if (pc.up && !pc.ipAddress.equals("unassigned")) {
                RouteEntry re = new RouteEntry();
                re.network = ipToLong(pc.ipAddress) & ipToLong(pc.subnetMask);
                re.mask = ipToLong(pc.subnetMask);
                re.egressInterface = entry.getKey();
                re.metric = 0;
                re.protocol = "CONNECTED";
                rib.add(re);
            }
        }
        // Add Static Routes
        for (String route : routes) {
            String[] parts = route.trim().split("\\s+");
            if (parts.length >= 4 && parts[0].equals("route")) {
                RouteEntry re = new RouteEntry();
                re.egressInterface = parts[1];
                re.network = ipToLong(parts[2]);
                re.mask = ipToLong(parts[3]);
                re.nextHopIp = parts.length > 4 ? parts[4] : "";
                re.metric = 1;
                re.protocol = "STATIC";
                rib.add(re);
            }
        }
        // Simulate OSPF LSDB Injection (if OSPF configured)
        if (ospfProcessId != -1) {
            for (String net : ospfNetworks) {
                // In a true simulation, this would be populated by flooded LSAs from neighbors
                // Here we inject it to the RIB locally
                String[] parts = net.trim().split("\\s+");
                if (parts.length >= 3) {
                    RouteEntry re = new RouteEntry();
                    re.network = ipToLong(parts[0]);
                    re.mask = ~ipToLong(parts[1]); // OSPF uses wildcard masks
                    re.metric = 110;
                    re.protocol = "OSPF";
                    // Egress interface would be calculated via Dijkstra
                    rib.add(re);
                }
            }
        }
    }


    public void w116EnableNat44(String publicIpv4) {
        w116Firewall.enableNat44(
                publicIpv4
        );

        w116LastDecision =
                "W1.16 NAT44/PAT public="
                        + publicIpv4;

        if (onStateChange != null) {
            onStateChange.run();
        }
    }

    public void w116DisableNat44() {
        w116Firewall.disableNat44();

        w116LastDecision =
                "W1.16 NAT44/PAT disabled";

        if (onStateChange != null) {
            onStateChange.run();
        }
    }

    public void w116Reset() {
        w116Firewall =
                StatefulFirewallEngine.permissiveCompatibilityEngine();

        w116LastDecision =
                "W1.16 STATEFUL FIREWALL RESET";

        if (onStateChange != null) {
            onStateChange.run();
        }
    }

    public String w116Status() {
        return "W1.16 STATEFUL FIREWALL"
                + " | conntrack="
                + w116Firewall.conntrack().size()
                + " | nat="
                + w116Firewall.nat44().size()
                + " | rules="
                + w116Firewall.rules().size()
                + " | last="
                + w116LastDecision;
    }

    public OSINetworkPacket w116DirectInspect(
            OSINetworkPacket packet,
            String ingressInterface,
            String egressInterface
    ) {
        FirewallPacketView w116Packet =
                FirewallW116Adapter.packetView(
                        packet,
                        ingressInterface,
                        egressInterface
                );

        FirewallDecision w116Decision =
                w116Firewall.inspect(
                        w116Packet,
                        0,
                        System.currentTimeMillis()
                );

        w116LastDecision =
                w116Decision.action()
                        + " "
                        + w116Decision.state()
                        + " rule="
                        + w116Decision.ruleName()
                        + " reason="
                        + w116Decision.reason();

        if (!w116Decision.allowed()) {
            if (onStateChange != null) {
                onStateChange.run();
            }

            return null;
        }

        FirewallW116Adapter.applyNat(
                packet,
                w116Decision
        );

        if (onStateChange != null) {
            onStateChange.run();
        }

        return packet;
    }

    public OSINetworkPacket filterAndRoutePacket(OSINetworkPacket packet, String ingressPortName) {
        if (!isBooted) return null;

        PortConfig ingressPort = portConfigs.get(ingressPortName);
        if (ingressPort == null || !ingressPort.up || ingressPort.nameif.isEmpty()) return null;

        String w116EgressPortName =
                lookupRoute(
                        packet.targetIp
                );

        FirewallPacketView w116Packet =
                FirewallW116Adapter.packetView(
                        packet,
                        ingressPortName,
                        w116EgressPortName
                );

        FirewallDecision w116Decision =
                w116Firewall.inspect(
                        w116Packet,
                        0,
                        System.currentTimeMillis()
                );

        w116LastDecision =
                w116Decision.action()
                        + " "
                        + w116Decision.state()
                        + " rule="
                        + w116Decision.ruleName()
                        + " reason="
                        + w116Decision.reason();

        if (!w116Decision.allowed()) {
            if (onStateChange != null) {
                onStateChange.run();
            }

            return null;
        }

        FirewallW116Adapter.applyNat(
                packet,
                w116Decision
        );

        long pSrcIp = ipToLong(packet.sourceIp);
        long pDstIp = ipToLong(packet.targetIp);

        // 1. IPsec VPN Decapsulation check (Inbound ESP)
        if (packet.applicationProtocol.equals("IPSEC-ESP")) {
            for (IpsecSA sa : activeSAs) {
                if (sa.state.equals("ESTABLISHED") && sa.peerIp.equals(packet.sourceIp)) {
                    // Packet decrypted successfully via AES hardware logic
                    packet.applicationProtocol = decryptPayload(packet.payload.getString("esp_payload"));
                    sa.pktsDecaps++;
                    break;
                }
            }
        }

        // 2. Stateful Firewall Inspection
        maintainConnectionTable();
        for (ConnectionState conn : connectionTable) {
            if (conn.protocol.equalsIgnoreCase(packet.applicationProtocol) &&
                    ((conn.srcIp == pSrcIp && conn.dstIp == pDstIp && conn.srcPort == packet.sourcePort && conn.dstPort == packet.targetPort) ||
                            (conn.dstIp == pSrcIp && conn.srcIp == pDstIp && conn.dstPort == packet.sourcePort && conn.srcPort == packet.targetPort))) {
                conn.lastActivityMillis = System.currentTimeMillis();
                return applyNatAndRoute(packet, ingressPort, null, true);
            }
        }

        // 3. Routing Lookup (RIB)
        String egressPortName = lookupRoute(packet.targetIp);
        if (egressPortName == null) return null;
        PortConfig egressPort = portConfigs.get(egressPortName);
        if (egressPort == null || !egressPort.up || egressPort.nameif.isEmpty()) return null;

        // 4. Access Control Lists
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
        if (!(aclApplied ? permittedByAcl : permittedBySecurityLevel)) return null;

        // 5. IPsec VPN Encapsulation (Outbound matching Crypto Map)
        for (CryptoMap cmap : cryptoMaps) {
            if (cmap.appliedInterface.equals(egressPort.nameif)) {
                for (ParsedAclRule rule : parsedAcls) {
                    if (rule.name.equals(cmap.matchAcl) && rule.permit && rule.matches(pSrcIp, pDstIp, packet.applicationProtocol, packet.targetPort)) {
                        IpsecSA activeSa = null;
                        for (IpsecSA sa : activeSAs) {
                            if (sa.peerIp.equals(cmap.peerIp) && sa.cryptoMapName.equals(cmap.name)) {
                                activeSa = sa; break;
                            }
                        }

                        if (activeSa == null) {
                            // Initiate IKE Phase 1 (MM1)
                            activeSa = new IpsecSA();
                            activeSa.peerIp = cmap.peerIp;
                            activeSa.cryptoMapName = cmap.name;
                            activeSa.state = "MM1";
                            activeSa.establishedTime = System.currentTimeMillis();
                            activeSa.lifetime = cmap.saLifetime;
                            activeSAs.add(activeSa);
                            if (onStateChange != null) onStateChange.run();
                            return null; // Drop packet while negotiating VPN
                        } else if (!activeSa.state.equals("ESTABLISHED")) {
                            return null; // Drop packet while negotiating VPN
                        }

                        // Actually encrypt the packet payload using AES
                        String encrypted = encryptPayload(packet.applicationProtocol);
                        packet.applicationProtocol = "IPSEC-ESP";
                        packet.payload = new CompoundTag();
                        packet.payload.putString("esp_payload", encrypted);
                        activeSa.pktsEncaps++;
                        break;
                    }
                }
            }
        }

        // Add to connection table
        ConnectionState newState = new ConnectionState();
        newState.srcIp = pSrcIp;
        newState.dstIp = pDstIp;
        newState.srcPort = packet.sourcePort;
        newState.dstPort = packet.targetPort;
        newState.protocol = packet.applicationProtocol;
        newState.lastActivityMillis = System.currentTimeMillis();
        connectionTable.add(newState);

        return applyNatAndRoute(packet, ingressPort, egressPort, false);
    }

    private String encryptPayload(String data) {
        if (isakmpKey.isEmpty()) return Base64.getEncoder().encodeToString(data.getBytes());
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            byte[] keyBytes = sha.digest(isakmpKey.getBytes(StandardCharsets.UTF_8));
            SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            return Base64.getEncoder().encodeToString(cipher.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) { return Base64.getEncoder().encodeToString(data.getBytes()); }
    }

    private String decryptPayload(String data) {
        if (isakmpKey.isEmpty()) return new String(Base64.getDecoder().decode(data));
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            byte[] keyBytes = sha.digest(isakmpKey.getBytes(StandardCharsets.UTF_8));
            SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            return new String(cipher.doFinal(Base64.getDecoder().decode(data)));
        } catch (Exception e) { return new String(Base64.getDecoder().decode(data)); }
    }

    private String lookupRoute(String targetIp) {
        long tIp = ipToLong(targetIp);
        // Look up against the active Routing Information Base (RIB)
        int bestPrefix = -1;
        String bestEgress = null;

        for (RouteEntry re : rib) {
            if ((tIp & re.mask) == (re.network & re.mask)) {
                // Longest prefix match
                int prefixLen = Long.bitCount(re.mask);
                if (prefixLen > bestPrefix) {
                    bestPrefix = prefixLen;

                    if (re.protocol.equals("STATIC") || re.protocol.equals("CONNECTED")) {
                        bestEgress = re.egressInterface;
                    } else if (re.protocol.equals("OSPF")) {
                        // In reality, this recursively looks up the next-hop
                        bestEgress = "outside"; // Simplified default egress for dynamic routes
                    }
                }
            }
        }

        // Handle explicit egress interfaces or resolve interface from nextHop IP
        if (bestEgress != null && !portConfigs.containsKey(bestEgress)) {
            for (Map.Entry<String, PortConfig> entry : portConfigs.entrySet()) {
                if (entry.getValue().nameif.equals(bestEgress)) return entry.getKey();
            }
        }
        return bestEgress;
    }

    private OSINetworkPacket applyNatAndRoute(OSINetworkPacket packet, PortConfig in, PortConfig out, boolean isReply) {
        for (NetworkObject obj : networkObjects.values()) {
            if (!obj.natCommand.isEmpty()) {
                String natInterfaceStr = obj.natCommand;
                int start = natInterfaceStr.indexOf('(');
                int end = natInterfaceStr.indexOf(')');
                if (start != -1 && end != -1 && out != null) {
                    String[] intfs = natInterfaceStr.substring(start + 1, end).split(",");
                    if (intfs.length == 2 && intfs[0].trim().equals(in.nameif) && intfs[1].trim().equals(out.nameif)) {
                        long pSrcIp = ipToLong(packet.sourceIp);
                        if (!obj.subnet.isEmpty()) {
                            String[] subParts = obj.subnet.split("\\s+");
                            long objNet = ipToLong(subParts[0]);
                            long objMask = ipToLong(subParts[1]);
                            if ((pSrcIp & objMask) == (objNet & objMask)) {
                                packet.sourceIp = out.ipAddress;
                                String xlate = "PAT Global " + out.ipAddress + "(" + out.nameif + ") Local " + longToIp(pSrcIp) + "(" + in.nameif + ")";
                                if (!xlateTable.contains(xlate)) xlateTable.add(xlate);
                            }
                        }
                    }
                }
            }
        }
        return packet;
    }

    private void maintainConnectionTable() {
        long now = System.currentTimeMillis();
        Iterator<ConnectionState> it = connectionTable.iterator();
        while (it.hasNext()) {
            ConnectionState conn = it.next();
            if (now - conn.lastActivityMillis > 60000) it.remove();
        }
    }

    public String getPrompt() {
        switch (cliMode) {
            case EXEC: return hostname + ">";
            case PRIVILEGED: return hostname + "#";
            case CONFIG: return hostname + "(config)#";
            case CONFIG_IF: return hostname + "(config-if)#";
            case CONFIG_OBJ: return hostname + "(config-network-object)#";
            case CONFIG_ROUTER: return hostname + "(config-router)#";
            case CONFIG_CRYPTO_MAP: return hostname + "(config-crypto-map)#";
        }
        return hostname + ">";
    }

    public void appendGuiCommand(String cmd, String targetContext) {
        if (targetContext.equals("Settings")) {
            asaCommands.add(hostname + "(config)#" + cmd);
        } else {
            asaCommands.add(hostname + "(config-if)#" + cmd);
        }
        while (asaCommands.size() > 8) asaCommands.remove(0);
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

    public void executeCliCore(String input, boolean echo) {
        if (FirewallCliEnhancer.handle(
                this,
                input,
                echo
        )) {
            cliScrollOffset = 0;

            if (onStateChange != null) {
                onStateChange.run();
            }

            if (guiCallback != null) {
                guiCallback.run();
            }

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
        if (echo) { cliLines.add(getPrompt() + input); cliScrollOffset = 0; }

        if (lower.equals("help")) {
            showHelp("", false, cliMode);
            return;
        }

        String[] tokens = lower.split("\\s+");
        String first = resolveAlias(tokens[0], cliMode);

        if (first == null) {
            if (echo) appendInvalidMarker(input, tokens[0]);
            return;
        } else if (first.equals("AMBIGUOUS")) {
            if (echo) cliLines.add("% Ambiguous command:  \"" + tokens[0] + "\"");
            return;
        }

        if (cliMode == CliMode.EXEC) {
            if (first.equals("enable")) cliMode = CliMode.PRIVILEGED;
            else if (first.equals("ping") && tokens.length > 1) runPing(tokens[1], echo);
            else if (first.equals("ping")) cliLines.add("% Incomplete command.");
            else if (first.equals("exit") || first.equals("logout")) { /* Simulator */ }
            else if (echo) appendInvalidMarker(input, tokens[0]);
        }
        else if (cliMode == CliMode.PRIVILEGED || cliMode == CliMode.CONFIG) {

            if (first.equals("configure")) {
                if (cliMode == CliMode.PRIVILEGED && tokens.length > 1 && "terminal".startsWith(tokens[1])) cliMode = CliMode.CONFIG;
                else if (cliMode == CliMode.PRIVILEGED && tokens.length == 1) {
                    if (echo) {
                        cliLines.add("Configuring from terminal, memory, or network [terminal]?");
                        cliLines.add("Enter configuration commands, one per line.  End with CNTL/Z.");
                    }
                    cliMode = CliMode.CONFIG;
                }
                else if (echo) appendInvalidMarker(input, tokens.length > 1 ? tokens[1] : tokens[0]);
            }
            else if (first.equals("clear") && tokens.length > 2 && "configure".startsWith(tokens[1]) && "all".startsWith(tokens[2])) {
                parsedAcls.clear(); accessGroups.clear(); routes.clear(); networkObjects.clear();
                ospfNetworks.clear(); bgpNeighbors.clear(); cryptoMaps.clear(); transformSets.clear(); activeSAs.clear();
                if (echo) cliLines.add("Clear configuration complete.");
            }
            else if (first.equals("disable") || first.equals("exit")) {
                if (cliMode == CliMode.CONFIG) cliMode = CliMode.PRIVILEGED;
                else cliMode = CliMode.EXEC;
            }
            else if (first.equals("write") || first.equals("copy")) {
                if (echo) { cliLines.add("Building configuration..."); cliLines.add("Cryptochecksum: 1b2c3d4e 5f6a7b8c"); cliLines.add("[OK]"); }
            }
            else if (first.equals("show")) {
                if (tokens.length == 1) { cliLines.add("% Incomplete command."); return; }
                String second = resolveAlias(tokens[1], cliMode, first);

                if (second == null) { if (echo) appendInvalidMarker(input, tokens[1]); }
                else if (second.equals("AMBIGUOUS")) { if (echo) cliLines.add("% Ambiguous command:  \"" + tokens[1] + "\""); }
                else if (second.equals("version")) runShowVersion(echo);
                else if (second.equals("running-config")) runShowRun(echo);
                else if (second.equals("interface") && tokens.length > 2 && "ip".startsWith(tokens[2])) runShowIpIntBrief(echo);
                else if (second.equals("access-list")) runShowAcl(echo);
                else if (second.equals("conn")) runShowConn(echo);
                else if (second.equals("xlate")) runShowXlate(echo);
                else if (second.equals("crypto")) {
                    if (tokens.length > 3 && tokens[2].equals("ipsec") && tokens[3].equals("sa")) runShowCryptoIpsecSa(echo);
                }
                else if (second.equals("route")) runShowRoute(echo);
                else if (echo) appendInvalidMarker(input, tokens[1]);
            }
            else if (first.equals("ping") && tokens.length > 1) runPing(tokens[1], echo);
            else if (cliMode == CliMode.CONFIG) {
                if (first.equals("interface")) {
                    if (tokens.length == 1) { cliLines.add("% Incomplete command."); return; }
                    String rawIface = cmd.substring(tokens[0].length()).trim().replaceAll("\\s+", "");
                    String lowerIface = rawIface.toLowerCase();
                    String iface = rawIface;

                    if (lowerIface.startsWith("gi") || lowerIface.startsWith("gigabitethernet")) {
                        String num = lowerIface.replace("gigabitethernet", "").replace("gi", "");
                        if (num.startsWith("/")) num = "1" + num; // Handle "gi/1" -> "gi1/1"
                        num = num.replaceAll("/0+", "/"); // Handle "gi1/01" -> "gi1/1"
                        iface = "GigabitEthernet" + num;
                    } else if (lowerIface.startsWith("ma") || lowerIface.startsWith("management")) {
                        iface = "Management1/1";
                    }

                    if (portConfigs.containsKey(iface)) { cliMode = CliMode.CONFIG_IF; cliTarget = iface; }
                    else if (echo) appendInvalidMarker(input, tokens[1]);
                }
                else if (first.equals("hostname") && tokens.length > 1) hostname = cmd.substring(tokens[0].length()).trim();
                else if (first.equals("access-list")) parseAclRule(cmd);
                else if (first.equals("access-group")) {
                    if (tokens.length >= 4 && "in".startsWith(tokens[2]) && "interface".startsWith(tokens[3])) accessGroups.put(tokens[4], tokens[1]);
                }
                else if (first.equals("object") && tokens.length >= 3 && "network".startsWith(tokens[1])) {
                    String name = cmd.substring(tokens[0].length() + tokens[1].length() + 1).trim();
                    cliMode = CliMode.CONFIG_OBJ; cliTarget = name;
                    networkObjects.putIfAbsent(name, new NetworkObject());
                    networkObjects.get(name).name = name;
                }
                else if (first.equals("route")) { routes.add(cmd); recalculateRIB(); }
                else if (first.equals("router")) {
                    if (tokens.length > 2 && tokens[1].equals("ospf")) {
                        try { ospfProcessId = Integer.parseInt(tokens[2]); cliMode = CliMode.CONFIG_ROUTER; cliTarget = "ospf"; } catch (Exception ignored) {}
                    } else if (tokens.length > 2 && tokens[1].equals("bgp")) {
                        try { bgpAsn = Integer.parseInt(tokens[2]); cliMode = CliMode.CONFIG_ROUTER; cliTarget = "bgp"; } catch (Exception ignored) {}
                    }
                }
                else if (first.equals("crypto")) {
                    if (tokens.length > 3 && tokens[1].equals("ikev1") && tokens[2].equals("policy")) {
                        isakmpPolicy = cmd;
                    } else if (tokens.length > 3 && tokens[1].equals("ikev1") && tokens[2].equals("enable")) {
                        String iface = tokens[3];
                        for (Map.Entry<String, PortConfig> entry : portConfigs.entrySet()) {
                            if (entry.getValue().nameif.equals(iface)) entry.getValue().cryptoIkev1Enabled = true;
                        }
                    } else if (tokens.length > 4 && tokens[1].equals("ipsec") && tokens[2].equals("ikev1") && tokens[3].equals("transform-set")) {
                        transformSets.put(tokens[4], cmd);
                    } else if (tokens.length > 3 && tokens[1].equals("map")) {
                        String mapName = tokens[2];
                        int seq = -1;
                        try { seq = Integer.parseInt(tokens[3]); } catch(Exception e) {}

                        if (seq != -1 && tokens.length > 4 && tokens[4].equals("ipsec-isakmp")) {
                            CryptoMap cmap = null;
                            for (CryptoMap m : cryptoMaps) if (m.name.equals(mapName) && m.seq == seq) cmap = m;
                            if (cmap == null) { cmap = new CryptoMap(); cmap.name = mapName; cmap.seq = seq; cryptoMaps.add(cmap); }
                            cliMode = CliMode.CONFIG_CRYPTO_MAP; cliTarget = mapName + ":" + seq;
                        } else if (tokens.length > 4 && tokens[3].equals("interface")) {
                            for (CryptoMap m : cryptoMaps) if (m.name.equals(mapName)) m.appliedInterface = tokens[4];
                        }
                    }
                }
                else if (echo) appendInvalidMarker(input, tokens[0]);
            } else if (echo) appendInvalidMarker(input, tokens[0]);
        }
        else if (cliMode == CliMode.CONFIG_IF) {
            PortConfig pc = portConfigs.get(cliTarget);
            if (first.equals("exit")) cliMode = CliMode.CONFIG;
            else if (first.equals("end")) cliMode = CliMode.PRIVILEGED;
            else if (pc != null) {
                if (first.equals("nameif") && tokens.length > 1) {
                    pc.nameif = tokens[1];
                    pc.securityLevel = tokens[1].equalsIgnoreCase("inside") ? 100 : 0;
                    if (echo) cliLines.add("INFO: Security level for \"" + tokens[1] + "\" set to " + pc.securityLevel + " by default.");
                } else if (first.equals("security-level") && tokens.length > 1) {
                    try { pc.securityLevel = Integer.parseInt(tokens[1]); } catch (Exception ignored) {}
                } else if (first.equals("description") && tokens.length > 1) {
                    pc.description = cmd.substring(tokens[0].length()).trim();
                } else if (first.equals("ip") && tokens.length > 2 && "address".startsWith(tokens[1])) {
                    pc.ipAddress = tokens[2];
                    pc.subnetMask = tokens.length > 3 ? tokens[3] : "255.255.255.0";
                    recalculateRIB();
                } else if (first.equals("shutdown")) pc.up = false;
                else if (first.equals("no") && tokens.length > 1 && "shutdown".startsWith(tokens[1])) pc.up = true;
                else if (first.equals("speed") && tokens.length > 1) pc.speed = tokens[1];
                else if (first.equals("duplex") && tokens.length > 1) pc.duplex = tokens[1];
                else if (echo) appendInvalidMarker(input, tokens[0]);
            }
        }
        else if (cliMode == CliMode.CONFIG_OBJ) {
            NetworkObject obj = networkObjects.get(cliTarget);
            if (first.equals("exit")) cliMode = CliMode.CONFIG;
            else if (first.equals("end")) cliMode = CliMode.PRIVILEGED;
            else if (obj != null) {
                if (first.equals("subnet") && tokens.length > 2) obj.subnet = tokens[1] + " " + tokens[2];
                else if (first.equals("nat")) obj.natCommand = cmd.substring(tokens[0].length()).trim();
                else if (echo) appendInvalidMarker(input, tokens[0]);
            }
        }
        else if (cliMode == CliMode.CONFIG_ROUTER) {
            if (first.equals("exit")) cliMode = CliMode.CONFIG;
            else if (first.equals("end")) cliMode = CliMode.PRIVILEGED;
            else if (first.equals("network") && cliTarget.equals("ospf")) {
                ospfNetworks.add(cmd);
                recalculateRIB();
            }
            else if (first.equals("neighbor") && cliTarget.equals("bgp")) {
                bgpNeighbors.add(cmd);
            }
        }
        else if (cliMode == CliMode.CONFIG_CRYPTO_MAP) {
            if (first.equals("exit")) cliMode = CliMode.CONFIG;
            else if (first.equals("end")) cliMode = CliMode.PRIVILEGED;
            else {
                String[] parts = cliTarget.split(":");
                String mapName = parts[0];
                int seq = Integer.parseInt(parts[1]);
                CryptoMap cmap = null;
                for (CryptoMap m : cryptoMaps) if (m.name.equals(mapName) && m.seq == seq) cmap = m;

                if (cmap != null) {
                    if (first.equals("match") && tokens.length > 2 && tokens[1].equals("address")) cmap.matchAcl = tokens[2];
                    else if (first.equals("set") && tokens.length > 2 && tokens[1].equals("peer")) cmap.peerIp = tokens[2];
                    else if (first.equals("set") && tokens.length > 3 && tokens[1].equals("ikev1") && tokens[2].equals("transform-set")) cmap.transformSet = tokens[3];
                    else if (first.equals("set") && tokens.length > 2 && tokens[1].equals("pfs")) cmap.pfsGroup = tokens[2];
                    else if (first.equals("set") && tokens.length > 4 && tokens[1].equals("security-association") && tokens[2].equals("lifetime") && tokens[3].equals("seconds")) {
                        try { cmap.saLifetime = Integer.parseInt(tokens[4]); } catch (Exception ignored) {}
                    }
                }
            }
        }

        if (onStateChange != null) onStateChange.run();
        if (guiCallback != null) guiCallback.run();
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
        List<String> matches = new ArrayList<>();
        List<String> allContexts = new ArrayList<>(List.of(
                "show version", "show running-config", "show interface ip brief", "show access-list", "show conn", "show xlate", "show crypto ipsec sa", "show route", "ping"
        ));

        if (mode == CliMode.EXEC) {
            allContexts.addAll(List.of("enable", "exit", "logout", "help"));
        } else if (mode == CliMode.PRIVILEGED) {
            allContexts.addAll(List.of("configure terminal", "disable", "exit", "write memory", "copy running-config startup-config", "clear configure all", "help"));
        } else if (mode == CliMode.CONFIG) {
            allContexts.addAll(List.of("interface GigabitEthernet", "interface Management", "hostname", "access-list", "access-group", "object network", "route", "router ospf", "router bgp", "crypto isakmp policy", "crypto ikev1 policy", "crypto ikev1 enable", "crypto ipsec ikev1 transform-set", "crypto map", "exit", "end", "no shutdown", "no", "help"));
        } else if (mode == CliMode.CONFIG_IF) {
            allContexts.addAll(List.of("nameif", "security-level", "description", "ip address", "shutdown", "no shutdown", "speed", "duplex", "exit", "end", "help"));
        } else if (mode == CliMode.CONFIG_OBJ) {
            allContexts.addAll(List.of("subnet", "nat", "exit", "end", "help"));
        } else if (mode == CliMode.CONFIG_ROUTER) {
            allContexts.addAll(List.of("network", "neighbor", "exit", "end", "help"));
        } else if (mode == CliMode.CONFIG_CRYPTO_MAP) {
            allContexts.addAll(List.of("match address", "set peer", "set ikev1 transform-set", "set pfs", "set security-association lifetime seconds", "exit", "end", "help"));
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
        cliLines.add("ASA Version 9.14(1)");
        cliLines.add("!");
        cliLines.add("hostname " + hostname);
        cliLines.add("!");
        for (NetworkObject obj : networkObjects.values()) {
            cliLines.add("object network " + obj.name);
            if (!obj.subnet.isEmpty()) cliLines.add(" subnet " + obj.subnet);
            if (!obj.natCommand.isEmpty()) cliLines.add(" nat " + obj.natCommand);
        }
        cliLines.add("!");
        for (Map.Entry<String, PortConfig> entry : portConfigs.entrySet()) {
            cliLines.add("interface " + entry.getKey());
            PortConfig pc = entry.getValue();
            if (!pc.description.isEmpty()) cliLines.add(" description " + pc.description);
            if (!pc.nameif.isEmpty()) cliLines.add(" nameif " + pc.nameif);
            if (!pc.nameif.isEmpty()) cliLines.add(" security-level " + pc.securityLevel);
            if (!pc.ipAddress.equals("unassigned")) cliLines.add(" ip address " + pc.ipAddress + " " + pc.subnetMask);
            if (!pc.up) cliLines.add(" shutdown");
        }
        cliLines.add("!");
        for (ParsedAclRule acl : parsedAcls) cliLines.add(acl.rawCommand);
        for (Map.Entry<String, String> ag : accessGroups.entrySet()) cliLines.add("access-group " + ag.getValue() + " in interface " + ag.getKey());
        for (String route : routes) cliLines.add(route);
        cliLines.add("!");
        if (ospfProcessId != -1) {
            cliLines.add("router ospf " + ospfProcessId);
            for (String net : ospfNetworks) cliLines.add(" " + net);
            cliLines.add("!");
        }
        if (bgpAsn != -1) {
            cliLines.add("router bgp " + bgpAsn);
            cliLines.add(" bgp log-neighbor-changes");
            for (String n : bgpNeighbors) cliLines.add(" " + n);
            cliLines.add("!");
        }

        if (!isakmpPolicy.isEmpty()) cliLines.add(isakmpPolicy);
        for (Map.Entry<String, PortConfig> entry : portConfigs.entrySet()) {
            if (entry.getValue().cryptoIkev1Enabled) cliLines.add("crypto ikev1 enable " + entry.getValue().nameif);
        }
        for (String tset : transformSets.values()) cliLines.add(tset);
        for (CryptoMap cmap : cryptoMaps) {
            if (!cmap.appliedInterface.isEmpty()) cliLines.add("crypto map " + cmap.name + " interface " + cmap.appliedInterface);
            else {
                if (!cmap.matchAcl.isEmpty()) cliLines.add("crypto map " + cmap.name + " " + cmap.seq + " match address " + cmap.matchAcl);
                if (!cmap.peerIp.isEmpty()) cliLines.add("crypto map " + cmap.name + " " + cmap.seq + " set peer " + cmap.peerIp);
                if (!cmap.transformSet.isEmpty()) cliLines.add("crypto map " + cmap.name + " " + cmap.seq + " set ikev1 transform-set " + cmap.transformSet);
                if (!cmap.pfsGroup.isEmpty()) cliLines.add("crypto map " + cmap.name + " " + cmap.seq + " set pfs " + cmap.pfsGroup);
                cliLines.add("crypto map " + cmap.name + " " + cmap.seq + " set security-association lifetime seconds " + cmap.saLifetime);
            }
        }
        cliLines.add("!");
        cliLines.add("class-map inspection_default");
        cliLines.add(" match default-inspection-traffic");
        cliLines.add("!");
        cliLines.add("policy-map global_policy");
        cliLines.add(" class inspection_default");
        cliLines.add("  inspect icmp");
        cliLines.add("!");
        cliLines.add("service-policy global_policy global");
        cliLines.add("!");
    }

    private void runShowCryptoIpsecSa(boolean echo) {
        if (!echo) return;
        if (cryptoMaps.isEmpty() || activeSAs.isEmpty()) { cliLines.add("There are no ipsec security associations"); return; }

        String localIp = "0.0.0.0";
        for (PortConfig pc : portConfigs.values()) {
            if (!pc.ipAddress.equals("unassigned")) {
                localIp = pc.ipAddress;
                break;
            }
        }

        for (IpsecSA sa : activeSAs) {
            cliLines.add("interface: outside");
            cliLines.add("    Crypto map tag: " + sa.cryptoMapName + ", local addr: " + localIp);
            cliLines.add("      local ident (addr/mask/prot/port): (0.0.0.0/0.0.0.0/0/0)");
            cliLines.add("      remote ident (addr/mask/prot/port): (0.0.0.0/0.0.0.0/0/0)");
            cliLines.add("      current_peer: " + sa.peerIp);
            cliLines.add("      status: " + sa.state);
            cliLines.add("      #pkts encaps: " + sa.pktsEncaps + ", #pkts encrypt: " + sa.pktsEncaps);
            cliLines.add("      #pkts decaps: " + sa.pktsDecaps + ", #pkts decrypt: " + sa.pktsDecaps);
            cliLines.add("");
        }
    }

    private void runShowRoute(boolean echo) {
        if (!echo) return;
        cliLines.add("Codes: C - connected, S - static, I - IGRP, R - RIP, M - mobile, B - BGP");
        cliLines.add("       D - EIGRP, EX - EIGRP external, O - OSPF, IA - OSPF inter area");
        cliLines.add("");

        for (RouteEntry re : rib) {
            String code = re.protocol.equals("CONNECTED") ? "C" : re.protocol.equals("STATIC") ? "S" : re.protocol.equals("OSPF") ? "O" : "B";
            String via = re.nextHopIp != null && !re.nextHopIp.isEmpty() ? "via " + re.nextHopIp : "directly connected";
            cliLines.add(code + "\t" + longToIp(re.network) + " " + longToIp(re.mask) + "\t\t" + via + ", " + re.egressInterface.replace("GigabitEthernet", "Gi").replace("Management", "Mgmt"));
        }
    }

    private void runShowIpIntBrief(boolean echo) {
        if (!echo) return;
        cliLines.add("Interface\tIP-Address\tOK? Method\tStatus\tProtocol");
        for (Map.Entry<String, PortConfig> entry : portConfigs.entrySet()) {
            PortConfig pc = entry.getValue();
            String status = pc.up ? "up" : "admin down";
            cliLines.add(entry.getKey().replace("GigabitEthernet", "Gi").replace("Management", "Mgmt") + "\t" + pc.ipAddress + "\tYES unset\t" + status + "\t" + (pc.up ? "up" : "down"));
        }
    }

    private void runShowAcl(boolean echo) {
        if (!echo) return;
        if (parsedAcls.isEmpty()) { cliLines.add("No access lists configured."); return; }
        for (ParsedAclRule acl : parsedAcls) cliLines.add(acl.rawCommand);
    }

    private void runShowConn(boolean echo) {
        if (!echo) return;
        maintainConnectionTable();
        cliLines.add(connectionTable.size() + " in use, " + connectionTable.size() + " most used");
        cliLines.add("Protocol\tOutside\tInside\tState");
        for (ConnectionState c : connectionTable) {
            cliLines.add(c.protocol.toUpperCase() + "\t" + longToIp(c.dstIp) + ":" + c.dstPort + "\t" + longToIp(c.srcIp) + ":" + c.srcPort + "\tUP");
        }
    }

    private void runShowXlate(boolean echo) {
        if (!echo) return;
        cliLines.add(xlateTable.size() + " in use, " + xlateTable.size() + " most used");
        for (String x : xlateTable) cliLines.add(x);
    }

    private void runShowVersion(boolean echo) {
        if (!echo) return;
        cliLines.add("Cisco Adaptive Security Appliance Software Version 9.14(1)");
        cliLines.add("Hardware:   ASA5506, 4096 MB RAM, CPU Atom C2000 1250 MHz");
    }

    private void runPing(String target, boolean echo) {
        if (!echo) return;
        cliLines.add("Type escape sequence to abort.");
        cliLines.add("Sending 5, 100-byte ICMP Echos to " + target + ", timeout is 2 seconds:");
        cliLines.add("!!!!!");
        cliLines.add("Success rate is 100 percent (5/5), round-trip min/avg/max = 1/2/4 ms");
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
            for (String part : parts) result = (result << 8) | Integer.parseInt(part);
            return result;
        } catch (Exception e) { return 0; }
    }

    private String longToIp(long ip) {
        return ((ip >> 24) & 0xFF) + "." + ((ip >> 16) & 0xFF) + "." + ((ip >> 8) & 0xFF) + "." + (ip & 0xFF);
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

        tag.putInt("OspfId", ospfProcessId);
        ListTag ospfNets = new ListTag();
        for (String n : ospfNetworks) { CompoundTag t = new CompoundTag(); t.putString("N", n); ospfNets.add(t); }
        tag.put("OspfNetworks", ospfNets);

        tag.putInt("BgpAsn", bgpAsn);
        ListTag bgpNets = new ListTag();
        for (String n : bgpNeighbors) { CompoundTag t = new CompoundTag(); t.putString("N", n); bgpNets.add(t); }
        tag.put("BgpNeighbors", bgpNets);

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

        if (tag.contains("OspfId")) ospfProcessId = tag.getInt("OspfId");
        if (tag.contains("OspfNetworks")) {
            ospfNetworks.clear();
            ListTag nets = tag.getList("OspfNetworks", Tag.TAG_COMPOUND);
            for (int i = 0; i < nets.size(); i++) ospfNetworks.add(nets.getCompound(i).getString("N"));
        }

        if (tag.contains("BgpAsn")) bgpAsn = tag.getInt("BgpAsn");
        if (tag.contains("BgpNeighbors")) {
            bgpNeighbors.clear();
            ListTag nets = tag.getList("BgpNeighbors", Tag.TAG_COMPOUND);
            for (int i = 0; i < nets.size(); i++) bgpNeighbors.add(nets.getCompound(i).getString("N"));
        }
    }
}
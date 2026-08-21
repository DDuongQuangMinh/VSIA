package com.k1ngtle.vsia.signality.internet.router;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class RouterOsSimulator {
    public enum CliMode {
        USER_EXEC,
        PRIVILEGED_EXEC,
        GLOBAL_CONFIG,
        INTERFACE_CONFIG
    }

    public String displayName = "RT-AC68U";
    public String hostname = "Router";
    public String wlanIp = "192.168.1.1";
    public String wlanMask = "255.255.255.0";
    public String wlanGateway = "";
    public boolean wlanAdminUp = true;
    public boolean forwardingEnabled = true;

    public String lan0Ip = "192.168.1.1";
    public String lan0Mask = "255.255.255.0";
    public String lan1Ip = "192.168.2.1";
    public String lan1Mask = "255.255.255.0";

    public CliMode cliMode = CliMode.USER_EXEC;
    public String cliTarget = "";
    public final List<String> cliLines = new ArrayList<>();
    public final List<String> history = new ArrayList<>();
    public final List<RouteEntry> staticRoutes = new ArrayList<>();

    private transient Runnable stateCallback;

    public RouterOsSimulator(Runnable stateCallback) {
        this.stateCallback = stateCallback;
        resetBanner();
    }

    public void setStateCallback(Runnable callback) {
        this.stateCallback = callback;
    }

    public String getPrompt() {
        return switch (cliMode) {
            case USER_EXEC -> hostname + ">";
            case PRIVILEGED_EXEC -> hostname + "#";
            case GLOBAL_CONFIG -> hostname + "(config)#";
            case INTERFACE_CONFIG -> hostname + "(config-if)#";
        };
    }

    public void resetBanner() {
        cliLines.clear();
        cliLines.add("");
        cliLines.add("VSIA Router OS Software, RT-AC68U Software");
        cliLines.add("Copyright (c) 2026 k1ngtle Systems, Inc.");
        cliLines.add("");
        cliLines.add("RT-AC68U processor with VSIA network simulation.");
        cliLines.add("2 routed LAN interfaces");
        cliLines.add("1 IEEE 802.11 wireless interface");
        cliLines.add("");
        cliLines.add("Press RETURN to get started.");
    }

    public void executeCliCore(String input, boolean echo) {
        if (input == null) return;
        String cmd = input.trim();
        if (cmd.isEmpty()) return;

        if (echo) cliLines.add(getPrompt() + cmd);
        history.add(cmd);
        while (history.size() > 100) history.remove(0);

        String lower = cmd.toLowerCase(Locale.ROOT);

        if (lower.equals("enable") && cliMode == CliMode.USER_EXEC) {
            cliMode = CliMode.PRIVILEGED_EXEC;
            return;
        }
        if (lower.equals("disable") && cliMode == CliMode.PRIVILEGED_EXEC) {
            cliMode = CliMode.USER_EXEC;
            return;
        }
        if ((lower.equals("configure terminal") || lower.equals("conf t"))
                && cliMode == CliMode.PRIVILEGED_EXEC) {
            cliMode = CliMode.GLOBAL_CONFIG;
            return;
        }
        if (lower.equals("end")) {
            cliMode = CliMode.PRIVILEGED_EXEC;
            cliTarget = "";
            return;
        }
        if (lower.equals("exit")) {
            if (cliMode == CliMode.INTERFACE_CONFIG) {
                cliMode = CliMode.GLOBAL_CONFIG;
                cliTarget = "";
            } else if (cliMode == CliMode.GLOBAL_CONFIG) {
                cliMode = CliMode.PRIVILEGED_EXEC;
            } else if (cliMode == CliMode.PRIVILEGED_EXEC) {
                cliMode = CliMode.USER_EXEC;
            }
            return;
        }

        if (lower.equals("show version")) {
            cliLines.add("VSIA Router OS Software, RT-AC68U Software");
            cliLines.add("System image: vsia:rt_ac68u_router");
            cliLines.add("Configuration register is persistent");
            return;
        }
        if (lower.equals("show ip interface brief") || lower.equals("show ip int brief")) {
            cliLines.add("Interface              IP-Address      OK? Method Status                Protocol");
            cliLines.add(String.format("%-22s %-15s YES manual %-21s %s",
                    "lan0", lan0Ip, "up", "up"));
            cliLines.add(String.format("%-22s %-15s YES manual %-21s %s",
                    "lan1", lan1Ip, "up", "up"));
            cliLines.add(String.format("%-22s %-15s YES manual %-21s %s",
                    "wlan0", wlanIp,
                    wlanAdminUp ? "up" : "administratively down",
                    wlanAdminUp ? "up" : "down"));
            return;
        }
        if (lower.equals("show ip route")) {
            cliLines.add("Codes: C - connected, S - static");
            cliLines.add("C    192.168.1.0/24 is directly connected, lan0");
            cliLines.add("C    192.168.2.0/24 is directly connected, lan1");
            for (RouteEntry route : staticRoutes) {
                cliLines.add("S    " + route.network + " via " + route.nextHop);
            }
            if (!wlanGateway.isBlank()) {
                cliLines.add("S*   0.0.0.0/0 via " + wlanGateway);
            }
            return;
        }
        if (lower.equals("show arp") || lower.equals("show ip arp")) {
            cliLines.add("Protocol  Address          Age (min)  Hardware Addr   Type   Interface");
            cliLines.add("ARP entries are maintained by the live VSIA host/router data plane.");
            return;
        }
        if (lower.equals("show history")) {
            int start = Math.max(0, history.size() - 20);
            for (int i = start; i < history.size(); i++) {
                cliLines.add(String.format("%3d  %s", i + 1, history.get(i)));
            }
            return;
        }
        if (lower.equals("show running-config") || lower.equals("show run")) {
            for (String line : runningConfig().split("\n")) cliLines.add(line);
            return;
        }
        if (lower.equals("write memory") || lower.equals("wr")
                || lower.equals("copy running-config startup-config")) {
            cliLines.add("Building configuration...");
            cliLines.add("[OK]");
            changed();
            return;
        }

        if (cliMode == CliMode.GLOBAL_CONFIG) {
            if (lower.startsWith("hostname ")) {
                hostname = cmd.substring(9).trim();
                if (hostname.isBlank()) hostname = "Router";
                changed();
                return;
            }
            if (lower.equals("ip routing")) {
                forwardingEnabled = true;
                changed();
                return;
            }
            if (lower.equals("no ip routing")) {
                forwardingEnabled = false;
                changed();
                return;
            }
            if (lower.startsWith("ip default-gateway ")) {
                wlanGateway = cmd.substring("ip default-gateway ".length()).trim();
                changed();
                return;
            }
            if (lower.equals("no ip default-gateway")) {
                wlanGateway = "";
                changed();
                return;
            }
            if (lower.startsWith("interface ")) {
                String target = normalizeInterface(cmd.substring(10).trim());
                if (target == null) {
                    cliLines.add("% Invalid interface.");
                    return;
                }
                cliTarget = target;
                cliMode = CliMode.INTERFACE_CONFIG;
                return;
            }
            if (lower.startsWith("ip route ")) {
                String[] t = cmd.split("\\s+");
                if (t.length < 5) {
                    cliLines.add("% Incomplete command.");
                    return;
                }
                String prefix = t[2] + "/" + maskToPrefix(t[3]);
                staticRoutes.removeIf(r -> r.network.equals(prefix));
                staticRoutes.add(new RouteEntry(prefix, t[4]));
                if ("0.0.0.0".equals(t[2]) && "0.0.0.0".equals(t[3])) {
                    wlanGateway = t[4];
                }
                changed();
                return;
            }
        }

        if (cliMode == CliMode.INTERFACE_CONFIG) {
            if (lower.equals("shutdown")) {
                if ("wlan0".equals(cliTarget)) wlanAdminUp = false;
                changed();
                return;
            }
            if (lower.equals("no shutdown")) {
                if ("wlan0".equals(cliTarget)) wlanAdminUp = true;
                changed();
                return;
            }
            if (lower.startsWith("ip address ")) {
                String[] t = cmd.split("\\s+");
                if (t.length < 4) {
                    cliLines.add("% Incomplete command.");
                    return;
                }
                setInterfaceAddress(cliTarget, t[2], t[3]);
                changed();
                return;
            }
            if (lower.equals("no ip address")) {
                setInterfaceAddress(cliTarget, "0.0.0.0", "0.0.0.0");
                changed();
                return;
            }
        }

        cliLines.add("% Invalid input detected at '^' marker.");
    }

    private String normalizeInterface(String value) {
        String v = value.toLowerCase(Locale.ROOT).replace(" ", "");
        if (v.equals("wlan0") || v.equals("dot11radio0") || v.equals("wireless0")) return "wlan0";
        if (v.equals("lan0") || v.equals("gigabitethernet0/0/0") || v.equals("gi0/0/0")) return "lan0";
        if (v.equals("lan1") || v.equals("gigabitethernet0/0/1") || v.equals("gi0/0/1")) return "lan1";
        return null;
    }

    private void setInterfaceAddress(String iface, String ip, String mask) {
        if ("wlan0".equals(iface)) {
            wlanIp = ip;
            wlanMask = mask;
        } else if ("lan0".equals(iface)) {
            lan0Ip = ip;
            lan0Mask = mask;
        } else if ("lan1".equals(iface)) {
            lan1Ip = ip;
            lan1Mask = mask;
        }
    }

    public String runningConfig() {
        StringBuilder out = new StringBuilder();
        out.append("version 15.0\n");
        out.append("hostname ").append(hostname).append('\n');
        out.append(forwardingEnabled ? "ip routing\n" : "no ip routing\n");
        appendInterface(out, "GigabitEthernet0/0/0", lan0Ip, lan0Mask, true);
        appendInterface(out, "GigabitEthernet0/0/1", lan1Ip, lan1Mask, true);
        appendInterface(out, "Dot11Radio0", wlanIp, wlanMask, wlanAdminUp);
        if (!wlanGateway.isBlank()) {
            out.append("ip default-gateway ").append(wlanGateway).append('\n');
        }
        for (RouteEntry route : staticRoutes) {
            out.append("ip route ").append(route.network).append(' ').append(route.nextHop).append('\n');
        }
        out.append("end");
        return out.toString();
    }

    private void appendInterface(StringBuilder out, String name, String ip, String mask, boolean up) {
        out.append("interface ").append(name).append('\n');
        if (!"0.0.0.0".equals(ip)) {
            out.append(" ip address ").append(ip).append(' ').append(mask).append('\n');
        }
        out.append(up ? " no shutdown\n" : " shutdown\n");
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
                staticRoutes.add(new RouteEntry(r.getString("Network"), r.getString("NextHop")));
            }
        }
    }

    private void changed() {
        if (stateCallback != null) stateCallback.run();
    }

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

    public record RouteEntry(String network, String nextHop) {}
}

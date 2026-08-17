package com.k1ngtle.vsia.client.screen;

import com.k1ngtle.vsia.world.inventory.FirewallMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class FirewallScreen extends AbstractContainerScreen<FirewallMenu> {

    public enum Tab {
        PHYSICAL("Physical"),
        CONFIG("Config"),
        CLI("CLI"),
        ATTRIBUTES("Attributes");

        public final String label;
        Tab(String label) { this.label = label; }
    }

    private enum CliMode {
        EXEC, PRIVILEGED, CONFIG, CONFIG_IF
    }

    private static class PortConfig {
        boolean up = true;
        String speed = "Auto";
        String duplex = "Auto";
        String ipAddress = "unassigned";
        String subnetMask = "unassigned";
    }

    private class FirewallState {
        final int id;
        String displayName;
        String hostname;
        String macAddress;

        final Map<String, PortConfig> portConfigs = new LinkedHashMap<>();

        final List<String> asaCommands = new ArrayList<>();
        final List<String> cliLines = new ArrayList<>();

        boolean isBooted = false;
        long bootStartTime;
        int bootStep = 0;

        CliMode cliMode = CliMode.EXEC;
        String cliTarget = "";
        String cliInput = "";
        int cliCursorPos = 0;
        int cliScrollOffset = 0;

        FirewallState(int rackId, int fwId, String initialHostname) {
            this.id = fwId;
            this.hostname = initialHostname;
            this.displayName = rackId + "_" + fwId;
            this.bootStartTime = System.currentTimeMillis();

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

        String getPrompt() {
            switch(cliMode) {
                case EXEC: return hostname + ">";
                case PRIVILEGED: return hostname + "#";
                case CONFIG: return hostname + "(config)#";
                case CONFIG_IF: return hostname + "(config-if)#";
            }
            return hostname + ">";
        }

        void executeCliCore(String input, boolean echo) {
            String cmd = input.trim();
            if (cmd.isEmpty() && echo) {
                cliLines.add(getPrompt());
                cliScrollOffset = 0;
                return;
            }

            if (echo) {
                cliLines.add(getPrompt() + cmd);
                cliScrollOffset = 0;
            }

            String lower = cmd.toLowerCase();

            if (cliMode == CliMode.EXEC) {
                if (lower.equals("en") || lower.equals("enable")) cliMode = CliMode.PRIVILEGED;
                else if (echo && !lower.isEmpty()) cliLines.add("% Invalid input detected");
            } else if (cliMode == CliMode.PRIVILEGED) {
                if (lower.equals("conf t") || lower.equals("configure terminal")) cliMode = CliMode.CONFIG;
                else if (lower.equals("disable") || lower.equals("exit")) cliMode = CliMode.EXEC;
                else if (lower.equals("write memory") || lower.equals("wr")) {
                    if (echo) {
                        cliLines.add("Building configuration...");
                        cliLines.add("[OK]");
                    }
                }
                else if (lower.startsWith("show version") && echo) {
                    cliLines.add("Cisco Adaptive Security Appliance Software Version 9.14(1)");
                    cliLines.add("Copyright (c) 1986-2026 by k1ngtle systems, Inc.");
                    cliLines.add("Hardware:   ASA5506, 4096 MB RAM, CPU Atom C2000 1250 MHz");
                    cliLines.add(hostname + " up 1 day 4 hours");
                }
                else if (lower.equals("show running-config") && echo) {
                    cliLines.add("Building configuration...");
                    cliLines.add("!");
                    cliLines.add("hostname " + hostname);
                    cliLines.add("!");
                    for (String p : portConfigs.keySet()) {
                        cliLines.add("interface " + p);
                        PortConfig pc = portConfigs.get(p);
                        if (!pc.ipAddress.equals("unassigned")) cliLines.add(" ip address " + pc.ipAddress + " " + pc.subnetMask);
                        if (!pc.up) cliLines.add(" shutdown");
                        cliLines.add("!");
                    }
                    cliLines.add("!");
                } else if (echo && !lower.isEmpty()) cliLines.add("% Invalid input detected");
            } else if (cliMode == CliMode.CONFIG) {
                if (lower.startsWith("int ") || lower.startsWith("interface ")) {
                    String iface = cmd.substring(lower.startsWith("int ") ? 4 : 10).trim();
                    if (iface.toLowerCase().startsWith("gi")) iface = "GigabitEthernet" + iface.substring(2);
                    else if (iface.toLowerCase().startsWith("ma")) iface = "Management1/1";

                    if (portConfigs.containsKey(iface)) {
                        cliMode = CliMode.CONFIG_IF;
                        cliTarget = iface;
                    } else if (echo) cliLines.add("% Invalid interface");
                } else if (lower.startsWith("hostname ")) {
                    hostname = cmd.substring(9).trim();
                    if (id == 1 && FirewallScreen.this.menu.blockEntity != null) {
                        FirewallScreen.this.menu.blockEntity.setDeviceName(hostname);
                    }
                } else if (lower.equals("exit")) {
                    cliMode = CliMode.PRIVILEGED;
                } else if (echo && !lower.isEmpty()) cliLines.add("% Invalid input detected");
            } else if (cliMode == CliMode.CONFIG_IF) {
                PortConfig pc = portConfigs.get(cliTarget);
                if (pc != null) {
                    if (lower.startsWith("ip address ")) {
                        String[] parts = lower.substring(11).trim().split(" ");
                        if (parts.length >= 2) {
                            pc.ipAddress = parts[0];
                            pc.subnetMask = parts[1];
                        }
                    } else if (lower.startsWith("speed ")) {
                        String s = lower.substring(6).trim();
                        if (s.equals("10") || s.equals("100") || s.equals("1000")) pc.speed = s;
                        else if (s.equals("auto")) pc.speed = "Auto";
                    } else if (lower.startsWith("duplex ")) {
                        String d = lower.substring(7).trim();
                        if (d.equals("half")) pc.duplex = "Half";
                        else if (d.equals("full")) pc.duplex = "Full";
                        else if (d.equals("auto")) pc.duplex = "Auto";
                    } else if (lower.equals("shutdown")) {
                        pc.up = false;
                    } else if (lower.equals("no shutdown")) {
                        pc.up = true;
                    } else if (lower.equals("exit")) {
                        cliMode = CliMode.CONFIG;
                    } else if (echo && !lower.isEmpty()) cliLines.add("% Invalid input detected");
                }
            }
            FirewallScreen.this.updateVisibility();
        }

        void appendGuiCommand(String command, String selectedConfigItem) {
            String pre = hostname + "(config)# ";
            if (portConfigs.containsKey(selectedConfigItem)) pre = hostname + "(config-if)# ";

            asaCommands.add(pre + command);
            if (asaCommands.size() > 8) asaCommands.remove(0);

            executeCliCore(command, false);
        }
    }

    private Tab currentTab = Tab.PHYSICAL;
    private int currentFirewallIndex = 0;
    private int baseId = 1;
    private final FirewallState[] firewalls = new FirewallState[7];

    private String selectedConfigItem = "Settings";
    private boolean isUpdatingVisibility = false;

    // GUI Elements
    private EditBox nameBox;
    private EditBox hostnameBox;

    private EditBox ipv4Box;
    private EditBox subnetMaskBox;

    // Config Tab Scroll State
    private float configScrollOffset = 0.0f;
    private int maxConfigScrollLines = 0;
    private final List<String[]> configTreeItems = new ArrayList<>();

    // Physical Tab Scroll State
    private float physicalScrollOffset = 0.0f;

    private final Random random = new Random();

    public FirewallScreen(FirewallMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 720;
        this.imageHeight = 460;

        String baseName = "ciscoasa";
        if (menu.blockEntity != null) {
            baseId = menu.blockEntity.getDeviceId();
            if (baseId <= 0) baseId = 1;

            baseName = menu.blockEntity.getDeviceName();
            if (baseName.startsWith("Firewall")) {
                baseName = "ASA" + baseId + "_1"; // Use default Cisco ASA naming logic if unmodified
            }
        }

        firewalls[0] = new FirewallState(baseId, 1, baseName);

        for (int i = 1; i < 7; i++) {
            firewalls[i] = new FirewallState(baseId, i + 1, "ASA" + baseId + "_" + (i + 1));
        }
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = 10000;
        this.inventoryLabelY = 10000;

        this.configTreeItems.clear();
        this.configTreeItems.add(new String[]{"GLOBAL", "0xDDDDDD", "0", "header"});
        this.configTreeItems.add(new String[]{"    Settings", "0xFFFFFF", "1", "item"});
        this.configTreeItems.add(new String[]{"    Algorithm Settings", "0xAAAAAA", "0", "item"});
        this.configTreeItems.add(new String[]{"", "0x000000", "0", "empty"});
        this.configTreeItems.add(new String[]{"CLIENTLESS VPN", "0xDDDDDD", "0", "header"});
        this.configTreeItems.add(new String[]{"    Bookmark Manager", "0xAAAAAA", "0", "item"});
        this.configTreeItems.add(new String[]{"    User Manager", "0xAAAAAA", "0", "item"});
        this.configTreeItems.add(new String[]{"", "0x000000", "0", "empty"});
        this.configTreeItems.add(new String[]{"INTERFACE", "0xDDDDDD", "0", "header"});
        for (int i = 1; i <= 8; i++) {
            this.configTreeItems.add(new String[]{"    GigabitEthernet1/" + i, "0xAAAAAA", "0", "item"});
        }
        this.configTreeItems.add(new String[]{"    Management1/1", "0xAAAAAA", "0", "item"});

        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        int contentX = x + 180;

        this.nameBox = new EditBox(this.font, contentX + 85, y + 62, 300, 12, Component.literal("Display Name"));
        this.nameBox.setMaxLength(32);
        this.nameBox.setBordered(false);
        this.nameBox.setTextColor(0xAAAAAA);
        this.nameBox.setResponder(val -> {
            if (!isUpdatingVisibility) firewalls[currentFirewallIndex].displayName = val;
        });
        this.addRenderableWidget(this.nameBox);

        this.hostnameBox = new EditBox(this.font, contentX + 85, y + 79, 300, 12, Component.literal("Hostname"));
        this.hostnameBox.setMaxLength(32);
        this.hostnameBox.setBordered(false);
        this.hostnameBox.setTextColor(0xAAAAAA);
        this.hostnameBox.setResponder(val -> {
            if (!isUpdatingVisibility) {
                firewalls[currentFirewallIndex].hostname = val;
                if (currentFirewallIndex == 0 && this.menu.blockEntity != null) this.menu.blockEntity.setDeviceName(val);
                firewalls[currentFirewallIndex].appendGuiCommand("hostname " + val, selectedConfigItem);
            }
        });
        this.addRenderableWidget(this.hostnameBox);

        this.ipv4Box = new EditBox(this.font, contentX + 225, y + 149, 300, 12, Component.literal("IPv4 Address"));
        this.ipv4Box.setBordered(false);
        this.ipv4Box.setTextColor(0xFFFFFF);
        this.ipv4Box.setResponder(val -> {
            if (!isUpdatingVisibility && "Management1/1".equals(selectedConfigItem)) {
                PortConfig pc = firewalls[currentFirewallIndex].portConfigs.get("Management1/1");
                if (pc != null) {
                    pc.ipAddress = val.isEmpty() ? "unassigned" : val;
                    if (!pc.ipAddress.equals("unassigned")) {
                        firewalls[currentFirewallIndex].appendGuiCommand("ip address " + pc.ipAddress + " " + pc.subnetMask, "Management1/1");
                    }
                }
            }
        });
        this.addRenderableWidget(this.ipv4Box);

        this.subnetMaskBox = new EditBox(this.font, contentX + 225, y + 166, 300, 12, Component.literal("Subnet Mask"));
        this.subnetMaskBox.setBordered(false);
        this.subnetMaskBox.setTextColor(0xFFFFFF);
        this.subnetMaskBox.setResponder(val -> {
            if (!isUpdatingVisibility && "Management1/1".equals(selectedConfigItem)) {
                PortConfig pc = firewalls[currentFirewallIndex].portConfigs.get("Management1/1");
                if (pc != null) {
                    pc.subnetMask = val.isEmpty() ? "unassigned" : val;
                    if (!pc.ipAddress.equals("unassigned") && !pc.subnetMask.equals("unassigned")) {
                        firewalls[currentFirewallIndex].appendGuiCommand("ip address " + pc.ipAddress + " " + pc.subnetMask, "Management1/1");
                    }
                }
            }
        });
        this.addRenderableWidget(this.subnetMaskBox);

        updateVisibility();
    }

    private void updateVisibility() {
        this.isUpdatingVisibility = true;
        FirewallState act = firewalls[currentFirewallIndex];

        boolean isSettings = this.currentTab == Tab.CONFIG && this.selectedConfigItem.equals("Settings");
        boolean isMgmt = this.currentTab == Tab.CONFIG && this.selectedConfigItem.equals("Management1/1");

        if (this.nameBox != null) {
            this.nameBox.setVisible(isSettings);
            if (!this.nameBox.isFocused()) this.nameBox.setValue(act.displayName);
        }
        if (this.hostnameBox != null) {
            this.hostnameBox.setVisible(isSettings);
            if (!this.hostnameBox.isFocused()) this.hostnameBox.setValue(act.hostname);
        }

        if (this.ipv4Box != null) {
            this.ipv4Box.setVisible(isMgmt);
            if (isMgmt) {
                PortConfig pc = act.portConfigs.get("Management1/1");
                if (!this.ipv4Box.isFocused() && pc != null) this.ipv4Box.setValue(pc.ipAddress.equals("unassigned") ? "" : pc.ipAddress);
            }
        }
        if (this.subnetMaskBox != null) {
            this.subnetMaskBox.setVisible(isMgmt);
            if (isMgmt) {
                PortConfig pc = act.portConfigs.get("Management1/1");
                if (!this.subnetMaskBox.isFocused() && pc != null) this.subnetMaskBox.setValue(pc.subnetMask.equals("unassigned") ? "" : pc.subnetMask);
            }
        }

        this.isUpdatingVisibility = false;
    }

    @Override
    public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
        FirewallState act = firewalls[currentFirewallIndex];

        if (this.currentTab == Tab.CLI) {
            if (pKeyCode == 259) { // BACKSPACE
                if (act.cliInput.length() > 0 && act.cliCursorPos > 0) {
                    act.cliInput = act.cliInput.substring(0, act.cliCursorPos - 1) + act.cliInput.substring(act.cliCursorPos);
                    act.cliCursorPos--;
                }
                return true;
            } else if (pKeyCode == 257 || pKeyCode == 335) { // ENTER
                act.executeCliCore(act.cliInput, true);
                act.cliInput = "";
                act.cliCursorPos = 0;
                return true;
            } else if (pKeyCode == 263) { // LEFT
                if (act.cliCursorPos > 0) act.cliCursorPos--;
                return true;
            } else if (pKeyCode == 262) { // RIGHT
                if (act.cliCursorPos < act.cliInput.length()) act.cliCursorPos++;
                return true;
            }
            if (this.minecraft != null && this.minecraft.options.keyInventory.matches(pKeyCode, pScanCode)) {
                return true;
            }
        }

        if (this.currentTab == Tab.CONFIG) {
            boolean handled = false;
            boolean anyFocused = false;

            if (this.nameBox != null && this.nameBox.isVisible() && this.nameBox.isFocused()) { anyFocused = true; if (this.nameBox.keyPressed(pKeyCode, pScanCode, pModifiers)) handled = true; }
            if (this.hostnameBox != null && this.hostnameBox.isVisible() && this.hostnameBox.isFocused()) { anyFocused = true; if (this.hostnameBox.keyPressed(pKeyCode, pScanCode, pModifiers)) handled = true; }
            if (this.ipv4Box != null && this.ipv4Box.isVisible() && this.ipv4Box.isFocused()) { anyFocused = true; if (this.ipv4Box.keyPressed(pKeyCode, pScanCode, pModifiers)) handled = true; }
            if (this.subnetMaskBox != null && this.subnetMaskBox.isVisible() && this.subnetMaskBox.isFocused()) { anyFocused = true; if (this.subnetMaskBox.keyPressed(pKeyCode, pScanCode, pModifiers)) handled = true; }

            if (handled) return true;
            if (anyFocused && this.minecraft != null && this.minecraft.options.keyInventory.matches(pKeyCode, pScanCode)) return true;
        }

        if (pKeyCode == 256) { this.onClose(); return true; }
        return super.keyPressed(pKeyCode, pScanCode, pModifiers);
    }

    @Override
    public boolean charTyped(char pCodePoint, int pModifiers) {
        FirewallState act = firewalls[currentFirewallIndex];

        if (this.currentTab == Tab.CLI && act.isBooted) {
            if (pCodePoint >= 32 && pCodePoint <= 126) {
                act.cliInput = act.cliInput.substring(0, act.cliCursorPos) + pCodePoint + act.cliInput.substring(act.cliCursorPos);
                act.cliCursorPos++;
                return true;
            }
        }
        if (this.currentTab == Tab.CONFIG) {
            if (this.nameBox != null && this.nameBox.isVisible() && this.nameBox.isFocused() && this.nameBox.charTyped(pCodePoint, pModifiers)) return true;
            if (this.hostnameBox != null && this.hostnameBox.isVisible() && this.hostnameBox.isFocused() && this.hostnameBox.charTyped(pCodePoint, pModifiers)) return true;
            if (this.ipv4Box != null && this.ipv4Box.isVisible() && this.ipv4Box.isFocused() && this.ipv4Box.charTyped(pCodePoint, pModifiers)) return true;
            if (this.subnetMaskBox != null && this.subnetMaskBox.isVisible() && this.subnetMaskBox.isFocused() && this.subnetMaskBox.charTyped(pCodePoint, pModifiers)) return true;
        }
        return super.charTyped(pCodePoint, pModifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        FirewallState act = firewalls[currentFirewallIndex];

        if (this.currentTab == Tab.PHYSICAL) {
            float maxScroll = Math.max(0, (7 * 90) - (this.imageHeight - 40));
            if (maxScroll > 0) {
                if (delta > 0 && this.physicalScrollOffset > 0) this.physicalScrollOffset = Math.max(0.0f, this.physicalScrollOffset - 0.1f);
                else if (delta < 0 && this.physicalScrollOffset < 1.0f) this.physicalScrollOffset = Math.min(1.0f, this.physicalScrollOffset + 0.1f);
                return true;
            }
        } else if (this.currentTab == Tab.CLI) {
            int maxScroll = Math.max(0, act.cliLines.size() - ((this.imageHeight - 50) / 12) + 1);
            if (delta > 0 && act.cliScrollOffset < maxScroll) act.cliScrollOffset++;
            else if (delta < 0 && act.cliScrollOffset > 0) act.cliScrollOffset--;
            return true;
        } else if (this.currentTab == Tab.CONFIG) {
            if (mouseX >= (this.width - this.imageWidth) / 2 && mouseX <= (this.width - this.imageWidth) / 2 + 160) {
                if (delta > 0 && this.configScrollOffset > 0) this.configScrollOffset = Math.max(0.0f, this.configScrollOffset - 0.1f);
                else if (delta < 0 && this.configScrollOffset < 1.0f) this.configScrollOffset = Math.min(1.0f, this.configScrollOffset + 0.1f);
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        // Top Main Tabs
        if (mouseY >= y + 10 && mouseY <= y + 30) {
            for (int i = 0; i < Tab.values().length; i++) {
                int tabX = x + 10 + (i * 82);
                if (mouseX >= tabX && mouseX <= tabX + 80) {
                    this.currentTab = Tab.values()[i];
                    updateVisibility();
                    return true;
                }
            }
        }

        // Left Side Firewall Selector Tabs
        if (this.currentTab != Tab.PHYSICAL) {
            int startY = y + 31;
            for (int i = 0; i < 7; i++) {
                int tabY = startY + (i * 40);
                if (mouseX >= x - 30 && mouseX <= x && mouseY >= tabY && mouseY <= tabY + 40) {
                    this.currentFirewallIndex = i;
                    updateVisibility();
                    return true;
                }
            }
        }

        if (this.currentTab == Tab.CONFIG) {
            FirewallState act = firewalls[currentFirewallIndex];
            int sbWidth = 160;
            int listY = y + 31;
            int listHeight = this.imageHeight - 31 - 110;

            // Sidebar Lists
            if (mouseX >= x && mouseX <= x + sbWidth && mouseY >= listY && mouseY <= listY + listHeight) {
                int visibleItemIndex = (int) ((mouseY - listY) / 15);
                int actualIndex = (int) (this.configScrollOffset * this.maxConfigScrollLines) + visibleItemIndex;

                if (actualIndex >= 0 && actualIndex < this.configTreeItems.size()) {
                    String[] item = this.configTreeItems.get(actualIndex);
                    if (item[3].equals("item")) {
                        for (String[] i : this.configTreeItems) if (i[3].equals("item")) { i[2] = "0"; i[1] = "0xAAAAAA"; }
                        item[2] = "1";
                        item[1] = "0xFFFFFF";
                        this.selectedConfigItem = item[0].trim();
                        updateVisibility();
                        return true;
                    }
                }
            }

            int contentX = x + 180;
            if (act.portConfigs.containsKey(this.selectedConfigItem)) {
                PortConfig pc = act.portConfigs.get(this.selectedConfigItem);
                int rightColX = x + 380;

                // Port Status
                if (mouseY >= y + 65 && mouseY <= y + 77 && mouseX >= rightColX + 110 && mouseX <= rightColX + 125) {
                    pc.up = !pc.up;
                    act.appendGuiCommand(pc.up ? "no shutdown" : "shutdown", this.selectedConfigItem);
                    return true;
                }

                // Link Speed
                if (mouseY >= y + 90 && mouseY <= y + 102) {
                    if (mouseX >= rightColX - 80 && mouseX < rightColX - 10) {
                        pc.speed = "1000"; act.appendGuiCommand("speed 1000", this.selectedConfigItem); return true;
                    }
                    if (mouseX >= rightColX && mouseX < rightColX + 65) {
                        pc.speed = "100"; act.appendGuiCommand("speed 100", this.selectedConfigItem); return true;
                    }
                    if (mouseX >= rightColX + 70 && mouseX < rightColX + 130) {
                        pc.speed = "10"; act.appendGuiCommand("speed 10", this.selectedConfigItem); return true;
                    }
                    if (mouseX >= rightColX + 140 && mouseX < rightColX + 190) {
                        pc.speed = "Auto"; act.appendGuiCommand("speed auto", this.selectedConfigItem); return true;
                    }
                }

                // Duplex
                if (mouseY >= y + 115 && mouseY <= y + 127) {
                    if (mouseX >= rightColX && mouseX < rightColX + 75) {
                        pc.duplex = "Half"; act.appendGuiCommand("duplex half", this.selectedConfigItem); return true;
                    }
                    if (mouseX >= rightColX + 80 && mouseX < rightColX + 150) {
                        pc.duplex = "Full"; act.appendGuiCommand("duplex full", this.selectedConfigItem); return true;
                    }
                    if (mouseX >= rightColX + 160 && mouseX < rightColX + 210) {
                        pc.duplex = "Auto"; act.appendGuiCommand("duplex auto", this.selectedConfigItem); return true;
                    }
                }
            }

            for (EditBox box : new EditBox[]{nameBox, hostnameBox, ipv4Box, subnetMaskBox}) {
                if (box != null && box.isVisible()) {
                    if (box.mouseClicked(mouseX, mouseY, button)) { box.setFocused(true); return true; }
                    else box.setFocused(false);
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g);
        super.render(g, mouseX, mouseY, partialTick);

        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        g.fill(x, y + 30, x + this.imageWidth, y + this.imageHeight, 0xFF1E1E1E);
        g.fill(x, y + 30, x + this.imageWidth, y + 31, 0xFF444444);
        g.fill(x, y + 30, x + 1, y + this.imageHeight, 0xFF444444);
        g.fill(x + this.imageWidth - 1, y + 30, x + this.imageWidth, y + this.imageHeight, 0xFF444444);
        g.fill(x, y + this.imageHeight - 1, x + this.imageWidth, y + this.imageHeight, 0xFF444444);

        for (int i = 0; i < Tab.values().length; i++) {
            int tabX = x + 10 + (i * 82);
            boolean isActive = (this.currentTab == Tab.values()[i]);

            int bgColor = isActive ? 0xFF1E1E1E : 0xFF2D2D2D;
            int textColor = isActive ? 0xFFFFFFFF : 0xFFAAAAAA;

            g.fill(tabX, y + 10, tabX + 80, y + 31, bgColor);
            g.fill(tabX, y + 10, tabX + 80, y + 11, 0xFFD32F2F); // Cisco Red Top Highlight
            g.fill(tabX, y + 10, tabX + 1, y + 31, 0xFF444444);
            g.fill(tabX + 79, y + 10, tabX + 80, y + 31, 0xFF444444);

            if (!isActive) g.fill(tabX, y + 30, tabX + 80, y + 31, 0xFFD32F2F); // Fill bottom line with Red when inactive

            int textWidth = this.font.width(Tab.values()[i].label);
            g.drawString(this.font, Tab.values()[i].label, tabX + (40 - textWidth / 2), y + 16, textColor, false);
        }

        renderFirewallSelector(g, x, y);

        switch (this.currentTab) {
            case PHYSICAL -> renderPhysicalTab(g, x, y, mouseX, mouseY);
            case CONFIG -> renderConfigTab(g, x, y, mouseX, mouseY);
            case CLI -> renderCLITab(g, x, y);
            case ATTRIBUTES -> renderAttributesTab(g, x, y);
        }
    }

    private void renderFirewallSelector(GuiGraphics g, int x, int y) {
        if (this.currentTab != Tab.PHYSICAL) {
            int startY = y + 31;
            int[] colors = {0xFFD32F2F, 0xFFE6A23C, 0xFFB82DB8, 0xFF22C55E, 0xFF0092C8, 0xFFEAB308, 0xFF06B6D4};

            for (int i = 0; i < 7; i++) {
                int tabY = startY + (i * 40);
                boolean isActive = (i == currentFirewallIndex);
                int bgColor = isActive ? 0xFF1E1E1E : 0xFF2A2A2A;

                g.fill(x - 30, tabY, x, tabY + 40, bgColor);
                g.fill(x - 30, tabY, x - 26, tabY + 40, colors[i]);

                g.drawString(this.font, baseId + "_" + (i + 1), x - 24, tabY + 16, isActive ? 0xFFFFFFFF : 0xFFAAAAAA, false);

                g.fill(x - 30, tabY, x, tabY + 1, 0xFF444444);
                g.fill(x - 30, tabY + 39, x, tabY + 40, 0xFF444444);
            }
            g.fill(x - 30, y + 31, x - 29, y + 31 + (7 * 40), 0xFF444444);
        }
    }

    private void renderPhysicalTab(GuiGraphics g, int x, int y, int mouseX, int mouseY) {
        g.drawString(this.font, "Physical Device View", x + 20, y + 45, 0xFFFFFF, false);

        int viewHeight = this.imageHeight - 60;
        g.enableScissor(x, y + 60, x + this.imageWidth, y + 60 + viewHeight);

        int contentHeight = 7 * 90;
        int maxScrollY = Math.max(0, contentHeight - viewHeight);
        int currentScrollY = (int)(this.physicalScrollOffset * maxScrollY);

        long time = System.currentTimeMillis();

        for (int fwIdx = 0; fwIdx < 7; fwIdx++) {
            FirewallState act = firewalls[fwIdx];
            int sX = x + 20;
            int sY = y + 65 + (fwIdx * 90) - currentScrollY;

            if (sY > y + this.imageHeight || sY + 80 < y + 60) continue;

            int sW = 680;
            int sH = 80;

            // Chassis Frame
            g.fill(sX, sY, sX + sW, sY + sH, 0xFF2B2D31);
            g.fill(sX + 1, sY + 1, sX + sW - 1, sY + sH - 1, 0xFF35383B);

            // Side Vent Array
            g.fill(sX + 10, sY + 15, sX + 60, sY + 65, 0xFF222222);
            for(int hole = 0; hole < 6; hole++) {
                g.fill(sX + 15, sY + 20 + (hole * 7), sX + 55, sY + 23 + (hole * 7), 0xFF111111);
            }

            // Chassis Branding
            g.pose().pushPose();
            g.pose().translate(sX + 80, sY + 20, 0);
            g.pose().scale(1.2f, 1.2f, 1.0f);
            g.drawString(this.font, "SECURE GATEWAY", 0, 0, 0xFFD32F2F, false);
            g.pose().popPose();
            g.drawString(this.font, "FW-Edge-PRO", sX + 80, sY + 40, 0xFFAAAAAA, false);

            // Status Indicators
            int ledX = sX + 260;
            int ledY = sY + 20;
            g.drawString(this.font, "PWR", ledX, ledY - 10, 0xFFAAAAAA, false);
            g.fill(ledX + 4, ledY, ledX + 12, ledY + 8, act.isBooted ? 0xFF22C55E : 0xFFE6A23C);

            g.drawString(this.font, "SYS", ledX + 30, ledY - 10, 0xFFAAAAAA, false);
            int sysColor = act.isBooted ? ((time % 1000) > 500 ? 0xFF22C55E : 0xFF16823B) : 0xFF444444;
            g.fill(ledX + 34, ledY, ledX + 42, ledY + 8, sysColor);

            // Interface Blocks
            int portStartX = sX + 380;
            int portStartY = sY + 20;

            drawPhysicalPort(g, portStartX - 40, portStartY, "MGMT", act.portConfigs.get("Management1/1"), act.isBooted, time, 0);

            for (int i = 0; i < 8; i++) {
                int px = portStartX + (i * 30);
                drawPhysicalPort(g, px, portStartY, "Gi1/" + (i + 1), act.portConfigs.get("GigabitEthernet1/" + (i + 1)), act.isBooted, time, i + 1);
            }
        }
        g.disableScissor();

        if (maxScrollY > 0) {
            int trackX = x + this.imageWidth - 8;
            g.fill(trackX, y + 60, trackX + 8, y + 60 + viewHeight, 0xFF1A1A1A);
            int thumbHeight = Math.max(20, (int)((viewHeight / (float)contentHeight) * viewHeight));
            int thumbY = y + 60 + (int)(this.physicalScrollOffset * (viewHeight - thumbHeight));
            g.fill(trackX, thumbY, trackX + 8, thumbY + thumbHeight, 0xFF555555);
        }
    }

    private void drawPhysicalPort(GuiGraphics g, int px, int py, String label, PortConfig pc, boolean isBooted, long time, int index) {
        g.fill(px, py, px + 22, py + 22, 0xFF000000);
        g.fill(px + 3, py + 5, px + 19, py + 18, 0xFF111111);

        boolean up = pc != null && pc.up && isBooted;
        int linkColor = up ? (((time + index * 200) % 600) > 300 ? 0xFF22C55E : 0xFF16823B) : 0xFF444444;

        g.fill(px + 3, py + 2, px + 7, py + 4, linkColor);
        g.fill(px + 15, py + 2, px + 19, py + 4, up ? 0xFFE6A23C : 0xFF444444);

        g.pose().pushPose();
        g.pose().translate(px + 2, py + 26, 0);
        g.pose().scale(0.6f, 0.6f, 1.0f);
        g.drawString(this.font, label, 0, 0, 0xFFAAAAAA, false);
        g.pose().popPose();
    }

    private void renderConfigTab(GuiGraphics g, int x, int y, int mouseX, int mouseY) {
        FirewallState act = firewalls[currentFirewallIndex];
        int sbWidth = 160;
        int listY = y + 31;
        int terminalHeight = 110;
        int listHeight = this.imageHeight - 31 - terminalHeight;

        // Configuration Nav Tree Panel
        g.fill(x, listY, x + sbWidth, listY + listHeight, 0xFF1E1E1E);
        g.fill(x + sbWidth, listY, x + sbWidth + 1, listY + listHeight, 0xFF444444);

        int totalItemsHeight = this.configTreeItems.size() * 15;
        this.maxConfigScrollLines = Math.max(0, this.configTreeItems.size() - (listHeight / 15) + 1);
        int startIndex = (int)(this.configScrollOffset * this.maxConfigScrollLines);

        int currentY = listY + 10;
        g.enableScissor(x, listY, x + sbWidth, listY + listHeight);
        for (int i = startIndex; i < this.configTreeItems.size() && currentY < listY + listHeight; i++) {
            String[] item = this.configTreeItems.get(i);
            String text = item[0];
            int color = Long.decode(item[1]).intValue();
            boolean isSelected = item[2].equals("1");
            String type = item[3];

            if (type.equals("empty")) { currentY += 10; continue; }

            if (isSelected) g.fill(x, currentY - 3, x + sbWidth, currentY + 11, 0xFF404040);
            if (type.equals("header")) {
                g.fill(x, currentY - 3, x + sbWidth, currentY + 11, 0xFF1E1E1E);
                g.fill(x, currentY + 10, x + sbWidth, currentY + 11, 0xFF444444);
            }
            g.drawString(this.font, text, x + 10, currentY, color, false);
            currentY += 15;
        }
        g.disableScissor();

        // Right Detail Editor Area
        int contentX = x + sbWidth + 20;
        int rightColX = x + 380;

        g.fill(contentX - 20, y + 31, x + this.imageWidth, y + 45, 0xFF1E1E1E);
        g.fill(contentX - 20, y + 45, x + this.imageWidth, y + 46, 0xFF444444);

        String headerText = this.selectedConfigItem.equals("Settings") ? "Global Settings" : this.selectedConfigItem;
        g.drawString(this.font, headerText, contentX - 20 + ((this.imageWidth - sbWidth) - this.font.width(headerText))/2, y + 35, 0xFFFFFF, false);

        if (this.selectedConfigItem.equals("Settings")) {
            drawFormBackground(g, contentX, y, 2, 60);
            g.drawString(this.font, "Display Name", contentX - 5, y + 64, 0xFFFFFF, false);
            g.drawString(this.font, "Hostname", contentX - 5, y + 81, 0xFFFFFF, false);

            int btnY = y + 98;
            g.fill(contentX - 10, btnY - 5, x + this.imageWidth - 10, btnY - 4, 0xFF444444);
            g.drawString(this.font, "NVRAM", contentX - 5, btnY + 4, 0xFFFFFF, false);
            drawBoxBtn(g, contentX + 81, btnY, 180, 16, "Erase");
            drawBoxBtn(g, contentX + 261, btnY, 180, 16, "Save");

            btnY += 22;
            g.fill(contentX - 10, btnY - 5, x + this.imageWidth - 10, btnY - 4, 0xFF444444);
            g.drawString(this.font, "Startup Config", contentX - 5, btnY + 4, 0xFFFFFF, false);
            drawBoxBtn(g, contentX + 81, btnY, 180, 16, "Load...");
            drawBoxBtn(g, contentX + 261, btnY, 180, 16, "Export...");

            btnY += 22;
            g.fill(contentX - 10, btnY - 5, x + this.imageWidth - 10, btnY - 4, 0xFF444444);
            g.drawString(this.font, "Running Config", contentX - 5, btnY + 4, 0xFFFFFF, false);
            drawBoxBtn(g, contentX + 81, btnY, 180, 16, "Export...");
            drawBoxBtn(g, contentX + 261, btnY, 180, 16, "Merge...");

            btnY += 22;
            g.fill(contentX - 10, btnY - 5, x + this.imageWidth - 10, btnY - 4, 0xFF444444);
            g.drawString(this.font, "Device Clock: 00:10:51 Mon Mar 1 1993 UTC", contentX - 5, btnY + 10, 0xAAAAAA, false);

            // Grid Columns for alignment
            g.fill(contentX - 10, y + 93, contentX - 9, btnY - 4, 0xFF444444);
            g.fill(contentX + 80, y + 93, contentX + 81, btnY - 4, 0xFF444444);
            g.fill(contentX + 261, y + 93, contentX + 262, btnY - 4, 0xFF444444);
            g.fill(x + this.imageWidth - 10, y + 93, x + this.imageWidth - 9, btnY - 4, 0xFF444444);

        } else if (this.selectedConfigItem.equals("Management1/1")) {
            PortConfig pc = act.portConfigs.get("Management1/1");

            drawFormBackground(g, contentX, y, 4, 60);

            int rowY = y + 64;
            g.drawString(this.font, "Port Status", contentX - 5, rowY, 0xFFFFFF, false);
            g.drawString(this.font, "On", rightColX + 125, rowY, 0xFFFFFF, false);
            drawCheckbox(g, rightColX + 110, rowY - 1, pc.up);

            rowY += 17;
            g.drawString(this.font, "Link Speed", contentX - 5, rowY, 0xFFFFFF, false);
            drawRadioButton(g, rightColX - 80, rowY - 1, pc.speed.equals("1000"));
            g.drawString(this.font, "1000 Mbps", rightColX - 68, rowY, 0xAAAAAA, false);
            drawRadioButton(g, rightColX, rowY - 1, pc.speed.equals("100"));
            g.drawString(this.font, "100 Mbps", rightColX + 12, rowY, 0xAAAAAA, false);
            drawRadioButton(g, rightColX + 70, rowY - 1, pc.speed.equals("10"));
            g.drawString(this.font, "10 Mbps", rightColX + 82, rowY, 0xAAAAAA, false);
            drawCheckbox(g, rightColX + 140, rowY - 1, pc.speed.equals("Auto"));
            g.drawString(this.font, "Auto", rightColX + 152, rowY, 0xFFFFFF, false);

            rowY += 17;
            g.drawString(this.font, "Duplex", contentX - 5, rowY, 0xFFFFFF, false);
            drawRadioButton(g, rightColX, rowY - 1, pc.duplex.equals("Half"));
            g.drawString(this.font, "Half Duplex", rightColX + 12, rowY, 0xAAAAAA, false);
            drawRadioButton(g, rightColX + 80, rowY - 1, pc.duplex.equals("Full"));
            g.drawString(this.font, "Full Duplex", rightColX + 92, rowY, 0xAAAAAA, false);
            drawCheckbox(g, rightColX + 160, rowY - 1, pc.duplex.equals("Auto"));
            g.drawString(this.font, "Auto", rightColX + 172, rowY, 0xFFFFFF, false);

            rowY += 17;
            g.drawString(this.font, "MAC Address", contentX - 5, rowY, 0xFFFFFF, false);
            g.drawString(this.font, act.macAddress, contentX + 220, rowY, 0xAAAAAA, false);

            // IP Configuration Sub-Section
            int ipSecY = rowY + 30;
            drawFormBackground(g, contentX, y, 2, ipSecY - y);

            g.drawString(this.font, "IP Configuration", contentX - 5, ipSecY + 4, 0xFFFFFF, false);

            ipSecY += 17;
            g.drawString(this.font, "IPv4 Address", contentX - 5, ipSecY + 4, 0xFFFFFF, false);
            // Re-align input box
            if (this.ipv4Box != null) this.ipv4Box.setPosition(contentX + 225, ipSecY + 4);

            ipSecY += 17;
            g.drawString(this.font, "Subnet Mask", contentX - 5, ipSecY + 4, 0xFFFFFF, false);
            if (this.subnetMaskBox != null) this.subnetMaskBox.setPosition(contentX + 225, ipSecY + 4);

            // Vertical divider for inputs
            g.fill(contentX + 220, y + 147, contentX + 221, y + 181, 0xFF444444);

        } else if (act.portConfigs.containsKey(this.selectedConfigItem)) {
            PortConfig pc = act.portConfigs.get(this.selectedConfigItem);
            int rowY = y + 65;

            g.drawString(this.font, "Port Status", contentX, rowY + 1, 0xFFFFFF, false);
            g.drawString(this.font, "On", rightColX + 125, rowY + 1, 0xFFFFFF, false);
            drawCheckbox(g, rightColX + 110, rowY, pc.up);
            rowY += 25;

            g.drawString(this.font, "Link Speed", contentX, rowY + 1, 0xFFFFFF, false);
            drawRadioButton(g, rightColX - 80, rowY, pc.speed.equals("1000"));
            g.drawString(this.font, "1000 Mbps", rightColX - 68, rowY + 1, 0xAAAAAA, false);
            drawRadioButton(g, rightColX, rowY, pc.speed.equals("100"));
            g.drawString(this.font, "100 Mbps", rightColX + 12, rowY + 1, 0xAAAAAA, false);
            drawRadioButton(g, rightColX + 70, rowY, pc.speed.equals("10"));
            g.drawString(this.font, "10 Mbps", rightColX + 82, rowY + 1, 0xAAAAAA, false);
            drawCheckbox(g, rightColX + 140, rowY, pc.speed.equals("Auto"));
            g.drawString(this.font, "Auto", rightColX + 152, rowY + 1, 0xFFFFFF, false);
            rowY += 25;

            g.drawString(this.font, "Duplex", contentX, rowY + 1, 0xFFFFFF, false);
            drawRadioButton(g, rightColX, rowY, pc.duplex.equals("Half"));
            g.drawString(this.font, "Half Duplex", rightColX + 12, rowY + 1, 0xAAAAAA, false);
            drawRadioButton(g, rightColX + 80, rowY, pc.duplex.equals("Full"));
            g.drawString(this.font, "Full Duplex", rightColX + 92, rowY + 1, 0xAAAAAA, false);
            drawCheckbox(g, rightColX + 160, rowY, pc.duplex.equals("Auto"));
            g.drawString(this.font, "Auto", rightColX + 172, rowY + 1, 0xFFFFFF, false);
            rowY += 25;

        } else {
            g.drawString(this.font, "Configuration module not implemented in simulation.", contentX, y + 65, 0x888888, false);
        }

        int terminalY = y + this.imageHeight - terminalHeight;
        g.fill(x, terminalY, x + this.imageWidth, y + this.imageHeight, 0xFF1E1E1E);
        g.fill(x, terminalY, x + this.imageWidth, terminalY + 1, 0xFF444444);
        g.drawString(this.font, "Equivalent ASA Commands", x + 10, terminalY + 5, 0xFFFFFF, false);

        g.fill(x + 10, terminalY + 18, x + this.imageWidth - 10, y + this.imageHeight - 10, 0xFFFFFFFF);
        g.fill(x + 10, terminalY + 18, x + this.imageWidth - 10, terminalY + 19, 0xFF888888);
        g.fill(x + 10, terminalY + 18, x + 11, y + this.imageHeight - 10, 0xFF888888);
        g.fill(x + this.imageWidth - 11, terminalY + 18, x + this.imageWidth - 10, y + this.imageHeight - 10, 0xFF888888);
        g.fill(x + 10, y + this.imageHeight - 11, x + this.imageWidth - 10, y + this.imageHeight - 10, 0xFF888888);

        int txtY = terminalY + 22;
        for (String cmd : act.asaCommands) {
            g.drawString(this.font, cmd, x + 14, txtY, 0x000000, false);
            txtY += 10;
        }
    }

    private void renderCLITab(GuiGraphics g, int x, int y) {
        FirewallState act = firewalls[currentFirewallIndex];

        if (!act.isBooted) {
            long bootTime = System.currentTimeMillis() - act.bootStartTime;
            if (act.bootStep == 0 && bootTime > 500) { act.cliLines.add("System Bootstrap, Version 2.1(0)FW"); act.bootStep++; }
            if (act.bootStep == 1 && bootTime > 1000) { act.cliLines.add("Copyright (c) 1986-2026 by k1ngtle systems, Inc."); act.bootStep++; }
            if (act.bootStep == 2 && bootTime > 1800) { act.cliLines.add("Platform ASA-5506-X, 4096 MB RAM, CPU Atom C2000"); act.bootStep++; }
            if (act.bootStep == 3 && bootTime > 2500) { act.cliLines.add("Loading disk0:/fw-os-9.1.4.bin... [OK]"); act.bootStep++; }
            if (act.bootStep == 4 && bootTime > 3100) { act.cliLines.add("Loading firewall rules... [OK]"); act.bootStep++; }
            if (act.bootStep == 5 && bootTime > 3800) { act.cliLines.add("Starting firewall services... [OK]"); act.bootStep++; }
            if (act.bootStep == 6 && bootTime > 4500) { act.cliLines.add(""); act.cliLines.add("Press RETURN to get started."); act.cliLines.add(""); act.bootStep++; act.isBooted = true; }
        }

        g.fill(x, y + 31, x + this.imageWidth, y + this.imageHeight, 0xFF000000);

        int textY = y + 40;
        int maxLines = (this.imageHeight - 50) / 12;
        int startLogIdx = Math.max(0, act.cliLines.size() - maxLines - act.cliScrollOffset + 1);

        for (int i = startLogIdx; i < act.cliLines.size() - act.cliScrollOffset; i++) {
            g.drawString(this.font, act.cliLines.get(i), x + 10, textY, 0xFFCCCCCC, false);
            textY += 12;
        }

        if (act.isBooted && act.cliScrollOffset == 0) {
            String prompt = act.getPrompt();
            g.drawString(this.font, prompt + act.cliInput, x + 10, textY, 0xFFFFFFFF, false);

            if ((System.currentTimeMillis() / 500) % 2 == 0) {
                int cursorX = x + 10 + this.font.width(prompt) + this.font.width(act.cliInput.substring(0, act.cliCursorPos));
                g.fill(cursorX, textY - 1, cursorX + 6, textY + 9, 0xFFFFFFFF);
            }
        }
    }

    private void renderAttributesTab(GuiGraphics g, int x, int y) {
        int tableX = x + 20;
        int tableY = y + 50;

        g.drawString(this.font, "Attribute", tableX, tableY, 0xFFFFFF, false);
        g.drawString(this.font, "Value", tableX + 150, tableY, 0xFFFFFF, false);
        g.fill(tableX, tableY + 12, x + this.imageWidth - 20, tableY + 13, 0xFF444444);

        int rowY = tableY + 20;
        String[][] attributes = {
                {"Model", "Security Appliance FW-Edge-PRO"}, {"Cost", "$ 995"}, {"Power", "20 W"},
                {"Interfaces", "8x GigabitEthernet, 1x Mgmt"}, {"Form Factor", "Desktop / 1RU Mountable"},
                {"Throughput", "2.5 Gbps"}, {"Concurrent Sessions", "50,000"}
        };
        for (String[] attr : attributes) {
            g.drawString(this.font, attr[0], tableX, rowY, 0xAAAAAA, false);
            g.drawString(this.font, attr[1], tableX + 150, rowY, 0xAAAAAA, false);
            rowY += 20;
        }
    }

    private void drawFormBackground(GuiGraphics g, int contentX, int y, int rows, int startYOffset) {
        int guiX = (this.width - this.imageWidth) / 2;
        int startY = y + startYOffset;
        int endY = startY + (17 * rows) - 1;
        g.fill(contentX - 10, startY, guiX + this.imageWidth - 10, endY, 0xFF1E1E1E);
        for (int i = 0; i <= rows; i++) {
            g.fill(contentX - 10, startY + (i * 17), guiX + this.imageWidth - 10, startY + (i * 17) + 1, 0xFF444444);
        }
        g.fill(contentX - 10, startY, contentX - 9, endY, 0xFF444444);
        g.fill(guiX + this.imageWidth - 10, startY, guiX + this.imageWidth - 9, endY, 0xFF444444);
    }

    private void drawBoxBtn(GuiGraphics g, int x, int y, int w, int h, String text) {
        g.fill(x, y, x + w, y + h, 0xFF2A2A2A);
        g.fill(x, y, x + w, y + 1, 0xFF444444);
        g.fill(x, y + h - 1, x + w, y + h, 0xFF444444);
        g.fill(x, y, x + 1, y + h, 0xFF444444);
        g.fill(x + w - 1, y, x + w, y + h, 0xFF444444);
        int tw = this.font.width(text);
        g.drawString(this.font, text, x + (w - tw)/2, y + 4, 0xFFFFFF, false);
    }

    private void drawCheckbox(GuiGraphics g, int x, int y, boolean checked) {
        g.fill(x, y, x + 9, y + 9, 0xFFFFFFFF);
        g.fill(x, y, x + 9, y + 1, 0xFF888888);
        g.fill(x, y, x + 1, y + 9, 0xFF888888);
        if (checked) g.fill(x + 2, y + 2, x + 7, y + 7, 0xFF0078D7); // Changed from Red to Blue for Management Checkbox match
    }

    private void drawRadioButton(GuiGraphics g, int x, int y, boolean checked) {
        g.fill(x, y, x + 8, y + 8, 0xFF777777);
        g.fill(x + 1, y + 1, x + 7, y + 7, 0xFF1E1E1E);
        if (checked) g.fill(x + 2, y + 2, x + 6, y + 6, 0xFFFFFFFF);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) { }
}
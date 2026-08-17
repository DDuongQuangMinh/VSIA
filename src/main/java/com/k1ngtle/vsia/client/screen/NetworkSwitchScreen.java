package com.k1ngtle.vsia.client.screen;

import com.k1ngtle.vsia.signality.internet.server.SwitchOsSimulator;
import com.k1ngtle.vsia.world.inventory.NetworkSwitchMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.client.gui.screens.Screen;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class NetworkSwitchScreen extends AbstractContainerScreen<NetworkSwitchMenu> {

    public enum Tab {
        PHYSICAL("Physical"),
        CONFIG("Config"),
        CLI("CLI"),
        MAC_TABLE("MAC Table"),
        ATTRIBUTES("Attributes");

        public final String label;
        Tab(String label) { this.label = label; }
    }

    private Tab currentTab = Tab.PHYSICAL;
    private int currentSwitchIndex = 0;
    private final SwitchOsSimulator[] switches = new SwitchOsSimulator[7];

    private String selectedConfigItem = "Settings";
    private boolean isUpdatingVisibility = false;

    private EditBox nameBox;
    private EditBox hostnameBox;
    private EditBox managementIpBox;
    private EditBox managementMaskBox;
    private EditBox vlanNumberBox;
    private EditBox vlanNameBox;
    private EditBox vlanBox;
    private EditBox txRingLimitBox;

    private float configScrollOffset = 0.0f;
    private int maxConfigScrollLines = 0;
    private final List<String[]> configTreeItems = new ArrayList<>();

    private float physicalScrollOffset = 0.0f;
    private final Random random = new Random();

    public NetworkSwitchScreen(NetworkSwitchMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 720;
        this.imageHeight = 460;

        Runnable guiUpdate = () -> updateVisibility();

        if (menu.blockEntity != null) {
            for (int i = 0; i < 7; i++) {
                switches[i] = menu.blockEntity.osSimulators[i];
                switches[i].guiCallback = guiUpdate;
            }
        } else {
            for (int i = 0; i < 7; i++) {
                switches[i] = new SwitchOsSimulator(i, "Switch" + (i + 1), guiUpdate);
            }
        }

        if (menu.blockEntity != null && switches[0].macTable.isEmpty()) {
            List<BlockPos> connections = menu.blockEntity.getConnectedDevices();
            for (int i = 0; i < connections.size(); i++) {
                BlockPos p = connections.get(i);
                String fallbackMac = String.format("00:1A:2B:%02X:%02X:%02X",
                        (p.getX() & 0xFF), (Math.abs(p.getY()) & 0xFF), (Math.abs(p.getZ()) & 0xFF));
                switches[0].macTable.put(fallbackMac, "FastEthernet0/" + (i + 1));
            }
        }
    }

    @Override
    public void onClose() {
        if (switches[0] != null) switches[0].guiCallback = null;
        super.onClose();
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
        this.configTreeItems.add(new String[]{"SWITCHING", "0xDDDDDD", "0", "header"});
        this.configTreeItems.add(new String[]{"    VLAN Database", "0xAAAAAA", "0", "item"});
        this.configTreeItems.add(new String[]{"", "0x000000", "0", "empty"});
        this.configTreeItems.add(new String[]{"INTERFACE", "0xDDDDDD", "0", "header"});
        for (int i = 1; i <= 24; i++) {
            this.configTreeItems.add(new String[]{"    FastEthernet0/" + i, "0xAAAAAA", "0", "item"});
        }
        this.configTreeItems.add(new String[]{"    GigabitEthernet0/1", "0xAAAAAA", "0", "item"});
        this.configTreeItems.add(new String[]{"    GigabitEthernet0/2", "0xAAAAAA", "0", "item"});

        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        int contentX = x + 180;

        this.nameBox = new EditBox(this.font, contentX + 85, y + 62, 300, 12, Component.literal("Display Name"));
        this.nameBox.setMaxLength(32);
        this.nameBox.setBordered(false);
        this.nameBox.setTextColor(0xAAAAAA);
        this.nameBox.setResponder(val -> {
            if (!isUpdatingVisibility) {
                if (currentSwitchIndex == 0 && this.menu.blockEntity != null) this.menu.blockEntity.setSwitchName(val);
            }
        });
        this.addRenderableWidget(this.nameBox);

        this.hostnameBox = new EditBox(this.font, contentX + 85, y + 79, 300, 12, Component.literal("Hostname"));
        this.hostnameBox.setMaxLength(32);
        this.hostnameBox.setBordered(false);
        this.hostnameBox.setTextColor(0xAAAAAA);
        this.hostnameBox.setResponder(val -> {
            if (!isUpdatingVisibility) {
                switches[currentSwitchIndex].switchHostname = val;
                switches[currentSwitchIndex].appendGuiCommand("hostname " + val, selectedConfigItem);
            }
        });
        this.addRenderableWidget(this.hostnameBox);

        this.managementIpBox = new EditBox(this.font, contentX + 85, y + 96, 140, 12, Component.literal("Management IP"));
        this.managementIpBox.setBordered(false);
        this.managementIpBox.setTextColor(0xFFFFFF);
        this.managementIpBox.setResponder(val -> {
            if (!isUpdatingVisibility) {
                switches[currentSwitchIndex].managementIp = val;
                switches[currentSwitchIndex].appendGuiCommand("interface vlan 1", selectedConfigItem);
                switches[currentSwitchIndex].appendGuiCommand("ip address " + val + " " + switches[currentSwitchIndex].managementMask, selectedConfigItem);
            }
        });
        this.addRenderableWidget(this.managementIpBox);

        this.managementMaskBox = new EditBox(this.font, contentX + 310, y + 96, 100, 12, Component.literal("Subnet Mask"));
        this.managementMaskBox.setBordered(false);
        this.managementMaskBox.setTextColor(0xFFFFFF);
        this.addRenderableWidget(this.managementMaskBox);

        this.vlanNumberBox = new EditBox(this.font, contentX + 85, y + 62, 300, 12, Component.literal("VLAN Number"));
        this.vlanNumberBox.setValue("");
        this.vlanNumberBox.setBordered(false);
        this.addRenderableWidget(this.vlanNumberBox);

        this.vlanNameBox = new EditBox(this.font, contentX + 85, y + 79, 300, 12, Component.literal("VLAN Name"));
        this.vlanNameBox.setValue("");
        this.vlanNameBox.setBordered(false);
        this.addRenderableWidget(this.vlanNameBox);

        this.vlanBox = new EditBox(this.font, 0, 0, 64, 12, Component.literal("VLAN"));
        this.vlanBox.setTextColor(0xFFFFFF);
        this.vlanBox.setBordered(false);
        this.vlanBox.setResponder(val -> {
            if (!isUpdatingVisibility && switches[currentSwitchIndex].portConfigs.containsKey(selectedConfigItem)) {
                switches[currentSwitchIndex].portConfigs.get(selectedConfigItem).accessVlan = val;
                switches[currentSwitchIndex].appendGuiCommand("switchport access vlan " + val, selectedConfigItem);
            }
        });
        this.addRenderableWidget(this.vlanBox);

        this.txRingLimitBox = new EditBox(this.font, 0, 0, 60, 12, Component.literal("Tx Ring Limit"));
        this.txRingLimitBox.setTextColor(0xFFFFFF);
        this.txRingLimitBox.setBordered(false);
        this.txRingLimitBox.setResponder(val -> {
            if (!isUpdatingVisibility && switches[currentSwitchIndex].portConfigs.containsKey(selectedConfigItem)) {
                switches[currentSwitchIndex].portConfigs.get(selectedConfigItem).txRingLimit = val;
                switches[currentSwitchIndex].appendGuiCommand("tx-ring-limit " + val, selectedConfigItem);
            }
        });
        this.addRenderableWidget(this.txRingLimitBox);

        updateVisibility();
    }

    private void updateVisibility() {
        this.isUpdatingVisibility = true;

        SwitchOsSimulator act = switches[currentSwitchIndex];

        boolean isSettings = this.currentTab == Tab.CONFIG && this.selectedConfigItem.equals("Settings");
        boolean isVlan = this.currentTab == Tab.CONFIG && this.selectedConfigItem.equals("VLAN Database");
        boolean isInterface = this.currentTab == Tab.CONFIG && act.portConfigs.containsKey(this.selectedConfigItem);

        if (this.nameBox != null) {
            this.nameBox.setVisible(isSettings);
            if (!this.nameBox.isFocused()) this.nameBox.setValue(act.switchHostname);
        }
        if (this.hostnameBox != null) {
            this.hostnameBox.setVisible(isSettings);
            if (!this.hostnameBox.isFocused()) this.hostnameBox.setValue(act.switchHostname);
        }
        if (this.managementIpBox != null) {
            this.managementIpBox.setVisible(isSettings);
            if (!this.managementIpBox.isFocused()) this.managementIpBox.setValue(act.managementIp.equals("unassigned") ? "" : act.managementIp);
        }
        if (this.managementMaskBox != null) {
            this.managementMaskBox.setVisible(isSettings);
            if (!this.managementMaskBox.isFocused()) this.managementMaskBox.setValue(act.managementMask.equals("unassigned") ? "" : act.managementMask);
        }

        if (this.vlanNumberBox != null) this.vlanNumberBox.setVisible(isVlan);
        if (this.vlanNameBox != null) this.vlanNameBox.setVisible(isVlan);

        if (this.vlanBox != null) this.vlanBox.setVisible(isInterface);
        if (this.txRingLimitBox != null) this.txRingLimitBox.setVisible(isInterface);

        if (isInterface) {
            SwitchOsSimulator.PortConfig pc = act.portConfigs.get(this.selectedConfigItem);
            if (!this.vlanBox.isFocused()) this.vlanBox.setValue(pc.accessVlan);
            if (!this.txRingLimitBox.isFocused()) this.txRingLimitBox.setValue(pc.txRingLimit);
        }

        this.isUpdatingVisibility = false;
    }

    private void syncCommand(String... commands) {
        if (this.menu.blockEntity != null) {
            com.k1ngtle.vsia.network.VsiaNetwork.sendToServer(new com.k1ngtle.vsia.network.DeviceCommandPacket(
                    this.menu.blockEntity.getBlockPos(),
                    this.currentSwitchIndex,
                    commands
            ));
        }
    }

    @Override
    public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
        SwitchOsSimulator act = switches[currentSwitchIndex];

        if (this.currentTab == Tab.CLI) {
            if (Screen.hasControlDown()) {
                if (pKeyCode == GLFW.GLFW_KEY_Z) {
                    if (act.cliMode == SwitchOsSimulator.CliMode.CONFIG || act.cliMode == SwitchOsSimulator.CliMode.CONFIG_IF || act.cliMode == SwitchOsSimulator.CliMode.CONFIG_VLAN) {
                        act.cliLines.add(act.getPrompt() + act.cliInput + "^Z");
                        act.executeCliCore("end", false);
                        syncCommand("end");
                        act.cliInput = "";
                        act.cliCursorPos = 0;
                    }
                    return true;
                } else if (pKeyCode == GLFW.GLFW_KEY_C) {
                    act.cliLines.add(act.getPrompt() + act.cliInput + "^C");
                    act.cliInput = "";
                    act.cliCursorPos = 0;
                    return true;
                }
            }

            if (pKeyCode == 258) { // TAB autocomplete
                act.handleAutocomplete();
                return true;
            } else if (pKeyCode == 259) { // BACKSPACE
                if (act.cliInput.length() > 0 && act.cliCursorPos > 0) {
                    act.cliInput = act.cliInput.substring(0, act.cliCursorPos - 1) + act.cliInput.substring(act.cliCursorPos);
                    act.cliCursorPos--;
                }
                return true;
            } else if (pKeyCode == 257 || pKeyCode == 335) { // ENTER
                String cmd = act.cliInput;
                act.executeCliCore(cmd, true);
                syncCommand(cmd);
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
            if (this.managementIpBox != null && this.managementIpBox.isVisible() && this.managementIpBox.isFocused()) { anyFocused = true; if (this.managementIpBox.keyPressed(pKeyCode, pScanCode, pModifiers)) handled = true; }
            if (this.managementMaskBox != null && this.managementMaskBox.isVisible() && this.managementMaskBox.isFocused()) { anyFocused = true; if (this.managementMaskBox.keyPressed(pKeyCode, pScanCode, pModifiers)) handled = true; }
            if (this.vlanNumberBox != null && this.vlanNumberBox.isVisible() && this.vlanNumberBox.isFocused()) { anyFocused = true; if (this.vlanNumberBox.keyPressed(pKeyCode, pScanCode, pModifiers)) handled = true; }
            if (this.vlanNameBox != null && this.vlanNameBox.isVisible() && this.vlanNameBox.isFocused()) { anyFocused = true; if (this.vlanNameBox.keyPressed(pKeyCode, pScanCode, pModifiers)) handled = true; }
            if (this.vlanBox != null && this.vlanBox.isVisible() && this.vlanBox.isFocused()) { anyFocused = true; if (this.vlanBox.keyPressed(pKeyCode, pScanCode, pModifiers)) handled = true; }
            if (this.txRingLimitBox != null && this.txRingLimitBox.isVisible() && this.txRingLimitBox.isFocused()) { anyFocused = true; if (this.txRingLimitBox.keyPressed(pKeyCode, pScanCode, pModifiers)) handled = true; }

            if (handled) return true;

            if (anyFocused && this.minecraft != null && this.minecraft.options.keyInventory.matches(pKeyCode, pScanCode)) {
                return true;
            }
        }

        if (pKeyCode == 256) { this.onClose(); return true; }
        return super.keyPressed(pKeyCode, pScanCode, pModifiers);
    }

    @Override
    public boolean charTyped(char pCodePoint, int pModifiers) {
        SwitchOsSimulator act = switches[currentSwitchIndex];

        if (this.currentTab == Tab.CLI) {
            if (pCodePoint >= 32 && pCodePoint <= 126) {
                act.cliInput = act.cliInput.substring(0, act.cliCursorPos) + pCodePoint + act.cliInput.substring(act.cliCursorPos);
                act.cliCursorPos++;

                // Real-time helper check for exactly typing '?' without pressing enter
                if (pCodePoint == '?') {
                    act.executeCliCore(act.cliInput, true);
                    act.cliInput = act.cliInput.substring(0, act.cliInput.length() - 1);
                    act.cliCursorPos--;
                }
                return true;
            }
        }

        if (this.currentTab == Tab.CONFIG) {
            if (this.nameBox != null && this.nameBox.isVisible() && this.nameBox.isFocused() && this.nameBox.charTyped(pCodePoint, pModifiers)) return true;
            if (this.hostnameBox != null && this.hostnameBox.isVisible() && this.hostnameBox.isFocused() && this.hostnameBox.charTyped(pCodePoint, pModifiers)) return true;
            if (this.managementIpBox != null && this.managementIpBox.isVisible() && this.managementIpBox.isFocused() && this.managementIpBox.charTyped(pCodePoint, pModifiers)) return true;
            if (this.managementMaskBox != null && this.managementMaskBox.isVisible() && this.managementMaskBox.isFocused() && this.managementMaskBox.charTyped(pCodePoint, pModifiers)) return true;
            if (this.vlanNumberBox != null && this.vlanNumberBox.isVisible() && this.vlanNumberBox.isFocused() && this.vlanNumberBox.charTyped(pCodePoint, pModifiers)) return true;
            if (this.vlanNameBox != null && this.vlanNameBox.isVisible() && this.vlanNameBox.isFocused() && this.vlanNameBox.charTyped(pCodePoint, pModifiers)) return true;
            if (this.vlanBox != null && this.vlanBox.isVisible() && this.vlanBox.isFocused() && this.vlanBox.charTyped(pCodePoint, pModifiers)) return true;
            if (this.txRingLimitBox != null && this.txRingLimitBox.isVisible() && this.txRingLimitBox.isFocused() && this.txRingLimitBox.charTyped(pCodePoint, pModifiers)) return true;
        }
        return super.charTyped(pCodePoint, pModifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        SwitchOsSimulator act = switches[currentSwitchIndex];

        if (this.currentTab == Tab.PHYSICAL) {
            float maxScroll = Math.max(0, (7 * 90) - (this.imageHeight - 40));
            if (maxScroll > 0) {
                if (delta > 0 && this.physicalScrollOffset > 0) {
                    this.physicalScrollOffset = Math.max(0.0f, this.physicalScrollOffset - 0.1f);
                } else if (delta < 0 && this.physicalScrollOffset < 1.0f) {
                    this.physicalScrollOffset = Math.min(1.0f, this.physicalScrollOffset + 0.1f);
                }
                return true;
            }
        } else if (this.currentTab == Tab.CLI) {
            int maxScroll = Math.max(0, act.cliLines.size() - ((this.imageHeight - 50) / 12) + 1);
            if (delta > 0 && act.cliScrollOffset < maxScroll) act.cliScrollOffset++;
            else if (delta < 0 && act.cliScrollOffset > 0) act.cliScrollOffset--;
            return true;
        } else if (this.currentTab == Tab.CONFIG) {
            if (mouseX >= (this.width - this.imageWidth) / 2 && mouseX <= (this.width - this.imageWidth) / 2 + 160) {
                if (delta > 0 && this.configScrollOffset > 0) {
                    this.configScrollOffset = Math.max(0.0f, this.configScrollOffset - 0.1f);
                } else if (delta < 0 && this.configScrollOffset < 1.0f) {
                    this.configScrollOffset = Math.min(1.0f, this.configScrollOffset + 0.1f);
                }
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

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

        if (this.currentTab != Tab.PHYSICAL) {
            int startY = y + 31;
            for (int i = 0; i < 7; i++) {
                int tabY = startY + (i * 40);
                if (mouseX >= x - 30 && mouseX <= x && mouseY >= tabY && mouseY <= tabY + 40) {
                    this.currentSwitchIndex = i;
                    updateVisibility();
                    return true;
                }
            }
        }

        if (this.currentTab == Tab.CONFIG) {
            SwitchOsSimulator act = switches[currentSwitchIndex];
            int sbWidth = 160;
            int listY = y + 31;
            int terminalHeight = 110;
            int listHeight = this.imageHeight - 31 - terminalHeight;

            if (mouseX >= x && mouseX <= x + sbWidth && mouseY >= listY && mouseY <= listY + listHeight) {
                int visibleItemIndex = (int) ((mouseY - listY) / 15);
                int actualIndex = (int) (this.configScrollOffset * this.maxConfigScrollLines) + visibleItemIndex;

                if (actualIndex >= 0 && actualIndex < this.configTreeItems.size()) {
                    String[] item = this.configTreeItems.get(actualIndex);
                    if (item[3].equals("item")) {
                        for (String[] i : this.configTreeItems) {
                            if (i[3].equals("item")) { i[2] = "0"; i[1] = "0xAAAAAA"; }
                        }
                        item[2] = "1";
                        item[1] = "0xFFFFFF";
                        this.selectedConfigItem = item[0].trim();

                        if (act.portConfigs.containsKey(this.selectedConfigItem)) {
                            act.iosCommands.add("Switch(config)#interface " + this.selectedConfigItem);
                            act.iosCommands.add("Switch(config-if)#");
                            if (act.iosCommands.size() > 8) act.iosCommands.remove(0);
                        } else if (this.selectedConfigItem.equals("VLAN Database")) {
                            act.iosCommands.add("Switch(config)#interface VLAN Database");
                            act.iosCommands.add("Switch(config-vlan)#");
                            if (act.iosCommands.size() > 8) act.iosCommands.remove(0);
                        }

                        updateVisibility();
                        return true;
                    }
                }
            }

            if (act.portConfigs.containsKey(this.selectedConfigItem)) {
                SwitchOsSimulator.PortConfig pc = act.portConfigs.get(this.selectedConfigItem);
                int rightColX = x + 380;

                if (mouseY >= y + 65 && mouseY <= y + 77 && mouseX >= rightColX + 110 && mouseX <= rightColX + 125) {
                    boolean targetUp = !pc.up;
                    syncCommand("end", "enable", "configure terminal", "interface " + this.selectedConfigItem, targetUp ? "no shutdown" : "shutdown", "end");
                    pc.up = targetUp;
                    act.appendGuiCommand(targetUp ? "no shutdown" : "shutdown", this.selectedConfigItem);
                    return true;
                }

                if (mouseY >= y + 90 && mouseY <= y + 102) {
                    boolean isGigabit = this.selectedConfigItem.startsWith("Gigabit");
                    if (isGigabit && mouseX >= rightColX - 80 && mouseX < rightColX - 10) {
                        syncCommand("end", "enable", "configure terminal", "interface " + this.selectedConfigItem, "speed 1000", "end");
                        pc.speed = "1000"; act.appendGuiCommand("speed 1000", this.selectedConfigItem); return true;
                    }
                    if (mouseX >= rightColX && mouseX < rightColX + 65) {
                        syncCommand("end", "enable", "configure terminal", "interface " + this.selectedConfigItem, "speed 100", "end");
                        pc.speed = "100"; act.appendGuiCommand("speed 100", this.selectedConfigItem); return true;
                    }
                    if (mouseX >= rightColX + 70 && mouseX < rightColX + 130) {
                        syncCommand("end", "enable", "configure terminal", "interface " + this.selectedConfigItem, "speed 10", "end");
                        pc.speed = "10"; act.appendGuiCommand("speed 10", this.selectedConfigItem); return true;
                    }
                    if (mouseX >= rightColX + 140 && mouseX < rightColX + 190) {
                        syncCommand("end", "enable", "configure terminal", "interface " + this.selectedConfigItem, "speed auto", "end");
                        pc.speed = "auto"; act.appendGuiCommand("speed auto", this.selectedConfigItem); return true;
                    }
                }

                if (mouseY >= y + 115 && mouseY <= y + 127) {
                    if (mouseX >= rightColX && mouseX < rightColX + 75) {
                        syncCommand("end", "enable", "configure terminal", "interface " + this.selectedConfigItem, "duplex half", "end");
                        pc.duplex = "half"; act.appendGuiCommand("duplex half", this.selectedConfigItem); return true;
                    }
                    if (mouseX >= rightColX + 80 && mouseX < rightColX + 150) {
                        syncCommand("end", "enable", "configure terminal", "interface " + this.selectedConfigItem, "duplex full", "end");
                        pc.duplex = "full"; act.appendGuiCommand("duplex full", this.selectedConfigItem); return true;
                    }
                    if (mouseX >= rightColX + 160 && mouseX < rightColX + 210) {
                        syncCommand("end", "enable", "configure terminal", "interface " + this.selectedConfigItem, "duplex auto", "end");
                        pc.duplex = "auto"; act.appendGuiCommand("duplex auto", this.selectedConfigItem); return true;
                    }
                }
            }

            if (this.nameBox != null && this.nameBox.isVisible()) {
                if (this.nameBox.mouseClicked(mouseX, mouseY, button)) { this.nameBox.setFocused(true); return true; }
                else this.nameBox.setFocused(false);
            }
            if (this.hostnameBox != null && this.hostnameBox.isVisible()) {
                if (this.hostnameBox.mouseClicked(mouseX, mouseY, button)) { this.hostnameBox.setFocused(true); return true; }
                else this.hostnameBox.setFocused(false);
            }
            if (this.managementIpBox != null && this.managementIpBox.isVisible()) {
                if (this.managementIpBox.mouseClicked(mouseX, mouseY, button)) { this.managementIpBox.setFocused(true); return true; }
                else this.managementIpBox.setFocused(false);
            }
            if (this.managementMaskBox != null && this.managementMaskBox.isVisible()) {
                if (this.managementMaskBox.mouseClicked(mouseX, mouseY, button)) { this.managementMaskBox.setFocused(true); return true; }
                else this.managementMaskBox.setFocused(false);
            }
            if (this.vlanNumberBox != null && this.vlanNumberBox.isVisible()) {
                if (this.vlanNumberBox.mouseClicked(mouseX, mouseY, button)) { this.vlanNumberBox.setFocused(true); return true; }
                else this.vlanNumberBox.setFocused(false);
            }
            if (this.vlanNameBox != null && this.vlanNameBox.isVisible()) {
                if (this.vlanNameBox.mouseClicked(mouseX, mouseY, button)) { this.vlanNameBox.setFocused(true); return true; }
                else this.vlanNameBox.setFocused(false);
            }
            if (this.vlanBox != null && this.vlanBox.isVisible()) {
                if (this.vlanBox.mouseClicked(mouseX, mouseY, button)) { this.vlanBox.setFocused(true); return true; }
                else this.vlanBox.setFocused(false);
            }
            if (this.txRingLimitBox != null && this.txRingLimitBox.isVisible()) {
                if (this.txRingLimitBox.mouseClicked(mouseX, mouseY, button)) { this.txRingLimitBox.setFocused(true); return true; }
                else this.txRingLimitBox.setFocused(false);
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
            g.fill(tabX, y + 10, tabX + 80, y + 11, 0xFF444444);
            g.fill(tabX, y + 10, tabX + 1, y + 31, 0xFF444444);
            g.fill(tabX + 79, y + 10, tabX + 80, y + 31, 0xFF444444);

            if (!isActive) {
                g.fill(tabX, y + 30, tabX + 80, y + 31, 0xFF444444);
            }

            int textWidth = this.font.width(Tab.values()[i].label);
            g.drawString(this.font, Tab.values()[i].label, tabX + (40 - textWidth / 2), y + 16, textColor, false);
        }

        renderSwitchSelector(g, x, y);

        switch (this.currentTab) {
            case PHYSICAL -> renderPhysicalTab(g, x, y, mouseX, mouseY);
            case CONFIG -> renderConfigTab(g, x, y, mouseX, mouseY);
            case CLI -> renderCLITab(g, x, y);
            case MAC_TABLE -> renderMacTableTab(g, x, y);
            case ATTRIBUTES -> renderAttributesTab(g, x, y);
        }
    }

    private void renderSwitchSelector(GuiGraphics g, int x, int y) {
        if (this.currentTab != Tab.PHYSICAL) {
            int startY = y + 31;
            int[] colors = {0xFF0092C8, 0xFFE6A23C, 0xFFB82DB8, 0xFF22C55E, 0xFFEF4444, 0xFFEAB308, 0xFF06B6D4};

            for (int i = 0; i < 7; i++) {
                int tabY = startY + (i * 40);
                boolean isActive = (i == currentSwitchIndex);
                int bgColor = isActive ? 0xFF1E1E1E : 0xFF2A2A2A;

                g.fill(x - 30, tabY, x, tabY + 40, bgColor);
                g.fill(x - 30, tabY, x - 26, tabY + 40, colors[i]);

                g.drawString(this.font, "SW" + (i + 1), x - 22, tabY + 16, isActive ? 0xFFFFFF : 0xFFAAAAAA, false);

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

        List<BlockPos> connections = this.menu.blockEntity != null ? this.menu.blockEntity.getConnectedDevices() : new ArrayList<>();
        long time = System.currentTimeMillis();

        for (int swIdx = 0; swIdx < 7; swIdx++) {
            SwitchOsSimulator act = switches[swIdx];
            int sX = x + 20;
            int sY = y + 65 + (swIdx * 90) - currentScrollY;

            if (sY > y + this.imageHeight || sY + 80 < y + 60) continue;

            int sW = 680;
            int sH = 80;

            g.fill(sX, sY, sX + sW, sY + sH, 0xFF35383B);
            g.fill(sX + 1, sY + 1, sX + sW - 1, sY + sH - 1, 0xFF3D4044);

            g.fill(sX + 10, sY + 15, sX + 60, sY + 65, 0xFF222222);
            for(int hole = 0; hole < 6; hole++) {
                g.fill(sX + 15, sY + 20 + (hole * 7), sX + 55, sY + 23 + (hole * 7), 0xFF111111);
            }

            int portStartX = sX + 100;
            int portStartY = sY + 15;

            for (int i = 0; i < 24; i++) {
                int col = i % 12;
                int row = i / 12;
                int quadGap = (col / 4) * 16;
                int px = portStartX + (col * 24) + quadGap;
                int py = portStartY + (row * 30);

                g.fill(px, py, px + 18, py + 16, 0xFF000000);
                g.fill(px + 3, py + 3, px + 15, py + 13, 0xFF181A1D);

                boolean isConnected = swIdx == 0 && i < connections.size();
                SwitchOsSimulator.PortConfig pc = act.portConfigs.get("FastEthernet0/" + (i + 1));
                boolean isPortUp = pc != null && pc.up;

                int ledColor = 0xFF444444;
                if (isConnected && isPortUp) {
                    if (random.nextFloat() > 0.3f || (time % 500) < 250) ledColor = 0xFF22C55E;
                    else ledColor = 0xFF16823B;
                } else if (isConnected && !isPortUp) {
                    ledColor = 0xFFC58322;
                }

                int ledY = (row == 0) ? py - 6 : py + 19;
                g.fill(px + 6, ledY, px + 12, ledY + 3, ledColor);

                g.pose().pushPose();
                g.pose().translate(px + 5, (row == 0) ? py - 13 : py + 24, 0);
                g.pose().scale(0.6f, 0.6f, 1.0f);
                g.drawString(this.font, String.valueOf(i + 1), 0, 0, 0xFFAAAAAA, false);
                g.pose().popPose();

                if (isConnected) {
                    g.fill(px + 4, py + 4, px + 14, py + 12, 0xFF888888);
                    g.fill(px + 7, py + 8, px + 11, (row == 0) ? py + 25 : py + 45, 0xFF555555);
                }

                if (isConnected && mouseX >= px && mouseX < px + 18 && mouseY >= py && mouseY < py + 16) {
                    BlockPos p = connections.get(i);
                    String vlan = pc != null ? pc.accessVlan : "1";

                    String mac = "Unknown";
                    for(Map.Entry<String, String> m : act.macTable.entrySet()) {
                        if (m.getValue().equals("FastEthernet0/" + (i + 1))) {
                            mac = m.getKey(); break;
                        }
                    }

                    g.renderTooltip(this.font, List.of(
                            Component.literal("Port Fa0/" + (i + 1)),
                            Component.literal("Target: " + p.getX() + ", " + p.getY() + ", " + p.getZ()),
                            Component.literal("VLAN: " + vlan),
                            Component.literal("MAC: " + mac)
                    ), java.util.Optional.empty(), mouseX, mouseY);
                }
            }

            int uplinkX = portStartX + (12 * 24) + (12 / 4 * 16) + 40;
            for (int i = 0; i < 2; i++) {
                int px = uplinkX + (i * 24);
                int py = portStartY + 15;

                g.fill(px, py, px + 18, py + 16, 0xFF000000);
                g.fill(px + 3, py + 3, px + 15, py + 13, 0xFF181A1D);
                g.fill(px + 6, py - 4, px + 12, py - 1, 0xFF444444);

                g.pose().pushPose();
                g.pose().translate(px + 4, py + 22, 0);
                g.pose().scale(0.6f, 0.6f, 1.0f);
                g.drawString(this.font, "Gi" + (i + 1), 0, 0, 0xFFAAAAAA, false);
                g.pose().popPose();
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

    private void renderConfigTab(GuiGraphics g, int x, int y, int mouseX, int mouseY) {
        SwitchOsSimulator act = switches[currentSwitchIndex];
        int sbWidth = 160;
        int listY = y + 31;
        int terminalHeight = 110;
        int listHeight = this.imageHeight - 31 - terminalHeight;

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

            if (type.equals("empty")) {
                currentY += 10;
                continue;
            }

            if (isSelected) g.fill(x, currentY - 3, x + sbWidth, currentY + 11, 0xFF404040);
            if (type.equals("header")) {
                g.fill(x, currentY - 3, x + sbWidth, currentY + 11, 0xFF1E1E1E);
                g.fill(x, currentY + 10, x + sbWidth, currentY + 11, 0xFF444444);
            }
            g.drawString(this.font, text, x + 10, currentY, color, false);
            currentY += 15;
        }
        g.disableScissor();

        if (this.maxConfigScrollLines > 0) {
            int trackX = x + sbWidth - 8;
            g.fill(trackX, listY, trackX + 8, listY + listHeight, 0xFF1A1A1A);
            int thumbHeight = Math.max(20, (int)((listHeight / (float)totalItemsHeight) * listHeight));
            int thumbY = listY + (int)(this.configScrollOffset * (listHeight - thumbHeight));
            g.fill(trackX, thumbY, trackX + 8, thumbY + thumbHeight, 0xFF555555);
        }

        int contentX = x + sbWidth + 20;
        int rightColX = x + 380;

        g.fill(contentX - 20, y + 31, x + this.imageWidth, y + 45, 0xFF1E1E1E);
        g.fill(contentX - 20, y + 45, x + this.imageWidth, y + 46, 0xFF444444);

        String headerText = this.selectedConfigItem;
        if (headerText.equals("Settings")) headerText = "Global Settings";
        if (headerText.equals("VLAN Database")) headerText = "VLAN Configuration";
        int textWidth = this.font.width(headerText);
        g.drawString(this.font, headerText, contentX - 20 + ((this.imageWidth - sbWidth) - textWidth)/2, y + 35, 0xFFFFFF, false);

        if (this.selectedConfigItem.equals("Settings")) {
            g.fill(contentX - 10, y + 60, x + this.imageWidth - 10, y + 76, 0xFF1E1E1E);
            g.fill(contentX - 10, y + 77, x + this.imageWidth - 10, y + 93, 0xFF1E1E1E);
            g.fill(contentX - 10, y + 94, x + this.imageWidth - 10, y + 110, 0xFF1E1E1E);

            g.fill(contentX - 10, y + 60, x + this.imageWidth - 10, y + 61, 0xFF444444);
            g.fill(contentX - 10, y + 76, x + this.imageWidth - 10, y + 77, 0xFF444444);
            g.fill(contentX - 10, y + 93, x + this.imageWidth - 10, y + 94, 0xFF444444);
            g.fill(contentX - 10, y + 110, x + this.imageWidth - 10, y + 111, 0xFF444444);
            g.fill(contentX - 10, y + 60, contentX - 9, y + 111, 0xFF444444);

            g.fill(contentX + 235, y + 93, contentX + 236, y + 111, 0xFF444444);
            g.fill(x + this.imageWidth - 10, y + 60, x + this.imageWidth - 9, y + 111, 0xFF444444);

            g.drawString(this.font, "Display Name", contentX - 5, y + 64, 0xFFFFFF, false);
            g.drawString(this.font, "Hostname", contentX - 5, y + 81, 0xFFFFFF, false);
            g.drawString(this.font, "Management IP", contentX - 5, y + 98, 0xFFFFFF, false);
            g.drawString(this.font, "Subnet Mask", contentX + 245, y + 98, 0xFFFFFF, false);

            int btnY = y + 130;
            int rowHeight = 22;

            g.fill(contentX - 10, btnY - 5, x + this.imageWidth - 10, btnY - 4, 0xFF444444);
            g.drawString(this.font, "NVRAM", contentX - 5, btnY + 4, 0xFFFFFF, false);
            drawBoxBtn(g, contentX + 81, btnY, 180, 16, "Erase");
            drawBoxBtn(g, contentX + 261, btnY, 180, 16, "Save");

            btnY += rowHeight;
            g.fill(contentX - 10, btnY - 5, x + this.imageWidth - 10, btnY - 4, 0xFF444444);
            g.drawString(this.font, "Startup Config", contentX - 5, btnY + 4, 0xFFFFFF, false);
            drawBoxBtn(g, contentX + 81, btnY, 180, 16, "Load...");
            drawBoxBtn(g, contentX + 261, btnY, 180, 16, "Export...");

            btnY += rowHeight;
            g.fill(contentX - 10, btnY - 5, x + this.imageWidth - 10, btnY - 4, 0xFF444444);
            g.drawString(this.font, "Running Config", contentX - 5, btnY + 4, 0xFFFFFF, false);
            drawBoxBtn(g, contentX + 81, btnY, 180, 16, "Export...");
            drawBoxBtn(g, contentX + 261, btnY, 180, 16, "Merge...");

            btnY += rowHeight;
            g.fill(contentX - 10, btnY - 5, x + this.imageWidth - 10, btnY - 4, 0xFF444444);

            g.fill(contentX - 10, y + 125, contentX - 9, btnY - 4, 0xFF444444);
            g.fill(contentX + 80, y + 125, contentX + 81, btnY - 4, 0xFF444444);
            g.fill(contentX + 261, y + 125, contentX + 262, btnY - 4, 0xFF444444);
            g.fill(x + this.imageWidth - 10, y + 125, x + this.imageWidth - 9, btnY - 4, 0xFF444444);

            g.drawString(this.font, "Device Clock: 00:35:31 Tue Mar 2 1993 UTC", contentX - 5, btnY + 10, 0xAAAAAA, false);

        } else if (this.selectedConfigItem.equals("VLAN Database")) {

            g.fill(contentX - 10, y + 60, x + this.imageWidth - 10, y + 76, 0xFF1E1E1E);
            g.fill(contentX - 10, y + 77, x + this.imageWidth - 10, y + 93, 0xFF1E1E1E);
            g.fill(contentX - 10, y + 60, x + this.imageWidth - 10, y + 61, 0xFF444444);
            g.fill(contentX - 10, y + 76, x + this.imageWidth - 10, y + 77, 0xFF444444);
            g.fill(contentX - 10, y + 93, x + this.imageWidth - 10, y + 94, 0xFF444444);
            g.fill(contentX - 10, y + 60, contentX - 9, y + 93, 0xFF444444);
            g.fill(contentX + 80, y + 60, contentX + 81, y + 93, 0xFF444444);
            g.fill(x + this.imageWidth - 10, y + 60, x + this.imageWidth - 9, y + 93, 0xFF444444);

            g.drawString(this.font, "VLAN Number", contentX - 5, y + 64, 0xFFFFFF, false);
            g.drawString(this.font, "VLAN Name", contentX - 5, y + 81, 0xFFFFFF, false);

            int rowY = y + 100;
            drawBoxBtn(g, contentX + 200, rowY, 70, 16, "Add");
            drawBoxBtn(g, contentX + 280, rowY, 70, 16, "Remove");

            int tableY = rowY + 25;
            g.fill(contentX - 10, tableY, x + this.imageWidth - 10, tableY + 14, 0xFF2A2A2A);
            g.fill(contentX - 10, tableY, x + this.imageWidth - 10, tableY + 1, 0xFF444444);
            g.fill(contentX - 10, tableY + 14, x + this.imageWidth - 10, tableY + 15, 0xFF444444);
            g.drawString(this.font, "VLAN No", contentX - 5, tableY + 4, 0xFFFFFF, false);
            g.drawString(this.font, "VLAN Name", contentX + 80, tableY + 4, 0xFFFFFF, false);

            int ty = tableY + 20;
            for (Map.Entry<String, String> v : act.vlanDatabase.entrySet()) {
                g.drawString(this.font, v.getKey(), contentX - 5, ty, 0xFFFFFF, false);
                g.drawString(this.font, v.getValue(), contentX + 80, ty, 0xFFFFFF, false);
                ty += 14;
            }

        } else if (act.portConfigs.containsKey(this.selectedConfigItem)) {
            SwitchOsSimulator.PortConfig pc = act.portConfigs.get(this.selectedConfigItem);
            int rowY = y + 65;

            g.drawString(this.font, "Port Status", contentX, rowY + 1, 0xFFFFFF, false);
            g.drawString(this.font, "On", rightColX + 125, rowY + 1, 0xFFFFFF, false);
            drawCheckbox(g, rightColX + 110, rowY, pc.up);
            rowY += 25;

            g.drawString(this.font, "Link Speed", contentX, rowY + 1, 0xFFFFFF, false);
            boolean isGig = this.selectedConfigItem.startsWith("Gigabit");
            if (isGig) {
                drawRadioButton(g, rightColX - 80, rowY, pc.speed.equals("1000"));
                g.drawString(this.font, "1000 Mbps", rightColX - 68, rowY + 1, 0xAAAAAA, false);
            }
            drawRadioButton(g, rightColX, rowY, pc.speed.equals("100"));
            g.drawString(this.font, "100 Mbps", rightColX + 12, rowY + 1, 0xAAAAAA, false);
            drawRadioButton(g, rightColX + 70, rowY, pc.speed.equals("10"));
            g.drawString(this.font, "10 Mbps", rightColX + 82, rowY + 1, 0xAAAAAA, false);
            drawCheckbox(g, rightColX + 140, rowY, pc.speed.equals("auto"));
            g.drawString(this.font, "Auto", rightColX + 152, rowY + 1, 0xFFFFFF, false);
            rowY += 25;

            g.drawString(this.font, "Duplex", contentX, rowY + 1, 0xFFFFFF, false);
            drawRadioButton(g, rightColX, rowY, pc.duplex.equals("half"));
            g.drawString(this.font, "Half Duplex", rightColX + 12, rowY + 1, 0xAAAAAA, false);
            drawRadioButton(g, rightColX + 80, rowY, pc.duplex.equals("full"));
            g.drawString(this.font, "Full Duplex", rightColX + 92, rowY + 1, 0xAAAAAA, false);
            drawCheckbox(g, rightColX + 160, rowY, pc.duplex.equals("auto"));
            g.drawString(this.font, "Auto", rightColX + 172, rowY + 1, 0xFFFFFF, false);
            rowY += 25;

            g.fill(contentX, rowY - 2, contentX + 80, rowY + 12, 0xFF1E1E1E);
            g.fill(contentX, rowY - 2, contentX + 80, rowY - 1, 0xFF444444);
            g.fill(contentX, rowY - 2, contentX + 1, rowY + 12, 0xFF444444);
            g.fill(contentX, rowY + 11, contentX + 80, rowY + 12, 0xFF444444);
            g.fill(contentX + 79, rowY - 2, contentX + 80, rowY + 12, 0xFF444444);
            g.drawString(this.font, "Access", contentX + 6, rowY + 1, 0xFFFFFF, false);
            g.drawString(this.font, "v", contentX + 70, rowY + 1, 0xFFAAAAAA, false);

            this.vlanBox.setPosition(rightColX + 38, rowY - 1);
            g.drawString(this.font, "VLAN", rightColX, rowY + 1, 0xFFFFFF, false);

            g.fill(rightColX + 36, rowY - 2, rightColX + 110, rowY + 12, 0x00000000);
            g.fill(rightColX + 36, rowY - 2, rightColX + 110, rowY - 1, 0xFF444444);
            g.fill(rightColX + 36, rowY + 11, rightColX + 110, rowY + 12, 0xFF444444);
            g.fill(rightColX + 36, rowY - 2, rightColX + 37, rowY + 12, 0xFF444444);
            g.fill(rightColX + 109, rowY - 2, rightColX + 110, rowY + 12, 0xFF444444);
            g.fill(rightColX + 96, rowY - 2, rightColX + 97, rowY + 12, 0xFF444444);
            g.drawString(this.font, "v", rightColX + 100, rowY + 1, 0xFFAAAAAA, false);

            rowY += 25;

            this.txRingLimitBox.setPosition(x + 180 + 80, rowY - 1);
            g.drawString(this.font, "Tx Ring Limit", contentX, rowY + 1, 0xFFFFFF, false);
        }

        int terminalY = y + this.imageHeight - terminalHeight;
        g.fill(x, terminalY, x + this.imageWidth, y + this.imageHeight, 0xFF1E1E1E);
        g.fill(x, terminalY, x + this.imageWidth, terminalY + 1, 0xFF444444);
        g.drawString(this.font, "Equivalent IOS Commands", x + 10, terminalY + 5, 0xFFFFFF, false);

        g.fill(x + 10, terminalY + 18, x + this.imageWidth - 10, y + this.imageHeight - 10, 0xFFFFFFFF);
        g.fill(x + 10, terminalY + 18, x + this.imageWidth - 10, terminalY + 19, 0xFF888888);
        g.fill(x + 10, terminalY + 18, x + 11, y + this.imageHeight - 10, 0xFF888888);
        g.fill(x + this.imageWidth - 11, terminalY + 18, x + this.imageWidth - 10, y + this.imageHeight - 10, 0xFF888888);
        g.fill(x + 10, y + this.imageHeight - 11, x + this.imageWidth - 10, y + this.imageHeight - 10, 0xFF888888);

        int txtY = terminalY + 22;
        for (int i = 0; i < act.iosCommands.size(); i++) {
            g.drawString(this.font, act.iosCommands.get(i), x + 14, txtY, 0x000000, false);
            txtY += 10;
        }
    }

    private void renderMacTableTab(GuiGraphics g, int x, int y) {
        SwitchOsSimulator act = switches[currentSwitchIndex];
        g.drawString(this.font, "Learned MAC Addresses", x + 20, y + 45, 0xFFFFFF, false);

        int headerY = y + 65;
        g.fill(x + 20, headerY, x + 700, headerY + 15, 0xFF2A2A2A);
        g.fill(x + 20, headerY, x + 700, headerY + 1, 0xFF444444);
        g.fill(x + 20, headerY + 14, x + 700, headerY + 15, 0xFF444444);

        g.drawString(this.font, "VLAN", x + 30, headerY + 4, 0xFFFFFF, false);
        g.drawString(this.font, "MAC Address", x + 120, headerY + 4, 0xFFFFFF, false);
        g.drawString(this.font, "Type", x + 320, headerY + 4, 0xFFFFFF, false);
        g.drawString(this.font, "Ports", x + 450, headerY + 4, 0xFFFFFF, false);

        int ty = headerY + 20;
        for(Map.Entry<String, String> entry : act.macTable.entrySet()) {
            String mac = entry.getKey();
            String port = entry.getValue();
            String vlan = "1";
            if (act.portConfigs.containsKey(port)) {
                vlan = act.portConfigs.get(port).accessVlan;
            }

            g.drawString(this.font, vlan, x + 30, ty, 0xFFAAAAAA, false);
            g.drawString(this.font, mac, x + 120, ty, 0xFF55FF55, false);
            g.drawString(this.font, "DYNAMIC", x + 320, ty, 0xFFAAAAAA, false);
            g.drawString(this.font, port.replace("Ethernet", "Eth"), x + 450, ty, 0xFF0092C8, false);
            ty += 14;
        }

        if (act.macTable.isEmpty()) {
            g.drawString(this.font, "No dynamically learned MAC addresses on active ports.", x + 30, ty + 10, 0xFF888888, false);
        }
    }

    private void renderCLITab(GuiGraphics g, int x, int y) {
        SwitchOsSimulator act = switches[currentSwitchIndex];
        g.fill(x, y + 31, x + this.imageWidth, y + this.imageHeight, 0xFF000000);

        int textY = y + 40;
        int maxLines = (this.imageHeight - 50) / 12;
        int startLogIdx = Math.max(0, act.cliLines.size() - maxLines - act.cliScrollOffset + 1);

        int[] COL_X = {0, 115, 230, 310, 420, 500, 580};

        for (int i = startLogIdx; i < act.cliLines.size() - act.cliScrollOffset; i++) {
            String line = act.cliLines.get(i);

            if (line.contains("\t")) {
                String[] parts = line.split("\t", -1);
                for (int p = 0; p < parts.length; p++) {
                    if (!parts[p].isEmpty()) {
                        int drawX = x + 10 + (p < COL_X.length ? COL_X[p] : COL_X[COL_X.length-1] + (p - COL_X.length + 1) * 80);
                        g.drawString(this.font, parts[p], drawX, textY, 0xFFCCCCCC, false);
                    }
                }
            } else {
                g.drawString(this.font, line, x + 10, textY, 0xFFCCCCCC, false);
            }
            textY += 12;
        }

        if (act.cliScrollOffset == 0) {
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
                {"Model", "Cisco Catalyst 2960-24TT"}, {"Cost", "$ 299"}, {"Power", "15 W"},
                {"Interfaces", "24x FastEthernet, 2x GigabitEthernet"}, {"Form Factor", "1RU Rack-mountable"},
                {"MAC Address Table", "8000 entries max"}, {"Flash Memory", "64 MB"}
        };
        for (String[] attr : attributes) {
            g.drawString(this.font, attr[0], tableX, rowY, 0xAAAAAA, false);
            g.drawString(this.font, attr[1], tableX + 150, rowY, 0xAAAAAA, false);
            rowY += 20;
        }
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
        if (checked) g.fill(x + 2, y + 2, x + 7, y + 7, 0xFF0078D7);
    }

    private void drawRadioButton(GuiGraphics g, int x, int y, boolean checked) {
        g.fill(x, y, x + 8, y + 8, 0xFF777777);
        g.fill(x + 1, y + 1, x + 7, y + 7, 0xFF1E1E1E);
        if (checked) g.fill(x + 2, y + 2, x + 6, y + 6, 0xFFFFFFFF);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) { }
}
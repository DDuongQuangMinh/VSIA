package com.k1ngtle.vsia.client.screen;

import com.k1ngtle.vsia.network.DeviceCommandPacket;
import com.k1ngtle.vsia.network.VsiaNetwork;
import com.k1ngtle.vsia.signality.internet.router.RouterOsSimulator;
import com.k1ngtle.vsia.world.inventory.RtAc68uRouterMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.client.gui.screens.Screen;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public final class RtAc68uRouterScreen extends AbstractContainerScreen<RtAc68uRouterMenu> {

    public enum Tab {
        PHYSICAL("Physical"),
        CONFIG("Config"),
        CLI("CLI"),
        ATTRIBUTES("Attributes");

        public final String label;
        Tab(String label) { this.label = label; }
    }

    private Tab currentTab = Tab.PHYSICAL;
    private String selectedConfigItem = "Settings";
    private final RouterOsSimulator os;
    private boolean isUpdatingVisibility = false;

    // UI State
    private float configScrollOffset = 0.0f;
    private int maxConfigScrollLines = 0;
    private final List<String[]> configTreeItems = new ArrayList<>();
    private float physicalScrollOffset = 0.0f;

    // Fields
    private EditBox displayNameBox;
    private EditBox hostnameBox;
    private EditBox ipBox;
    private EditBox maskBox;
    private EditBox gatewayBox;
    private EditBox routeNetworkBox;
    private EditBox routeMaskBox;
    private EditBox routeNextHopBox;

    public RtAc68uRouterScreen(RtAc68uRouterMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 720;
        this.imageHeight = 460;

        if (menu.blockEntity != null) {
            this.os = menu.blockEntity.routerOs;
            this.os.guiCallback = this::updateVisibility;
        } else {
            this.os = new RouterOsSimulator(this::updateVisibility);
        }
    }

    @Override
    public void onClose() {
        if (this.os != null) this.os.guiCallback = null;
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
        this.configTreeItems.add(new String[]{"", "0x000000", "0", "empty"});
        this.configTreeItems.add(new String[]{"ROUTING", "0xDDDDDD", "0", "header"});
        this.configTreeItems.add(new String[]{"    Static Routes", "0xAAAAAA", "0", "item"});
        this.configTreeItems.add(new String[]{"", "0x000000", "0", "empty"});
        this.configTreeItems.add(new String[]{"INTERFACE", "0xDDDDDD", "0", "header"});
        this.configTreeItems.add(new String[]{"    GigabitEthernet0/0/0", "0xAAAAAA", "0", "item"});
        this.configTreeItems.add(new String[]{"    GigabitEthernet0/0/1", "0xAAAAAA", "0", "item"});
        this.configTreeItems.add(new String[]{"    Dot11Radio0", "0xAAAAAA", "0", "item"});

        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        int contentX = x + 180;

        this.displayNameBox = new EditBox(this.font, contentX + 85, y + 62, 300, 12, Component.literal("Display Name"));
        this.displayNameBox.setMaxLength(32);
        this.displayNameBox.setBordered(false);
        this.displayNameBox.setTextColor(0xAAAAAA);
        this.displayNameBox.setResponder(val -> {
            if (!isUpdatingVisibility) os.displayName = val;
        });
        this.addRenderableWidget(this.displayNameBox);

        this.hostnameBox = new EditBox(this.font, contentX + 85, y + 79, 300, 12, Component.literal("Hostname"));
        this.hostnameBox.setMaxLength(32);
        this.hostnameBox.setBordered(false);
        this.hostnameBox.setTextColor(0xAAAAAA);
        this.hostnameBox.setResponder(val -> {
            if (!isUpdatingVisibility) {
                os.hostname = val;
                os.appendGuiCommand("hostname " + val, selectedConfigItem);
            }
        });
        this.addRenderableWidget(this.hostnameBox);

        this.ipBox = new EditBox(this.font, contentX + 85, y + 96, 140, 12, Component.literal("IP Address"));
        this.ipBox.setBordered(false);
        this.ipBox.setTextColor(0xFFFFFF);
        this.ipBox.setResponder(val -> handleIpUpdate());
        this.addRenderableWidget(this.ipBox);

        this.maskBox = new EditBox(this.font, contentX + 310, y + 96, 100, 12, Component.literal("Subnet Mask"));
        this.maskBox.setBordered(false);
        this.maskBox.setTextColor(0xFFFFFF);
        this.maskBox.setResponder(val -> handleIpUpdate());
        this.addRenderableWidget(this.maskBox);

        this.gatewayBox = new EditBox(this.font, contentX + 85, y + 113, 140, 12, Component.literal("Gateway"));
        this.gatewayBox.setBordered(false);
        this.gatewayBox.setTextColor(0xFFFFFF);
        this.gatewayBox.setResponder(val -> {
            if (!isUpdatingVisibility && "Dot11Radio0".equals(selectedConfigItem)) {
                os.wlanGateway = val;
                if (!val.isBlank() && !val.equals("unassigned")) {
                    os.appendGuiCommand("ip default-gateway " + val, "Settings");
                }
            }
        });
        this.addRenderableWidget(this.gatewayBox);

        this.routeNetworkBox = new EditBox(this.font, contentX - 5, y + 80, 120, 12, Component.literal("Network"));
        this.routeNetworkBox.setBordered(false);
        this.routeNetworkBox.setTextColor(0xFFFFFF);
        this.addRenderableWidget(this.routeNetworkBox);

        this.routeMaskBox = new EditBox(this.font, contentX + 130, y + 80, 110, 12, Component.literal("Mask"));
        this.routeMaskBox.setBordered(false);
        this.routeMaskBox.setTextColor(0xFFFFFF);
        this.addRenderableWidget(this.routeMaskBox);

        this.routeNextHopBox = new EditBox(this.font, contentX + 255, y + 80, 110, 12, Component.literal("Next Hop"));
        this.routeNextHopBox.setBordered(false);
        this.routeNextHopBox.setTextColor(0xFFFFFF);
        this.addRenderableWidget(this.routeNextHopBox);

        updateVisibility();
    }

    private void handleIpUpdate() {
        if (isUpdatingVisibility) return;

        String ip = ipBox.getValue().isEmpty() ? "unassigned" : ipBox.getValue();
        String mask = maskBox.getValue().isEmpty() ? "unassigned" : maskBox.getValue();

        if (selectedConfigItem.equals("GigabitEthernet0/0/0")) {
            os.lan0Ip = ip; os.lan0Mask = mask;
        } else if (selectedConfigItem.equals("GigabitEthernet0/0/1")) {
            os.lan1Ip = ip; os.lan1Mask = mask;
        } else if (selectedConfigItem.equals("Dot11Radio0")) {
            os.wlanIp = ip; os.wlanMask = mask;
        }

        if (!ip.equals("unassigned") && !mask.equals("unassigned")) {
            os.appendGuiCommand("ip address " + ip + " " + mask, selectedConfigItem);
        }
    }

    private void updateVisibility() {
        this.isUpdatingVisibility = true;

        boolean isSettings = this.currentTab == Tab.CONFIG && this.selectedConfigItem.equals("Settings");
        boolean isRouting = this.currentTab == Tab.CONFIG && this.selectedConfigItem.equals("Static Routes");
        boolean isInterface = this.currentTab == Tab.CONFIG && (this.selectedConfigItem.startsWith("Gigabit") || this.selectedConfigItem.equals("Dot11Radio0"));
        boolean isWlan = isInterface && this.selectedConfigItem.equals("Dot11Radio0");

        if (this.displayNameBox != null) {
            this.displayNameBox.setVisible(isSettings);
            if (!this.displayNameBox.isFocused()) this.displayNameBox.setValue(os.displayName);
        }
        if (this.hostnameBox != null) {
            this.hostnameBox.setVisible(isSettings);
            if (!this.hostnameBox.isFocused()) this.hostnameBox.setValue(os.hostname);
        }

        if (this.routeNetworkBox != null) this.routeNetworkBox.setVisible(isRouting);
        if (this.routeMaskBox != null) this.routeMaskBox.setVisible(isRouting);
        if (this.routeNextHopBox != null) this.routeNextHopBox.setVisible(isRouting);

        if (this.ipBox != null) {
            this.ipBox.setVisible(isInterface);
            if (isInterface && !this.ipBox.isFocused()) {
                if (selectedConfigItem.equals("GigabitEthernet0/0/0")) this.ipBox.setValue(os.lan0Ip.equals("unassigned") ? "" : os.lan0Ip);
                else if (selectedConfigItem.equals("GigabitEthernet0/0/1")) this.ipBox.setValue(os.lan1Ip.equals("unassigned") ? "" : os.lan1Ip);
                else if (selectedConfigItem.equals("Dot11Radio0")) this.ipBox.setValue(os.wlanIp.equals("unassigned") ? "" : os.wlanIp);
            }
        }
        if (this.maskBox != null) {
            this.maskBox.setVisible(isInterface);
            if (isInterface && !this.maskBox.isFocused()) {
                if (selectedConfigItem.equals("GigabitEthernet0/0/0")) this.maskBox.setValue(os.lan0Mask.equals("unassigned") ? "" : os.lan0Mask);
                else if (selectedConfigItem.equals("GigabitEthernet0/0/1")) this.maskBox.setValue(os.lan1Mask.equals("unassigned") ? "" : os.lan1Mask);
                else if (selectedConfigItem.equals("Dot11Radio0")) this.maskBox.setValue(os.wlanMask.equals("unassigned") ? "" : os.wlanMask);
            }
        }
        if (this.gatewayBox != null) {
            this.gatewayBox.setVisible(isWlan);
            if (isWlan && !this.gatewayBox.isFocused()) {
                this.gatewayBox.setValue(os.wlanGateway.equals("unassigned") ? "" : os.wlanGateway);
            }
        }

        this.isUpdatingVisibility = false;
    }

    private void syncCommand(String... commands) {
        if (this.menu.blockEntity != null) {
            VsiaNetwork.sendToServer(new DeviceCommandPacket(
                    this.menu.blockEntity.getBlockPos(),
                    0, // Device index 0 for Router
                    commands
            ));
        }
    }

    @Override
    public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
        if (this.currentTab == Tab.CLI) {
            if (Screen.hasControlDown()) {
                if (pKeyCode == GLFW.GLFW_KEY_Z) {
                    if (os.cliMode == RouterOsSimulator.CliMode.GLOBAL_CONFIG || os.cliMode == RouterOsSimulator.CliMode.INTERFACE_CONFIG) {
                        os.cliLines.add(os.getPrompt() + os.cliInput + "^Z");
                        os.executeCliCore("end", false);
                        syncCommand("end");
                        os.cliInput = "";
                        os.cliCursorPos = 0;
                    }
                    return true;
                } else if (pKeyCode == GLFW.GLFW_KEY_C) {
                    os.cliLines.add(os.getPrompt() + os.cliInput + "^C");
                    os.cliInput = "";
                    os.cliCursorPos = 0;
                    return true;
                }
            }

            if (pKeyCode == 258) { // TAB autocomplete
                os.handleAutocomplete();
                return true;
            } else if (pKeyCode == 259) { // BACKSPACE
                if (os.cliInput.length() > 0 && os.cliCursorPos > 0) {
                    os.cliInput = os.cliInput.substring(0, os.cliCursorPos - 1) + os.cliInput.substring(os.cliCursorPos);
                    os.cliCursorPos--;
                }
                return true;
            } else if (pKeyCode == 257 || pKeyCode == 335) { // ENTER
                String cmd = os.cliInput;
                os.executeCliCore(cmd, true);
                syncCommand(cmd);
                os.cliInput = "";
                os.cliCursorPos = 0;
                return true;
            } else if (pKeyCode == 263) { // LEFT
                if (os.cliCursorPos > 0) os.cliCursorPos--;
                return true;
            } else if (pKeyCode == 262) { // RIGHT
                if (os.cliCursorPos < os.cliInput.length()) os.cliCursorPos++;
                return true;
            } else if (pKeyCode == GLFW.GLFW_KEY_UP) {
                if (!os.history.isEmpty()) {
                    os.cliInput = os.history.get(Math.max(0, os.history.size() - 1));
                    os.cliCursorPos = os.cliInput.length();
                }
                return true;
            } else if (pKeyCode == GLFW.GLFW_KEY_DOWN) {
                os.cliInput = "";
                os.cliCursorPos = 0;
                return true;
            }

            if (this.minecraft != null && this.minecraft.options.keyInventory.matches(pKeyCode, pScanCode)) {
                return true;
            }
        }

        if (this.currentTab == Tab.CONFIG) {
            boolean handled = false;
            boolean anyFocused = false;

            for (EditBox box : new EditBox[]{displayNameBox, hostnameBox, ipBox, maskBox, gatewayBox, routeNetworkBox, routeMaskBox, routeNextHopBox}) {
                if (box != null && box.isVisible() && box.isFocused()) {
                    anyFocused = true;
                    if (box.keyPressed(pKeyCode, pScanCode, pModifiers)) handled = true;
                }
            }
            if (handled) return true;
            if (anyFocused && this.minecraft != null && this.minecraft.options.keyInventory.matches(pKeyCode, pScanCode)) return true;
        }

        if (pKeyCode == 256) { this.onClose(); return true; }
        return super.keyPressed(pKeyCode, pScanCode, pModifiers);
    }

    @Override
    public boolean charTyped(char pCodePoint, int pModifiers) {
        if (this.currentTab == Tab.CLI && os.isBooted) {
            if (pCodePoint >= 32 && pCodePoint <= 126) {
                os.cliInput = os.cliInput.substring(0, os.cliCursorPos) + pCodePoint + os.cliInput.substring(os.cliCursorPos);
                os.cliCursorPos++;
                if (pCodePoint == '?') {
                    os.executeCliCore(os.cliInput, true);
                    os.cliInput = os.cliInput.substring(0, os.cliInput.length() - 1);
                    os.cliCursorPos--;
                }
                return true;
            }
        }
        if (this.currentTab == Tab.CONFIG) {
            for (EditBox box : new EditBox[]{displayNameBox, hostnameBox, ipBox, maskBox, gatewayBox, routeNetworkBox, routeMaskBox, routeNextHopBox}) {
                if (box != null && box.isVisible() && box.isFocused() && box.charTyped(pCodePoint, pModifiers)) return true;
            }
        }
        return super.charTyped(pCodePoint, pModifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (this.currentTab == Tab.PHYSICAL) {
            float maxScroll = Math.max(0, 300 - (this.imageHeight - 40));
            if (maxScroll > 0) {
                if (delta > 0 && this.physicalScrollOffset > 0) this.physicalScrollOffset = Math.max(0.0f, this.physicalScrollOffset - 0.1f);
                else if (delta < 0 && this.physicalScrollOffset < 1.0f) this.physicalScrollOffset = Math.min(1.0f, this.physicalScrollOffset + 0.1f);
                return true;
            }
        } else if (this.currentTab == Tab.CLI) {
            int maxScroll = Math.max(0, os.cliLines.size() - ((this.imageHeight - 50) / 12) + 1);
            if (delta > 0 && os.cliScrollOffset < maxScroll) os.cliScrollOffset++;
            else if (delta < 0 && os.cliScrollOffset > 0) os.cliScrollOffset--;
            return true;
        } else if (this.currentTab == Tab.CONFIG) {
            int x = (this.width - this.imageWidth) / 2;
            if (mouseX >= x && mouseX <= x + 160) {
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

        if (this.currentTab == Tab.CONFIG) {
            int sbWidth = 160;
            int listY = y + 31;
            int listHeight = this.imageHeight - 31 - 110;

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

                        if (this.selectedConfigItem.startsWith("Gigabit") || this.selectedConfigItem.equals("Dot11Radio0")) {
                            os.iosCommands.add(os.hostname + "(config)#interface " + this.selectedConfigItem);
                            os.iosCommands.add(os.hostname + "(config-if)#");
                            if (os.iosCommands.size() > 8) os.iosCommands.remove(0);
                        }
                        updateVisibility();
                        return true;
                    }
                }
            }

            int contentX = x + 180;
            if (this.selectedConfigItem.equals("Settings")) {
                if (mouseY >= y + 115 && mouseY <= y + 125) {
                    if (mouseX >= contentX + 85 && mouseX <= contentX + 100) {
                        os.forwardingEnabled = !os.forwardingEnabled;
                        os.appendGuiCommand(os.forwardingEnabled ? "ip routing" : "no ip routing", "Settings");
                        syncCommand("configure terminal", os.forwardingEnabled ? "ip routing" : "no ip routing", "end");
                        return true;
                    }
                }
            } else if (this.selectedConfigItem.equals("Static Routes")) {
                int rowY = y + 100;
                if (mouseY >= rowY && mouseY <= rowY + 16) {
                    if (mouseX >= contentX + 380 && mouseX <= contentX + 450) {
                        // Add Route
                        String net = routeNetworkBox.getValue().trim();
                        String mask = routeMaskBox.getValue().trim();
                        String nexthop = routeNextHopBox.getValue().trim();
                        if (!net.isEmpty() && !mask.isEmpty() && !nexthop.isEmpty()) {
                            String cmd = "ip route " + net + " " + mask + " " + nexthop;
                            os.executeCliCore(cmd, false);
                            syncCommand("configure terminal", cmd, "end");
                            os.appendGuiCommand(cmd, "Settings");
                            routeNetworkBox.setValue(""); routeMaskBox.setValue(""); routeNextHopBox.setValue("");
                        }
                        return true;
                    }
                }

                int ty = y + 140;
                for (int i = 0; i < os.staticRoutes.size(); i++) {
                    if (mouseY >= ty && mouseY < ty + 16 && mouseX >= contentX + 380 && mouseX <= contentX + 450) {
                        RouterOsSimulator.RouteEntry r = os.staticRoutes.get(i);
                        String cmd = "no ip route " + r.network + " " + r.mask + " " + r.nextHop;
                        os.executeCliCore(cmd, false);
                        syncCommand("configure terminal", cmd, "end");
                        os.appendGuiCommand(cmd, "Settings");
                        return true;
                    }
                    ty += 16;
                }
            } else if (this.selectedConfigItem.startsWith("Gigabit") || this.selectedConfigItem.equals("Dot11Radio0")) {
                int rightColX = x + 380;

                if (mouseY >= y + 65 && mouseY <= y + 77 && mouseX >= rightColX + 110 && mouseX <= rightColX + 125) {
                    boolean currentUp = this.selectedConfigItem.equals("Dot11Radio0") ? os.wlanAdminUp : true; // Assuming wired ports are always admin up for simplification in this UI unless extended
                    boolean targetUp = !currentUp;
                    if (this.selectedConfigItem.equals("Dot11Radio0")) os.wlanAdminUp = targetUp;
                    syncCommand("configure terminal", "interface " + this.selectedConfigItem, targetUp ? "no shutdown" : "shutdown", "end");
                    os.appendGuiCommand(targetUp ? "no shutdown" : "shutdown", this.selectedConfigItem);
                    return true;
                }
            }

            for (EditBox box : new EditBox[]{displayNameBox, hostnameBox, ipBox, maskBox, gatewayBox, routeNetworkBox, routeMaskBox, routeNextHopBox}) {
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
            g.fill(tabX, y + 10, tabX + 80, y + 11, 0xFF0092C8);
            g.fill(tabX, y + 10, tabX + 1, y + 31, 0xFF444444);
            g.fill(tabX + 79, y + 10, tabX + 80, y + 31, 0xFF444444);

            if (!isActive) g.fill(tabX, y + 30, tabX + 80, y + 31, 0xFF0092C8);

            int textWidth = this.font.width(Tab.values()[i].label);
            g.drawString(this.font, Tab.values()[i].label, tabX + (40 - textWidth / 2), y + 16, textColor, false);
        }

        switch (this.currentTab) {
            case PHYSICAL -> renderPhysicalTab(g, x, y, mouseX, mouseY);
            case CONFIG -> renderConfigTab(g, x, y, mouseX, mouseY);
            case CLI -> renderCLITab(g, x, y);
            case ATTRIBUTES -> renderAttributesTab(g, x, y);
        }
    }

    private void renderPhysicalTab(GuiGraphics g, int x, int y, int mouseX, int mouseY) {
        g.drawString(this.font, "Physical Device View", x + 20, y + 45, 0xFFFFFF, false);

        int px = x + 180;
        int py = y + 80;
        int pw = 430;
        int ph = 120;

        g.fill(px, py, px + pw, py + ph, 0xFF111111);
        g.fill(px + 1, py + 1, px + pw - 1, py + ph - 1, 0xFF222222);

        g.pose().pushPose();
        g.pose().translate(px + 14, py + 14, 0);
        g.pose().scale(1.2f, 1.2f, 1.0f);
        g.drawString(this.font, "ASUS RT-AC68U", 0, 0, 0xFF0092C8, false);
        g.pose().popPose();

        g.drawString(this.font, "Wireless-AC1900 Gigabit Router", px + 14, py + 34, 0xFFAAAAAA, false);

        long time = System.currentTimeMillis();

        // 3 Antennas
        for (int i = 0; i < 3; i++) {
            int antX = px + 150 + (i * 90);
            g.fill(antX, py - 40, antX + 12, py, 0xFF050505);
            g.fill(antX + 2, py - 38, antX + 10, py, 0xFF181A1D);
        }

        // WAN Port (Blue)
        int wanX = px + 80;
        int wanY = py + 70;
        g.fill(wanX, wanY, wanX + 26, wanY + 22, 0xFF000000);
        g.fill(wanX + 3, wanY + 3, wanX + 23, wanY + 19, 0xFF1A3355);
        boolean wanUp = true;
        g.fill(wanX + 6, wanY - 4, wanX + 12, wanY - 1, wanUp ? (((time % 800) > 400) ? 0xFF22C55E : 0xFF16823B) : 0xFF444444);
        g.drawString(this.font, "WAN", wanX + 2, wanY + 25, 0xFF0092C8, false);

        // LAN Ports (Yellow)
        for (int i = 0; i < 4; i++) {
            int lanX = px + 150 + (i * 40);
            int lanY = py + 70;
            g.fill(lanX, lanY, lanX + 26, lanY + 22, 0xFF000000);
            g.fill(lanX + 3, lanY + 3, lanX + 23, lanY + 19, 0xFF55551A);

            boolean lanUp = true;
            g.fill(lanX + 6, lanY - 4, lanX + 12, lanY - 1, lanUp ? (((time + i * 150) % 800) > 400 ? 0xFF22C55E : 0xFF16823B) : 0xFF444444);
            g.drawString(this.font, "LAN" + (i + 1), lanX + 2, lanY + 25, 0xFFE6A23C, false);
        }
    }

    private void renderConfigTab(GuiGraphics g, int x, int y, int mouseX, int mouseY) {
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

        int contentX = x + sbWidth + 20;
        int rightColX = x + 380;

        g.fill(contentX - 20, y + 31, x + this.imageWidth, y + 45, 0xFF1E1E1E);
        g.fill(contentX - 20, y + 45, x + this.imageWidth, y + 46, 0xFF444444);

        String headerText = this.selectedConfigItem.equals("Settings") ? "Global Settings" : this.selectedConfigItem;
        g.drawString(this.font, headerText, contentX - 20 + ((this.imageWidth - sbWidth) - this.font.width(headerText))/2, y + 35, 0xFFFFFF, false);

        if (this.selectedConfigItem.equals("Settings")) {
            drawFormBackground(g, contentX, y, 4, 60);
            g.drawString(this.font, "Display Name", contentX - 5, y + 64, 0xFFFFFF, false);
            g.drawString(this.font, "Hostname", contentX - 5, y + 81, 0xFFFFFF, false);

            g.drawString(this.font, "IP Routing", contentX - 5, y + 115, 0xFFFFFF, false);
            g.drawString(this.font, "On", rightColX + 125, y + 115, 0xFFFFFF, false);
            drawCheckbox(g, contentX + 85, y + 115, os.forwardingEnabled);

            int btnY = y + 140;
            g.fill(contentX - 10, btnY - 5, x + this.imageWidth - 10, btnY - 4, 0xFF444444);
            g.drawString(this.font, "NVRAM", contentX - 5, btnY + 4, 0xFFFFFF, false);
            drawBoxBtn(g, contentX + 81, btnY, 180, 16, "Erase");
            drawBoxBtn(g, contentX + 261, btnY, 180, 16, "Save");

            btnY += 22;
            g.fill(contentX - 10, btnY - 5, x + this.imageWidth - 10, btnY - 4, 0xFF444444);
            g.drawString(this.font, "Device Clock: 00:10:51 Mon Mar 1 1993 UTC", contentX - 5, btnY + 10, 0xAAAAAA, false);

            g.fill(contentX - 10, y + 135, contentX - 9, btnY - 4, 0xFF444444);
            g.fill(contentX + 80, y + 135, contentX + 81, btnY - 4, 0xFF444444);
            g.fill(contentX + 261, y + 135, contentX + 262, btnY - 4, 0xFF444444);
            g.fill(x + this.imageWidth - 10, y + 135, x + this.imageWidth - 9, btnY - 4, 0xFF444444);

        } else if (this.selectedConfigItem.equals("Static Routes")) {
            drawFormBackground(g, contentX, y, 1, 76);
            drawBoxBtn(g, contentX + 380, y + 78, 70, 16, "Add Route");

            int tableY = y + 120;
            g.fill(contentX - 10, tableY, x + this.imageWidth - 10, tableY + 14, 0xFF2A2A2A);
            g.fill(contentX - 10, tableY, x + this.imageWidth - 10, tableY + 1, 0xFF444444);
            g.fill(contentX - 10, tableY + 14, x + this.imageWidth - 10, tableY + 15, 0xFF444444);
            g.drawString(this.font, "Network", contentX - 5, tableY + 4, 0xFFFFFF, false);
            g.drawString(this.font, "Mask", contentX + 130, tableY + 4, 0xFFFFFF, false);
            g.drawString(this.font, "Next Hop", contentX + 255, tableY + 4, 0xFFFFFF, false);

            int ty = tableY + 20;
            for (RouterOsSimulator.RouteEntry r : os.staticRoutes) {
                g.drawString(this.font, r.network, contentX - 5, ty + 4, 0xFFFFFF, false);
                g.drawString(this.font, r.mask, contentX + 130, ty + 4, 0xFFFFFF, false);
                g.drawString(this.font, r.nextHop, contentX + 255, ty + 4, 0xFFFFFF, false);
                drawBoxBtn(g, contentX + 380, ty, 70, 16, "Remove");
                ty += 16;
            }
        } else if (this.selectedConfigItem.startsWith("Gigabit") || this.selectedConfigItem.equals("Dot11Radio0")) {
            boolean isWlan = this.selectedConfigItem.equals("Dot11Radio0");
            boolean portUp = isWlan ? os.wlanAdminUp : true;

            int rowY = y + 65;

            g.drawString(this.font, "Port Status", contentX, rowY + 1, 0xFFFFFF, false);
            g.drawString(this.font, "On", rightColX + 125, rowY + 1, 0xFFFFFF, false);
            drawCheckbox(g, rightColX + 110, rowY, portUp);

            rowY += 25;
            g.fill(contentX - 10, rowY - 5, x + this.imageWidth - 10, rowY - 4, 0xFF444444);
            g.drawString(this.font, "IP Configuration", contentX, rowY + 4, 0xFFFFFF, false);

            rowY += 25;
            g.drawString(this.font, "IP Address", contentX, rowY + 1, 0xFFFFFF, false);
            g.drawString(this.font, "Subnet Mask", contentX + 225, rowY + 1, 0xFFFFFF, false);
            this.ipBox.setPosition(contentX + 85, rowY - 1);
            this.maskBox.setPosition(contentX + 310, rowY - 1);

            if (isWlan) {
                rowY += 17;
                g.drawString(this.font, "Gateway", contentX, rowY + 1, 0xFFFFFF, false);
                this.gatewayBox.setPosition(contentX + 85, rowY - 1);
            }
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
        for (String cmd : os.iosCommands) {
            g.drawString(this.font, cmd, x + 14, txtY, 0x000000, false);
            txtY += 10;
        }
    }

    private void renderCLITab(GuiGraphics g, int x, int y) {
        if (!os.isBooted) {
            long bootTime = System.currentTimeMillis() - os.bootStartTime;
            if (os.bootStep == 0 && bootTime > 500) { os.cliLines.add("System Bootstrap, Version 2.1(0)RT"); os.bootStep++; }
            if (os.bootStep == 1 && bootTime > 1000) { os.cliLines.add("Copyright (c) 1986-2026 by k1ngtle systems, Inc."); os.bootStep++; }
            if (os.bootStep == 2 && bootTime > 1800) { os.cliLines.add("Platform ASUS RT-AC68U, 256 MB RAM, Broadcom BCM4708"); os.bootStep++; }
            if (os.bootStep == 3 && bootTime > 2500) { os.cliLines.add("Loading disk0:/rt-os-9.1.4.bin... [OK]"); os.bootStep++; }
            if (os.bootStep == 4 && bootTime > 3100) { os.cliLines.add("Starting networking services... [OK]"); os.bootStep++; }
            if (os.bootStep == 5 && bootTime > 3800) { os.cliLines.add(""); os.cliLines.add("Press RETURN to get started."); os.cliLines.add(""); os.bootStep++; os.isBooted = true; }
        }

        g.fill(x, y + 31, x + this.imageWidth, y + this.imageHeight, 0xFF000000);

        int textY = y + 40;
        int maxLines = (this.imageHeight - 50) / 12;
        int startLogIdx = Math.max(0, os.cliLines.size() - maxLines - os.cliScrollOffset + 1);

        int[] COL_X = {0, 115, 230, 310, 420, 500, 580};

        for (int i = startLogIdx; i < os.cliLines.size() - os.cliScrollOffset; i++) {
            String line = os.cliLines.get(i);

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

        if (os.isBooted && os.cliScrollOffset == 0) {
            String prompt = os.getPrompt();
            g.drawString(this.font, prompt + os.cliInput, x + 10, textY, 0xFFFFFFFF, false);

            if ((System.currentTimeMillis() / 500) % 2 == 0) {
                int cursorX = x + 10 + this.font.width(prompt) + this.font.width(os.cliInput.substring(0, os.cliCursorPos));
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
                {"Model", "ASUS RT-AC68U"}, {"Cost", "$ 199"}, {"Power", "12 W"},
                {"Interfaces", "4x GigabitEthernet LAN, 1x WAN, 1x WLAN"}, {"Form Factor", "Desktop"},
                {"Wireless Standard", "802.11ac (WiFi 5)"}, {"Firmware", "VSIA RouterOS 15.0"}
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
        if (checked) g.fill(x + 2, y + 2, x + 7, y + 7, 0xFF0078D7);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {}
    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {}
}
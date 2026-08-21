package com.k1ngtle.vsia.client.screen;

import com.k1ngtle.vsia.network.DeviceCommandPacket;
import com.k1ngtle.vsia.network.VsiaNetwork;
import com.k1ngtle.vsia.signality.internet.router.RouterOsSimulator;
import com.k1ngtle.vsia.world.inventory.RtAc68uRouterMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public final class RtAc68uRouterScreen extends AbstractContainerScreen<RtAc68uRouterMenu> {
    private enum Tab {
        PHYSICAL("Physical"), CONFIG("Config"), CLI("CLI"), ATTRIBUTES("Attributes");
        final String label;
        Tab(String label) { this.label = label; }
    }

    private Tab tab = Tab.PHYSICAL;
    private String configItem = "Settings";
    private final RouterOsSimulator os;

    private EditBox displayNameBox;
    private EditBox hostnameBox;
    private EditBox ipBox;
    private EditBox maskBox;
    private EditBox gatewayBox;
    private EditBox cliBox;
    private Button applyButton;

    private static final List<String> CONFIG_ITEMS = List.of(
            "GLOBAL", "Settings",
            "ROUTING", "Static",
            "INTERFACE", "GigabitEthernet0/0/0", "GigabitEthernet0/0/1", "Dot11Radio0"
    );

    public RtAc68uRouterScreen(RtAc68uRouterMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 760;
        this.imageHeight = 460;
        this.os = menu.blockEntity.routerOs;
    }

    @Override
    protected void init() {
        super.init();
        titleLabelX = 10000;
        inventoryLabelY = 10000;

        int cx = leftPos + 185;
        int y = topPos;

        displayNameBox = makeBox(cx + 112, y + 68, 280, "Display Name");
        hostnameBox = makeBox(cx + 112, y + 89, 280, "Hostname");
        ipBox = makeBox(cx + 112, y + 111, 170, "IP Address");
        maskBox = makeBox(cx + 112, y + 132, 170, "Subnet Mask");
        gatewayBox = makeBox(cx + 112, y + 153, 170, "Default Gateway");

        applyButton = addRenderableWidget(
                Button.builder(Component.literal("Apply"), b -> applyConfig())
                        .bounds(cx + 305, y + 151, 74, 20)
                        .build()
        );

        cliBox = new EditBox(
                font,
                leftPos + 24,
                topPos + imageHeight - 32,
                imageWidth - 48,
                18,
                Component.literal("Router CLI")
        );
        cliBox.setMaxLength(512);
        addRenderableWidget(cliBox);

        refreshWidgets();
    }

    private EditBox makeBox(int x, int y, int width, String name) {
        EditBox box = new EditBox(font, x, y, width, 16, Component.literal(name));
        box.setMaxLength(64);
        addRenderableWidget(box);
        return box;
    }

    private void applyConfig() {
        if ("Settings".equals(configItem)) {
            os.displayName = displayNameBox.getValue().isBlank()
                    ? "RT-AC68U"
                    : displayNameBox.getValue().trim();

            String host = hostnameBox.getValue().trim();
            if (!host.isBlank()) {
                sendCli("enable", "configure terminal", "hostname " + host, "end");
            }
            return;
        }

        String iface = switch (configItem) {
            case "GigabitEthernet0/0/0" -> "gigabitethernet0/0/0";
            case "GigabitEthernet0/0/1" -> "gigabitethernet0/0/1";
            case "Dot11Radio0" -> "dot11radio0";
            default -> null;
        };

        if (iface == null) return;

        if ("Dot11Radio0".equals(configItem)) {
            sendCli(
                    "enable",
                    "configure terminal",
                    "interface " + iface,
                    "ip address " + ipBox.getValue().trim() + " " + maskBox.getValue().trim(),
                    "no shutdown",
                    "exit",
                    "ip default-gateway " + gatewayBox.getValue().trim(),
                    "end"
            );
        } else {
            sendCli(
                    "enable",
                    "configure terminal",
                    "interface " + iface,
                    "ip address " + ipBox.getValue().trim() + " " + maskBox.getValue().trim(),
                    "no shutdown",
                    "end"
            );
        }
        refreshWidgets();
    }

    private void sendCli(String... commands) {
        for (String command : commands) {
            os.executeCliCore(command, false);
        }
        VsiaNetwork.sendToServer(
                new DeviceCommandPacket(
                        menu.blockEntity.getBlockPos(),
                        0,
                        commands
                )
        );
    }

    private void refreshWidgets() {
        if (displayNameBox == null) return;

        boolean config = tab == Tab.CONFIG;
        boolean settings = config && "Settings".equals(configItem);
        boolean iface = config && (
                "GigabitEthernet0/0/0".equals(configItem)
                        || "GigabitEthernet0/0/1".equals(configItem)
                        || "Dot11Radio0".equals(configItem)
        );

        displayNameBox.setVisible(settings);
        hostnameBox.setVisible(settings);
        ipBox.setVisible(iface);
        maskBox.setVisible(iface);
        gatewayBox.setVisible(iface && "Dot11Radio0".equals(configItem));
        applyButton.visible = settings || iface;
        cliBox.setVisible(tab == Tab.CLI);

        if (settings) {
            if (!displayNameBox.isFocused()) displayNameBox.setValue(os.displayName);
            if (!hostnameBox.isFocused()) hostnameBox.setValue(os.hostname);
        } else if (iface) {
            if ("GigabitEthernet0/0/0".equals(configItem)) {
                if (!ipBox.isFocused()) ipBox.setValue(os.lan0Ip);
                if (!maskBox.isFocused()) maskBox.setValue(os.lan0Mask);
            } else if ("GigabitEthernet0/0/1".equals(configItem)) {
                if (!ipBox.isFocused()) ipBox.setValue(os.lan1Ip);
                if (!maskBox.isFocused()) maskBox.setValue(os.lan1Mask);
            } else {
                if (!ipBox.isFocused()) ipBox.setValue(os.wlanIp);
                if (!maskBox.isFocused()) maskBox.setValue(os.wlanMask);
                if (!gatewayBox.isFocused()) gatewayBox.setValue(os.wlanGateway);
            }
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (tab == Tab.CLI
                && cliBox.isFocused()
                && (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER)) {
            String command = cliBox.getValue().trim();
            if (!command.isEmpty()) {
                os.executeCliCore(command, true);
                VsiaNetwork.sendToServer(
                        new DeviceCommandPacket(
                                menu.blockEntity.getBlockPos(),
                                0,
                                command
                        )
                );
                cliBox.setValue("");
            }
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int x = leftPos;
        int y = topPos;

        int tx = x + 8;
        for (Tab candidate : Tab.values()) {
            int width = Math.max(76, font.width(candidate.label) + 24);
            if (inside(mouseX, mouseY, tx, y + 7, width, 22)) {
                tab = candidate;
                refreshWidgets();
                return true;
            }
            tx += width + 3;
        }

        if (tab == Tab.CONFIG) {
            int rowY = y + 55;
            for (String item : CONFIG_ITEMS) {
                boolean header = isHeader(item);
                if (!header && inside(mouseX, mouseY, x + 12, rowY - 2, 160, 18)) {
                    configItem = item;
                    refreshWidgets();
                    return true;
                }
                rowY += 19;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private static boolean isHeader(String item) {
        return "GLOBAL".equals(item) || "ROUTING".equals(item) || "INTERFACE".equals(item);
    }

    private static boolean inside(double mx, double my, int x, int y, int width, int height) {
        return mx >= x && mx < x + width && my >= y && my < y + height;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;

        g.fill(x, y, x + imageWidth, y + imageHeight, 0xFF202020);
        g.fill(x, y, x + imageWidth, y + 35, 0xFF292929);
        g.fill(x, y + 34, x + imageWidth, y + 35, 0xFF707070);

        int tx = x + 8;
        for (Tab candidate : Tab.values()) {
            int width = Math.max(76, font.width(candidate.label) + 24);
            g.fill(tx, y + 7, tx + width, y + 29, candidate == tab ? 0xFF4B4B4B : 0xFF292929);
            g.renderOutline(tx, y + 7, width, 22, 0xFF777777);
            g.drawCenteredString(font, candidate.label, tx + width / 2, y + 14, 0xFFFFFFFF);
            tx += width + 3;
        }

        switch (tab) {
            case PHYSICAL -> renderPhysical(g, x, y);
            case CONFIG -> renderConfig(g, x, y);
            case CLI -> renderCli(g, x, y);
            case ATTRIBUTES -> renderAttributes(g, x, y);
        }
    }

    private void renderPhysical(GuiGraphics g, int x, int y) {
        g.fill(x + 10, y + 44, x + 165, y + imageHeight - 18, 0xFF252525);
        g.renderOutline(x + 10, y + 44, 155, imageHeight - 62, 0xFF555555);
        g.drawString(font, "MODULES", x + 23, y + 52, 0xFFFFFFFF, false);

        String[] modules = {
                "NIM-2T", "NIM-Cover", "NIM-ES2-4", "GLC-GE-100FX",
                "GLC-LH-SMD", "GLC-T", "GLC-TE"
        };
        int yy = y + 71;
        for (String module : modules) {
            g.fill(x + 18, yy - 2, x + 154, yy + 12, 0xFF313131);
            g.drawString(font, module, x + 26, yy, 0xFFD0D0D0, false);
            yy += 17;
        }

        int px = x + 180;
        int right = x + imageWidth - 12;
        g.drawCenteredString(
                font,
                "Zoom In                 Original Size                 Zoom Out",
                (px + right) / 2,
                y + 51,
                0xFFD0D0D0
        );
        g.fill(px, y + 68, right, y + imageHeight - 76, 0xFF1D1D1D);
        g.renderOutline(px, y + 68, right - px, imageHeight - 144, 0xFF555555);

        int dx = px + 55;
        int dy = y + 135;
        g.fill(dx, dy, dx + 430, dy + 94, 0xFF0D0D0D);
        g.renderOutline(dx, dy, 430, 94, 0xFF666666);
        g.drawString(font, "ASUS RT-AC68U", dx + 14, dy + 10, 0xFFB8B8B8, false);

        for (int i = 0; i < 5; i++) {
            int portX = dx + 50 + i * 62;
            g.fill(portX, dy + 48, portX + 39, dy + 69, 0xFF151515);
            g.renderOutline(portX, dy + 48, 39, 21, 0xFF858585);
        }

        g.drawString(
                font,
                "Option B: consumer RT-AC68U hardware with Packet Tracer-style management.",
                px + 14,
                y + imageHeight - 57,
                0xFFBDBDBD,
                false
        );
    }

    private void renderConfig(GuiGraphics g, int x, int y) {
        g.fill(x + 10, y + 44, x + 175, y + imageHeight - 18, 0xFF252525);
        g.renderOutline(x + 10, y + 44, 165, imageHeight - 62, 0xFF555555);

        int rowY = y + 55;
        for (String item : CONFIG_ITEMS) {
            boolean header = isHeader(item);
            if (!header && item.equals(configItem)) {
                g.fill(x + 14, rowY - 2, x + 168, rowY + 14, 0xFF515151);
            }
            g.drawString(
                    font,
                    header ? item : "   " + item,
                    x + 18,
                    rowY,
                    header ? 0xFFFFFFFF : 0xFFC8C8C8,
                    false
            );
            rowY += 19;
        }

        int cx = x + 185;
        g.drawCenteredString(font, configItem, cx + 280, y + 48, 0xFFE5E5E5);

        if ("Settings".equals(configItem)) {
            g.drawString(font, "Display Name", cx + 12, y + 72, 0xFFCCCCCC, false);
            g.drawString(font, "Hostname", cx + 12, y + 93, 0xFFCCCCCC, false);
            g.drawString(font, "NVRAM", cx + 12, y + 127, 0xFFCCCCCC, false);
            g.fill(cx + 112, y + 123, cx + 250, y + 141, 0xFF393939);
            g.drawCenteredString(font, "Erase", cx + 181, y + 128, 0xFFDADADA);
            g.fill(cx + 255, y + 123, cx + 393, y + 141, 0xFF393939);
            g.drawCenteredString(font, "Save", cx + 324, y + 128, 0xFFDADADA);
            g.drawString(font, "Device Clock", cx + 12, y + 180, 0xFFCCCCCC, false);
            g.drawString(font, "VSIA server/world clock", cx + 112, y + 180, 0xFFFFFFFF, false);
        } else if ("Static".equals(configItem)) {
            g.drawString(font, "Static Routes", cx + 14, y + 73, 0xFFFFFFFF, false);
            int yy = y + 96;
            if (os.staticRoutes.isEmpty()) {
                g.drawString(font, "No static routes configured.", cx + 14, yy, 0xFFAAAAAA, false);
            } else {
                for (RouterOsSimulator.RouteEntry route : os.staticRoutes) {
                    g.drawString(font, route.network() + " via " + route.nextHop(), cx + 14, yy, 0xFFCCCCCC, false);
                    yy += 16;
                }
            }
            g.drawString(font, "Configure additional routes in CLI with 'ip route ...'.", cx + 14, yy + 16, 0xFF8FC7FF, false);
        } else {
            g.drawString(font, "IP Address", cx + 12, y + 115, 0xFFCCCCCC, false);
            g.drawString(font, "Subnet Mask", cx + 12, y + 136, 0xFFCCCCCC, false);
            if ("Dot11Radio0".equals(configItem)) {
                g.drawString(font, "Default Gateway", cx + 12, y + 157, 0xFFCCCCCC, false);
                g.drawString(
                        font,
                        "W1.20 station/application IPv4. Configure Router B here.",
                        cx + 12,
                        y + 198,
                        0xFF8FC7FF,
                        false
                );
            }
        }

        g.drawString(font, "Equivalent IOS Commands", cx + 8, y + imageHeight - 104, 0xFFCCCCCC, false);
        g.fill(cx, y + imageHeight - 87, x + imageWidth - 12, y + imageHeight - 20, 0xFFF6F6F6);

        String equivalent = "show running-config";
        if ("Dot11Radio0".equals(configItem)) {
            equivalent = "interface dot11radio0 ; ip address " + os.wlanIp + " " + os.wlanMask;
        }
        g.drawString(font, equivalent, cx + 8, y + imageHeight - 70, 0xFF202020, false);
    }

    private void renderCli(GuiGraphics g, int x, int y) {
        g.fill(x + 10, y + 45, x + imageWidth - 10, y + imageHeight - 43, 0xFFF7F7F7);

        int yy = y + 55;
        int start = Math.max(0, os.cliLines.size() - 27);
        for (int i = start; i < os.cliLines.size(); i++) {
            g.drawString(font, os.cliLines.get(i), x + 18, yy, 0xFF151515, false);
            yy += 13;
        }

        g.drawString(font, os.getPrompt(), x + 18, y + imageHeight - 60, 0xFF151515, false);
    }

    private void renderAttributes(GuiGraphics g, int x, int y) {
        g.drawString(font, "Attributes:", x + 18, y + 52, 0xFFFFFFFF, false);

        String[][] rows = {
                {"MTBF", "587250 hours"},
                {"cost", "3000"},
                {"power source", "Internal"},
                {"rack units", "1"},
                {"wattage", "30 W"},
                {"PT_MODEL", "RT-AC68U"},
                {"PT_VERSION", "VSIA W1.20.4"}
        };

        int yy = y + 77;
        for (String[] row : rows) {
            g.fill(x + 18, yy - 3, x + 285, yy + 15, 0xFF303030);
            g.fill(x + 287, yy - 3, x + imageWidth - 18, yy + 15, 0xFF292929);
            g.drawString(font, row[0], x + 26, yy, 0xFFD0D0D0, false);
            g.drawString(font, row[1], x + 300, yy, 0xFFFFFFFF, false);
            yy += 22;
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
    }
}

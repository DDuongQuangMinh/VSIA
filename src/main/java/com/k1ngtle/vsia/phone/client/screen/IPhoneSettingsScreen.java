package com.k1ngtle.vsia.phone.client.screen;

import com.k1ngtle.vsia.phone.network.PhoneNetworkState;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public class IPhoneSettingsScreen extends IPhoneScreen {
    private int groupX;
    private int groupY;
    private int groupWidth;

    public IPhoneSettingsScreen() {
        super(Component.literal("Settings"));
    }

    @Override
    protected void init() {
        super.init();
        groupX = phoneX + 14;
        groupY = phoneY + 96;
        groupWidth = PHONE_WIDTH - 28;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderPhoneShell(graphics, 0xFF1C1C1E);
        renderStatusBar(graphics);

        graphics.drawString(font, "Settings", phoneX + 18, phoneY + 50, 0xFFFFFFFF, false);
        graphics.drawString(font, "CONNECTIVITY", groupX + 4, groupY - 18, 0xFF8E8E93, false);

        roundedRect(graphics, groupX, groupY, groupWidth, 156, 14, 0xFF2C2C2E);
        drawRow(graphics, groupY, RowType.WIFI, "Wi-Fi", wifiSummary());
        divider(graphics, groupY + 52);
        drawRow(graphics, groupY + 52, RowType.CELLULAR, "Cellular", cellularSummary());
        divider(graphics, groupY + 104);
        drawRow(graphics, groupY + 104, RowType.BROWSER, "Browser", "VS:IA Web");

        int statusY = groupY + 178;
        graphics.drawString(font, "PHONE", groupX + 4, statusY - 18, 0xFF8E8E93, false);
        roundedRect(graphics, groupX, statusY, groupWidth, 52, 14, 0xFF2C2C2E);
        drawRow(graphics, statusY, RowType.STATUS, "Network Status", activeInterface());

        renderHomeIndicator(graphics);
    }

    private String wifiSummary() {
        var wifi = PhoneNetworkState.get().getWifi();
        if (!wifi.enabled()) return "Off";
        if (wifi.connected()) return fit(wifi.ssid(), 72);
        return "Not Connected";
    }

    private String cellularSummary() {
        var cellular = PhoneNetworkState.get().getCellular();
        if (!cellular.enabled()) return "Off";
        if (cellular.registered()) return fit(cellular.carrier(), 70);
        return "No Service";
    }

    private String activeInterface() {
        PhoneNetworkState state = PhoneNetworkState.get();
        if (state.isWifiUsable()) return "Wi-Fi";
        if (state.isCellularUsable()) return state.getCellular().radioLabel();
        return "Offline";
    }

    private String fit(String value, int maxWidth) {
        String text = value == null ? "" : value;
        if (font.width(text) <= maxWidth) return text;
        while (!text.isEmpty() && font.width(text + "...") > maxWidth) {
            text = text.substring(0, text.length() - 1);
        }
        return text + "...";
    }

    private void drawRow(GuiGraphics g, int y, RowType type, String title, String summary) {
        int iconX = groupX + 10;
        int iconY = y + 10;
        drawIcon(g, type, iconX, iconY);

        g.drawString(font, title, groupX + 52, y + 18, 0xFFFFFFFF, false);
        int sw = font.width(summary);
        g.drawString(font, summary, groupX + groupWidth - sw - 28, y + 18, 0xFFAEAEB2, false);
        g.drawString(font, ">", groupX + groupWidth - 15, y + 18, 0xFF636366, false);
    }

    private void drawIcon(GuiGraphics g, RowType type, int x, int y) {
        int bg = switch (type) {
            case WIFI -> 0xFF007AFF;
            case CELLULAR -> 0xFF34C759;
            case BROWSER -> 0xFFF2F2F7;
            case STATUS -> 0xFF2F3036;
        };
        roundedRect(g, x, y, 32, 32, 8, bg);

        switch (type) {
            case WIFI -> drawWifi(g, x + 9, y + 8, 0xFFFFFFFF);
            case CELLULAR -> drawCellular(g, x + 8, y + 7);
            case BROWSER -> drawCompass(g, x + 16, y + 16);
            case STATUS -> drawStatus(g, x + 7, y + 7);
        }
    }

    private void drawWifi(GuiGraphics g, int x, int y, int color) {
        g.fill(x, y + 1, x + 14, y + 3, color);
        g.fill(x + 2, y + 5, x + 12, y + 7, color);
        g.fill(x + 5, y + 9, x + 9, y + 11, color);
        g.fill(x + 6, y + 13, x + 8, y + 15, color);
    }

    private void drawCellular(GuiGraphics g, int x, int y) {
        for (int i = 0; i < 4; i++) {
            int h = 5 + i * 4;
            g.fill(x + i * 4, y + 18 - h, x + i * 4 + 3, y + 18, 0xFFFFFFFF);
        }
    }

    private void drawCompass(GuiGraphics g, int cx, int cy) {
        roundedRect(g, cx - 11, cy - 11, 22, 22, 11, 0xFF0A84FF);
        roundedRect(g, cx - 8, cy - 8, 16, 16, 8, 0xFFFFFFFF);
        g.fill(cx - 1, cy - 7, cx + 1, cy + 1, 0xFFFF453A);
        g.fill(cx, cy, cx + 2, cy + 8, 0xFF0A84FF);
    }

    private void drawStatus(GuiGraphics g, int x, int y) {
        int[] heights = {5, 9, 13, 18};
        int[] colors = {0xFF30D158, 0xFFFFD60A, 0xFFFF9F0A, 0xFFFF453A};
        for (int i = 0; i < 4; i++) {
            g.fill(x + i * 4, y + 18 - heights[i], x + i * 4 + 3, y + 18, colors[i]);
        }
    }

    private void divider(GuiGraphics g, int y) {
        g.fill(groupX + 52, y, groupX + groupWidth - 12, y + 1, 0xFF3A3A3C);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (inside(mouseX, mouseY, groupX, groupY, groupWidth, 52)) {
                minecraft.setScreen(new IPhoneWifiScreen());
                return true;
            }
            if (inside(mouseX, mouseY, groupX, groupY + 52, groupWidth, 52)) {
                minecraft.setScreen(new IPhoneCellularScreen());
                return true;
            }
            if (inside(mouseX, mouseY, groupX, groupY + 104, groupWidth, 52)) {
                minecraft.setScreen(new IPhoneBrowserScreen());
                return true;
            }
            if (inside(mouseX, mouseY, groupX, groupY + 178, groupWidth, 52)) {
                minecraft.setScreen(new IPhoneStatusScreen());
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private enum RowType {
        WIFI,
        CELLULAR,
        BROWSER,
        STATUS
    }
}

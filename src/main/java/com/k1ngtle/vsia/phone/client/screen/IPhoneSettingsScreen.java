package com.k1ngtle.vsia.phone.client.screen;

import com.k1ngtle.vsia.phone.client.PhoneAccessibilityClientPreferences;
import com.k1ngtle.vsia.phone.network.PhoneNetworkState;
import com.k1ngtle.vsia.phone.subscriber.PhoneSubscriberClientState;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public class IPhoneSettingsScreen extends IPhoneScreen {
    private static final int BACKGROUND = 0xFF111216;
    private static final int CARD = 0xFF2C2C2E;
    private static final int CARD_ALT = 0xFF232428;
    private static final int TEXT = 0xFFFFFFFF;
    private static final int SECONDARY = 0xFFAEAEB2;
    private static final int TERTIARY = 0xFF8E8E93;
    private static final int DIVIDER = 0xFF3A3A3C;
    private static final int BLUE = 0xFF0A84FF;
    private static final int GREEN = 0xFF30D158;
    private static final int ORANGE = 0xFFFF9F0A;
    private static final int GREY = 0xFF5E5E62;
    private static final int ROW_HEIGHT = 38;

    private int contentX;
    private int contentWidth;
    private int titleY;
    private int searchY;
    private int group1Y;
    private int group2Y;
    private int group3Y;

    public IPhoneSettingsScreen() {
        super(Component.literal("Settings"));
    }

    @Override
    protected void init() {
        super.init();
        contentX = phoneX + 14;
        contentWidth = PHONE_WIDTH - 28;
        titleY = phoneY + 49;
        searchY = phoneY + 73;
        group1Y = searchY + 36;
        group2Y = group1Y + 134;
        group3Y = group2Y + 100;
        PhoneSubscriberClientState.get().requestRefresh();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderPhoneShell(graphics, BACKGROUND);
        renderStatusBar(graphics);

        graphics.drawCenteredString(font, "Settings", phoneX + PHONE_WIDTH / 2, titleY, TEXT);
        drawSearchBar(graphics, searchY);

        drawGroup(graphics, group1Y, 3);
        drawSettingsRow(graphics, group1Y, Row.WIFI, wifiSummary());
        drawSettingsRow(graphics, group1Y + ROW_HEIGHT, Row.CELLULAR, cellularSummary());
        drawSettingsRow(graphics, group1Y + ROW_HEIGHT * 2, Row.BROWSER, "VS:IA Web");

        drawGroup(graphics, group2Y, 2);
        drawSettingsRow(graphics, group2Y, Row.ACCESSIBILITY, "");
        drawSettingsRow(graphics, group2Y + ROW_HEIGHT, Row.DISPLAY_TEXT_SIZE, displaySummary());

        drawGroup(graphics, group3Y, 1);
        drawSettingsRow(graphics, group3Y, Row.NETWORK_STATUS, activeInterface());

        renderHomeIndicator(graphics);
    }

    private void drawSearchBar(GuiGraphics graphics, int y) {
        roundedRect(graphics, contentX, y, contentWidth, 26, 10, CARD_ALT);
        graphics.drawString(font, "Search", contentX + 34, y + 9, TERTIARY, false);
        drawSearchIcon(graphics, contentX + 12, y + 8, TERTIARY);
    }

    private void drawGroup(GuiGraphics graphics, int y, int rows) {
        roundedRect(graphics, contentX, y, contentWidth, rows * ROW_HEIGHT, 14, CARD);
        for (int i = 1; i < rows; i++) {
            int dividerY = y + ROW_HEIGHT * i;
            graphics.fill(contentX + 48, dividerY, contentX + contentWidth - 12, dividerY + 1, DIVIDER);
        }
    }

    private void drawSettingsRow(GuiGraphics graphics, int y, Row row, String summary) {
        int iconX = contentX + 10;
        int iconY = y + 8;
        drawRowIcon(graphics, row, iconX, iconY);
        graphics.drawString(font, row.title, contentX + 46, y + 14, TEXT, false);

        if (summary != null && !summary.isBlank()) {
            String fitSummary = fit(summary, 82);
            int summaryWidth = font.width(fitSummary);
            graphics.drawString(font, fitSummary, contentX + contentWidth - summaryWidth - 18, y + 14, SECONDARY, false);
        }
        graphics.drawString(font, ">", contentX + contentWidth - 11, y + 14, TERTIARY, false);
    }

    private void drawRowIcon(GuiGraphics graphics, Row row, int x, int y) {
        roundedRect(graphics, x, y, 22, 22, 6, row.color);
        switch (row) {
            case WIFI -> drawWifiIcon(graphics, x + 5, y + 5);
            case CELLULAR -> drawCellularIcon(graphics, x + 5, y + 4);
            case BROWSER -> drawCompassIcon(graphics, x + 11, y + 11);
            case ACCESSIBILITY -> drawAccessibilityIcon(graphics, x + 5, y + 4);
            case DISPLAY_TEXT_SIZE -> drawTextSizeIcon(graphics, x + 4, y + 5);
            case NETWORK_STATUS -> drawStatusIcon(graphics, x + 4, y + 4);
        }
    }

    private void drawSearchIcon(GuiGraphics graphics, int x, int y, int color) {
        graphics.fill(x + 1, y + 1, x + 7, y + 2, color);
        graphics.fill(x, y + 2, x + 1, y + 6, color);
        graphics.fill(x + 7, y + 2, x + 8, y + 6, color);
        graphics.fill(x + 1, y + 6, x + 7, y + 7, color);
        graphics.fill(x + 6, y + 6, x + 10, y + 10, color);
    }

    private void drawWifiIcon(GuiGraphics graphics, int x, int y) {
        graphics.fill(x + 1, y, x + 9, y + 1, TEXT);
        graphics.fill(x + 2, y + 3, x + 8, y + 4, TEXT);
        graphics.fill(x + 3, y + 6, x + 7, y + 7, TEXT);
        graphics.fill(x + 4, y + 9, x + 6, y + 10, TEXT);
    }

    private void drawCellularIcon(GuiGraphics graphics, int x, int y) {
        graphics.fill(x, y + 8, x + 2, y + 12, TEXT);
        graphics.fill(x + 3, y + 6, x + 5, y + 12, TEXT);
        graphics.fill(x + 6, y + 3, x + 8, y + 12, TEXT);
        graphics.fill(x + 9, y, x + 11, y + 12, TEXT);
    }

    private void drawCompassIcon(GuiGraphics graphics, int cx, int cy) {
        roundedRect(graphics, cx - 6, cy - 6, 12, 12, 6, TEXT);
        graphics.fill(cx - 1, cy - 5, cx + 1, cy + 1, 0xFFFF453A);
        graphics.fill(cx, cy, cx + 2, cy + 5, BLUE);
    }

    private void drawAccessibilityIcon(GuiGraphics graphics, int x, int y) {
        graphics.fill(x + 4, y + 1, x + 6, y + 3, TEXT);
        graphics.fill(x + 3, y + 4, x + 7, y + 5, TEXT);
        graphics.fill(x + 4, y + 5, x + 5, y + 11, TEXT);
        graphics.fill(x + 2, y + 7, x + 4, y + 8, TEXT);
        graphics.fill(x + 5, y + 7, x + 8, y + 8, TEXT);
        graphics.fill(x + 2, y + 11, x + 4, y + 13, TEXT);
        graphics.fill(x + 5, y + 11, x + 7, y + 13, TEXT);
    }

    private void drawTextSizeIcon(GuiGraphics graphics, int x, int y) {
        graphics.drawString(font, "A", x, y + 1, TEXT, false);
        graphics.drawString(font, "A", x + 6, y - 1, TEXT, false);
    }

    private void drawStatusIcon(GuiGraphics graphics, int x, int y) {
        graphics.fill(x, y + 9, x + 2, y + 12, 0xFF30D158);
        graphics.fill(x + 3, y + 6, x + 5, y + 12, 0xFF64D2FF);
        graphics.fill(x + 6, y + 3, x + 8, y + 12, 0xFFFFD60A);
        graphics.fill(x + 9, y, x + 11, y + 12, 0xFFFF453A);
    }

    private String wifiSummary() {
        PhoneNetworkState.WifiStatus wifi = PhoneNetworkState.get().getWifi();
        if (!wifi.enabled()) return "Off";
        if (wifi.connected()) return wifi.ssid();
        return "Not Connected";
    }

    private String cellularSummary() {
        if (!PhoneSubscriberClientState.get().hasActiveSubscription()) {
            return "No SIM";
        }
        if (!PhoneSubscriberClientState.get().snapshot().cellularDataEnabled()) {
            return "Data Off";
        }
        PhoneNetworkState.CellularStatus cellular = PhoneNetworkState.get().getCellular();
        if (cellular.registered()) return cellular.carrier();
        return "No Service";
    }

    private String displaySummary() {
        return PhoneAccessibilityClientPreferences.largerText() ? "On" : "Off";
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

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (inside(mouseX, mouseY, contentX, group1Y, contentWidth, ROW_HEIGHT)) {
                minecraft.setScreen(new IPhoneWifiScreen());
                return true;
            }
            if (inside(mouseX, mouseY, contentX, group1Y + ROW_HEIGHT, contentWidth, ROW_HEIGHT)) {
                minecraft.setScreen(new IPhoneCellularScreen());
                return true;
            }
            if (inside(mouseX, mouseY, contentX, group1Y + ROW_HEIGHT * 2, contentWidth, ROW_HEIGHT)) {
                minecraft.setScreen(new IPhoneBrowserScreen());
                return true;
            }
            if (inside(mouseX, mouseY, contentX, group2Y, contentWidth, ROW_HEIGHT)) {
                minecraft.setScreen(new IPhoneAccessibilityScreen());
                return true;
            }
            if (inside(mouseX, mouseY, contentX, group2Y + ROW_HEIGHT, contentWidth, ROW_HEIGHT)) {
                minecraft.setScreen(new IPhoneDisplayTextSizeScreen());
                return true;
            }
            if (inside(mouseX, mouseY, contentX, group3Y, contentWidth, ROW_HEIGHT)) {
                minecraft.setScreen(new IPhoneStatusScreen());
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private enum Row {
        WIFI("Wi-Fi", BLUE),
        CELLULAR("Cellular", GREEN),
        BROWSER("Browser", BLUE),
        ACCESSIBILITY("Accessibility", BLUE),
        DISPLAY_TEXT_SIZE("Display & Text Size", GREY),
        NETWORK_STATUS("Network Status", ORANGE);

        private final String title;
        private final int color;

        Row(String title, int color) {
            this.title = title;
            this.color = color;
        }
    }
}

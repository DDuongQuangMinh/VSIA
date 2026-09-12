package com.k1ngtle.vsia.phone.client;

import com.k1ngtle.vsia.phone.network.PhoneNetworkState;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public final class IPhoneStatusBar {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    private IPhoneStatusBar() {
    }

    public static void render(GuiGraphics graphics, Font font, int x, int y, int width) {
        PhoneNetworkState state = PhoneNetworkState.get();

        graphics.drawString(font, LocalTime.now().format(TIME), x + 18, y + 16, 0xFFFFFFFF, false);

        int islandWidth = 72;
        int islandX = x + (width - islandWidth) / 2;
        roundedRect(graphics, islandX, y + 10, islandWidth, 18, 9, 0xFF000000);

        String label;
        int bars;

        if (state.isWifiUsable()) {
            label = "Wi-Fi";
            bars = wifiBars(state.getWifi().rssiDbm());
        } else if (state.isCellularUsable()) {
            label = state.getCellular().radioLabel();
            bars = cellularBars(state.getCellular().rsrpDbm(), state.getCellular().sinrDb());
        } else {
            label = "SOS";
            bars = 0;
        }

        int right = x + width - 17;
        String battery = state.getBatteryPercent() + "%";
        int batteryWidth = font.width(battery);
        graphics.drawString(font, battery, right - batteryWidth, y + 16, 0xFFFFFFFF, false);

        right -= batteryWidth + 8;
        drawBars(graphics, right - 26, y + 15, bars);

        int labelWidth = font.width(label);
        graphics.drawString(font, label, right - 32 - labelWidth, y + 16, 0xFFFFFFFF, false);
    }

    private static int wifiBars(int rssi) {
        if (rssi >= -55) return 4;
        if (rssi >= -67) return 3;
        if (rssi >= -75) return 2;
        if (rssi >= -85) return 1;
        return 0;
    }

    private static int cellularBars(int rsrp, double sinr) {
        int rsrpBars;
        if (rsrp >= -80) rsrpBars = 4;
        else if (rsrp >= -90) rsrpBars = 3;
        else if (rsrp >= -100) rsrpBars = 2;
        else if (rsrp >= -110) rsrpBars = 1;
        else rsrpBars = 0;

        if (sinr < 0) return Math.min(rsrpBars, 1);
        if (sinr < 10) return Math.min(rsrpBars, 2);
        if (sinr < 20) return Math.min(rsrpBars, 3);
        return rsrpBars;
    }

    private static void drawBars(GuiGraphics graphics, int x, int y, int bars) {
        for (int i = 0; i < 4; i++) {
            int h = 3 + i * 2;
            int color = i < bars ? 0xFFFFFFFF : 0xFF666666;
            graphics.fill(x + i * 5, y + 8 - h, x + i * 5 + 3, y + 8, color);
        }
    }

    private static void roundedRect(GuiGraphics g, int x, int y, int w, int h, int r, int color) {
        g.fill(x + r, y, x + w - r, y + h, color);
        g.fill(x, y + r, x + w, y + h - r, color);
        for (int i = 0; i < r; i++) {
            int inset = (int)Math.ceil(r - Math.sqrt(r * r - (r - i) * (r - i)));
            g.fill(x + inset, y + i, x + w - inset, y + i + 1, color);
            g.fill(x + inset, y + h - i - 1, x + w - inset, y + h - i, color);
        }
    }
}

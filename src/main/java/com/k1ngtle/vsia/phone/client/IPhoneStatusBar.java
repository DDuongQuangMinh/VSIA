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

        int islandWidth = 60;
        int islandX = x + (width - islandWidth) / 2;
        int islandY = y + 10;
        roundedRect(graphics, islandX, islandY, islandWidth, 18, 9, 0xFF000000);

        int islandRight = islandX + islandWidth;
        int batteryX = x + width - 31;
        drawBattery(graphics, batteryX, y + 14, state.getBatteryPercent());

        boolean wifiEnabled = state.getWifi().enabled();
        int wifiX = batteryX - 18;

        if (wifiEnabled) {
            drawWifiIcon(
                    graphics,
                    wifiX,
                    y + 14,
                    wifiBars(state.getWifi().rssiDbm()),
                    state.isWifiUsable() ? 0xFFFFFFFF : 0xFF8E8E93
            );
        }

        if (state.getCellular().enabled()) {
            int cellularX = wifiEnabled ? wifiX - 24 : batteryX - 24;
            cellularX = Math.max(cellularX, islandRight + 8);
            drawCellularBars(
                    graphics,
                    cellularX,
                    y + 15,
                    cellularBars(state.getCellular().rsrpDbm(), state.getCellular().sinrDb())
            );
        } else if (!wifiEnabled) {
            graphics.drawString(font, "SOS", islandRight + 10, y + 16, 0xFFFFFFFF, false);
        }
    }

    private static int wifiBars(int rssi) {
        if (rssi >= -55) return 3;
        if (rssi >= -67) return 2;
        if (rssi >= -80) return 1;
        return 0;
    }

    private static int cellularBars(int rsrp, double sinr) {
        int bars;
        if (rsrp >= -80) bars = 4;
        else if (rsrp >= -90) bars = 3;
        else if (rsrp >= -100) bars = 2;
        else if (rsrp >= -110) bars = 1;
        else bars = 0;

        if (sinr < 0.0D) return Math.min(bars, 1);
        if (sinr < 10.0D) return Math.min(bars, 2);
        if (sinr < 20.0D) return Math.min(bars, 3);
        return bars;
    }

    private static void drawCellularBars(GuiGraphics graphics, int x, int y, int bars) {
        for (int i = 0; i < 4; i++) {
            int h = 3 + i * 3;
            int left = x + i * 4;
            graphics.fill(left, y + 9 - h, left + 2, y + 9, i < bars ? 0xFFFFFFFF : 0xFF626267);
        }
    }

    private static void drawWifiIcon(GuiGraphics graphics, int x, int y, int strength, int activeColor) {
        int off = 0xFF626267;
        int outer = strength >= 3 ? activeColor : off;
        int middle = strength >= 2 ? activeColor : off;
        int inner = strength >= 1 ? activeColor : off;

        graphics.fill(x, y + 2, x + 2, y + 4, outer);
        graphics.fill(x + 2, y + 1, x + 10, y + 3, outer);
        graphics.fill(x + 10, y + 2, x + 12, y + 4, outer);

        graphics.fill(x + 2, y + 5, x + 4, y + 7, middle);
        graphics.fill(x + 4, y + 4, x + 8, y + 6, middle);
        graphics.fill(x + 8, y + 5, x + 10, y + 7, middle);

        graphics.fill(x + 4, y + 8, x + 8, y + 9, inner);
        graphics.fill(x + 5, y + 10, x + 7, y + 12, inner);
    }

    private static void drawBattery(GuiGraphics graphics, int x, int y, int percent) {
        graphics.fill(x, y, x + 19, y + 9, 0xFFFFFFFF);
        graphics.fill(x + 1, y + 1, x + 18, y + 8, 0xFF16181D);
        graphics.fill(x + 19, y + 3, x + 21, y + 6, 0xFFFFFFFF);

        int fill = Math.max(1, Math.min(16, (int) Math.round(percent / 100.0D * 16.0D)));
        int color = percent <= 20 ? 0xFFFF453A : 0xFFFFFFFF;
        graphics.fill(x + 2, y + 2, x + 2 + fill, y + 7, color);
    }

    private static void roundedRect(GuiGraphics graphics, int x, int y, int width, int height, int radius, int color) {
        graphics.fill(x + radius, y, x + width - radius, y + height, color);
        graphics.fill(x, y + radius, x + width, y + height - radius, color);

        for (int i = 0; i < radius; i++) {
            int dy = radius - i;
            int inset = (int) Math.ceil(radius - Math.sqrt(Math.max(0, radius * radius - dy * dy)));
            graphics.fill(x + inset, y + i, x + width - inset, y + i + 1, color);
            graphics.fill(x + inset, y + height - i - 1, x + width - inset, y + height - i, color);
        }
    }
}

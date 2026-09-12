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

        graphics.drawString(
                font,
                LocalTime.now().format(TIME),
                x + 18,
                y + 16,
                0xFFFFFFFF,
                false
        );

        int islandWidth = 60;
        int islandHeight = 18;
        int islandX = x + (width - islandWidth) / 2;
        int islandY = y + 10;
        roundedRect(graphics, islandX, islandY, islandWidth, islandHeight, 9, 0xFF000000);

        int islandRight = islandX + islandWidth;

        int batteryX = x + width - 31;
        int batteryY = y + 14;
        drawBattery(graphics, batteryX, batteryY, state.getBatteryPercent());

        int wifiX = batteryX - 18;
        int wifiY = y + 14;

        boolean wifiEnabled = state.getWifi().enabled();
        boolean wifiUsable = state.isWifiUsable();

        if (wifiEnabled) {
            drawWifiIcon(
                    graphics,
                    wifiX,
                    wifiY,
                    wifiUsable ? 0xFFFFFFFF : 0xFF8D8D93
            );
        }

        boolean cellularEnabled = state.getCellular().enabled();

        if (cellularEnabled) {
            int bars = cellularBars(
                    state.getCellular().rsrpDbm(),
                    state.getCellular().sinrDb()
            );

            int cellularX = wifiEnabled ? wifiX - 24 : batteryX - 24;
            int minCellularX = islandRight + 8;

            if (cellularX < minCellularX) {
                cellularX = minCellularX;
            }

            drawCellularBars(
                    graphics,
                    cellularX,
                    y + 15,
                    bars,
                    0xFFFFFFFF
            );
        } else if (!wifiEnabled) {
            graphics.drawString(
                    font,
                    "SOS",
                    islandRight + 10,
                    y + 16,
                    0xFFFFFFFF,
                    false
            );
        }
    }

    private static int cellularBars(int rsrp, double sinr) {
        int rsrpBars;

        if (rsrp >= -80) {
            rsrpBars = 4;
        } else if (rsrp >= -90) {
            rsrpBars = 3;
        } else if (rsrp >= -100) {
            rsrpBars = 2;
        } else if (rsrp >= -110) {
            rsrpBars = 1;
        } else {
            rsrpBars = 0;
        }

        if (sinr < 0.0D) {
            return Math.min(rsrpBars, 1);
        }
        if (sinr < 10.0D) {
            return Math.min(rsrpBars, 2);
        }
        if (sinr < 20.0D) {
            return Math.min(rsrpBars, 3);
        }
        return rsrpBars;
    }

    private static void drawCellularBars(
            GuiGraphics graphics,
            int x,
            int y,
            int bars,
            int onColor
    ) {
        int offColor = 0xFF666666;

        for (int i = 0; i < 4; i++) {
            int barHeight = 3 + i * 3;
            int left = x + i * 4;
            int top = y + 9 - barHeight;
            int color = i < bars ? onColor : offColor;

            graphics.fill(left, top, left + 2, y + 9, color);
        }
    }

    private static void drawWifiIcon(
            GuiGraphics graphics,
            int x,
            int y,
            int color
    ) {
        graphics.fill(x + 5, y + 8, x + 7, y + 10, color);

        graphics.fill(x + 3, y + 6, x + 9, y + 7, color);
        graphics.fill(x + 2, y + 5, x + 3, y + 8, color);
        graphics.fill(x + 9, y + 5, x + 10, y + 8, color);

        graphics.fill(x + 1, y + 3, x + 11, y + 4, color);
        graphics.fill(x, y + 2, x + 1, y + 5, color);
        graphics.fill(x + 11, y + 2, x + 12, y + 5, color);
    }

    private static void drawBattery(
            GuiGraphics graphics,
            int x,
            int y,
            int percent
    ) {
        graphics.fill(x, y, x + 19, y + 9, 0xFFFFFFFF);
        graphics.fill(x + 1, y + 1, x + 18, y + 8, 0xFF16181D);
        graphics.fill(x + 19, y + 3, x + 21, y + 6, 0xFFFFFFFF);

        int fill = Math.max(1, Math.min(16, (int) Math.round(percent / 100.0D * 16.0D)));
        graphics.fill(x + 2, y + 2, x + 2 + fill, y + 7, 0xFFFFFFFF);
    }

    private static void roundedRect(
            GuiGraphics graphics,
            int x,
            int y,
            int width,
            int height,
            int radius,
            int color
    ) {
        graphics.fill(x + radius, y, x + width - radius, y + height, color);
        graphics.fill(x, y + radius, x + width, y + height - radius, color);

        for (int i = 0; i < radius; i++) {
            int dy = radius - i;
            int inset = (int) Math.ceil(
                    radius - Math.sqrt(Math.max(0, radius * radius - dy * dy))
            );

            graphics.fill(x + inset, y + i, x + width - inset, y + i + 1, color);
            graphics.fill(x + inset, y + height - i - 1, x + width - inset, y + height - i, color);
        }
    }
}
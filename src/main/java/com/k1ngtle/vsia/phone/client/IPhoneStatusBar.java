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

        int islandWidth = 72;
        int islandX = x + (width - islandWidth) / 2;
        roundedRect(graphics, islandX, y + 10, islandWidth, 18, 9, 0xFF000000);

        int batteryX = x + width - 42;
        drawBattery(graphics, batteryX, y + 14, state.getBatteryPercent());

        int wifiX = batteryX - 22;
        if (state.isWifiUsable()) {
            drawWifiIcon(
                    graphics,
                    wifiX,
                    y + 14,
                    wifiStrength(state.getWifi().rssiDbm())
            );
        }

        int cellularX = wifiX - 27;
        if (state.getCellular().enabled() && state.getCellular().registered()) {
            drawCellularBars(
                    graphics,
                    cellularX,
                    y + 14,
                    cellularBars(
                            state.getCellular().rsrpDbm(),
                            state.getCellular().sinrDb()
                    )
            );
        } else {
            String sos = "SOS";
            graphics.drawString(
                    font,
                    sos,
                    cellularX - font.width(sos) + 19,
                    y + 16,
                    0xFFFFFFFF,
                    false
            );
        }
    }

    private static int wifiStrength(int rssi) {
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

        if (sinr < 0.0) return Math.min(bars, 1);
        if (sinr < 10.0) return Math.min(bars, 2);
        if (sinr < 20.0) return Math.min(bars, 3);
        return bars;
    }

    private static void drawCellularBars(GuiGraphics graphics, int x, int y, int bars) {
        for (int i = 0; i < 4; i++) {
            int barHeight = 3 + i * 2;
            int color = i < bars ? 0xFFFFFFFF : 0xFF666970;

            graphics.fill(
                    x + i * 5,
                    y + 9 - barHeight,
                    x + i * 5 + 3,
                    y + 9,
                    color
            );
        }
    }

    private static void drawWifiIcon(GuiGraphics graphics, int x, int y, int strength) {
        int active = 0xFFFFFFFF;
        int inactive = 0xFF666970;

        int topColor = strength >= 3 ? active : inactive;
        int middleColor = strength >= 2 ? active : inactive;
        int lowerColor = strength >= 1 ? active : inactive;

        graphics.fill(x + 1, y + 1, x + 12, y + 2, topColor);
        graphics.fill(x, y + 2, x + 2, y + 4, topColor);
        graphics.fill(x + 11, y + 2, x + 13, y + 4, topColor);

        graphics.fill(x + 3, y + 5, x + 10, y + 6, middleColor);
        graphics.fill(x + 2, y + 6, x + 4, y + 8, middleColor);
        graphics.fill(x + 9, y + 6, x + 11, y + 8, middleColor);

        graphics.fill(x + 5, y + 9, x + 8, y + 11, lowerColor);
    }

    private static void drawBattery(GuiGraphics graphics, int x, int y, int percent) {
        int outline = 0xFFFFFFFF;
        int inside = 0xFF16181D;

        roundedRect(graphics, x, y, 24, 11, 3, outline);
        roundedRect(graphics, x + 2, y + 2, 20, 7, 2, inside);

        graphics.fill(x + 24, y + 3, x + 26, y + 8, outline);

        int fillWidth = Math.max(
                0,
                Math.min(
                        18,
                        Math.round(18.0F * percent / 100.0F)
                )
        );

        if (fillWidth > 0) {
            roundedRect(
                    graphics,
                    x + 3,
                    y + 3,
                    fillWidth,
                    5,
                    1,
                    percent <= 20 ? 0xFFFF453A : 0xFFFFFFFF
            );
        }
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
            int inset = (int) Math.ceil(
                    radius - Math.sqrt(
                            radius * radius
                                    - (radius - i) * (radius - i)
                    )
            );

            graphics.fill(x + inset, y + i, x + width - inset, y + i + 1, color);
            graphics.fill(x + inset, y + height - i - 1, x + width - inset, y + height - i, color);
        }
    }
}

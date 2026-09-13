package com.k1ngtle.vsia.phone.client;

import com.k1ngtle.vsia.phone.network.PhoneNetworkState;
import com.k1ngtle.vsia.phone.subscriber.PhoneSubscriberClientState;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public final class IPhoneStatusBar {
    private static final DateTimeFormatter TIME_24 = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter TIME_12 = DateTimeFormatter.ofPattern("h:mm");

    private IPhoneStatusBar() {
    }

    public static void render(
            GuiGraphics graphics,
            Font font,
            int x,
            int y,
            int width
    ) {
        PhoneNetworkState state = PhoneNetworkState.get();

        graphics.drawString(
                font,
                PhoneText.component(
                        LocalTime.now().format(
                                PhoneSystemSettings.use24HourTime()
                                        ? TIME_24
                                        : TIME_12
                        )
                ),
                x + 18,
                y + 16,
                0xFFFFFFFF,
                false
        );

        int islandWidth = 60;
        int islandX = x + (width - islandWidth) / 2;
        int islandY = y + 10;

        roundedRect(
                graphics,
                islandX,
                islandY,
                islandWidth,
                18,
                9,
                0xFF000000
        );

        int islandRight = islandX + islandWidth;
        int batteryX = x + width - 31;

        drawBattery(
                graphics,
                batteryX,
                y + 14,
                state.getBatteryPercent()
        );

        boolean wifiConnected = state.isWifiUsable();
        int wifiX = batteryX - 18;

        if (wifiConnected) {
            drawWifiIcon(
                    graphics,
                    wifiX,
                    y + 14,
                    wifiBars(state.getWifi().rssiDbm()),
                    0xFFFFFFFF
            );
        }

        PhoneNetworkState.CellularStatus cellular = state.getCellular();
        boolean hasSubscriber = PhoneSubscriberClientState.get().hasActiveSubscription();

        int cellularX = wifiConnected
                ? wifiX - 24
                : batteryX - 24;
        cellularX = Math.max(cellularX, islandRight + 8);

        if (PhoneDeviceSettingsClientState.airplaneMode()) {
            drawAirplaneIcon(
                    graphics,
                    cellularX,
                    y + 14
            );
            return;
        }

        if (hasSubscriber && cellular.enabled()) {
            drawCellularBars(
                    graphics,
                    cellularX,
                    y + 15,
                    cellular.registered()
                            ? cellularBars(cellular.rsrpDbm(), cellular.sinrDb())
                            : 0
            );
        } else {
            graphics.drawString(
                    font,
                    PhoneText.component("SOS"),
                    cellularX,
                    y + 16,
                    0xFFFFFFFF,
                    false
            );
        }
    }

    private static void drawAirplaneIcon(
            GuiGraphics graphics,
            int x,
            int y
    ) {
        graphics.fill(x + 5, y, x + 7, y + 12, 0xFFFFFFFF);
        graphics.fill(x, y + 5, x + 12, y + 7, 0xFFFFFFFF);
        graphics.fill(x + 2, y + 3, x + 6, y + 5, 0xFFFFFFFF);
        graphics.fill(x + 6, y + 7, x + 10, y + 9, 0xFFFFFFFF);
        graphics.fill(x + 4, y + 10, x + 6, y + 13, 0xFFFFFFFF);
        graphics.fill(x + 7, y + 10, x + 9, y + 12, 0xFFFFFFFF);
    }

    private static int wifiBars(int rssi) {
        if (rssi >= -55) return 3;
        if (rssi >= -67) return 2;
        if (rssi >= -80) return 1;
        return 0;
    }

    private static int cellularBars(int rsrp, double sinr) {
        int bars;
        if (rsrp >= -85) bars = 4;
        else if (rsrp >= -95) bars = 3;
        else if (rsrp >= -105) bars = 2;
        else if (rsrp >= -115) bars = 1;
        else bars = 0;

        if (sinr < -5.0) return Math.min(bars, 1);
        if (sinr < 5.0) return Math.min(bars, 2);
        if (sinr < 15.0) return Math.min(bars, 3);
        return bars;
    }

    private static void drawCellularBars(
            GuiGraphics graphics,
            int x,
            int y,
            int bars
    ) {
        for (int i = 0; i < 4; i++) {
            int height = 3 + i * 3;
            int left = x + i * 4;
            graphics.fill(
                    left,
                    y + 9 - height,
                    left + 2,
                    y + 9,
                    i < bars ? 0xFFFFFFFF : 0xFF626267
            );
        }
    }

    private static void drawWifiIcon(
            GuiGraphics graphics,
            int x,
            int y,
            int strength,
            int activeColor
    ) {
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

    private static void drawBattery(
            GuiGraphics graphics,
            int x,
            int y,
            int percent
    ) {
        graphics.fill(x, y, x + 19, y + 9, 0xFFFFFFFF);
        graphics.fill(x + 1, y + 1, x + 18, y + 8, 0xFF16181D);
        graphics.fill(x + 19, y + 3, x + 21, y + 6, 0xFFFFFFFF);

        int fill = Math.max(
                1,
                Math.min(
                        16,
                        (int) Math.round(percent / 100.0 * 16.0)
                )
        );

        int color =
                PhoneSystemSettings.lowPowerMode()
                        ? 0xFFFFD60A
                        : percent <= 20
                        ? 0xFFFF453A
                        : 0xFFFFFFFF;
        graphics.fill(x + 2, y + 2, x + 2 + fill, y + 7, color);
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

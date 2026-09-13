package com.k1ngtle.vsia.phone.client.screen;

import com.k1ngtle.vsia.phone.network.PhoneNetworkController;
import com.k1ngtle.vsia.phone.network.PhoneNetworkState;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.Locale;

public class IPhoneWifiDetailsScreen
        extends IPhoneScreen {

    private int contentX;
    private int contentWidth;

    public IPhoneWifiDetailsScreen() {
        super(
                Component.literal(
                        "Wi-Fi Details"
                )
        );
    }

    @Override
    protected void init() {
        super.init();

        contentX =
                phoneX + 14;

        contentWidth =
                PHONE_WIDTH - 28;

        PhoneNetworkController
                .get()
                .requestRefresh();
    }

    @Override
    public void render(
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        renderPhoneShell(
                graphics,
                0xFF1C1C1E
        );

        renderStatusBar(
                graphics
        );

        renderHeader(
                graphics,
                "Wi-Fi",
                "Network"
        );

        PhoneNetworkState.WifiStatus wifi =
                PhoneNetworkState
                        .get()
                        .getWifi();

        int titleY =
                phoneY + 77;

        graphics.drawCenteredString(
                font,
                wifi.ssid()
                        .isBlank()
                        ? "No Network"
                        : wifi.ssid(),
                phoneX
                        + PHONE_WIDTH
                        / 2,
                titleY,
                0xFFFFFFFF
        );

        graphics.drawCenteredString(
                font,
                wifi.connected()
                        ? wifi.quality()
                        : wifi.status(),
                phoneX
                        + PHONE_WIDTH
                        / 2,
                titleY + 17,
                wifi.connected()
                        ? 0xFF30D158
                        : 0xFFFF9F0A
        );

        int radioY =
                titleY + 42;

        section(
                graphics,
                "RADIO LINK",
                radioY - 16
        );

        roundedRect(
                graphics,
                contentX,
                radioY,
                contentWidth,
                140,
                14,
                0xFF2C2C2E
        );

        pair(
                graphics,
                radioY,
                "BSSID",
                wifi.bssid()
        );

        divider(
                graphics,
                radioY + 28
        );

        pair(
                graphics,
                radioY + 28,
                "RSSI",
                wifi.rssiDbm()
                        + " dBm"
        );

        divider(
                graphics,
                radioY + 56
        );

        pair(
                graphics,
                radioY + 56,
                "SINR",
                String.format(
                        Locale.ROOT,
                        "%.1f dB",
                        wifi.sinrDb()
                )
        );

        divider(
                graphics,
                radioY + 84
        );

        pair(
                graphics,
                radioY + 84,
                "Channel",
                wifi.channel()
                        + " / "
                        + String.format(
                        Locale.ROOT,
                        "%.3f GHz",
                        wifi.frequencyHz()
                                / 1.0E9
                )
        );

        divider(
                graphics,
                radioY + 112
        );

        pair(
                graphics,
                radioY + 112,
                "AP Distance",
                String.format(
                        Locale.ROOT,
                        "%.1f m",
                        wifi.distanceBlocks()
                )
        );

        int ipY =
                radioY + 160;

        section(
                graphics,
                "IP CONFIGURATION",
                ipY - 16
        );

        roundedRect(
                graphics,
                contentX,
                ipY,
                contentWidth,
                112,
                14,
                0xFF2C2C2E
        );

        pair(
                graphics,
                ipY,
                "IP Address",
                wifi.ipAddress()
        );

        divider(
                graphics,
                ipY + 28
        );

        pair(
                graphics,
                ipY + 28,
                "Subnet",
                wifi.subnetMask()
        );

        divider(
                graphics,
                ipY + 56
        );

        pair(
                graphics,
                ipY + 56,
                "Router",
                wifi.gateway()
        );

        divider(
                graphics,
                ipY + 84
        );

        pair(
                graphics,
                ipY + 84,
                "DNS",
                wifi.dns()
        );

        graphics.drawCenteredString(
                font,
                "Tap here to disconnect",
                phoneX
                        + PHONE_WIDTH
                        / 2,
                ipY + 132,
                0xFFFF453A
        );

        renderHomeIndicator(
                graphics
        );
    }

    private void section(
            GuiGraphics graphics,
            String value,
            int y
    ) {
        graphics.drawString(
                font,
                value,
                contentX + 4,
                y,
                0xFF8E8E93,
                false
        );
    }

    private void pair(
            GuiGraphics graphics,
            int y,
            String key,
            String value
    ) {
        graphics.drawString(
                font,
                key,
                contentX + 13,
                y + 10,
                0xFFFFFFFF,
                false
        );

        String shown =
                fit(
                        value,
                        112
                );

        int width =
                font.width(
                        shown
                );

        graphics.drawString(
                font,
                shown,
                contentX
                        + contentWidth
                        - width
                        - 13,
                y + 10,
                0xFFAEAEB2,
                false
        );
    }

    private void divider(
            GuiGraphics graphics,
            int y
    ) {
        graphics.fill(
                contentX + 13,
                y,
                contentX
                        + contentWidth
                        - 13,
                y + 1,
                0xFF3A3A3C
        );
    }

    private String fit(
            String value,
            int maxWidth
    ) {
        String text =
                value == null
                        ? ""
                        : value;

        if (font.width(
                text
        ) <= maxWidth) {
            return text;
        }

        while (!text.isEmpty()
                && font.width(
                text + "..."
        ) > maxWidth) {
            text =
                    text.substring(
                            0,
                            text.length() - 1
                    );
        }

        return text + "...";
    }

    @Override
    public boolean mouseClicked(
            double mouseX,
            double mouseY,
            int button
    ) {
        if (button == 0
                && clickedBack(
                mouseX,
                mouseY
        )) {
            minecraft.setScreen(
                    new IPhoneWifiScreen()
            );

            return true;
        }

        if (button == 0
                && mouseY >= phoneY + 355
                && mouseY <= phoneY + 400) {
            PhoneNetworkController
                    .get()
                    .disconnectWifi();

            minecraft.setScreen(
                    new IPhoneWifiScreen()
            );

            return true;
        }

        return super.mouseClicked(
                mouseX,
                mouseY,
                button
        );
    }
}

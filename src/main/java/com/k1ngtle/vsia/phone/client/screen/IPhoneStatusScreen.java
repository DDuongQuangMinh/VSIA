package com.k1ngtle.vsia.phone.client.screen;

import com.k1ngtle.vsia.phone.network.PhoneNetworkState;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.Locale;

public class IPhoneStatusScreen
        extends IPhoneScreen {

    public IPhoneStatusScreen() {
        super(
                Component.literal(
                        "Network Status"
                )
        );
    }

    @Override
    public void render(
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        renderPhoneBase(
                graphics
        );

        renderHeader(
                graphics,
                "Home",
                "Status"
        );

        PhoneNetworkState state =
                PhoneNetworkState.get();

        PhoneNetworkState.WifiStatus wifi =
                state.getWifi();

        PhoneNetworkState.CellularStatus cellular =
                state.getCellular();

        int x =
                phoneX + 18;

        int y =
                phoneY + 82;

        String active =
                state.isWifiUsable()
                        ? "Wi-Fi"
                        : state.isCellularUsable()
                        ? cellular.radioLabel()
                        + " Cellular"
                        : "No Connection";

        y = pair(
                graphics,
                x,
                y,
                "Active Interface",
                active
        );

        y = pair(
                graphics,
                x,
                y,
                "Battery",
                state.getBatteryPercent()
                        + "%"
        );

        y += 14;

        graphics.drawString(
                font,
                "WI-FI",
                x,
                y,
                0xFF8E8E93,
                false
        );

        y += 19;

        y = pair(
                graphics,
                x,
                y,
                "State",
                wifi.stage()
        );

        y = pair(
                graphics,
                x,
                y,
                "SSID",
                wifi.ssid()
                        .isBlank()
                        ? "—"
                        : wifi.ssid()
        );

        y = pair(
                graphics,
                x,
                y,
                "RSSI",
                wifi.rssiDbm()
                        + " dBm"
        );

        y = pair(
                graphics,
                x,
                y,
                "SINR",
                String.format(
                        Locale.ROOT,
                        "%.1f dB",
                        wifi.sinrDb()
                )
        );

        y += 14;

        graphics.drawString(
                font,
                "CELLULAR",
                x,
                y,
                0xFF8E8E93,
                false
        );

        y += 19;

        y = pair(
                graphics,
                x,
                y,
                "RRC",
                cellular.rrcState()
        );

        y = pair(
                graphics,
                x,
                y,
                "NAS",
                cellular.nasState()
        );

        y = pair(
                graphics,
                x,
                y,
                "PDU",
                cellular.pduState()
        );

        y = pair(
                graphics,
                x,
                y,
                "RSRP",
                cellular.rsrpDbm()
                        + " dBm"
        );

        pair(
                graphics,
                x,
                y,
                "SINR",
                String.format(
                        Locale.ROOT,
                        "%.1f dB",
                        cellular.sinrDb()
                )
        );

        renderHomeIndicator(
                graphics
        );
    }

    private int pair(
            GuiGraphics graphics,
            int x,
            int y,
            String key,
            String value
    ) {
        graphics.drawString(
                font,
                key,
                x,
                y,
                0xFFA8A8AD,
                false
        );

        String shown =
                value == null
                        ? ""
                        : value;

        int width =
                font.width(
                        shown
                );

        graphics.drawString(
                font,
                shown,
                phoneX
                        + PHONE_WIDTH
                        - 18
                        - width,
                y,
                0xFFFFFFFF,
                false
        );

        return y + 21;
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
                    new IPhoneHomeScreen()
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

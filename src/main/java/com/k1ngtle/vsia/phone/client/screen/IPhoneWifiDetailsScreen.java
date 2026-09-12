package com.k1ngtle.vsia.phone.client.screen;

import com.k1ngtle.vsia.phone.network.PhoneNetworkState;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public class IPhoneWifiDetailsScreen extends IPhoneScreen {
    public IPhoneWifiDetailsScreen() {
        super(Component.literal("Wi-Fi Details"));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderPhoneBase(graphics);
        renderHeader(graphics, "Wi-Fi", "Network");

        var wifi = PhoneNetworkState.get().getWifi();

        int x = phoneX + 18;
        int y = phoneY + 82;

        graphics.drawString(font, wifi.ssid().isBlank() ? "No Network" : wifi.ssid(), x, y, 0xFFFFFFFF, false);

        int line = y + 30;
        line = pair(graphics, x, line, "Configure IP", "DHCP");
        line = pair(graphics, x, line, "IP Address", wifi.ipAddress());
        line = pair(graphics, x, line, "Subnet Mask", wifi.subnetMask());
        line = pair(graphics, x, line, "Router", wifi.gateway());

        line += 12;
        line = pair(graphics, x, line, "Configure DNS", "Automatic");
        line = pair(graphics, x, line, "DNS", wifi.dns());

        line += 12;
        line = pair(graphics, x, line, "BSSID", wifi.bssid());
        line = pair(graphics, x, line, "Channel", Integer.toString(wifi.channel()));
        line = pair(graphics, x, line, "RSSI", wifi.rssiDbm() + " dBm");
        line = pair(graphics, x, line, "PHY", wifi.phy());

        renderHomeIndicator(graphics);
    }

    private int pair(GuiGraphics g, int x, int y, String key, String value) {
        g.drawString(font, key, x, y, 0xFFA8A8AD, false);
        int vw = font.width(value);
        g.drawString(font, value, phoneX + PHONE_WIDTH - 18 - vw, y, 0xFFFFFFFF, false);
        return y + 22;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && clickedBack(mouseX, mouseY)) {
            minecraft.setScreen(new IPhoneWifiScreen());
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}

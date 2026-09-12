package com.k1ngtle.vsia.phone.client.screen;

import com.k1ngtle.vsia.phone.network.PhoneNetworkState;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public class IPhoneWifiDetailsScreen extends IPhoneScreen {
    private int contentX;
    private int contentWidth;

    public IPhoneWifiDetailsScreen() {
        super(Component.literal("Wi-Fi Details"));
    }

    @Override
    protected void init() {
        super.init();
        contentX = phoneX + 14;
        contentWidth = PHONE_WIDTH - 28;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderPhoneShell(graphics, 0xFF1C1C1E);
        renderStatusBar(graphics);
        renderHeaderLocal(graphics);

        var wifi = PhoneNetworkState.get().getWifi();
        int titleY = phoneY + 78;

        graphics.drawCenteredString(font, wifi.ssid().isBlank() ? "No Network" : wifi.ssid(), phoneX + PHONE_WIDTH / 2, titleY, 0xFFFFFFFF);
        if (wifi.connected()) {
            graphics.drawCenteredString(font, "Connected", phoneX + PHONE_WIDTH / 2, titleY + 18, 0xFF30D158);
        }

        int ipv4Y = titleY + 44;
        sectionLabel(graphics, "IPV4 ADDRESS", ipv4Y - 17);
        roundedRect(graphics, contentX, ipv4Y, contentWidth, 112, 14, 0xFF2C2C2E);
        pair(graphics, ipv4Y, "Configure IP", "DHCP");
        divider(graphics, ipv4Y + 28);
        pair(graphics, ipv4Y + 28, "IP Address", wifi.ipAddress());
        divider(graphics, ipv4Y + 56);
        pair(graphics, ipv4Y + 56, "Subnet Mask", wifi.subnetMask());
        divider(graphics, ipv4Y + 84);
        pair(graphics, ipv4Y + 84, "Router", wifi.gateway());

        int dnsY = ipv4Y + 132;
        sectionLabel(graphics, "DNS", dnsY - 17);
        roundedRect(graphics, contentX, dnsY, contentWidth, 56, 14, 0xFF2C2C2E);
        pair(graphics, dnsY, "Configure DNS", "Automatic");
        divider(graphics, dnsY + 28);
        pair(graphics, dnsY + 28, "DNS", wifi.dns());

        int radioY = dnsY + 76;
        sectionLabel(graphics, "RADIO", radioY - 17);
        roundedRect(graphics, contentX, radioY, contentWidth, 112, 14, 0xFF2C2C2E);
        pair(graphics, radioY, "BSSID", wifi.bssid());
        divider(graphics, radioY + 28);
        pair(graphics, radioY + 28, "Channel", Integer.toString(wifi.channel()));
        divider(graphics, radioY + 56);
        pair(graphics, radioY + 56, "RSSI", wifi.rssiDbm() + " dBm");
        divider(graphics, radioY + 84);
        pair(graphics, radioY + 84, "PHY", wifi.phy());

        renderHomeIndicator(graphics);
    }

    private void renderHeaderLocal(GuiGraphics g) {
        g.drawString(font, "< Wi-Fi", phoneX + 16, phoneY + 49, 0xFF0A84FF, false);
        int tw = font.width("Network");
        g.drawString(font, "Network", phoneX + (PHONE_WIDTH - tw) / 2, phoneY + 49, 0xFFFFFFFF, false);
    }

    private void sectionLabel(GuiGraphics g, String text, int y) {
        g.drawString(font, text, contentX + 4, y, 0xFF8E8E93, false);
    }

    private void pair(GuiGraphics g, int y, String key, String value) {
        g.drawString(font, key, contentX + 13, y + 10, 0xFFFFFFFF, false);
        String shown = fit(value, 90);
        int vw = font.width(shown);
        g.drawString(font, shown, contentX + contentWidth - vw - 13, y + 10, 0xFFAEAEB2, false);
    }

    private void divider(GuiGraphics g, int y) {
        g.fill(contentX + 13, y, contentX + contentWidth - 13, y + 1, 0xFF3A3A3C);
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
        if (button == 0 && clickedBack(mouseX, mouseY)) {
            minecraft.setScreen(new IPhoneWifiScreen());
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}

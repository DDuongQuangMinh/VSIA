package com.k1ngtle.vsia.phone.client.screen;

import com.k1ngtle.vsia.phone.network.PhoneNetworkState;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public class IPhoneStatusScreen extends IPhoneScreen {
    public IPhoneStatusScreen() {
        super(Component.literal("Network Status"));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderPhoneBase(graphics);
        renderHeader(graphics, "Home", "Status");

        PhoneNetworkState state = PhoneNetworkState.get();
        var wifi = state.getWifi();
        var cellular = state.getCellular();

        int x = phoneX + 18;
        int y = phoneY + 82;

        String active = state.isWifiUsable()
                ? "Wi-Fi"
                : state.isCellularUsable() ? cellular.radioLabel() + " Cellular" : "No Connection";

        y = pair(graphics, x, y, "Active Interface", active);
        y = pair(graphics, x, y, "Battery", state.getBatteryPercent() + "%");

        y += 16;
        graphics.drawString(font, "WI-FI", x, y, 0xFF8E8E93, false);
        y += 20;
        y = pair(graphics, x, y, "State", wifi.stage().displayName());
        y = pair(graphics, x, y, "SSID", wifi.ssid().isBlank() ? "—" : wifi.ssid());
        y = pair(graphics, x, y, "RSSI", wifi.rssiDbm() + " dBm");

        y += 16;
        graphics.drawString(font, "CELLULAR", x, y, 0xFF8E8E93, false);
        y += 20;
        y = pair(graphics, x, y, "RRC", cellular.rrcState());
        y = pair(graphics, x, y, "NAS", cellular.nasState());
        y = pair(graphics, x, y, "PDU", cellular.pduState());
        y = pair(graphics, x, y, "RSRP", cellular.rsrpDbm() + " dBm");
        y = pair(graphics, x, y, "SINR", cellular.sinrDb() + " dB");

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
            minecraft.setScreen(new IPhoneHomeScreen());
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}

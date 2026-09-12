package com.k1ngtle.vsia.phone.client.screen;

import com.k1ngtle.vsia.phone.network.PhoneNetworkState;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public class IPhoneSettingsScreen extends IPhoneScreen {
    public IPhoneSettingsScreen() {
        super(Component.literal("Settings"));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderPhoneBase(graphics);
        renderHeader(graphics, null, "Settings");

        int x = phoneX + 14;
        int y = phoneY + 82;
        int w = PHONE_WIDTH - 28;

        roundedRect(graphics, x, y, w, 92, 14, 0xFF292C33);
        row(graphics, x, y, w, "Wi-Fi", wifiSummary(), true);
        graphics.fill(x + 12, y + 45, x + w - 12, y + 46, 0xFF3B3E45);
        row(graphics, x, y + 46, w, "Cellular", cellularSummary(), true);

        roundedRect(graphics, x, y + 112, w, 46, 14, 0xFF292C33);
        row(graphics, x, y + 112, w, "Browser", "Network-aware", false);

        renderHomeIndicator(graphics);
    }

    private String wifiSummary() {
        var wifi = PhoneNetworkState.get().getWifi();
        return wifi.connected() ? wifi.ssid() : "Not Connected";
    }

    private String cellularSummary() {
        var cellular = PhoneNetworkState.get().getCellular();
        return cellular.registered() ? cellular.carrier() : "No Service";
    }

    private void row(GuiGraphics g, int x, int y, int w, String left, String right, boolean chevron) {
        g.drawString(font, left, x + 14, y + 18, 0xFFFFFFFF, false);
        int rw = font.width(right);
        g.drawString(font, right, x + w - rw - (chevron ? 24 : 14), y + 18, 0xFFA8A8AD, false);
        if (chevron) {
            g.drawString(font, "›", x + w - 15, y + 18, 0xFF88888D, false);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int x = phoneX + 14;
            int y = phoneY + 82;
            int w = PHONE_WIDTH - 28;

            if (inside(mouseX, mouseY, x, y, w, 46)) {
                minecraft.setScreen(new IPhoneWifiScreen());
                return true;
            }
            if (inside(mouseX, mouseY, x, y + 46, w, 46)) {
                minecraft.setScreen(new IPhoneCellularScreen());
                return true;
            }
            if (inside(mouseX, mouseY, x, y + 112, w, 46)) {
                minecraft.setScreen(new IPhoneBrowserScreen());
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}

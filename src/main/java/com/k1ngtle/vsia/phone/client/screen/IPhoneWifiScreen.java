package com.k1ngtle.vsia.phone.client.screen;

import com.k1ngtle.vsia.phone.network.PhoneNetworkController;
import com.k1ngtle.vsia.phone.network.PhoneNetworkState;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public class IPhoneWifiScreen extends IPhoneScreen {
    public IPhoneWifiScreen() {
        super(Component.literal("Wi-Fi"));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderPhoneBase(graphics);
        renderHeader(graphics, "Settings", "Wi-Fi");

        PhoneNetworkState state = PhoneNetworkState.get();
        int x = phoneX + 14;
        int y = phoneY + 80;
        int w = PHONE_WIDTH - 28;

        roundedRect(graphics, x, y, w, 46, 14, 0xFF292C33);
        graphics.drawString(font, "Wi-Fi", x + 14, y + 18, 0xFFFFFFFF, false);
        drawToggle(graphics, x + w - 48, y + 11, state.getWifi().enabled());

        int listY = y + 62;
        drawNetwork(graphics, x, listY, w, "VSIA-LAB-WIFI", false, -51,
                state.getWifi().connected() && "VSIA-LAB-WIFI".equals(state.getWifi().ssid()));
        drawNetwork(graphics, x, listY + 48, w, "HomeNetwork", false, -64,
                state.getWifi().connected() && "HomeNetwork".equals(state.getWifi().ssid()));
        drawNetwork(graphics, x, listY + 96, w, "Engineering-AP", true, -72,
                state.getWifi().connected() && "Engineering-AP".equals(state.getWifi().ssid()));
        drawNetwork(graphics, x, listY + 144, w, "GuestNetwork", true, -83,
                state.getWifi().connected() && "GuestNetwork".equals(state.getWifi().ssid()));

        graphics.drawString(font, "Connection Stage", x, listY + 208, 0xFF8E8E93, false);
        graphics.drawString(font, state.getWifi().stage().displayName(), x, listY + 226, 0xFFFFFFFF, false);

        if (state.getWifi().connected()) {
            graphics.drawString(font, "ⓘ tap connected network for details", x, listY + 248, 0xFF8E8E93, false);
        }

        renderHomeIndicator(graphics);
    }

    private void drawNetwork(GuiGraphics g, int x, int y, int w, String ssid, boolean locked, int rssi, boolean selected) {
        roundedRect(g, x, y, w, 44, 12, 0xFF292C33);
        g.drawString(font, selected ? "✓  " + ssid : ssid, x + 12, y + 17, 0xFFFFFFFF, false);
        String right = (locked ? "🔒 " : "") + signalGlyph(rssi) + "  ⓘ";
        int rw = font.width(right);
        g.drawString(font, right, x + w - rw - 10, y + 17, 0xFFD0D0D5, false);
    }

    private String signalGlyph(int rssi) {
        if (rssi >= -55) return "))))";
        if (rssi >= -67) return ")))";
        if (rssi >= -75) return "))";
        return ")";
    }

    private void drawToggle(GuiGraphics g, int x, int y, boolean on) {
        roundedRect(g, x, y, 38, 22, 11, on ? 0xFF34C759 : 0xFF5C5C62);
        int knobX = on ? x + 18 : x + 2;
        roundedRect(g, knobX, y + 2, 18, 18, 9, 0xFFFFFFFF);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (clickedBack(mouseX, mouseY)) {
                minecraft.setScreen(new IPhoneSettingsScreen());
                return true;
            }

            int x = phoneX + 14;
            int y = phoneY + 80;
            int w = PHONE_WIDTH - 28;

            if (inside(mouseX, mouseY, x + w - 56, y + 6, 50, 34)) {
                PhoneNetworkController.get().setWifiEnabled(!PhoneNetworkState.get().getWifi().enabled());
                return true;
            }

            int listY = y + 62;
            String[] ssids = {"VSIA-LAB-WIFI", "HomeNetwork", "Engineering-AP", "GuestNetwork"};
            int[] rssis = {-51, -64, -72, -83};

            for (int i = 0; i < ssids.length; i++) {
                if (inside(mouseX, mouseY, x, listY + i * 48, w, 44)) {
                    if (PhoneNetworkState.get().getWifi().connected()
                            && ssids[i].equals(PhoneNetworkState.get().getWifi().ssid())) {
                        minecraft.setScreen(new IPhoneWifiDetailsScreen());
                    } else {
                        PhoneNetworkController.get().connectWifi(ssids[i], rssis[i], i >= 2);
                    }
                    return true;
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }
}

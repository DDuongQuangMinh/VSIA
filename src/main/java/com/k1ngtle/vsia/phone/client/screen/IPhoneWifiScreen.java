package com.k1ngtle.vsia.phone.client.screen;

import com.k1ngtle.vsia.phone.network.PhoneNetworkController;
import com.k1ngtle.vsia.phone.network.PhoneNetworkState;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public class IPhoneWifiScreen extends IPhoneScreen {
    private static final NetworkEntry[] NETWORKS = {
            new NetworkEntry("VSIA-LAB-WIFI", false, -51),
            new NetworkEntry("HomeNetwork", false, -64),
            new NetworkEntry("Engineering-AP", true, -72),
            new NetworkEntry("GuestNetwork", true, -83)
    };

    private int contentX;
    private int contentWidth;
    private int toggleY;
    private int listY;

    public IPhoneWifiScreen() {
        super(Component.literal("Wi-Fi"));
    }

    @Override
    protected void init() {
        super.init();
        contentX = phoneX + 14;
        contentWidth = PHONE_WIDTH - 28;
        toggleY = phoneY + 80;
        listY = toggleY + 78;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderPhoneShell(graphics, 0xFF1C1C1E);
        renderStatusBar(graphics);
        renderHeaderLocal(graphics);

        PhoneNetworkState state = PhoneNetworkState.get();

        roundedRect(graphics, contentX, toggleY, contentWidth, 50, 14, 0xFF2C2C2E);
        graphics.drawString(font, "Wi-Fi", contentX + 14, toggleY + 20, 0xFFFFFFFF, false);
        drawToggle(graphics, contentX + contentWidth - 52, toggleY + 13, state.getWifi().enabled());

        graphics.drawString(font, "NETWORKS", contentX + 4, listY - 19, 0xFF8E8E93, false);

        if (!state.getWifi().enabled()) {
            roundedRect(graphics, contentX, listY, contentWidth, 70, 14, 0xFF2C2C2E);
            graphics.drawCenteredString(font, "Wi-Fi is Off", phoneX + PHONE_WIDTH / 2, listY + 20, 0xFFFFFFFF);
            graphics.drawCenteredString(font, "Turn on Wi-Fi to discover networks.", phoneX + PHONE_WIDTH / 2, listY + 41, 0xFF8E8E93);
            renderHomeIndicator(graphics);
            return;
        }

        int groupHeight = NETWORKS.length * 48;
        roundedRect(graphics, contentX, listY, contentWidth, groupHeight, 14, 0xFF2C2C2E);

        for (int i = 0; i < NETWORKS.length; i++) {
            int rowY = listY + i * 48;
            if (i > 0) {
                graphics.fill(contentX + 14, rowY, contentX + contentWidth - 14, rowY + 1, 0xFF3A3A3C);
            }
            NetworkEntry entry = NETWORKS[i];
            boolean selected = state.getWifi().connected() && entry.ssid().equals(state.getWifi().ssid());
            drawNetworkRow(graphics, entry, rowY, selected);
        }

        int stageY = listY + groupHeight + 24;
        graphics.drawString(font, "CONNECTION", contentX + 4, stageY - 18, 0xFF8E8E93, false);
        roundedRect(graphics, contentX, stageY, contentWidth, 52, 14, 0xFF2C2C2E);
        graphics.drawString(font, "Stage", contentX + 14, stageY + 18, 0xFFFFFFFF, false);

        String stage = state.getWifi().stage().displayName();
        int sw = font.width(stage);
        graphics.drawString(font, stage, contentX + contentWidth - sw - 14, stageY + 18, stageColor(state.getWifi().stage()), false);

        renderHomeIndicator(graphics);
    }

    private void renderHeaderLocal(GuiGraphics g) {
        g.drawString(font, "< Settings", phoneX + 16, phoneY + 49, 0xFF0A84FF, false);
        int tw = font.width("Wi-Fi");
        g.drawString(font, "Wi-Fi", phoneX + (PHONE_WIDTH - tw) / 2, phoneY + 49, 0xFFFFFFFF, false);
    }

    private void drawNetworkRow(GuiGraphics g, NetworkEntry entry, int y, boolean selected) {
        int textX = contentX + 14;
        if (selected) {
            drawCheck(g, textX, y + 19, 0xFF0A84FF);
            textX += 17;
        }

        g.drawString(font, entry.ssid(), textX, y + 18, 0xFFFFFFFF, false);

        int infoX = contentX + contentWidth - 16;
        drawInfo(g, infoX, y + 23);

        int wifiX = infoX - 29;
        drawWifiStrength(g, wifiX, y + 16, entry.rssi());

        if (entry.locked()) {
            drawLock(g, wifiX - 15, y + 16, 0xFFD1D1D6);
        }
    }

    private void drawToggle(GuiGraphics g, int x, int y, boolean on) {
        roundedRect(g, x, y, 40, 24, 12, on ? 0xFF34C759 : 0xFF636366);
        int knobX = on ? x + 18 : x + 2;
        roundedRect(g, knobX, y + 2, 20, 20, 10, 0xFFFFFFFF);
    }

    private void drawWifiStrength(GuiGraphics g, int x, int y, int rssi) {
        int strength = rssi >= -55 ? 3 : rssi >= -67 ? 2 : rssi >= -80 ? 1 : 0;
        int on = 0xFFFFFFFF;
        int off = 0xFF636366;
        g.fill(x, y + 1, x + 14, y + 3, strength >= 3 ? on : off);
        g.fill(x + 2, y + 5, x + 12, y + 7, strength >= 2 ? on : off);
        g.fill(x + 5, y + 9, x + 9, y + 11, strength >= 1 ? on : off);
        g.fill(x + 6, y + 13, x + 8, y + 15, strength >= 1 ? on : off);
    }

    private void drawLock(GuiGraphics g, int x, int y, int color) {
        g.fill(x, y + 5, x + 9, y + 12, color);
        g.fill(x + 2, y + 1, x + 7, y + 3, color);
        g.fill(x + 1, y + 2, x + 3, y + 7, color);
        g.fill(x + 6, y + 2, x + 8, y + 7, color);
    }

    private void drawInfo(GuiGraphics g, int cx, int cy) {
        roundedRect(g, cx - 7, cy - 7, 14, 14, 7, 0xFF0A84FF);
        roundedRect(g, cx - 5, cy - 5, 10, 10, 5, 0xFF2C2C2E);
        g.fill(cx, cy - 2, cx + 1, cy + 4, 0xFF0A84FF);
        g.fill(cx, cy - 5, cx + 1, cy - 4, 0xFF0A84FF);
    }

    private void drawCheck(GuiGraphics g, int x, int y, int color) {
        g.fill(x, y, x + 3, y + 2, color);
        g.fill(x + 2, y + 1, x + 4, y + 4, color);
        g.fill(x + 4, y - 3, x + 6, y + 3, color);
    }

    private int stageColor(PhoneNetworkState.WifiStage stage) {
        return switch (stage) {
            case CONNECTED -> 0xFF30D158;
            case FAILED -> 0xFFFF453A;
            case IDLE -> 0xFF8E8E93;
            default -> 0xFFFFD60A;
        };
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (clickedBack(mouseX, mouseY)) {
                minecraft.setScreen(new IPhoneSettingsScreen());
                return true;
            }

            if (inside(mouseX, mouseY, contentX + contentWidth - 58, toggleY + 7, 54, 38)) {
                PhoneNetworkController.get().setWifiEnabled(!PhoneNetworkState.get().getWifi().enabled());
                return true;
            }

            if (!PhoneNetworkState.get().getWifi().enabled()) {
                return super.mouseClicked(mouseX, mouseY, button);
            }

            for (int i = 0; i < NETWORKS.length; i++) {
                int rowY = listY + i * 48;
                if (!inside(mouseX, mouseY, contentX, rowY, contentWidth, 48)) continue;

                NetworkEntry entry = NETWORKS[i];
                var wifi = PhoneNetworkState.get().getWifi();

                if (wifi.connected() && entry.ssid().equals(wifi.ssid())) {
                    minecraft.setScreen(new IPhoneWifiDetailsScreen());
                } else {
                    PhoneNetworkController.get().connectWifi(entry.ssid(), entry.rssi(), entry.locked());
                }
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private record NetworkEntry(String ssid, boolean locked, int rssi) {
    }
}

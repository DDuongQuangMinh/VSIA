package com.k1ngtle.vsia.phone.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public class IPhoneHomeScreen extends IPhoneScreen {
    private int settingsX;
    private int browserX;
    private int networkX;
    private int statusX;
    private int row1Y;
    private int row2Y;

    public IPhoneHomeScreen() {
        super(Component.literal("Temporary iPhone"));
    }

    @Override
    protected void init() {
        super.init();
        settingsX = phoneX + 26;
        browserX = phoneX + 132;
        networkX = phoneX + 26;
        statusX = phoneX + 132;
        row1Y = phoneY + 92;
        row2Y = phoneY + 184;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderPhoneBase(graphics);

        drawApp(graphics, settingsX, row1Y, "⚙", "Settings");
        drawApp(graphics, browserX, row1Y, "◉", "Browser");
        drawApp(graphics, networkX, row2Y, "▣", "Network");
        drawApp(graphics, statusX, row2Y, "◫", "Status");

        roundedRect(graphics, phoneX + 54, phoneY + 296, 128, 58, 14, 0xAA2D3038);
        graphics.drawCenteredString(font, "browser", phoneX + PHONE_WIDTH / 2, phoneY + 310, 0xFFD9D9D9);
        graphics.drawCenteredString(font, "settings", phoneX + PHONE_WIDTH / 2, phoneY + 330, 0xFFD9D9D9);

        renderHomeIndicator(graphics);
    }

    private void drawApp(GuiGraphics graphics, int x, int y, String glyph, String label) {
        roundedRect(graphics, x, y, 54, 54, 13, 0xFF2E6BE6);
        graphics.drawCenteredString(font, glyph, x + 27, y + 20, 0xFFFFFFFF);
        graphics.drawCenteredString(font, label, x + 27, y + 61, 0xFFFFFFFF);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (inside(mouseX, mouseY, settingsX, row1Y, 54, 74)) {
                minecraft.setScreen(new IPhoneSettingsScreen());
                return true;
            }
            if (inside(mouseX, mouseY, browserX, row1Y, 54, 74)) {
                minecraft.setScreen(new IPhoneBrowserScreen());
                return true;
            }
            if (inside(mouseX, mouseY, networkX, row2Y, 54, 74)) {
                minecraft.setScreen(new IPhoneWifiScreen());
                return true;
            }
            if (inside(mouseX, mouseY, statusX, row2Y, 54, 74)) {
                minecraft.setScreen(new IPhoneStatusScreen());
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}

package com.k1ngtle.vsia.phone.client.screen;

import com.k1ngtle.vsia.phone.browser.BrowserResponse;
import com.k1ngtle.vsia.phone.browser.PhoneBrowser;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public class IPhoneBrowserScreen extends IPhoneScreen {
    private final PhoneBrowser browser = new PhoneBrowser();
    private String address = "intranet.vsia";
    private BrowserResponse response;

    public IPhoneBrowserScreen() {
        super(Component.literal("Browser"));
    }

    @Override
    protected void init() {
        super.init();
        response = browser.open(address);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderPhoneBase(graphics);

        int x = phoneX + 15;
        int contentY = phoneY + 62;
        int contentW = PHONE_WIDTH - 30;
        int contentH = 280;

        if (response.success()) {
            renderSuccess(graphics, x, contentY, contentW, contentH);
        } else {
            renderError(graphics, x, contentY, contentW, contentH);
        }

        int navY = phoneY + PHONE_HEIGHT - 87;
        graphics.drawCenteredString(font, "‹       ›       ↻       □       ☰", phoneX + PHONE_WIDTH / 2, navY, 0xFFFFFFFF);

        roundedRect(graphics, phoneX + 15, navY + 19, PHONE_WIDTH - 30, 34, 12, 0xFF2A2D33);
        graphics.drawString(font, response.success() ? "🔒  " + address : address, phoneX + 28, navY + 31, 0xFFFFFFFF, false);

        renderHomeIndicator(graphics);
    }

    private void renderSuccess(GuiGraphics g, int x, int y, int w, int h) {
        g.drawCenteredString(font, response.title(), phoneX + PHONE_WIDTH / 2, y + 22, 0xFFFFFFFF);

        int line = y + 55;
        for (String text : response.lines()) {
            if (text.startsWith("[") && text.endsWith("]")) {
                roundedRect(g, x + 8, line - 5, w - 16, 42, 10, 0xFF292C33);
                g.drawString(font, text.substring(1, text.length() - 1), x + 18, line + 8, 0xFFFFFFFF, false);
                line += 50;
            } else {
                g.drawString(font, text, x + 8, line, 0xFFD7D7D9, false);
                line += 17;
            }
        }

        if (response.route() != null) {
            g.drawString(font, "Route: " + response.route().summary(), x + 8, y + h - 24, 0xFF8E8E93, false);
        }
    }

    private void renderError(GuiGraphics g, int x, int y, int w, int h) {
        g.drawCenteredString(font, "Safari cannot open the page", phoneX + PHONE_WIDTH / 2, y + 34, 0xFFFFFFFF);

        int line = y + 72;
        for (String text : response.lines()) {
            g.drawCenteredString(font, text, phoneX + PHONE_WIDTH / 2, line, 0xFFBBBBBF);
            line += 18;
        }

        if (response.openWifiSettingsSuggested()) {
            roundedRect(g, x + 25, y + 160, w - 50, 36, 10, 0xFF2D6CDF);
            g.drawCenteredString(font, "Open Wi-Fi Settings", phoneX + PHONE_WIDTH / 2, y + 174, 0xFFFFFFFF);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && response != null && response.openWifiSettingsSuggested()) {
            int x = phoneX + 15;
            int y = phoneY + 62;
            int w = PHONE_WIDTH - 30;
            if (inside(mouseX, mouseY, x + 25, y + 160, w - 50, 36)) {
                minecraft.setScreen(new IPhoneWifiScreen());
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}

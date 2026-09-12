package com.k1ngtle.vsia.phone.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public class IPhoneHomeScreen extends IPhoneScreen {
    private static final int APP_SIZE = 46;

    private int settingsX;
    private int browserX;
    private int networkX;
    private int statusX;
    private int appY;
    private int dockY;

    public IPhoneHomeScreen() {
        super(Component.literal("Temporary iPhone"));
    }

    @Override
    protected void init() {
        super.init();

        settingsX = phoneX + 16;
        browserX = phoneX + 70;
        networkX = phoneX + 124;
        statusX = phoneX + 178;

        appY = phoneY + 82;
        dockY = phoneY + PHONE_HEIGHT - 86;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderPhoneShell(graphics, 0xFFF06D22);
        renderWallpaper(graphics);
        renderStatusBar(graphics);

        drawApp(graphics, settingsX, appY, AppKind.SETTINGS, "Settings");
        drawApp(graphics, browserX, appY, AppKind.BROWSER, "Browser");
        drawApp(graphics, networkX, appY, AppKind.NETWORK, "Network");
        drawApp(graphics, statusX, appY, AppKind.STATUS, "Status");

        renderPageDots(graphics);
        renderDock(graphics);
        renderHomeIndicator(graphics);
    }

    private void renderWallpaper(GuiGraphics graphics) {
        int x = phoneX + 6;
        int y = phoneY + 6;
        int w = PHONE_WIDTH - 12;
        int h = PHONE_HEIGHT - 12;

        roundedRect(graphics, x, y, w, h, 20, 0xFFF06D22);
        roundedRect(graphics, x - 35, y + 112, 190, 150, 72, 0xCCFFCC47);
        roundedRect(graphics, x + 76, y + 72, 205, 170, 80, 0xBFFF453A);
        roundedRect(graphics, x - 45, y + 228, 190, 155, 75, 0xD9F5F5F7);
        roundedRect(graphics, x + 55, y + 280, 210, 125, 62, 0xC9E93CAC);
        roundedRect(graphics, x + 90, y + 330, 170, 110, 54, 0xB95E5CE6);
        roundedRect(graphics, x + 62, y + 16, 210, 100, 50, 0x33FFFFFF);
    }

    private void drawApp(GuiGraphics graphics, int x, int y, AppKind kind, String label) {
        int background = switch (kind) {
            case SETTINGS -> 0xFFE5E5EA;
            case BROWSER -> 0xFFF9F9FB;
            case NETWORK -> 0xFF34C759;
            case STATUS -> 0xFF1C1C1E;
        };

        roundedRect(graphics, x, y, APP_SIZE, APP_SIZE, 11, background);
        drawIcon(graphics, x, y, kind, APP_SIZE);
        drawScaledCenteredLabel(graphics, label, x + APP_SIZE / 2, y + APP_SIZE + 7, 0.72F);
    }

    private void drawIcon(GuiGraphics graphics, int x, int y, AppKind kind, int size) {
        switch (kind) {
            case SETTINGS -> drawSettingsIcon(graphics, x, y, size);
            case BROWSER -> drawBrowserIcon(graphics, x, y, size);
            case NETWORK -> drawNetworkIcon(graphics, x, y, size);
            case STATUS -> drawStatusIcon(graphics, x, y, size);
        }
    }

    private void drawSettingsIcon(GuiGraphics graphics, int x, int y, int size) {
        int cx = x + size / 2;
        int cy = y + size / 2;
        int gear = 0xFF636366;
        int outer = Math.max(9, size / 4);

        graphics.fill(cx - 2, cy - outer - 5, cx + 2, cy - outer + 1, gear);
        graphics.fill(cx - 2, cy + outer - 1, cx + 2, cy + outer + 5, gear);
        graphics.fill(cx - outer - 5, cy - 2, cx - outer + 1, cy + 2, gear);
        graphics.fill(cx + outer - 1, cy - 2, cx + outer + 5, cy + 2, gear);

        graphics.fill(cx - outer, cy - outer, cx - outer + 4, cy - outer + 4, gear);
        graphics.fill(cx + outer - 4, cy + outer - 4, cx + outer, cy + outer, gear);
        graphics.fill(cx + outer - 4, cy - outer, cx + outer, cy - outer + 4, gear);
        graphics.fill(cx - outer, cy + outer - 4, cx - outer + 4, cy + outer, gear);

        int ring = Math.max(18, size / 2);
        roundedRect(graphics, cx - ring / 2, cy - ring / 2, ring, ring, ring / 2, gear);

        int hole = Math.max(8, ring / 2);
        roundedRect(
                graphics,
                cx - hole / 2,
                cy - hole / 2,
                hole,
                hole,
                hole / 2,
                0xFFE5E5EA
        );

        int hub = Math.max(4, hole / 2);
        roundedRect(
                graphics,
                cx - hub / 2,
                cy - hub / 2,
                hub,
                hub,
                hub / 2,
                gear
        );
    }

    private void drawBrowserIcon(GuiGraphics graphics, int x, int y, int size) {
        int cx = x + size / 2;
        int cy = y + size / 2;
        int diameter = Math.max(28, size - 14);

        roundedRect(
                graphics,
                cx - diameter / 2,
                cy - diameter / 2,
                diameter,
                diameter,
                diameter / 2,
                0xFF0A84FF
        );

        int inner = diameter - 6;
        roundedRect(
                graphics,
                cx - inner / 2,
                cy - inner / 2,
                inner,
                inner,
                inner / 2,
                0xFFF9F9FB
        );

        graphics.fill(cx - 1, cy - inner / 2, cx + 1, cy + inner / 2, 0xFFD1D1D6);
        graphics.fill(cx - inner / 2, cy - 1, cx + inner / 2, cy + 1, 0xFFD1D1D6);

        graphics.fill(cx - 2, cy - 11, cx + 1, cy + 2, 0xFFFF453A);
        graphics.fill(cx - 1, cy, cx + 3, cy + 12, 0xFF0A84FF);
    }

    private void drawNetworkIcon(GuiGraphics graphics, int x, int y, int size) {
        int cx = x + size / 2;
        int cy = y + size / 2;
        int white = 0xFFFFFFFF;
        int reach = Math.max(9, size / 4);

        graphics.hLine(cx - reach, cx + reach, cy, white);
        graphics.vLine(cx, cy - reach, cy + reach, white);

        roundedRect(graphics, cx - reach - 4, cy - 4, 8, 8, 4, white);
        roundedRect(graphics, cx + reach - 4, cy - 4, 8, 8, 4, white);
        roundedRect(graphics, cx - 4, cy - reach - 4, 8, 8, 4, white);
        roundedRect(graphics, cx - 4, cy + reach - 4, 8, 8, 4, white);

        roundedRect(graphics, cx - 5, cy - 5, 10, 10, 5, 0xFF0A7C32);
    }

    private void drawStatusIcon(GuiGraphics graphics, int x, int y, int size) {
        int left = x + Math.max(7, size / 5);
        int bottom = y + size - Math.max(9, size / 4);
        int gap = Math.max(6, size / 6);
        int barWidth = Math.max(4, size / 10);

        graphics.fill(left, bottom - 8, left + barWidth, bottom, 0xFF32D74B);
        graphics.fill(left + gap, bottom - 14, left + gap + barWidth, bottom, 0xFFFFD60A);
        graphics.fill(left + gap * 2, bottom - 20, left + gap * 2 + barWidth, bottom, 0xFFFF9F0A);
        graphics.fill(left + gap * 3, bottom - 26, left + gap * 3 + barWidth, bottom, 0xFFFF453A);
        graphics.hLine(left - 1, left + gap * 3 + barWidth + 1, bottom + 3, 0xFF8E8E93);
    }

    private void renderPageDots(GuiGraphics graphics) {
        int centerX = phoneX + PHONE_WIDTH / 2;
        int y = dockY - 18;

        for (int i = 0; i < 3; i++) {
            int dotX = centerX - 10 + i * 8;
            int color = i == 1 ? 0xFFFFFFFF : 0x88FFFFFF;
            roundedRect(graphics, dotX, y, 4, 4, 2, color);
        }
    }

    private void renderDock(GuiGraphics graphics) {
        int dockX = phoneX + 18;
        int dockWidth = PHONE_WIDTH - 36;

        roundedRect(graphics, dockX, dockY, dockWidth, 54, 18, 0x88F5B2CF);

        int firstX = dockX + 15;
        int gap = 44;

        drawDockIcon(graphics, firstX, dockY + 8, AppKind.BROWSER);
        drawDockIcon(graphics, firstX + gap, dockY + 8, AppKind.NETWORK);
        drawDockIcon(graphics, firstX + gap * 2, dockY + 8, AppKind.STATUS);
        drawDockIcon(graphics, firstX + gap * 3, dockY + 8, AppKind.SETTINGS);
    }

    private void drawDockIcon(GuiGraphics graphics, int x, int y, AppKind kind) {
        int size = 38;

        int background = switch (kind) {
            case SETTINGS -> 0xFFE5E5EA;
            case BROWSER -> 0xFFF9F9FB;
            case NETWORK -> 0xFF34C759;
            case STATUS -> 0xFF1C1C1E;
        };

        roundedRect(graphics, x, y, size, size, 10, background);
        drawIcon(graphics, x, y, kind, size);
    }

    private void drawScaledCenteredLabel(
            GuiGraphics graphics,
            String text,
            int centerX,
            int y,
            float scale
    ) {
        graphics.pose().pushPose();
        graphics.pose().scale(scale, scale, 1.0F);

        graphics.drawCenteredString(
                font,
                text,
                Math.round(centerX / scale),
                Math.round(y / scale),
                0xFFFFFFFF
        );

        graphics.pose().popPose();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (inside(mouseX, mouseY, settingsX, appY, APP_SIZE, APP_SIZE + 18)) {
                minecraft.setScreen(new IPhoneSettingsScreen());
                return true;
            }

            if (inside(mouseX, mouseY, browserX, appY, APP_SIZE, APP_SIZE + 18)) {
                minecraft.setScreen(new IPhoneBrowserScreen());
                return true;
            }

            if (inside(mouseX, mouseY, networkX, appY, APP_SIZE, APP_SIZE + 18)) {
                minecraft.setScreen(new IPhoneWifiScreen());
                return true;
            }

            if (inside(mouseX, mouseY, statusX, appY, APP_SIZE, APP_SIZE + 18)) {
                minecraft.setScreen(new IPhoneStatusScreen());
                return true;
            }

            int dockX = phoneX + 18;
            int firstX = dockX + 15;
            int gap = 44;

            if (inside(mouseX, mouseY, firstX, dockY + 8, 38, 38)) {
                minecraft.setScreen(new IPhoneBrowserScreen());
                return true;
            }

            if (inside(mouseX, mouseY, firstX + gap, dockY + 8, 38, 38)) {
                minecraft.setScreen(new IPhoneWifiScreen());
                return true;
            }

            if (inside(mouseX, mouseY, firstX + gap * 2, dockY + 8, 38, 38)) {
                minecraft.setScreen(new IPhoneStatusScreen());
                return true;
            }

            if (inside(mouseX, mouseY, firstX + gap * 3, dockY + 8, 38, 38)) {
                minecraft.setScreen(new IPhoneSettingsScreen());
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private enum AppKind {
        SETTINGS,
        BROWSER,
        NETWORK,
        STATUS
    }
}

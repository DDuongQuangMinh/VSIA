package com.k1ngtle.vsia.phone.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public class IPhoneHomeScreen extends IPhoneScreen {
    private static final int GRID_ICON = 31;
    private static final int DOCK_ICON = 34;
    private static final int TEXT = 0xFFFFFFFF;
    private static final int LABEL_SHADOW = 0x90000000;
    private static final int SCREEN_FRAME = 0xFF0B0B0F;
    private static final int SCREEN_RADIUS = 18;
    private static final int SCREEN_INSET = 8;

    private int innerX;
    private int innerY;
    private int innerW;
    private int innerH;

    private int widgetY;
    private int gridY;
    private int searchY;
    private int dockY;

    public IPhoneHomeScreen() {
        super(Component.literal("Temporary iPhone"));
    }

    @Override
    protected void init() {
        super.init();

        innerX = phoneX + SCREEN_INSET;
        innerY = phoneY + SCREEN_INSET;
        innerW = PHONE_WIDTH - SCREEN_INSET * 2;
        innerH = PHONE_HEIGHT - SCREEN_INSET * 2;

        widgetY = phoneY + 48;
        gridY = phoneY + 157;
        searchY = phoneY + 333;
        dockY = phoneY + 360;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderPhoneShell(graphics, SCREEN_FRAME);

        roundedRect(
                graphics,
                innerX,
                innerY,
                innerW,
                innerH,
                SCREEN_RADIUS,
                0xFF6ECFE6
        );

        graphics.enableScissor(
                innerX,
                innerY,
                innerX + innerW,
                innerY + innerH
        );

        renderWallpaper(graphics);
        renderWidgets(graphics);
        renderAppGrid(graphics);
        renderSearchPill(graphics);
        renderDock(graphics);
        maskRoundedScreenCorners(graphics);
        renderStatusBar(graphics);

        graphics.disableScissor();
        renderHomeIndicator(graphics);
    }

    private void renderWallpaper(GuiGraphics graphics) {
        int x = innerX;
        int y = innerY;
        int w = innerW;
        int h = innerH;

        graphics.fill(x, y, x + w, y + h, 0xFF6ECFE6);

        graphics.fill(x, y + 64, x + w, y + h, 0xFF65E0C3);
        graphics.fill(x, y + 118, x + w, y + h, 0xFF4ED5C6);
        graphics.fill(x, y + 190, x + w, y + h, 0xFF19B9D7);
        graphics.fill(x, y + 265, x + w, y + h, 0xFF087CC7);
        graphics.fill(x, y + 330, x + w, y + h, 0xFF0157B2);

        roundedRect(graphics, x - 38, y + 84, 164, 96, 48, 0x55FFFFFF);
        roundedRect(graphics, x + 92, y - 18, 151, 126, 58, 0x553C63D8);
        roundedRect(graphics, x + 122, y + 78, 138, 184, 66, 0x44214AA8);
        roundedRect(graphics, x - 40, y + 215, 161, 119, 58, 0x445DF0BF);
        roundedRect(graphics, x + 48, y + 296, 182, 112, 56, 0x4431C8F0);
        roundedRect(graphics, x + 148, y + 243, 104, 154, 49, 0x332B5CC5);

        graphics.fill(x, y + 205, x + w, y + 207, 0x20FFFFFF);
        graphics.fill(x, y + 319, x + w, y + 321, 0x18FFFFFF);
    }

    private void maskRoundedScreenCorners(GuiGraphics graphics) {
        int radius = SCREEN_RADIUS;

        for (int i = 0; i < radius; i++) {
            int dy = radius - i;
            int inset = (int) Math.ceil(
                    radius - Math.sqrt(Math.max(0, radius * radius - dy * dy))
            );

            if (inset <= 0) {
                continue;
            }

            graphics.fill(
                    innerX,
                    innerY + i,
                    innerX + inset,
                    innerY + i + 1,
                    SCREEN_FRAME
            );

            graphics.fill(
                    innerX + innerW - inset,
                    innerY + i,
                    innerX + innerW,
                    innerY + i + 1,
                    SCREEN_FRAME
            );

            graphics.fill(
                    innerX,
                    innerY + innerH - i - 1,
                    innerX + inset,
                    innerY + innerH - i,
                    SCREEN_FRAME
            );

            graphics.fill(
                    innerX + innerW - inset,
                    innerY + innerH - i - 1,
                    innerX + innerW,
                    innerY + innerH - i,
                    SCREEN_FRAME
            );
        }
    }

    private void renderWidgets(GuiGraphics graphics) {
        int margin = 15;
        int gap = 10;
        int widgetWidth = 96;
        int widgetHeight = 92;

        int leftX = phoneX + margin;
        int rightX = leftX + widgetWidth + gap;

        drawWeatherWidget(graphics, leftX, widgetY, widgetWidth, widgetHeight);
        drawLocationWidget(graphics, rightX, widgetY, widgetWidth, widgetHeight);

        drawCenteredSmallLabel(graphics, "Weather", leftX + widgetWidth / 2, widgetY + widgetHeight + 4);
        drawCenteredSmallLabel(graphics, "Find My", rightX + widgetWidth / 2, widgetY + widgetHeight + 4);
    }

    private void drawWeatherWidget(GuiGraphics graphics, int x, int y, int w, int h) {
        roundedRect(graphics, x, y, w, h, 16, 0xD92567A6);
        roundedRect(graphics, x + 1, y + 1, w - 2, 21, 14, 0x203FFFFF);

        graphics.drawString(font, "Overworld", x + 10, y + 10, TEXT, false);
        graphics.drawString(font, "53°", x + 10, y + 27, TEXT, false);

        drawCloudSun(graphics, x + 13, y + 55);
        graphics.drawString(font, "Partly Cloudy", x + 31, y + 56, TEXT, false);
        graphics.drawString(font, "H:56°  L:50°", x + 10, y + 72, 0xFFE4F5FF, false);
    }

    private void drawLocationWidget(GuiGraphics graphics, int x, int y, int w, int h) {
        roundedRect(graphics, x, y, w, h, 16, 0xB82B6C8D);
        roundedRect(graphics, x + 2, y + 2, w - 4, h - 4, 14, 0xFFF1EFE7);

        int left = x + 4;
        int right = x + w - 4;

        graphics.fill(left, y + 5, right, y + 27, 0xFF8BD6F0);
        graphics.fill(left, y + 27, right, y + 48, 0xFFB7E6B0);
        graphics.fill(left, y + 48, right, y + h - 5, 0xFFF1EFE7);

        graphics.fill(x + 11, y + 19, x + w - 11, y + 21, 0xFFFFFFFF);
        graphics.fill(x + 19, y + 37, x + w - 8, y + 39, 0xFFE6D8A6);
        graphics.fill(x + 12, y + 55, x + w - 22, y + 57, 0xFFD3D3D3);

        drawAvatarPin(graphics, x + w - 25, y + 20);

        graphics.drawString(font, "Now", x + 10, y + 52, 0xFF7A7A7A, false);
        graphics.drawString(font, "Current Position", x + 10, y + 64, 0xFF232323, false);
        graphics.drawString(font, "VS:IA", x + 10, y + 77, 0xFF666666, false);
    }

    private void renderAppGrid(GuiGraphics graphics) {
        App[] apps = new App[]{
                new App("FaceTime", AppKind.FACETIME),
                new App("Calendar", AppKind.CALENDAR),
                new App("Photos", AppKind.PHOTOS),
                new App("Camera", AppKind.CAMERA),
                new App("Mail", AppKind.MAIL),
                new App("Notes", AppKind.NOTES),
                new App("Reminders", AppKind.REMINDERS),
                new App("Clock", AppKind.CLOCK),
                new App("News", AppKind.NEWS),
                new App("TV", AppKind.TV),
                new App("Podcasts", AppKind.PODCASTS),
                new App("App Store", AppKind.APPSTORE),
                new App("Maps", AppKind.MAPS),
                new App("Health", AppKind.HEALTH),
                new App("Wallet", AppKind.WALLET),
                new App("Settings", AppKind.SETTINGS)
        };

        int left = phoneX + 15;
        int colGap = 22;
        int rowGap = 42;

        for (int i = 0; i < apps.length; i++) {
            int col = i % 4;
            int row = i / 4;

            int x = left + col * (GRID_ICON + colGap);
            int y = gridY + row * rowGap;

            drawGridApp(graphics, x, y, apps[i]);
        }
    }

    private void drawGridApp(GuiGraphics graphics, int x, int y, App app) {
        drawAppIcon(graphics, x, y, GRID_ICON, app.kind);
        drawCenteredTinyLabel(graphics, app.label, x + GRID_ICON / 2, y + GRID_ICON + 3);
    }

    private void renderSearchPill(GuiGraphics graphics) {
        int w = 66;
        int h = 19;
        int x = phoneX + (PHONE_WIDTH - w) / 2;

        roundedRect(graphics, x, searchY, w, h, 10, 0x4DFFFFFF);
        drawSearchGlyph(graphics, x + 11, searchY + 6);
        graphics.drawString(font, "Search", x + 23, searchY + 6, 0xEFFFFFFF, false);
    }

    private void renderDock(GuiGraphics graphics) {
        int x = phoneX + 12;
        int w = PHONE_WIDTH - 24;
        int h = 56;

        roundedRect(graphics, x, dockY, w, h, 18, 0x55D7F4F7);
        roundedRect(graphics, x + 1, dockY + 1, w - 2, 18, 16, 0x22FFFFFF);

        int first = x + 13;
        int gap = 43;

        drawDockApp(graphics, first, dockY + 11, AppKind.PHONE);
        drawDockApp(graphics, first + gap, dockY + 11, AppKind.BROWSER);
        drawDockApp(graphics, first + gap * 2, dockY + 11, AppKind.MESSAGES);
        drawDockApp(graphics, first + gap * 3, dockY + 11, AppKind.MUSIC);
    }

    private void drawDockApp(GuiGraphics graphics, int x, int y, AppKind kind) {
        drawAppIcon(graphics, x, y, DOCK_ICON, kind);
    }

    private void drawAppIcon(GuiGraphics graphics, int x, int y, int size, AppKind kind) {
        int bg = switch (kind) {
            case FACETIME, PHONE, MESSAGES -> 0xFF34C759;
            case CALENDAR, NOTES, REMINDERS, HEALTH -> 0xFFF9F9FB;
            case PHOTOS -> 0xFFFFFFFF;
            case CAMERA -> 0xFFB9BEC6;
            case MAIL -> 0xFF2F8BF0;
            case CLOCK, TV, WALLET -> 0xFF1C1C1E;
            case NEWS, MUSIC -> 0xFFFF375F;
            case PODCASTS -> 0xFFB341D0;
            case APPSTORE, BROWSER -> 0xFF2196F3;
            case MAPS -> 0xFF8FD2A1;
            case SETTINGS -> 0xFFB9BEC6;
        };

        roundedRect(graphics, x, y, size, size, Math.max(7, size / 4), bg);
        roundedRect(graphics, x + 1, y + 1, size - 2, Math.max(5, size / 3), Math.max(5, size / 5), 0x28FFFFFF);

        switch (kind) {
            case FACETIME -> drawVideoGlyph(graphics, x, y, size);
            case CALENDAR -> drawCalendarGlyph(graphics, x, y, size);
            case PHOTOS -> drawPhotosGlyph(graphics, x, y, size);
            case CAMERA -> drawCameraGlyph(graphics, x, y, size);
            case MAIL -> drawMailGlyph(graphics, x, y, size);
            case NOTES -> drawNotesGlyph(graphics, x, y, size);
            case REMINDERS -> drawRemindersGlyph(graphics, x, y, size);
            case CLOCK -> drawClockGlyph(graphics, x, y, size);
            case NEWS -> drawNewsGlyph(graphics, x, y, size);
            case TV -> drawTvGlyph(graphics, x, y, size);
            case PODCASTS -> drawPodcastsGlyph(graphics, x, y, size);
            case APPSTORE -> drawAppStoreGlyph(graphics, x, y, size);
            case MAPS -> drawMapsGlyph(graphics, x, y, size);
            case HEALTH -> drawHealthGlyph(graphics, x, y, size);
            case WALLET -> drawWalletGlyph(graphics, x, y, size);
            case SETTINGS -> drawSettingsGlyph(graphics, x, y, size);
            case PHONE -> drawPhoneGlyph(graphics, x, y, size);
            case BROWSER -> drawBrowserGlyph(graphics, x, y, size);
            case MESSAGES -> drawMessagesGlyph(graphics, x, y, size);
            case MUSIC -> drawMusicGlyph(graphics, x, y, size);
        }
    }

    private void drawVideoGlyph(GuiGraphics g, int x, int y, int s) {
        g.fill(x + 7, y + 9, x + s - 10, y + s - 9, TEXT);
        g.fill(x + s - 10, y + 12, x + s - 5, y + s - 12, TEXT);
    }

    private void drawCalendarGlyph(GuiGraphics g, int x, int y, int s) {
        g.drawCenteredString(font, "1", x + s / 2, y + 13, 0xFF222222);
        g.drawCenteredString(font, "Tue", x + s / 2, y + 4, 0xFFFF453A);
    }

    private void drawPhotosGlyph(GuiGraphics g, int x, int y, int s) {
        int cx = x + s / 2;
        int cy = y + s / 2;
        roundedRect(g, cx - 3, cy - 12, 6, 12, 3, 0xFFFF453A);
        roundedRect(g, cx, cy - 4, 12, 7, 3, 0xFFFFD60A);
        roundedRect(g, cx - 3, cy, 7, 12, 3, 0xFF30D158);
        roundedRect(g, cx - 12, cy - 3, 12, 7, 3, 0xFF0A84FF);
        roundedRect(g, cx - 8, cy - 10, 8, 8, 4, 0xFFBF5AF2);
    }

    private void drawCameraGlyph(GuiGraphics g, int x, int y, int s) {
        roundedRect(g, x + 6, y + 9, s - 12, s - 17, 5, 0xFF404348);
        roundedRect(g, x + 11, y + 12, s - 22, s - 22, (s - 22) / 2, 0xFF78B7FF);
        roundedRect(g, x + 14, y + 15, s - 28, s - 28, (s - 28) / 2, 0xFF14213D);
    }

    private void drawMailGlyph(GuiGraphics g, int x, int y, int s) {
        g.fill(x + 6, y + 9, x + s - 6, y + s - 8, 0xFFFFFFFF);
        g.hLine(x + 8, x + s / 2, y + 11, 0xFFBBD8F9);
        g.hLine(x + s / 2, x + s - 8, y + 11, 0xFFBBD8F9);
        g.hLine(x + 9, x + s / 2, y + s - 10, 0xFFBBD8F9);
        g.hLine(x + s / 2, x + s - 9, y + s - 10, 0xFFBBD8F9);
    }

    private void drawNotesGlyph(GuiGraphics g, int x, int y, int s) {
        g.fill(x + 5, y + 8, x + s - 5, y + s - 6, 0xFFFFFFFF);
        g.fill(x + 5, y + 8, x + s - 5, y + 13, 0xFFFFD60A);
        for (int i = 0; i < 3; i++) {
            g.fill(x + 8, y + 17 + i * 4, x + s - 8, y + 18 + i * 4, 0xFFD1D1D6);
        }
    }

    private void drawRemindersGlyph(GuiGraphics g, int x, int y, int s) {
        int[] colors = {0xFFFF453A, 0xFFFFD60A, 0xFF30D158};
        for (int i = 0; i < 3; i++) {
            roundedRect(g, x + 6, y + 8 + i * 7, 4, 4, 2, colors[i]);
            g.fill(x + 13, y + 9 + i * 7, x + s - 6, y + 10 + i * 7, 0xFFB0B0B5);
        }
    }

    private void drawClockGlyph(GuiGraphics g, int x, int y, int s) {
        roundedRect(g, x + 4, y + 4, s - 8, s - 8, (s - 8) / 2, 0xFFF5F5F7);
        int cx = x + s / 2;
        int cy = y + s / 2;
        g.fill(cx, cy - 7, cx + 1, cy + 1, 0xFF1C1C1E);
        g.fill(cx, cy, cx + 6, cy + 1, 0xFF1C1C1E);
        g.fill(cx - 1, cy - 1, cx + 1, cy + 1, 0xFFFF453A);
    }

    private void drawNewsGlyph(GuiGraphics g, int x, int y, int s) {
        g.fill(x + 7, y + 5, x + 12, y + s - 5, 0xFFFFFFFF);
        g.fill(x + 12, y + 5, x + s - 6, y + 10, 0xFFFFFFFF);
        g.fill(x + s - 11, y + 10, x + s - 6, y + s - 5, 0xFFFFFFFF);
        g.fill(x + 12, y + s - 10, x + s - 11, y + s - 5, 0xFFFFFFFF);
    }

    private void drawTvGlyph(GuiGraphics g, int x, int y, int s) {
        g.drawCenteredString(font, "tv", x + s / 2, y + 11, TEXT);
    }

    private void drawPodcastsGlyph(GuiGraphics g, int x, int y, int s) {
        int cx = x + s / 2;
        roundedRect(g, cx - 5, y + 6, 10, 10, 5, 0xFFFFFFFF);
        roundedRect(g, cx - 7, y + 15, 14, 5, 2, 0xAAFFFFFF);
        roundedRect(g, cx - 3, y + 18, 6, 9, 3, 0xFFFFFFFF);
    }

    private void drawAppStoreGlyph(GuiGraphics g, int x, int y, int s) {
        int cx = x + s / 2;
        g.fill(cx - 1, y + 6, cx + 1, y + s - 6, TEXT);
        g.fill(x + 7, y + s - 9, x + s - 7, y + s - 7, TEXT);
        g.fill(x + 9, y + 7, x + 12, y + 10, TEXT);
        g.fill(x + s - 12, y + 7, x + s - 9, y + 10, TEXT);
    }

    private void drawMapsGlyph(GuiGraphics g, int x, int y, int s) {
        g.fill(x + 4, y + 4, x + 10, y + s - 4, 0xFF6DD66A);
        g.fill(x + 10, y + 4, x + 18, y + s - 4, 0xFFDFE6EE);
        g.fill(x + 18, y + 4, x + s - 4, y + s - 4, 0xFF6AB4EF);
        g.fill(x + 4, y + 14, x + s - 4, y + 16, 0xFFFFFFFF);
        roundedRect(g, x + 16, y + 10, 8, 8, 4, 0xFF0A84FF);
    }

    private void drawHealthGlyph(GuiGraphics g, int x, int y, int s) {
        roundedRect(g, x + 7, y + 8, 8, 8, 4, 0xFFFF375F);
        roundedRect(g, x + 14, y + 8, 8, 8, 4, 0xFFFF375F);
        g.fill(x + 9, y + 13, x + 21, y + 18, 0xFFFF375F);
        g.fill(x + 12, y + 18, x + 18, y + 22, 0xFFFF375F);
    }

    private void drawWalletGlyph(GuiGraphics g, int x, int y, int s) {
        roundedRect(g, x + 5, y + 8, s - 10, s - 14, 4, 0xFF3A3A3C);
        g.fill(x + 8, y + 10, x + s - 8, y + 13, 0xFFFF453A);
        g.fill(x + 8, y + 14, x + s - 8, y + 17, 0xFFFFD60A);
        g.fill(x + 8, y + 18, x + s - 8, y + 21, 0xFF30D158);
    }

    private void drawSettingsGlyph(GuiGraphics g, int x, int y, int s) {
        int cx = x + s / 2;
        int cy = y + s / 2;
        roundedRect(g, cx - 9, cy - 9, 18, 18, 9, 0xFF6A6A70);
        roundedRect(g, cx - 4, cy - 4, 8, 8, 4, 0xFFD9D9DD);
        roundedRect(g, cx - 2, cy - 2, 4, 4, 2, 0xFF6A6A70);
        g.fill(cx - 2, cy - 13, cx + 2, cy - 9, 0xFF6A6A70);
        g.fill(cx - 2, cy + 9, cx + 2, cy + 13, 0xFF6A6A70);
        g.fill(cx - 13, cy - 2, cx - 9, cy + 2, 0xFF6A6A70);
        g.fill(cx + 9, cy - 2, cx + 13, cy + 2, 0xFF6A6A70);
    }

    private void drawPhoneGlyph(GuiGraphics g, int x, int y, int s) {
        g.fill(x + 9, y + 7, x + 13, y + 14, TEXT);
        g.fill(x + 11, y + 12, x + 18, y + 20, TEXT);
        g.fill(x + 17, y + 18, x + 24, y + 23, TEXT);
        roundedRect(g, x + 7, y + 5, 7, 7, 3, TEXT);
        roundedRect(g, x + 20, y + 20, 7, 7, 3, TEXT);
    }

    private void drawBrowserGlyph(GuiGraphics g, int x, int y, int s) {
        int cx = x + s / 2;
        int cy = y + s / 2;
        roundedRect(g, x + 3, y + 3, s - 6, s - 6, (s - 6) / 2, 0xFFFFFFFF);
        roundedRect(g, x + 6, y + 6, s - 12, s - 12, (s - 12) / 2, 0xFF0A84FF);
        g.fill(cx - 1, y + 7, cx + 1, cy + 2, 0xFFFF453A);
        g.fill(cx, cy, cx + 2, y + s - 7, TEXT);
    }

    private void drawMessagesGlyph(GuiGraphics g, int x, int y, int s) {
        roundedRect(g, x + 6, y + 7, s - 12, s - 16, 10, 0xFFFFFFFF);
        g.fill(x + 10, y + s - 12, x + 14, y + s - 6, 0xFFFFFFFF);
    }

    private void drawMusicGlyph(GuiGraphics g, int x, int y, int s) {
        g.fill(x + 16, y + 7, x + 19, y + 23, TEXT);
        g.fill(x + 18, y + 7, x + 25, y + 10, TEXT);
        roundedRect(g, x + 9, y + 21, 8, 7, 3, TEXT);
        roundedRect(g, x + 18, y + 19, 8, 7, 3, TEXT);
    }

    private void drawCloudSun(GuiGraphics g, int x, int y) {
        roundedRect(g, x + 1, y, 7, 7, 3, 0xFFFFD60A);
        roundedRect(g, x, y + 5, 14, 6, 3, 0xFFFFFFFF);
    }

    private void drawAvatarPin(GuiGraphics g, int x, int y) {
        roundedRect(g, x - 8, y - 8, 16, 16, 8, 0xFFFFFFFF);
        roundedRect(g, x - 6, y - 6, 12, 12, 6, 0xFFC1A3FF);
        roundedRect(g, x - 2, y - 4, 4, 4, 2, 0xFF303030);
        g.fill(x - 4, y + 1, x + 4, y + 5, 0xFF303030);
    }

    private void drawSearchGlyph(GuiGraphics g, int x, int y) {
        roundedRect(g, x, y, 6, 6, 3, 0xCCFFFFFF);
        roundedRect(g, x + 1, y + 1, 4, 4, 2, 0x6690DCEB);
        g.fill(x + 5, y + 5, x + 8, y + 7, 0xCCFFFFFF);
    }

    private void drawCenteredSmallLabel(GuiGraphics graphics, String text, int centerX, int y) {
        drawScaledCenteredLabel(graphics, text, centerX, y, 0.70F);
    }

    private void drawCenteredTinyLabel(GuiGraphics graphics, String text, int centerX, int y) {
        drawScaledCenteredLabel(graphics, text, centerX, y, 0.56F);
    }

    private void drawScaledCenteredLabel(GuiGraphics graphics, String text, int centerX, int y, float scale) {
        graphics.pose().pushPose();
        graphics.pose().scale(scale, scale, 1.0F);

        int scaledX = Math.round(centerX / scale);
        int scaledY = Math.round(y / scale);

        graphics.drawCenteredString(font, text, scaledX + 1, scaledY + 1, LABEL_SHADOW);
        graphics.drawCenteredString(font, text, scaledX, scaledY, TEXT);

        graphics.pose().popPose();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int settingsCol = 3;
            int settingsRow = 3;
            int left = phoneX + 15;
            int colGap = 22;
            int rowGap = 42;
            int settingsX = left + settingsCol * (GRID_ICON + colGap);
            int settingsY = gridY + settingsRow * rowGap;

            if (inside(mouseX, mouseY, settingsX, settingsY, GRID_ICON, GRID_ICON + 10)) {
                minecraft.setScreen(new IPhoneSettingsScreen());
                return true;
            }

            int dockX = phoneX + 12;
            int first = dockX + 13;
            int gap = 43;

            if (inside(mouseX, mouseY, first + gap, dockY + 11, DOCK_ICON, DOCK_ICON)) {
                minecraft.setScreen(new IPhoneBrowserScreen());
                return true;
            }

            if (inside(mouseX, mouseY, first + gap * 2, dockY + 11, DOCK_ICON, DOCK_ICON)) {
                minecraft.setScreen(new IPhoneWifiScreen());
                return true;
            }

            if (inside(mouseX, mouseY, first + gap * 3, dockY + 11, DOCK_ICON, DOCK_ICON)) {
                minecraft.setScreen(new IPhoneStatusScreen());
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private record App(String label, AppKind kind) {
    }

    private enum AppKind {
        FACETIME,
        CALENDAR,
        PHOTOS,
        CAMERA,
        MAIL,
        NOTES,
        REMINDERS,
        CLOCK,
        NEWS,
        TV,
        PODCASTS,
        APPSTORE,
        MAPS,
        HEALTH,
        WALLET,
        SETTINGS,
        PHONE,
        BROWSER,
        MESSAGES,
        MUSIC
    }
}

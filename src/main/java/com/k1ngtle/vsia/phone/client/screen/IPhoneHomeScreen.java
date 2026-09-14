package com.k1ngtle.vsia.phone.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public class IPhoneHomeScreen extends IPhoneScreen {
    private static final int GRID_ICON = 34;
    private static final int DOCK_ICON = 36;
    private static final int TEXT = 0xFFFFFFFF;
    private static final int LABEL_SHADOW = 0xB0000000;

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
        innerX = displayX();
        innerY = displayY();
        innerW = displayWidth();
        innerH = displayHeight();
        widgetY = phoneY + 48;
        gridY = phoneY + 157;
        searchY = phoneY + 333;
        dockY = phoneY + 360;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderPhoneShell(graphics, 0xFF0A0A0A);

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

        graphics.disableScissor();

        maskDisplayCorners(graphics);
        renderStatusBar(graphics);
        renderHomeIndicator(graphics);
    }

    private void renderWallpaper(GuiGraphics graphics) {
        graphics.fill(innerX, innerY, innerX + innerW, innerY + innerH, 0xFF65CDE2);
        graphics.fill(innerX, innerY + 88, innerX + innerW, innerY + innerH, 0xFF55DAC6);
        graphics.fill(innerX, innerY + 194, innerX + innerW, innerY + innerH, 0xFF16B6D4);
        graphics.fill(innerX, innerY + 274, innerX + innerW, innerY + innerH, 0xFF087EC9);
        graphics.fill(innerX, innerY + 348, innerX + innerW, innerY + innerH, 0xFF075CB9);

        roundedRect(graphics, innerX - 34, innerY + 92, 156, 102, 52, 0x4CFFFFFF);
        roundedRect(graphics, innerX + 92, innerY - 24, 150, 142, 62, 0x453357D0);
        roundedRect(graphics, innerX + 126, innerY + 86, 135, 174, 62, 0x382A4EAC);
        roundedRect(graphics, innerX - 45, innerY + 231, 165, 112, 58, 0x394EF0C2);
        roundedRect(graphics, innerX + 50, innerY + 306, 185, 108, 54, 0x3431C9F3);
    }

    private void renderWidgets(GuiGraphics graphics) {
        int leftX = phoneX + 15;
        int rightX = phoneX + 125;
        drawWeatherWidget(graphics, leftX, widgetY, 96, 92);
        drawFindMyWidget(graphics, rightX, widgetY, 96, 92);
        drawLabel(graphics, "Weather", leftX + 48, widgetY + 96, 0.70F);
        drawLabel(graphics, "Find My", rightX + 48, widgetY + 96, 0.70F);
    }

    private void drawWeatherWidget(GuiGraphics g, int x, int y, int w, int h) {
        roundedRect(g, x - 2, y - 2, w + 4, h + 4, 18, 0xAA164F7A);
        roundedRect(g, x, y, w, h, 16, 0xE8246BA6);
        roundedRect(g, x + 2, y + 2, w - 4, 18, 12, 0x2FFFFFFF);
        drawUiText(g, "Overworld", x + 10, y + 9, TEXT);
        drawUiText(g, "12°", x + 10, y + 27, TEXT);
        roundedRect(g, x + 12, y + 58, 15, 7, 3, 0xFFFFFFFF);
        roundedRect(g, x + 17, y + 54, 7, 7, 3, 0xFFFFD60A);
        drawUiText(g, "Partly Cloudy", x + 31, y + 56, TEXT);
        drawUiText(g, "H:13°  L:10°", x + 10, y + 72, 0xFFE3F5FF);
    }

    private void drawFindMyWidget(GuiGraphics g, int x, int y, int w, int h) {
        roundedRect(g, x - 2, y - 2, w + 4, h + 4, 18, 0xAA174C68);
        roundedRect(g, x, y, w, h, 16, 0xFFF3F1E9);
        g.fill(x + 3, y + 3, x + w - 3, y + 29, 0xFF88D7EF);
        g.fill(x + 3, y + 29, x + w - 3, y + 49, 0xFFB7E6B0);
        g.fill(x + 13, y + 19, x + w - 13, y + 21, 0xFFFFFFFF);
        g.fill(x + 20, y + 38, x + w - 10, y + 40, 0xFFE5D69F);
        roundedRect(g, x + w - 31, y + 12, 18, 18, 9, 0xFFFFFFFF);
        roundedRect(g, x + w - 28, y + 15, 12, 12, 6, 0xFFB99CF7);
        g.fill(x + w - 24, y + 18, x + w - 20, y + 23, 0xFF303030);
        drawUiText(g, "Now", x + 9, y + 52, 0xFF777777);
        drawUiText(g, "Current Position", x + 9, y + 64, 0xFF222222);
        drawUiText(g, "VS:IA", x + 9, y + 77, 0xFF666666);
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

        int stepX = 52;
        int stepY = 42;
        int columns = 4;
        int totalWidth = GRID_ICON + stepX * (columns - 1);
        int left = phoneX + (PHONE_WIDTH - totalWidth) / 2;

        for (int i = 0; i < apps.length; i++) {
            int x = left + (i % 4) * stepX;
            int y = gridY + (i / 4) * stepY;
            drawAppIcon(graphics, x, y, GRID_ICON, apps[i].kind());
            drawLabel(graphics, apps[i].label(), x + GRID_ICON / 2, y + GRID_ICON + 2, 0.54F);
        }
    }

    private void renderSearchPill(GuiGraphics graphics) {
        int w = 68;
        int h = 19;
        int x = phoneX + (PHONE_WIDTH - w) / 2;
        roundedRect(graphics, x, searchY, w, h, 10, 0x4AFFFFFF);
        roundedRect(graphics, x + 10, searchY + 6, 6, 6, 3, 0xDDFFFFFF);
        graphics.fill(x + 15, searchY + 11, x + 18, searchY + 13, 0xDDFFFFFF);
        drawUiText(graphics, "Search", x + 23, searchY + 6, 0xF0FFFFFF);
    }

    private void renderDock(GuiGraphics graphics) {
        int x = phoneX + 12;
        int w = PHONE_WIDTH - 24;
        roundedRect(graphics, x, dockY, w, 56, 18, 0x66D9F4F8);
        roundedRect(graphics, x + 1, dockY + 1, w - 2, 16, 14, 0x22FFFFFF);

        int gap = 43;
        int iconSpan = DOCK_ICON + gap * 3;
        int first = x + (w - iconSpan) / 2;
        drawAppIcon(graphics, first, dockY + 10, DOCK_ICON, AppKind.PHONE);
        drawAppIcon(graphics, first + gap, dockY + 10, DOCK_ICON, AppKind.BROWSER);
        drawAppIcon(graphics, first + gap * 2, dockY + 10, DOCK_ICON, AppKind.MESSAGES);
        drawAppIcon(graphics, first + gap * 3, dockY + 10, DOCK_ICON, AppKind.MUSIC);
    }

    private void drawAppIcon(GuiGraphics g, int x, int y, int size, AppKind kind) {
        int bg = switch (kind) {
            case FACETIME, PHONE, MESSAGES -> 0xFF32D05A;
            case CALENDAR, PHOTOS, NOTES, REMINDERS, HEALTH -> 0xFFF8F8FA;
            case CAMERA, SETTINGS -> 0xFFB9BEC7;
            case MAIL, APPSTORE, BROWSER -> 0xFF168AF2;
            case CLOCK, TV, WALLET -> 0xFF1B1B1D;
            case NEWS, MUSIC -> 0xFFFF3B61;
            case PODCASTS -> 0xFFAF42D4;
            case MAPS -> 0xFF8FD7A5;
        };

        roundedRect(g, x, y, size, size, 9, bg);
        roundedRect(g, x + 2, y + 2, size - 4, 8, 6, 0x30FFFFFF);

        int cx = x + size / 2;
        int cy = y + size / 2;

        switch (kind) {
            case PHONE -> {
                roundedRect(g, x + 8, y + 20, 7, 6, 3, TEXT);
                g.fill(x + 10, y + 15, x + 17, y + 23, TEXT);
                roundedRect(g, x + 16, y + 9, 9, 7, 4, TEXT);
                g.fill(x + 15, y + 13, x + 23, y + 18, TEXT);
            }
            case MESSAGES -> {
                roundedRect(g, x + 6, y + 7, size - 12, size - 15, 10, TEXT);
                g.fill(x + 11, y + size - 12, x + 15, y + size - 6, TEXT);
            }
            case BROWSER -> {
                roundedRect(g, x + 3, y + 3, size - 6, size - 6, 15, 0xFFFFFFFF);
                roundedRect(g, x + 6, y + 6, size - 12, size - 12, 13, 0xFF0A84FF);
                g.fill(cx - 1, y + 7, cx + 1, cy + 2, 0xFFFF453A);
                g.fill(cx, cy, cx + 2, y + size - 7, 0xFFFFFFFF);
            }
            case SETTINGS -> {
                roundedRect(g, cx - 10, cy - 10, 20, 20, 10, 0xFF66666D);
                roundedRect(g, cx - 5, cy - 5, 10, 10, 5, 0xFFE2E2E6);
                roundedRect(g, cx - 2, cy - 2, 4, 4, 2, 0xFF66666D);
            }
            case CALENDAR -> {
                drawUiCentered(g, "Mon", cx, y + 5, 0xFFFF3B30);
                drawUiCentered(g, "14", cx, y + 17, 0xFF111111);
            }
            case PHOTOS -> {
                roundedRect(g, cx - 3, cy - 12, 6, 12, 3, 0xFFFF453A);
                roundedRect(g, cx, cy - 4, 12, 7, 3, 0xFFFFD60A);
                roundedRect(g, cx - 3, cy, 7, 12, 3, 0xFF30D158);
                roundedRect(g, cx - 12, cy - 3, 12, 7, 3, 0xFF0A84FF);
                roundedRect(g, cx - 8, cy - 10, 8, 8, 4, 0xFFBF5AF2);
            }
            case CAMERA -> {
                roundedRect(g, x + 7, y + 9, size - 14, size - 17, 5, 0xFF404348);
                roundedRect(g, cx - 6, cy - 6, 12, 12, 6, 0xFF79B8FF);
                roundedRect(g, cx - 3, cy - 3, 6, 6, 3, 0xFF17213B);
            }
            case MAIL -> {
                roundedRect(g, x + 6, y + 8, size - 12, size - 15, 4, 0xFFFFFFFF);
                g.fill(x + 8, y + 10, cx, cy, 0xFFB9D8F9);
                g.fill(cx, cy, x + size - 8, y + 10, 0xFFB9D8F9);
            }
            case NOTES -> {
                roundedRect(g, x + 5, y + 7, size - 10, size - 12, 4, 0xFFFFFFFF);
                g.fill(x + 5, y + 7, x + size - 5, y + 12, 0xFFFFD60A);
                g.fill(x + 9, y + 17, x + size - 9, y + 19, 0xFFC7C7CC);
                g.fill(x + 9, y + 23, x + size - 9, y + 25, 0xFFC7C7CC);
            }
            case REMINDERS -> {
                for (int i = 0; i < 3; i++) {
                    int yy = y + 10 + i * 7;
                    roundedRect(g, x + 7, yy, 3, 3, 2, i == 0 ? 0xFFFF453A : i == 1 ? 0xFFFFD60A : 0xFF30D158);
                    g.fill(x + 13, yy + 1, x + size - 7, yy + 2, 0xFF9A9A9E);
                }
            }
            case CLOCK -> {
                roundedRect(g, x + 5, y + 5, size - 10, size - 10, 12, 0xFFF4F4F5);
                g.fill(cx, y + 9, cx + 1, cy + 1, 0xFF111111);
                g.fill(cx, cy, cx + 7, cy + 1, 0xFFFF453A);
            }
            case NEWS -> {
                roundedRect(g, x + 7, y + 7, size - 14, size - 14, 3, 0xFFFFFFFF);
                g.fill(x + 11, y + 11, x + size - 11, y + size - 11, 0xFFFF375F);
            }
            case TV -> drawUiCentered(g, "tv", cx, cy - 3, 0xFFFFFFFF);
            case PODCASTS -> {
                roundedRect(g, cx - 3, cy - 3, 6, 6, 3, 0xFFFFFFFF);
                roundedRect(g, cx - 7, cy - 7, 14, 14, 7, 0x66FFFFFF);
                roundedRect(g, cx - 2, cy + 3, 4, 10, 2, 0xFFFFFFFF);
            }
            case APPSTORE -> {
                g.fill(cx - 1, y + 10, cx + 1, y + 24, 0xFFFFFFFF);
                g.fill(x + 10, y + 21, x + size - 10, y + 23, 0xFFFFFFFF);
                g.fill(x + 11, y + 21, x + 18, y + 10, 0xFFFFFFFF);
                g.fill(x + size - 18, y + 10, x + size - 11, y + 21, 0xFFFFFFFF);
            }
            case MAPS -> {
                roundedRect(g, x + 5, y + 5, size - 10, size - 10, 4, 0xFFFFFFFF);
                g.fill(x + 6, y + 5, x + 13, y + size - 5, 0xFF74D95D);
                g.fill(x + 13, y + 5, x + 20, y + size - 5, 0xFFF5F7FA);
                g.fill(x + 20, y + 5, x + size - 6, y + size - 5, 0xFF6AB6F3);
                g.fill(cx - 1, y + 8, cx + 1, y + size - 9, 0xFFFFFFFF);
                roundedRect(g, cx - 3, cy - 4, 7, 9, 3, 0xFF0A84FF);
                roundedRect(g, cx - 2, cy - 3, 5, 5, 2, 0xFFFFFFFF);
                roundedRect(g, cx - 1, cy + 3, 3, 5, 1, 0xFF0A84FF);
            }
            case HEALTH -> {
                roundedRect(g, cx - 8, cy - 8, 8, 8, 4, 0xFFFF375F);
                roundedRect(g, cx, cy - 8, 8, 8, 4, 0xFFFF375F);
                g.fill(cx - 7, cy - 4, cx + 7, cy + 5, 0xFFFF375F);
                g.fill(cx - 5, cy + 5, cx + 5, cy + 11, 0xFFFF375F);
            }
            case WALLET -> {
                roundedRect(g, x + 6, y + 8, size - 12, size - 15, 4, 0xFF3A3A3C);
                g.fill(x + 9, y + 11, x + size - 9, y + 14, 0xFFFF453A);
                g.fill(x + 9, y + 15, x + size - 9, y + 18, 0xFFFFD60A);
                g.fill(x + 9, y + 19, x + size - 9, y + 22, 0xFF30D158);
            }
            case FACETIME -> {
                roundedRect(g, x + 8, y + 12, 16, 10, 4, 0xFFFFFFFF);
                g.fill(x + 20, y + 14, x + 27, y + 17, 0xFFFFFFFF);
                g.fill(x + 20, y + 17, x + 25, y + 21, 0xFFFFFFFF);
            }
            case MUSIC -> {
                g.fill(cx + 11, y + 8, cx + 15, y + 24, 0xFFFFFFFF);
                g.fill(cx - 2, y + 11, x + size - 9, y + 15, 0xFFFFFFFF);
                roundedRect(g, cx - 11, y + 21, 9, 8, 4, 0xFFFFFFFF);
                roundedRect(g, cx + 4, y + 18, 10, 10, 5, 0xFFFFFFFF);
            }
        }
    }

    private void drawLabel(GuiGraphics graphics, String text, int centerX, int y, float scale) {
        graphics.pose().pushPose();
        graphics.pose().scale(scale, scale, 1.0F);
        int sx = Math.round(centerX / scale);
        int sy = Math.round(y / scale);
        drawUiCentered(graphics, text, sx + 1, sy + 1, LABEL_SHADOW);
        drawUiCentered(graphics, text, sx, sy, TEXT);
        graphics.pose().popPose();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int stepX = 52;
            int stepY = 42;
            int columns = 4;
            int totalWidth = GRID_ICON + stepX * (columns - 1);
            int left = phoneX + (PHONE_WIDTH - totalWidth) / 2;

            int faceTimeX = left;
            int faceTimeY = gridY;
            if (inside(mouseX, mouseY, faceTimeX, faceTimeY, GRID_ICON, GRID_ICON + 10)) {
                minecraft.setScreen(new IPhoneFaceTimeScreen());
                return true;
            }

            int calendarX = left + stepX;
            int calendarY = gridY;
            if (inside(mouseX, mouseY, calendarX, calendarY, GRID_ICON, GRID_ICON + 10)) {
                minecraft.setScreen(new IPhoneCalendarScreen());
                return true;
            }

            int photosX = left + 2 * stepX;
            int photosY = gridY;
            if (inside(mouseX, mouseY, photosX, photosY, GRID_ICON, GRID_ICON + 10)) {
                minecraft.setScreen(new IPhonePhotosScreen());
                return true;
            }

            int cameraX = left + 3 * stepX;
            int cameraY = gridY;
            if (inside(mouseX, mouseY, cameraX, cameraY, GRID_ICON, GRID_ICON + 10)) {
                minecraft.setScreen(new IPhoneCameraScreen());
                return true;
            }

            int mailX = left;
            int mailY = gridY + stepY;
            if (inside(mouseX, mouseY, mailX, mailY, GRID_ICON, GRID_ICON + 10)) {
                minecraft.setScreen(new IPhoneMailScreen());
                return true;
            }

            int notesX = left + stepX;
            int notesY = gridY + stepY;
            if (inside(mouseX, mouseY, notesX, notesY, GRID_ICON, GRID_ICON + 10)) {
                minecraft.setScreen(new IPhoneNotesScreen());
                return true;
            }

            int remindersX = left + 2 * stepX;
            int remindersY = gridY + stepY;
            if (inside(mouseX, mouseY, remindersX, remindersY, GRID_ICON, GRID_ICON + 10)) {
                minecraft.setScreen(new IPhoneRemindersScreen());
                return true;
            }

            int clockX = left + 3 * stepX;
            int clockY = gridY + stepY;
            if (inside(mouseX, mouseY, clockX, clockY, GRID_ICON, GRID_ICON + 10)) {
                minecraft.setScreen(new IPhoneClockScreen());
                return true;
            }

            int newsX = left;
            int newsY = gridY + 2 * stepY;
            if (inside(mouseX, mouseY, newsX, newsY, GRID_ICON, GRID_ICON + 10)) {
                minecraft.setScreen(new IPhoneNewsScreen());
                return true;
            }

            int tvX = left + stepX;
            int tvY = gridY + 2 * stepY;
            if (inside(mouseX, mouseY, tvX, tvY, GRID_ICON, GRID_ICON + 10)) {
                minecraft.setScreen(new IPhoneTVScreen());
                return true;
            }

            int podcastsX = left + 2 * stepX;
            int podcastsY = gridY + 2 * stepY;
            if (inside(mouseX, mouseY, podcastsX, podcastsY, GRID_ICON, GRID_ICON + 10)) {
                minecraft.setScreen(new IPhonePodcastsScreen());
                return true;
            }

            int appStoreX = left + 3 * stepX;
            int appStoreY = gridY + 2 * stepY;
            if (inside(mouseX, mouseY, appStoreX, appStoreY, GRID_ICON, GRID_ICON + 10)) {
                minecraft.setScreen(new IPhoneAppStoreScreen());
                return true;
            }

            int mapsX = left;
            int mapsY = gridY + 3 * stepY;
            if (inside(mouseX, mouseY, mapsX, mapsY, GRID_ICON, GRID_ICON + 10)) {
                minecraft.setScreen(new IPhoneMapsScreen());
                return true;
            }

            int healthX = left + stepX;
            int healthY = gridY + 3 * stepY;
            if (inside(mouseX, mouseY, healthX, healthY, GRID_ICON, GRID_ICON + 10)) {
                minecraft.setScreen(new IPhoneHealthScreen());
                return true;
            }

            int walletX = left + 2 * stepX;
            int walletY = gridY + 3 * stepY;
            if (inside(mouseX, mouseY, walletX, walletY, GRID_ICON, GRID_ICON + 10)) {
                minecraft.setScreen(new IPhoneWalletScreen());
                return true;
            }

            int settingsX = left + 3 * stepX;
            int settingsY = gridY + 3 * stepY;
            if (inside(mouseX, mouseY, settingsX, settingsY, GRID_ICON, GRID_ICON + 10)) {
                minecraft.setScreen(new IPhoneSettingsScreen());
                return true;
            }

            int dockX = phoneX + 12;
            int dockW = PHONE_WIDTH - 24;
            int gap = 43;
            int iconSpan = DOCK_ICON + gap * 3;
            int first = dockX + (dockW - iconSpan) / 2;

            if (inside(mouseX, mouseY, first, dockY + 10, DOCK_ICON, DOCK_ICON)) {
                minecraft.setScreen(new IPhoneCellularScreen());
                return true;
            }

            if (inside(mouseX, mouseY, first + gap, dockY + 10, DOCK_ICON, DOCK_ICON)) {
                minecraft.setScreen(new IPhoneBrowserScreen());
                return true;
            }

            if (inside(mouseX, mouseY, first + gap * 2, dockY + 10, DOCK_ICON, DOCK_ICON)) {
                minecraft.setScreen(new IPhoneMessagesScreen());
                return true;
            }

            if (inside(mouseX, mouseY, first + gap * 3, dockY + 10, DOCK_ICON, DOCK_ICON)) {
                minecraft.setScreen(new IPhoneMusicScreen());
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

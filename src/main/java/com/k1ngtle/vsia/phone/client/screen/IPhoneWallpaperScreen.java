package com.k1ngtle.vsia.phone.client.screen;

import com.k1ngtle.vsia.phone.client.PhoneSystemSettings;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public final class IPhoneWallpaperScreen extends IPhoneScreen {
    private static final int BG = 0xFF1C1C1E;
    private static final int CARD = 0xFF2C2C2E;
    private static final int TEXT = 0xFFFFFFFF;
    private static final int MUTED = 0xFFAEAEB2;
    private static final int BLUE = 0xFF0A84FF;

    private int contentX;
    private int contentWidth;
    private int firstY;

    public IPhoneWallpaperScreen() {
        super(Component.literal("Wallpaper"));
    }

    @Override
    protected void init() {
        super.init();
        contentX = phoneX + 14;
        contentWidth = PHONE_WIDTH - 28;
        firstY = phoneY + 86;
    }

    @Override
    public void render(
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        renderPhoneShell(graphics, BG);
        renderStatusBar(graphics);
        renderHeader(graphics, "Settings", "Wallpaper");

        beginPhoneClip(graphics, 68);

        drawWallpaperChoice(
                graphics,
                firstY,
                PhoneSystemSettings.Wallpaper.AERO
        );

        drawWallpaperChoice(
                graphics,
                firstY + 74,
                PhoneSystemSettings.Wallpaper.DUSK
        );

        drawWallpaperChoice(
                graphics,
                firstY + 148,
                PhoneSystemSettings.Wallpaper.GRAPHITE
        );

        endPhoneClip(graphics);
        renderHomeIndicator(graphics);
    }

    private void drawWallpaperChoice(
            GuiGraphics graphics,
            int y,
            PhoneSystemSettings.Wallpaper wallpaper
    ) {
        boolean selected =
                PhoneSystemSettings.wallpaper()
                        == wallpaper;

        roundedRect(
                graphics,
                contentX,
                y,
                contentWidth,
                58,
                14,
                CARD
        );

        int previewX = contentX + 10;
        int previewY = y + 8;

        drawPreview(
                graphics,
                previewX,
                previewY,
                wallpaper
        );

        drawUiText(
                graphics,
                wallpaper.displayName(),
                contentX + 64,
                y + 13,
                TEXT
        );

        drawUiText(
                graphics,
                selected
                        ? "Current"
                        : "Tap to Apply",
                contentX + 64,
                y + 32,
                selected
                        ? BLUE
                        : MUTED
        );

        if (selected) {
            drawUiText(
                    graphics,
                    "✓",
                    contentX + contentWidth - 22,
                    y + 23,
                    BLUE
            );
        }
    }

    private void drawPreview(
            GuiGraphics graphics,
            int x,
            int y,
            PhoneSystemSettings.Wallpaper wallpaper
    ) {
        int c1;
        int c2;
        int c3;

        switch (wallpaper) {
            case AERO -> {
                c1 = 0xFF65CDE2;
                c2 = 0xFF16B6D4;
                c3 = 0xFF075CB9;
            }
            case DUSK -> {
                c1 = 0xFF9865D8;
                c2 = 0xFF6445AF;
                c3 = 0xFF27255C;
            }
            case GRAPHITE -> {
                c1 = 0xFF656871;
                c2 = 0xFF353941;
                c3 = 0xFF17191E;
            }
            default -> {
                c1 = 0xFF65CDE2;
                c2 = 0xFF16B6D4;
                c3 = 0xFF075CB9;
            }
        }

        roundedRect(
                graphics,
                x,
                y,
                42,
                42,
                9,
                c1
        );

        graphics.fill(
                x,
                y + 14,
                x + 42,
                y + 28,
                c2
        );

        graphics.fill(
                x,
                y + 28,
                x + 42,
                y + 42,
                c3
        );
    }

    @Override
    public boolean mouseClicked(
            double mouseX,
            double mouseY,
            int button
    ) {
        if (button == 0) {
            if (clickedBack(
                    mouseX,
                    mouseY
            )) {
                minecraft.setScreen(
                        new IPhoneSettingsScreen()
                );
                return true;
            }

            if (applyIfClicked(
                    mouseX,
                    mouseY,
                    firstY,
                    PhoneSystemSettings.Wallpaper.AERO
            )) {
                return true;
            }

            if (applyIfClicked(
                    mouseX,
                    mouseY,
                    firstY + 74,
                    PhoneSystemSettings.Wallpaper.DUSK
            )) {
                return true;
            }

            if (applyIfClicked(
                    mouseX,
                    mouseY,
                    firstY + 148,
                    PhoneSystemSettings.Wallpaper.GRAPHITE
            )) {
                return true;
            }
        }

        return super.mouseClicked(
                mouseX,
                mouseY,
                button
        );
    }

    private boolean applyIfClicked(
            double mouseX,
            double mouseY,
            int y,
            PhoneSystemSettings.Wallpaper wallpaper
    ) {
        if (!inside(
                mouseX,
                mouseY,
                contentX,
                y,
                contentWidth,
                58
        )) {
            return false;
        }

        PhoneSystemSettings.setWallpaper(
                wallpaper
        );

        return true;
    }
}

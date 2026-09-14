package com.k1ngtle.vsia.phone.client.screen;

import com.k1ngtle.vsia.phone.client.PhoneLocaleSettings;
import com.k1ngtle.vsia.phone.client.PhoneNowPlaying;
import com.k1ngtle.vsia.phone.client.PhoneSystemSettings;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.time.LocalDateTime;

public final class IPhoneLockScreen extends IPhoneScreen {
    private static final int TEXT = 0xFFFFFFFF;
    private static final int MUTED = 0xFFD1D1D6;
    private static final int CARD = 0xB92C2C2E;
    private static final int PINK = 0xFFFF375F;
    private static final int PURPLE = 0xFFBF5AF2;
    private static final int BLUE = 0xFF0A84FF;

    private int playerX;
    private int playerY;
    private int playerW;
    private int playerH;

    public IPhoneLockScreen() {
        super(Component.literal("Lock Screen"));
    }

    @Override
    protected void init() {
        super.init();

        playerW = PHONE_WIDTH - 34;
        playerH = 128;
        playerX =
                phoneX
                        + (PHONE_WIDTH - playerW)
                        / 2;

        playerY = phoneY + 176;
    }

    @Override
    public void render(
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        renderPhoneShell(
                graphics,
                0xFF142637
        );

        beginPhoneClip(
                graphics,
                DISPLAY_INSET
        );

        renderLockWallpaper(
                graphics
        );

        LocalDateTime now =
                LocalDateTime.now(
                        PhoneLocaleSettings.regionZoneId()
                );

        drawUiCentered(
                graphics,
                PhoneLocaleSettings.formatDate(
                        now.toLocalDate()
                ),
                phoneX + PHONE_WIDTH / 2,
                phoneY + 72,
                MUTED
        );

        drawLargeTime(
                graphics,
                PhoneLocaleSettings.formatTime(
                        now.toLocalTime(),
                        PhoneSystemSettings.use24HourTime()
                )
        );

        if (PhoneNowPlaying.hasActive()) {
            renderNowPlayingCard(
                    graphics
            );
        } else {
            roundedRect(
                    graphics,
                    playerX,
                    playerY + 18,
                    playerW,
                    64,
                    18,
                    0x6F1D1D20
            );

            drawUiCentered(
                    graphics,
                    "No Media Playing",
                    phoneX + PHONE_WIDTH / 2,
                    playerY + 41,
                    MUTED
            );
        }

        drawUiCentered(
                graphics,
                "Tap to Open",
                phoneX + PHONE_WIDTH / 2,
                phoneY + PHONE_HEIGHT - 48,
                MUTED
        );

        endPhoneClip(
                graphics
        );

        maskDisplayCorners(
                graphics
        );

        renderStatusBar(
                graphics
        );

        renderHomeIndicator(
                graphics
        );
    }

    private void renderLockWallpaper(
            GuiGraphics graphics
    ) {
        graphics.fill(
                displayX(),
                displayY(),
                displayX() + displayWidth(),
                displayY() + displayHeight(),
                0xFF1C2F42
        );

        roundedRect(
                graphics,
                displayX() - 26,
                displayY() + 92,
                145,
                148,
                70,
                0x443FA7D7
        );

        roundedRect(
                graphics,
                displayX() + 118,
                displayY() + 20,
                118,
                176,
                58,
                0x394C5BD5
        );

        roundedRect(
                graphics,
                displayX() + 48,
                displayY() + 262,
                196,
                126,
                56,
                0x3152CFBE
        );
    }

    private void drawLargeTime(
            GuiGraphics graphics,
            String time
    ) {
        float scale = 2.15F;

        graphics.pose().pushPose();
        graphics.pose().scale(
                scale,
                scale,
                1.0F
        );

        int center =
                Math.round(
                        (
                                phoneX
                                        + PHONE_WIDTH / 2.0F
                        )
                                / scale
                );

        int y =
                Math.round(
                        (phoneY + 92)
                                / scale
                );

        drawUiCentered(
                graphics,
                time,
                center,
                y,
                TEXT
        );

        graphics.pose().popPose();
    }

    private void renderNowPlayingCard(
            GuiGraphics graphics
    ) {
        roundedRect(
                graphics,
                playerX,
                playerY,
                playerW,
                playerH,
                18,
                CARD
        );

        int accent = accentColor();

        roundedRect(
                graphics,
                playerX + 11,
                playerY + 11,
                48,
                48,
                12,
                accent
        );

        drawMediaGlyph(
                graphics,
                playerX + 35,
                playerY + 35
        );

        drawUiText(
                graphics,
                fitUi(
                        PhoneNowPlaying.title(),
                        playerW - 85
                ),
                playerX + 70,
                playerY + 14,
                TEXT
        );

        drawUiText(
                graphics,
                fitUi(
                        PhoneNowPlaying.subtitle(),
                        playerW - 85
                ),
                playerX + 70,
                playerY + 31,
                MUTED
        );

        drawUiText(
                graphics,
                formatTime(
                        PhoneNowPlaying.elapsedMillis()
                ),
                playerX + 70,
                playerY + 47,
                MUTED
        );

        int progressX = playerX + 12;
        int progressY = playerY + 69;
        int progressW = playerW - 24;

        roundedRect(
                graphics,
                progressX,
                progressY,
                progressW,
                3,
                2,
                0xFF55555A
        );

        int fill = (int) Math.min(
                progressW,
                Math.max(
                        0,
                        progressW
                                * (
                                PhoneNowPlaying.elapsedMillis()
                                        % 480000L
                        )
                                / 480000L
                )
        );

        if (fill > 0) {
            roundedRect(
                    graphics,
                    progressX,
                    progressY,
                    fill,
                    3,
                    2,
                    accent
            );
        }

        int controlsY = playerY + 88;

        roundedRect(
                graphics,
                playerX + 26,
                controlsY,
                31,
                31,
                15,
                0xFF3A3A3F
        );

        roundedRect(
                graphics,
                phoneX + PHONE_WIDTH / 2 - 23,
                controlsY - 2,
                46,
                35,
                17,
                0xFF3A3A3F
        );

        roundedRect(
                graphics,
                playerX + playerW - 57,
                controlsY,
                31,
                31,
                15,
                0xFF3A3A3F
        );

        drawUiCentered(
                graphics,
                "‹‹",
                playerX + 41,
                controlsY + 11,
                TEXT
        );

        drawUiCentered(
                graphics,
                PhoneNowPlaying.playing()
                        ? "Pause"
                        : "Play",
                phoneX + PHONE_WIDTH / 2,
                controlsY + 10,
                TEXT
        );

        drawUiCentered(
                graphics,
                "››",
                playerX + playerW - 41,
                controlsY + 11,
                TEXT
        );
    }

    private int accentColor() {
        return switch (PhoneNowPlaying.kind()) {
            case MUSIC ->
                    PINK;

            case PODCAST ->
                    PURPLE;

            case TV ->
                    BLUE;

            default ->
                    0xFF636366;
        };
    }

    private void drawMediaGlyph(
            GuiGraphics graphics,
            int cx,
            int cy
    ) {
        switch (PhoneNowPlaying.kind()) {
            case MUSIC -> {
                graphics.fill(
                        cx + 6,
                        cy - 11,
                        cx + 9,
                        cy + 7,
                        TEXT
                );

                graphics.fill(
                        cx - 5,
                        cy - 5,
                        cx - 2,
                        cy + 10,
                        TEXT
                );

                graphics.fill(
                        cx - 5,
                        cy - 8,
                        cx + 9,
                        cy - 5,
                        TEXT
                );

                roundedRect(
                        graphics,
                        cx - 10,
                        cy + 7,
                        8,
                        6,
                        3,
                        TEXT
                );

                roundedRect(
                        graphics,
                        cx + 3,
                        cy + 4,
                        9,
                        7,
                        4,
                        TEXT
                );
            }

            case PODCAST -> {
                roundedRect(
                        graphics,
                        cx - 3,
                        cy - 3,
                        6,
                        6,
                        3,
                        TEXT
                );

                roundedRect(
                        graphics,
                        cx - 8,
                        cy - 8,
                        16,
                        16,
                        8,
                        0x66FFFFFF
                );
            }

            case TV ->
                    drawUiCentered(
                            graphics,
                            "tv",
                            cx,
                            cy - 4,
                            TEXT
                    );

            default -> {
            }
        }
    }

    @Override
    public boolean mouseClicked(
            double mouseX,
            double mouseY,
            int button
    ) {
        if (button == 0) {
            if (PhoneNowPlaying.hasActive()
                    && inside(
                    mouseX,
                    mouseY,
                    playerX,
                    playerY,
                    playerW,
                    playerH
            )) {
                int third =
                        playerW / 3;

                if (mouseY
                        >= playerY + 80) {
                    if (mouseX
                            < playerX + third) {
                        PhoneNowPlaying.previous();
                    } else if (mouseX
                            > playerX + third * 2) {
                        PhoneNowPlaying.next();
                    } else {
                        PhoneNowPlaying.toggle();
                    }

                    return true;
                }

                openActiveMediaScreen();
                return true;
            }

            if (mouseY
                    >= phoneY + PHONE_HEIGHT - 90) {
                minecraft.setScreen(
                        new IPhoneHomeScreen()
                );

                return true;
            }
        }

        return super.mouseClicked(
                mouseX,
                mouseY,
                button
        );
    }

    private void openActiveMediaScreen() {
        switch (PhoneNowPlaying.kind()) {
            case MUSIC ->
                    minecraft.setScreen(
                            new IPhoneMusicScreen()
                    );

            case PODCAST ->
                    minecraft.setScreen(
                            new IPhonePodcastsScreen()
                    );

            case TV ->
                    minecraft.setScreen(
                            new IPhoneTVScreen()
                    );

            default ->
                    minecraft.setScreen(
                            new IPhoneHomeScreen()
                    );
        }
    }

    private static String formatTime(
            long millis
    ) {
        long seconds =
                millis / 1000L;

        return String.format(
                "%02d:%02d",
                seconds / 60L,
                seconds % 60L
        );
    }
}

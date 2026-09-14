package com.k1ngtle.vsia.phone.client.screen;

import com.k1ngtle.vsia.phone.client.PhoneCoreAppsState;
import com.k1ngtle.vsia.phone.client.PhoneNowPlaying;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public final class IPhoneMusicScreen extends IPhoneScreen {
    private static final int BG = 0xFF111113;
    private static final int CARD = 0xFF2C2C2E;
    private static final int TEXT = 0xFFFFFFFF;
    private static final int MUTED = 0xFFAEAEB2;
    private static final int PINK = 0xFFFF375F;

    private static final String[] TRACKS = {
            "Overworld Signal",
            "Night Operations",
            "Digital Horizon",
            "Return to Base"
    };

    private static final String[] ARTISTS = {
            "VS:IA Audio",
            "Signality",
            "Network Lab",
            "VS:IA Audio"
    };

    private static final int[] ART_COLORS = {
            0xFFFF375F,
            0xFF5E5CE6,
            0xFF0A84FF,
            0xFFFF9F0A
    };

    private int x;
    private int w;
    private int artX;
    private int artY;
    private int artSize;
    private int listY;
    private int rowHeight;
    private int repeatY;
    private int controlsY;

    public IPhoneMusicScreen() {
        super(Component.literal("Music"));
    }

    @Override
    protected void init() {
        super.init();

        PhoneNowPlaying.mark(
                PhoneNowPlaying.Kind.MUSIC
        );

        x = phoneX + 14;
        w = PHONE_WIDTH - 28;
        artSize = 76;
        artX = phoneX + (PHONE_WIDTH - artSize) / 2;
        artY = phoneY + 74;
        listY = phoneY + 232;
        rowHeight = 27;
        repeatY = phoneY + 343;
        controlsY = phoneY + 373;
    }

    @Override
    public void render(
            GuiGraphics g,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        renderPhoneShell(g, BG);
        renderStatusBar(g);

        drawUiCentered(
                g,
                "Apple Music",
                phoneX + PHONE_WIDTH / 2,
                phoneY + 51,
                TEXT
        );

        beginPhoneClip(g, 68);

        int track = Math.max(
                0,
                Math.min(
                        TRACKS.length - 1,
                        PhoneCoreAppsState.musicTrack()
                )
        );

        drawAlbumArt(g, track);

        drawUiCentered(
                g,
                TRACKS[track],
                phoneX + PHONE_WIDTH / 2,
                phoneY + 158,
                TEXT
        );

        drawUiCentered(
                g,
                ARTISTS[track],
                phoneX + PHONE_WIDTH / 2,
                phoneY + 173,
                MUTED
        );

        drawUiText(
                g,
                formatTime(
                        PhoneCoreAppsState.musicElapsedMillis()
                ),
                x,
                phoneY + 191,
                MUTED
        );

        drawUiText(
                g,
                "08:00",
                x + w - 29,
                phoneY + 191,
                MUTED
        );

        int progressWidth = w - 48;
        int progressX = x + 24;
        int progressY = phoneY + 207;

        roundedRect(
                g,
                progressX,
                progressY,
                progressWidth,
                4,
                2,
                0xFF3A3A3C
        );

        int fill = Math.min(
                progressWidth,
                Math.max(
                        0,
                        (int) (
                                progressWidth
                                        * (
                                        PhoneCoreAppsState
                                                .musicElapsedMillis()
                                                % 480000L
                                )
                                        / 480000.0D
                        )
                )
        );

        if (fill > 0) {
            roundedRect(
                    g,
                    progressX,
                    progressY,
                    fill,
                    4,
                    2,
                    PINK
            );
        }

        drawUiText(
                g,
                "Up Next",
                x + 2,
                phoneY + 219,
                MUTED
        );

        for (int i = 0;
             i < TRACKS.length;
             i++) {
            int y = listY + i * rowHeight;
            boolean selected = track == i;

            roundedRect(
                    g,
                    x,
                    y,
                    w,
                    rowHeight - 2,
                    8,
                    selected
                            ? 0xFF4A2330
                            : CARD
            );

            drawUiText(
                    g,
                    fitUi(
                            TRACKS[i],
                            w - 58
                    ),
                    x + 10,
                    y + 5,
                    TEXT
            );

            drawUiText(
                    g,
                    fitUi(
                            ARTISTS[i],
                            w - 58
                    ),
                    x + 10,
                    y + 16,
                    MUTED
            );

            drawUiText(
                    g,
                    selected
                            ? "Now"
                            : "›",
                    x + w
                            - (
                            selected
                                    ? 26
                                    : 12
                    ),
                    y + 10,
                    selected
                            ? PINK
                            : MUTED
            );
        }

        roundedRect(
                g,
                x,
                repeatY,
                w,
                24,
                10,
                0xFF1D1D20
        );

        drawUiText(
                g,
                "Repeat",
                x + 10,
                repeatY + 8,
                MUTED
        );

        drawUiText(
                g,
                PhoneCoreAppsState.musicRepeat()
                        ? "On"
                        : "Off",
                x + w - 22,
                repeatY + 8,
                PINK
        );

        roundedRect(
                g,
                x,
                controlsY,
                w,
                34,
                12,
                CARD
        );

        drawUiCentered(
                g,
                "‹‹",
                x + 36,
                controlsY + 12,
                TEXT
        );

        drawUiCentered(
                g,
                PhoneCoreAppsState.musicPlaying()
                        ? "Pause"
                        : "Play",
                phoneX + PHONE_WIDTH / 2,
                controlsY + 12,
                PINK
        );

        drawUiCentered(
                g,
                "››",
                x + w - 36,
                controlsY + 12,
                TEXT
        );

        endPhoneClip(g);
        renderHomeIndicator(g);
    }

    private void drawAlbumArt(
            GuiGraphics g,
            int track
    ) {
        int color = ART_COLORS[
                Math.max(
                        0,
                        Math.min(
                                ART_COLORS.length - 1,
                                track
                        )
                )
        ];

        roundedRect(
                g,
                artX,
                artY,
                artSize,
                artSize,
                18,
                color
        );

        roundedRect(
                g,
                artX + 2,
                artY + 2,
                artSize - 4,
                15,
                11,
                0x22FFFFFF
        );

        roundedRect(
                g,
                artX + 7,
                artY + 7,
                artSize - 14,
                artSize - 14,
                14,
                0x16111113
        );

        drawMusicNote(
                g,
                artX + artSize / 2,
                artY + artSize / 2 + 1,
                0xFFFFFFFF
        );

        roundedRect(
                g,
                artX + artSize - 20,
                artY + artSize - 18,
                14,
                12,
                6,
                0x99000000
        );

        drawUiCentered(
                g,
                Integer.toString(track + 1),
                artX + artSize - 13,
                artY + artSize - 15,
                0xFFFFFFFF
        );
    }

    private void drawMusicNote(
            GuiGraphics g,
            int cx,
            int cy,
            int color
    ) {
        g.fill(
                cx + 13,
                cy - 28,
                cx + 19,
                cy + 19,
                color
        );

        g.fill(
                cx - 12,
                cy - 14,
                cx - 7,
                cy + 25,
                color
        );

        g.fill(
                cx - 12,
                cy - 18,
                cx + 19,
                cy - 13,
                color
        );

        g.fill(
                cx - 8,
                cy - 22,
                cx + 19,
                cy - 18,
                color
        );

        g.fill(
                cx - 4,
                cy - 26,
                cx + 19,
                cy - 22,
                color
        );

        roundedRect(
                g,
                cx - 23,
                cy + 18,
                18,
                14,
                7,
                color
        );

        roundedRect(
                g,
                cx + 7,
                cy + 11,
                20,
                16,
                8,
                color
        );
    }

    @Override
    public boolean mouseClicked(
            double mouseX,
            double mouseY,
            int button
    ) {
        if (button == 0) {
            for (int i = 0;
                 i < TRACKS.length;
                 i++) {
                int y = listY + i * rowHeight;

                if (inside(
                        mouseX,
                        mouseY,
                        x,
                        y,
                        w,
                        rowHeight - 2
                )) {
                    while (PhoneCoreAppsState.musicTrack()
                            != i) {
                        PhoneCoreAppsState.nextMusicTrack();
                    }

                    if (!PhoneCoreAppsState.musicPlaying()) {
                        PhoneCoreAppsState.toggleMusic();
                    }

                    PhoneNowPlaying.mark(
                            PhoneNowPlaying.Kind.MUSIC
                    );

                    return true;
                }
            }

            if (inside(
                    mouseX,
                    mouseY,
                    x,
                    repeatY,
                    w,
                    24
            )) {
                PhoneCoreAppsState.toggleMusicRepeat();
                return true;
            }

            if (inside(
                    mouseX,
                    mouseY,
                    x,
                    controlsY,
                    w,
                    34
            )) {
                if (mouseX
                        < x + w / 3.0D) {
                    PhoneCoreAppsState.previousMusicTrack();
                } else if (mouseX
                        > x + w * 2.0D / 3.0D) {
                    PhoneCoreAppsState.nextMusicTrack();
                } else {
                    PhoneCoreAppsState.toggleMusic();
                }

                PhoneNowPlaying.mark(
                        PhoneNowPlaying.Kind.MUSIC
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

    private static String formatTime(
            long millis
    ) {
        long seconds = millis / 1000L;

        return String.format(
                "%02d:%02d",
                seconds / 60L,
                seconds % 60L
        );
    }
}

package com.k1ngtle.vsia.phone.client.screen;

import com.k1ngtle.vsia.phone.client.PhoneCoreAppsState;
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

    private int x;
    private int w;
    private int artY;
    private int listY;
    private int controlsY;

    public IPhoneMusicScreen() {
        super(Component.literal("Music"));
    }

    @Override
    protected void init() {
        super.init();
        x = phoneX + 14;
        w = PHONE_WIDTH - 28;
        artY = phoneY + 82;
        listY = artY + 190;
        controlsY = phoneY + PHONE_HEIGHT - 74;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderPhoneShell(g, BG);
        renderStatusBar(g);
        drawUiCentered(g, "Apple Music", phoneX + PHONE_WIDTH / 2, phoneY + 51, TEXT);
        beginPhoneClip(g, 68);

        int track = PhoneCoreAppsState.musicTrack();

        roundedRect(g, phoneX + 48, artY, PHONE_WIDTH - 96, 132, 18, PINK);
        drawUiCentered(g, "♪", phoneX + PHONE_WIDTH / 2, artY + 47, TEXT);
        drawUiCentered(g, TRACKS[track], phoneX + PHONE_WIDTH / 2, artY + 146, TEXT);
        drawUiCentered(g, ARTISTS[track], phoneX + PHONE_WIDTH / 2, artY + 163, MUTED);
        drawUiCentered(g, formatTime(PhoneCoreAppsState.musicElapsedMillis()), phoneX + PHONE_WIDTH / 2, artY + 179, MUTED);

        for (int i = 0; i < TRACKS.length; i++) {
            int y = listY + i * 28;
            roundedRect(g, x, y, w, 24, 8, track == i ? 0xFF3A1F2A : CARD);
            drawUiText(g, fitUi(TRACKS[i], w - 74), x + 10, y + 8, TEXT);
            drawUiText(g, ARTISTS[i], x + w - 58, y + 8, MUTED);
        }

        roundedRect(g, x, controlsY, w, 42, 12, CARD);
        drawUiCentered(g, "‹‹", x + 34, controlsY + 15, TEXT);
        drawUiCentered(g, PhoneCoreAppsState.musicPlaying() ? "Pause" : "Play", phoneX + PHONE_WIDTH / 2, controlsY + 15, PINK);
        drawUiCentered(g, "››", x + w - 34, controlsY + 15, TEXT);

        roundedRect(g, x, controlsY - 30, w, 24, 10, 0xFF1D1D20);
        drawUiText(g, "Repeat", x + 10, controlsY - 22, MUTED);
        drawUiText(g, PhoneCoreAppsState.musicRepeat() ? "On" : "Off", x + w - 22, controlsY - 22, PINK);

        endPhoneClip(g);
        renderHomeIndicator(g);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            for (int i = 0; i < TRACKS.length; i++) {
                int y = listY + i * 28;
                if (inside(mouseX, mouseY, x, y, w, 24)) {
                    while (PhoneCoreAppsState.musicTrack() != i) {
                        PhoneCoreAppsState.nextMusicTrack();
                    }
                    if (!PhoneCoreAppsState.musicPlaying()) {
                        PhoneCoreAppsState.toggleMusic();
                    }
                    return true;
                }
            }

            if (inside(mouseX, mouseY, x, controlsY - 30, w, 24)) {
                PhoneCoreAppsState.toggleMusicRepeat();
                return true;
            }

            if (inside(mouseX, mouseY, x, controlsY, w, 42)) {
                if (mouseX < x + w / 3.0D) {
                    PhoneCoreAppsState.previousMusicTrack();
                } else if (mouseX > x + w * 2.0D / 3.0D) {
                    PhoneCoreAppsState.nextMusicTrack();
                } else {
                    PhoneCoreAppsState.toggleMusic();
                }
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private static String formatTime(long millis) {
        long seconds = millis / 1000L;
        return String.format("%02d:%02d", seconds / 60L, seconds % 60L);
    }
}

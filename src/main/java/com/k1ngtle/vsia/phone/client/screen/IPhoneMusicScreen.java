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
    private static final int SOFT_PINK = 0xFF4A2330;

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
        listY = artY + 188;
        controlsY = phoneY + PHONE_HEIGHT - 74;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderPhoneShell(g, BG);
        renderStatusBar(g);
        drawUiCentered(g, "Apple Music", phoneX + PHONE_WIDTH / 2, phoneY + 51, TEXT);
        beginPhoneClip(g, 68);

        int track = PhoneCoreAppsState.musicTrack();

        roundedRect(g, phoneX + 48, artY, PHONE_WIDTH - 96, 130, 18, PINK);
        roundedRect(g, phoneX + 51, artY + 3, PHONE_WIDTH - 102, 18, 12, 0x18FFFFFF);
        drawAlbumNote(g, phoneX + PHONE_WIDTH / 2, artY + 51);

        drawUiCentered(g, TRACKS[track], phoneX + PHONE_WIDTH / 2, artY + 145, TEXT);
        drawUiCentered(g, ARTISTS[track], phoneX + PHONE_WIDTH / 2, artY + 160, MUTED);
        drawUiCentered(g, formatTime(PhoneCoreAppsState.musicElapsedMillis()), phoneX + PHONE_WIDTH / 2, artY + 175, MUTED);

        for (int i = 0; i < TRACKS.length; i++) {
            int y = listY + i * 28;
            boolean selected = track == i;
            roundedRect(g, x, y, w, 24, 8, selected ? SOFT_PINK : CARD);
            drawUiText(g, fitUi(TRACKS[i], w - 58), x + 10, y + 8, TEXT);
            drawUiText(g, selected ? "Now" : "›", x + w - (selected ? 24 : 10), y + 8, selected ? PINK : MUTED);
        }

        roundedRect(g, x, controlsY - 30, w, 24, 10, 0xFF1D1D20);
        drawUiText(g, "Repeat", x + 10, controlsY - 22, MUTED);
        drawUiText(g, PhoneCoreAppsState.musicRepeat() ? "On" : "Off", x + w - 22, controlsY - 22, PINK);

        roundedRect(g, x, controlsY, w, 42, 12, CARD);
        drawUiCentered(g, "‹‹", x + 34, controlsY + 15, TEXT);
        drawUiCentered(g, PhoneCoreAppsState.musicPlaying() ? "Pause" : "Play", phoneX + PHONE_WIDTH / 2, controlsY + 15, PINK);
        drawUiCentered(g, "››", x + w - 34, controlsY + 15, TEXT);

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

    private void drawAlbumNote(GuiGraphics g, int cx, int cy) {
        g.fill(cx + 12, cy - 26, cx + 18, cy + 18, TEXT);
        g.fill(cx - 4, cy - 18, cx + 18, cy - 13, TEXT);
        g.fill(cx - 12, cy - 15, cx + 18, cy - 10, TEXT);
        roundedRect(g, cx - 20, cy + 10, 18, 18, 9, TEXT);
        roundedRect(g, cx + 8, cy + 4, 18, 18, 9, TEXT);
        g.fill(cx - 2, cy - 6, cx + 3, cy + 19, TEXT);
    }

    private static String formatTime(long millis) {
        long seconds = millis / 1000L;
        return String.format("%02d:%02d", seconds / 60L, seconds % 60L);
    }
}

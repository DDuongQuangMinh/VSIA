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
        controlsY = phoneY + 300;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderPhoneShell(g, BG);
        renderStatusBar(g);
        drawUiCentered(g, "Apple Music", phoneX + PHONE_WIDTH / 2, phoneY + 51, TEXT);
        beginPhoneClip(g, 68);

        int track = PhoneCoreAppsState.musicTrack();

        roundedRect(g, phoneX + 50, artY, PHONE_WIDTH - 100, 140, 18, 0xFFFF3B61);
        roundedRect(g, phoneX + 74, artY + 24, PHONE_WIDTH - 148, 92, 28, 0xFF1C1C1E);
        drawUiCentered(g, "♫", phoneX + PHONE_WIDTH / 2, artY + 60, TEXT);

        drawUiCentered(g, TRACKS[track], phoneX + PHONE_WIDTH / 2, artY + 157, TEXT);
        drawUiCentered(g, ARTISTS[track], phoneX + PHONE_WIDTH / 2, artY + 175, MUTED);
        drawUiCentered(g, formatTime(PhoneCoreAppsState.musicElapsedMillis()), phoneX + PHONE_WIDTH / 2, artY + 197, MUTED);

        roundedRect(g, x, controlsY, w, 54, 14, CARD);
        drawUiCentered(g, "‹‹", x + 42, controlsY + 20, TEXT);
        drawUiCentered(g, PhoneCoreAppsState.musicPlaying() ? "Pause" : "Play", phoneX + PHONE_WIDTH / 2, controlsY + 20, PINK);
        drawUiCentered(g, "››", x + w - 42, controlsY + 20, TEXT);

        drawUiWrappedCentered(
                g,
                "VS:IA Music currently provides local playback state. Add licensed audio assets later for actual track audio.",
                phoneX + PHONE_WIDTH / 2,
                controlsY + 70,
                w - 18,
                11,
                4,
                MUTED
        );

        endPhoneClip(g);
        renderHomeIndicator(g);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && inside(mouseX, mouseY, x, controlsY, w, 54)) {
            if (mouseX < x + w / 3.0D) {
                PhoneCoreAppsState.previousMusicTrack();
            } else if (mouseX > x + w * 2.0D / 3.0D) {
                PhoneCoreAppsState.nextMusicTrack();
            } else {
                PhoneCoreAppsState.toggleMusic();
            }

            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private static String formatTime(long millis) {
        long seconds = millis / 1000L;
        return String.format("%02d:%02d", seconds / 60L, seconds % 60L);
    }
}

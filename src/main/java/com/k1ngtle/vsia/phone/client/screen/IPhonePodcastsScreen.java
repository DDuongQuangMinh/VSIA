package com.k1ngtle.vsia.phone.client.screen;

import com.k1ngtle.vsia.phone.client.PhoneCoreAppsState;
import com.k1ngtle.vsia.phone.client.PhoneNowPlaying;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public final class IPhonePodcastsScreen extends IPhoneScreen {
    private static final int BG = 0xFF111113;
    private static final int CARD = 0xFF2C2C2E;
    private static final int TEXT = 0xFFFFFFFF;
    private static final int MUTED = 0xFFAEAEB2;
    private static final int PURPLE = 0xFFBF5AF2;

    private static final String[] TITLES = {
            "Building VS:IA",
            "Signals & Networks",
            "Ships, Physics & Systems"
    };

    private static final String[] SHOWS = {
            "Developer Log",
            "VS:IA Engineering",
            "Systems Lab"
    };

    private int x;
    private int w;
    private int listY;
    private int playerY;
    private int transportY;

    public IPhonePodcastsScreen() {
        super(Component.literal("Podcasts"));
    }

    @Override
    protected void init() {
        super.init();
        PhoneNowPlaying.mark(
                PhoneNowPlaying.Kind.PODCAST
        );

        x = phoneX + 14;
        w = PHONE_WIDTH - 28;
        listY = phoneY + 82;
        playerY = phoneY + 274;
        transportY = playerY + 64;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderPhoneShell(g, BG);
        renderStatusBar(g);
        drawUiCentered(g, "Podcasts", phoneX + PHONE_WIDTH / 2, phoneY + 51, TEXT);
        beginPhoneClip(g, 68);

        for (int i = 0; i < TITLES.length; i++) {
            int y = listY + i * 56;
            roundedRect(g, x, y, w, 48, 12, CARD);
            roundedRect(g, x + 8, y + 8, 32, 32, 8, PURPLE);
            drawUiCentered(g, "◉", x + 24, y + 18, TEXT);
            drawUiText(g, fitUi(TITLES[i], w - 58), x + 49, y + 10, TEXT);
            drawUiText(g, SHOWS[i], x + 49, y + 27, MUTED);
            if (PhoneCoreAppsState.podcastEpisode() == i) {
                drawUiText(g, "Now", x + w - 30, y + 27, PURPLE);
            }
        }

        roundedRect(g, x, playerY, w, 56, 14, CARD);
        int current = PhoneCoreAppsState.podcastEpisode();
        drawUiText(g, fitUi(TITLES[current], w - 80), x + 12, playerY + 10, TEXT);
        drawUiText(g, SHOWS[current], x + 12, playerY + 28, MUTED);
        drawUiText(g, formatTime(PhoneCoreAppsState.podcastElapsedMillis()), x + 12, playerY + 42, MUTED);
        drawUiText(g, PhoneCoreAppsState.podcastSpeedLabel(), x + w - 26, playerY + 42, PURPLE);

        roundedRect(g, x, transportY, w, 44, 14, CARD);
        drawUiCentered(g, "-15", x + 34, transportY + 15, TEXT);
        drawUiCentered(g, PhoneCoreAppsState.podcastPlaying() ? "Pause" : "Play", phoneX + PHONE_WIDTH / 2, transportY + 15, PURPLE);
        drawUiCentered(g, "+30", x + w - 34, transportY + 15, TEXT);

        endPhoneClip(g);
        renderHomeIndicator(g);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            for (int i = 0; i < TITLES.length; i++) {
                int y = listY + i * 56;
                if (inside(mouseX, mouseY, x, y, w, 48)) {
                    PhoneCoreAppsState.setPodcastEpisode(i);
                    if (!PhoneCoreAppsState.podcastPlaying()) {
                        PhoneCoreAppsState.togglePodcast();
                    }
                    return true;
                }
            }

            if (inside(mouseX, mouseY, x, playerY, w, 56)) {
                PhoneCoreAppsState.cyclePodcastSpeed();
                return true;
            }

            if (inside(mouseX, mouseY, x, transportY, w, 44)) {
                if (mouseX < x + w / 3.0D) {
                    PhoneCoreAppsState.skipPodcastMillis(-15000L);
                } else if (mouseX > x + w * 2.0D / 3.0D) {
                    PhoneCoreAppsState.skipPodcastMillis(30000L);
                } else {
                    PhoneCoreAppsState.togglePodcast();
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

package com.k1ngtle.vsia.phone.client.screen;

import com.k1ngtle.vsia.phone.client.PhoneCoreAppsState;
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

    public IPhonePodcastsScreen() {
        super(Component.literal("Podcasts"));
    }

    @Override
    protected void init() {
        super.init();
        x = phoneX + 14;
        w = PHONE_WIDTH - 28;
        listY = phoneY + 82;
        playerY = phoneY + 300;
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

        roundedRect(g, x, playerY, w, 54, 14, CARD);
        int current = PhoneCoreAppsState.podcastEpisode();
        drawUiText(g, fitUi(TITLES[current], w - 70), x + 12, playerY + 11, TEXT);
        drawUiText(g, formatTime(PhoneCoreAppsState.podcastElapsedMillis()), x + 12, playerY + 31, MUTED);
        drawUiText(g, PhoneCoreAppsState.podcastPlaying() ? "Pause" : "Play", x + w - 45, playerY + 21, PURPLE);

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

            if (inside(mouseX, mouseY, x, playerY, w, 54)) {
                PhoneCoreAppsState.togglePodcast();
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

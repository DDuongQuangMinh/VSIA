package com.k1ngtle.vsia.phone.client.screen;

import com.k1ngtle.vsia.phone.client.PhoneCoreAppsState;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public final class IPhoneFitnessScreen extends IPhoneScreen {
    private static final int BG = 0xFF111113;
    private static final int CARD = 0xFF2C2C2E;
    private static final int TEXT = 0xFFFFFFFF;
    private static final int MUTED = 0xFFAEAEB2;
    private static final int GREEN = 0xFF30D158;
    private static final int RED = 0xFFFF453A;
    private static final int YELLOW = 0xFFFFD60A;

    private int x;
    private int w;
    private int y;
    private int actionY;

    public IPhoneFitnessScreen() {
        super(Component.literal("Fitness"));
    }

    @Override
    protected void init() {
        super.init();
        PhoneCoreAppsState.ensureLoaded();
        x = phoneX + 14;
        w = PHONE_WIDTH - 28;
        y = phoneY + 82;
        actionY = phoneY + PHONE_HEIGHT - 74;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderPhoneShell(g, BG);
        renderStatusBar(g);
        drawUiCentered(g, "Fitness", phoneX + PHONE_WIDTH / 2, phoneY + 51, TEXT);
        beginPhoneClip(g, 68);

        roundedRect(g, x, y, w, 80, 16, CARD);
        drawUiCentered(g, PhoneCoreAppsState.fitnessWorkoutActive() ? "Workout Active" : "Ready", phoneX + PHONE_WIDTH / 2, y + 14, PhoneCoreAppsState.fitnessWorkoutActive() ? GREEN : MUTED);
        drawUiCentered(g, formatDuration(PhoneCoreAppsState.fitnessSeconds()), phoneX + PHONE_WIDTH / 2, y + 42, TEXT);

        metric(g, y + 98, "Distance", String.format("%.1f m", PhoneCoreAppsState.fitnessDistance()), GREEN);
        metric(g, y + 152, "Steps", Integer.toString(PhoneCoreAppsState.fitnessSteps()), YELLOW);
        metric(g, y + 206, "Move", String.format("%.0f cal", PhoneCoreAppsState.fitnessDistance() * 0.055D), RED);

        roundedRect(g, x, actionY, w, 36, 12, PhoneCoreAppsState.fitnessWorkoutActive() ? 0xFF492023 : 0xFF17371F);
        drawUiCentered(g, PhoneCoreAppsState.fitnessWorkoutActive() ? "End Workout" : "Start Workout", phoneX + PHONE_WIDTH / 2, actionY + 13, PhoneCoreAppsState.fitnessWorkoutActive() ? RED : GREEN);

        endPhoneClip(g);
        renderHomeIndicator(g);
    }

    private void metric(GuiGraphics g, int top, String label, String value, int color) {
        roundedRect(g, x, top, w, 44, 12, CARD);
        roundedRect(g, x + 10, top + 10, 24, 24, 12, color);
        drawUiText(g, label, x + 44, top + 10, TEXT);
        drawUiText(g, value, x + 44, top + 26, MUTED);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && inside(mouseX, mouseY, x, actionY, w, 36)) {
            if (PhoneCoreAppsState.fitnessWorkoutActive()) {
                PhoneCoreAppsState.stopFitnessWorkout();
            } else {
                PhoneCoreAppsState.startFitnessWorkout();
            }

            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private static String formatDuration(long seconds) {
        return String.format("%02d:%02d:%02d", seconds / 3600L, (seconds / 60L) % 60L, seconds % 60L);
    }
}
